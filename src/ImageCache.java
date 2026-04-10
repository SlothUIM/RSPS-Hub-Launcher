import javafx.scene.image.Image;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Local disk cache for server images.
 * First load downloads to ~/.rsps_hub/cache/images/ — every load after is instant.
 */
public class ImageCache {

    private static final Path CACHE_DIR = Paths.get(
        System.getProperty("user.home"), ".rsps_hub", "cache", "images"
    );

    private static final ConcurrentHashMap<String, Image> memCache = new ConcurrentHashMap<>();

    static {
        try { Files.createDirectories(CACHE_DIR); } catch (Exception ignored) {}
    }

    /**
     * Load an image from cache (memory → disk → network).
     * Calls onLoaded on the JavaFX thread when ready. Never blocks.
     */
    public static void load(String url, Consumer<Image> onLoaded) {
        if (url == null || url.isEmpty()) return;

        // 1. Memory cache hit — instant
        Image cached = memCache.get(url);
        if (cached != null) {
            javafx.application.Platform.runLater(() -> onLoaded.accept(cached));
            return;
        }

        // 2. Disk cache hit — fast
        Path diskPath = getDiskPath(url);
        if (Files.exists(diskPath)) {
            Image img = new Image(diskPath.toUri().toString(), true);
            memCache.put(url, img);
            javafx.application.Platform.runLater(() -> onLoaded.accept(img));
            return;
        }

        // 3. Download in background
        CompletableFuture.runAsync(() -> {
            // Double-check: another thread may have finished while we were queued
            Image raceWin = memCache.get(url);
            if (raceWin != null) {
                javafx.application.Platform.runLater(() -> onLoaded.accept(raceWin));
                return;
            }
            if (Files.exists(diskPath)) {
                Image img = new Image(diskPath.toUri().toString(), true);
                memCache.put(url, img);
                javafx.application.Platform.runLater(() -> onLoaded.accept(img));
                return;
            }

            // Unique tmp name per attempt — avoids race condition when multiple
            // threads download the same URL simultaneously
            Path tmp = diskPath.resolveSibling(
                diskPath.getFileName().toString() + "." + System.nanoTime() + ".tmp");
            try {
                HttpURLConnection conn = (HttpURLConnection)
                    new URL(stripQuery(url)).openConnection(java.net.Proxy.NO_PROXY);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("User-Agent", "RSPSHub-Launcher/1.0");
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    Files.createDirectories(diskPath.getParent());
                    try (InputStream in = conn.getInputStream();
                         OutputStream out = Files.newOutputStream(tmp)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    Files.move(tmp, diskPath, StandardCopyOption.REPLACE_EXISTING);

                    Image img = new Image(diskPath.toUri().toString(), true);
                    memCache.put(url, img);
                    javafx.application.Platform.runLater(() -> onLoaded.accept(img));
                } else {
                    System.err.println("ImageCache HTTP " + conn.getResponseCode() + " [" + url + "]");
                }
            } catch (Exception e) {
                System.err.println("ImageCache download failed [" + url + "]: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        });
    }

    /** Synchronously get cached Image if available in memory or disk, else null. */
    public static Image getIfCached(String url) {
        if (url == null || url.isEmpty()) return null;
        Image mem = memCache.get(url);
        if (mem != null) return mem;
        Path p = getDiskPath(url);
        if (Files.exists(p)) {
            Image img = new Image(p.toUri().toString());
            memCache.put(url, img);
            return img;
        }
        return null;
    }

    /** Delete all cached images (e.g. for a refresh) */
    public static void clearDisk() {
        memCache.clear();
        try {
            Files.list(CACHE_DIR).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
        } catch (Exception ignored) {}
    }

    private static Path getDiskPath(String url) {
        // Use a hash of the URL (without query params) as the filename
        String clean = stripQuery(url);
        String ext = clean.contains(".png") ? ".png" : clean.contains(".gif") ? ".gif" : ".jpg";
        String hash = Integer.toHexString(clean.hashCode() & 0xfffffff);
        return CACHE_DIR.resolve(hash + ext);
    }

    private static String stripQuery(String url) {
        int q = url.indexOf('?');
        return q >= 0 ? url.substring(0, q) : url;
    }
}
