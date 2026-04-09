import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    // Session
    public static String currentUsername = "";
    public static String sessionToken    = "";
    public static String avatarImagePath = null;
    public static String statusMessage   = "";
    public static String activeServer    = null;

    // Settings
    public static String downloadPath = System.getProperty("user.home") + "/.rsps_hub/";
    public static boolean minimizeOnLaunch = false;
    public static boolean autoUpdateClients = false;
    public static boolean lightMode = false;
    public static boolean autoLaunch = false;
    public static String accentColor = "#ff981f";
    public static Set<String> favouriteServers = new LinkedHashSet<>();
    public static Map<String, String> serverNotes = new HashMap<>();
    public static boolean friendActivityNotifications = true;
    public static boolean notifFriendRequests = true;
    public static boolean notifFriendOnline   = true;
    public static boolean notifServerUpdates  = true;
    public static boolean notifSystem         = true;
    public static boolean notifStreakReminder = true;
    public static boolean hasCompletedOnboarding = false;
    public static List<String> preferredTags = new ArrayList<>();
    public static String profilePrivacy = "public"; // "public", "friends", "private"

    private static final Path SETTINGS_PATH = Paths.get(System.getProperty("user.home"), ".rsps_hub", "settings.json");
    private static final Path ACCENT_CSS_PATH = Paths.get(System.getProperty("user.home"), ".rsps_hub", "accent.css");

    private static class SettingsData {
        String downloadPath, statusMessage, accentColor, profilePrivacy;
        boolean minimizeOnLaunch, autoUpdateClients, lightMode, autoLaunch;
        List<String> favouriteServers, preferredTags;
        Map<String, String> serverNotes;
        Boolean friendActivityNotifications, notifFriendRequests, notifFriendOnline, notifServerUpdates, notifSystem, notifStreakReminder, hasCompletedOnboarding;
    }

    public static void saveSettings() {
        try {
            SettingsData d = new SettingsData();
            d.downloadPath = downloadPath; d.statusMessage = statusMessage; d.minimizeOnLaunch = minimizeOnLaunch;
            d.autoUpdateClients = autoUpdateClients; d.lightMode = lightMode; d.autoLaunch = autoLaunch;
            d.accentColor = accentColor; d.favouriteServers = new ArrayList<>(favouriteServers);
            d.serverNotes = serverNotes; d.friendActivityNotifications = friendActivityNotifications;
            d.notifFriendRequests = notifFriendRequests; d.notifFriendOnline = notifFriendOnline;
            d.notifServerUpdates = notifServerUpdates; d.notifSystem = notifSystem;
            d.notifStreakReminder = notifStreakReminder; d.hasCompletedOnboarding = hasCompletedOnboarding;
            d.preferredTags = new ArrayList<>(preferredTags); d.profilePrivacy = profilePrivacy;
            Files.createDirectories(SETTINGS_PATH.getParent());
            Files.writeString(SETTINGS_PATH, new Gson().toJson(d));
        } catch (Exception e) { System.err.println("Failed to save settings: " + e.getMessage()); }
    }

    public static void loadSettings() {
        try {
            if (!Files.exists(SETTINGS_PATH)) return;
            SettingsData d = new Gson().fromJson(Files.readString(SETTINGS_PATH), SettingsData.class);
            if (d.downloadPath != null) downloadPath = d.downloadPath;
            if (d.statusMessage != null) statusMessage = d.statusMessage;
            if (d.accentColor != null) accentColor = d.accentColor;
            if (d.favouriteServers != null) favouriteServers = new LinkedHashSet<>(d.favouriteServers);
            if (d.serverNotes != null) serverNotes = d.serverNotes;
            if (d.friendActivityNotifications != null) friendActivityNotifications = d.friendActivityNotifications;
            if (d.notifFriendRequests != null) notifFriendRequests = d.notifFriendRequests;
            if (d.notifFriendOnline != null) notifFriendOnline = d.notifFriendOnline;
            if (d.notifServerUpdates != null) notifServerUpdates = d.notifServerUpdates;
            if (d.notifSystem != null) notifSystem = d.notifSystem;
            if (d.notifStreakReminder != null) notifStreakReminder = d.notifStreakReminder;
            if (d.hasCompletedOnboarding != null) hasCompletedOnboarding = d.hasCompletedOnboarding;
            if (d.preferredTags != null) preferredTags = d.preferredTags;
            if (d.profilePrivacy != null) profilePrivacy = d.profilePrivacy;
            minimizeOnLaunch = d.minimizeOnLaunch; autoUpdateClients = d.autoUpdateClients;
            lightMode = d.lightMode; autoLaunch = d.autoLaunch;
        } catch (Exception e) { System.err.println("Failed to load settings: " + e.getMessage()); }
    }

    public static List<String> getStylesheets(Class<?> cls) {
        List<String> sheets = new ArrayList<>();
        sheets.add(cls.getResource("style.css").toExternalForm());
        if (lightMode) sheets.add(cls.getResource("style-light.css").toExternalForm());
        if (Files.exists(ACCENT_CSS_PATH)) sheets.add(getAccentCssUrl());
        return sheets;
    }

    public static String getAccentCssUrl() { return ACCENT_CSS_PATH.toUri().toString() + "#" + System.currentTimeMillis(); }

    public static void setAccentColor(String hex) { accentColor = hex; writeAccentCss(); saveSettings(); }

    public static void writeAccentCss() {
        try {
            Files.createDirectories(ACCENT_CSS_PATH.getParent());
            Files.writeString(ACCENT_CSS_PATH, ".nav-tab-active { -fx-text-fill: " + accentColor + "; -fx-border-color: " + accentColor + "; }\n.play-button { -fx-background-color: " + accentColor + "; }");
        } catch (Exception e) { System.err.println("Failed to write accent CSS: " + e.getMessage()); }
    }

    public static void setAutoLaunch(boolean enable) {
        try {
            if (enable) {
                String jarPath = LauncherEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                if (jarPath.startsWith("/")) jarPath = jarPath.substring(1);
                new ProcessBuilder("reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", "RSPSHub", "/t", "REG_SZ", "/d", "javaw -jar \"" + jarPath + "\"", "/f").start().waitFor();
            } else {
                new ProcessBuilder("reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", "RSPSHub", "/f").start().waitFor();
            }
        } catch (Exception e) { System.err.println("Auto-launch toggle failed: " + e.getMessage()); }
    }

    public static void init() {
        loadSettings();
        writeAccentCss();
        try {
            Files.createDirectories(Paths.get(downloadPath));
            System.out.println("Hub initialized at: " + downloadPath);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** FETCHES FROM YOUR DUCKDNS API NOW! */
    public static List<ServerProfile> fetchServers() {
        try {
            // Blocks until the async future completes
            return ApiClient.getLiveServers().join(); 
        } catch (Exception e) {
            System.err.println("Error fetching server list: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static String jarFileName(ServerProfile server) {
        if (server.jarUrl == null || server.jarUrl.isEmpty()) return "client.jar";
        String raw = server.jarUrl.replaceAll("\\?.*", ""); 
        String name = raw.substring(raw.lastIndexOf('/') + 1);
        return name.endsWith(".jar") ? name : "client.jar";
    }

    public static boolean downloadClient(ServerProfile server) { return downloadClient(server, null, null); }

    public static boolean downloadClient(ServerProfile server, DoubleConsumer onProgress, boolean[] cancelledFlag) {
        Path jarPath = null;
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            Files.createDirectories(serverFolder);
            jarPath = serverFolder.resolve(jarFileName(server));

            System.out.println("Downloading " + server.name + "...");

            URL jarUrl = new URL(server.jarUrl);
            HttpURLConnection conn = (HttpURLConnection) jarUrl.openConnection();
            conn.connect();
            long contentLength = conn.getContentLengthLong();

            try (InputStream in = conn.getInputStream(); java.io.OutputStream out = Files.newOutputStream(jarPath)) {
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
                    if (onProgress != null && contentLength > 0) onProgress.accept((double) downloaded / contentLength);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (jarPath != null) { try { Files.deleteIfExists(jarPath); } catch (Exception ignored) {} }
            return false;
        }
    }

    /** THE MAGIC SANDBOX LAUNCHER! */
    public static Process launchGame(ServerProfile server) {
        try {
            Path baseFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            Path sandboxDir = baseFolder.resolve("Instance"); // This isolates the cache!
            
            if (!Files.exists(sandboxDir)) {
                Files.createDirectories(sandboxDir);
            }

            Path jarPath = baseFolder.resolve(jarFileName(server));

            // Injecting the Duser.home variable to trick the client
            ProcessBuilder pb = new ProcessBuilder(
                "java", 
                "-Duser.home=" + sandboxDir.toAbsolutePath().toString(),
                "-jar", 
                jarPath.getFileName().toString()
            );
            
            pb.directory(baseFolder.toFile()); 
            return pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isDownloaded(ServerProfile server) {
        Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), jarFileName(server));
        return Files.exists(jarPath);
    }

    public static boolean uninstallServer(ServerProfile server) {
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            if (!Files.exists(serverFolder)) return true;
            Files.walk(serverFolder).sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean isUpdateAvailable(ServerProfile server) {
        try {
            Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), jarFileName(server));
            if (!Files.exists(jarPath)) return false;
            long localSize = Files.size(jarPath);
            HttpURLConnection conn = (HttpURLConnection) new URL(server.jarUrl).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            long remoteSize = conn.getContentLengthLong();
            conn.disconnect();
            return remoteSize > 0 && remoteSize != localSize;
        } catch (Exception e) { return false; }
    }
}