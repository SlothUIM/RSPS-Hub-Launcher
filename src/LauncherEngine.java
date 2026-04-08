import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.function.DoubleConsumer;

import com.google.gson.Gson;

public class LauncherEngine {

    private static final String API_URL = "http://slothscape.duckdns.org/mock_store.php?api=true";

    // Session
    public static String currentUsername = "";
    public static String avatarImagePath = null;
    public static String statusMessage   = "";
    public static String activeServer    = null;

    // Settings
    public static String downloadPath = System.getProperty("user.home") + "/.rsps_hub/";
    public static boolean minimizeOnLaunch = false;
    public static boolean autoUpdateClients = false;
    public static boolean lightMode = false;
    public static boolean autoLaunch = false;
    public static Set<String> favouriteServers = new LinkedHashSet<>();
    public static Map<String, String> serverNotes = new HashMap<>();

    private static final Path SETTINGS_PATH =
        Paths.get(System.getProperty("user.home"), ".rsps_hub", "settings.json");

    private static class SettingsData {
        String downloadPath;
        String statusMessage;
        boolean minimizeOnLaunch;
        boolean autoUpdateClients;
        boolean lightMode;
        boolean autoLaunch;
        List<String> favouriteServers;
        Map<String, String> serverNotes;
    }

    public static void saveSettings() {
        try {
            SettingsData d = new SettingsData();
            d.downloadPath      = downloadPath;
            d.statusMessage     = statusMessage;
            d.minimizeOnLaunch  = minimizeOnLaunch;
            d.autoUpdateClients = autoUpdateClients;
            d.lightMode         = lightMode;
            d.autoLaunch        = autoLaunch;
            d.favouriteServers  = new ArrayList<>(favouriteServers);
            d.serverNotes       = serverNotes;
            Files.createDirectories(SETTINGS_PATH.getParent());
            Files.writeString(SETTINGS_PATH, new Gson().toJson(d));
        } catch (Exception e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    public static void loadSettings() {
        try {
            if (!Files.exists(SETTINGS_PATH)) return;
            SettingsData d = new Gson().fromJson(Files.readString(SETTINGS_PATH), SettingsData.class);
            if (d.downloadPath       != null) downloadPath      = d.downloadPath;
            if (d.statusMessage      != null) statusMessage     = d.statusMessage;
            if (d.favouriteServers   != null) favouriteServers  = new LinkedHashSet<>(d.favouriteServers);
            if (d.serverNotes        != null) serverNotes       = d.serverNotes;
            minimizeOnLaunch  = d.minimizeOnLaunch;
            autoUpdateClients = d.autoUpdateClients;
            lightMode         = d.lightMode;
            autoLaunch        = d.autoLaunch;
        } catch (Exception e) {
            System.err.println("Failed to load settings: " + e.getMessage());
        }
    }

    public static List<String> getStylesheets(Class<?> cls) {
        List<String> sheets = new ArrayList<>();
        sheets.add(cls.getResource("style.css").toExternalForm());
        if (lightMode) sheets.add(cls.getResource("style-light.css").toExternalForm());
        return sheets;
    }

    public static void setAutoLaunch(boolean enable) {
        try {
            if (enable) {
                String jarPath = LauncherEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                // Normalise Windows path
                if (jarPath.startsWith("/")) jarPath = jarPath.substring(1);
                ProcessBuilder pb = new ProcessBuilder("reg", "add",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "RSPSHub", "/t", "REG_SZ",
                    "/d", "javaw -jar \"" + jarPath + "\"", "/f");
                pb.start().waitFor();
            } else {
                new ProcessBuilder("reg", "delete",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v", "RSPSHub", "/f").start().waitFor();
            }
        } catch (Exception e) {
            System.err.println("Auto-launch toggle failed: " + e.getMessage());
        }
    }

    /**
     * Initializes the Hub directory on the user's PC.
     */
    public static void init() {
        loadSettings();
        try {
            Files.createDirectories(Paths.get(downloadPath));
            System.out.println("Hub initialized at: " + downloadPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches the server list from the VPS. 
     * The UI will call this to populate the storefront.
     */
    public static List<ServerProfile> fetchServers() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "RSPS-Hub-Launcher/1.0");

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            ServerProfile[] servers = new Gson().fromJson(reader, ServerProfile[].class);
            reader.close();

            return Arrays.asList(servers);
        } catch (Exception e) {
            System.err.println("Error fetching server list: " + e.getMessage());
            return new ArrayList<>(); // Return empty list instead of null to prevent UI crashes
        }
    }

    /**
     * Downloads a specific server's client (no-arg overload).
     */
    public static boolean downloadClient(ServerProfile server) {
        return downloadClient(server, null, null);
    }

    /**
     * Downloads a specific server's client with optional progress reporting and cancel support.
     * The UI will call this when 'Play' is clicked if the file is missing.
     */
    public static boolean downloadClient(ServerProfile server, DoubleConsumer onProgress, boolean[] cancelledFlag) {
        Path jarPath = null;
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            Files.createDirectories(serverFolder);

            jarPath = serverFolder.resolve("SlothLite.jar");

            System.out.println("Downloading " + server.name + "...");

            URL jarUrl = new URL(server.jar_url);
            HttpURLConnection conn = (HttpURLConnection) jarUrl.openConnection();
            conn.connect();
            long contentLength = conn.getContentLengthLong();

            try (InputStream in = conn.getInputStream();
                 java.io.OutputStream out = Files.newOutputStream(jarPath)) {
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (cancelledFlag != null && cancelledFlag[0]) {
                        out.close();
                        Files.deleteIfExists(jarPath);
                        return false;
                    }
                    out.write(buffer, 0, read);
                    downloaded += read;
                    if (onProgress != null && contentLength > 0) {
                        final double progress = (double) downloaded / contentLength;
                        onProgress.accept(progress);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (jarPath != null) {
                try { Files.deleteIfExists(jarPath); } catch (Exception ignored) {}
            }
            return false;
        }
    }

    /**
     * Launches the .jar for a server. Returns the Process, or null on failure.
     */
    public static Process launchGame(ServerProfile server) {
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            // Note: We use the serverFolder as the working directory so the game
            // saves its own cache/settings in the right spot!
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", "SlothLite.jar");
            pb.directory(serverFolder.toFile());
            return pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper to check if a server is already downloaded.
     */
    public static boolean isDownloaded(ServerProfile server) {
        Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), "SlothLite.jar");
        return Files.exists(jarPath);
    }

    /**
     * Deletes all files for the given server from the download folder.
     */
    public static boolean uninstallServer(ServerProfile server) {
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            if (!Files.exists(serverFolder)) return true;
            Files.walk(serverFolder)
                .sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to uninstall " + server.name + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the remote JAR differs from the local one by comparing file sizes.
     * Returns false if the check fails so we don't block the user unnecessarily.
     */
    public static boolean isUpdateAvailable(ServerProfile server) {
        try {
            Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), "SlothLite.jar");
            if (!Files.exists(jarPath)) return false;

            long localSize = Files.size(jarPath);

            HttpURLConnection conn = (HttpURLConnection) new URL(server.jar_url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            long remoteSize = conn.getContentLengthLong();
            conn.disconnect();

            return remoteSize > 0 && remoteSize != localSize;
        } catch (Exception e) {
            System.err.println("Update check failed for " + server.name + ": " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        // Now the main method just starts the Hub environment
        init();
        System.out.println("Engine ready. Waiting for UI controller...");
        
        // Next step: Start the UI!
    }
}