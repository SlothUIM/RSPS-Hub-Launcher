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
    public static boolean isStaff       = false;
    public static String activeServer    = null;

    // Settings
    public static String downloadPath = System.getProperty("user.home") + "/.rsps_hub/";
    public static boolean minimizeOnLaunch = false;
    public static boolean autoUpdateClients = false;
    public static boolean lightMode = false;
    public static boolean autoLaunch = false;
    public static String accentColor = "#9b5de5";
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
    public static List<String> groups = new ArrayList<>();
    public static Map<String, List<String>> groupMembers = new HashMap<>();

    // Global (machine-wide) settings path
    private static final Path SETTINGS_PATH    = Paths.get(System.getProperty("user.home"), ".rsps_hub", "settings.json");
    private static final Path ACCENT_CSS_PATH  = Paths.get(System.getProperty("user.home"), ".rsps_hub", "accent.css");

    // Per-user settings path — dynamic based on logged-in account
    private static Path userSettingsPath() {
        if (currentUsername == null || currentUsername.isEmpty()) return null;
        return Paths.get(System.getProperty("user.home"), ".rsps_hub", currentUsername, "profile_settings.json");
    }

    // ── Global settings (machine-wide, not account-specific) ─────────────────
    private static class GlobalSettings {
        String downloadPath;
        boolean minimizeOnLaunch, autoUpdateClients, lightMode, autoLaunch, hasCompletedOnboarding;
    }

    // ── Per-user settings (one file per account) ──────────────────────────────
    private static class UserSettings {
        String statusMessage, accentColor, profilePrivacy, avatarImagePath;
        List<String> favouriteServers, preferredTags, groups;
        Map<String, String> serverNotes;
        Map<String, List<String>> groupMembers;
        Boolean friendActivityNotifications, notifFriendRequests, notifFriendOnline,
                notifServerUpdates, notifSystem, notifStreakReminder;
    }

    /** Save both global and per-user settings. */
    public static void saveSettings() {
        try {
            // Global
            GlobalSettings g = new GlobalSettings();
            g.downloadPath = downloadPath; g.minimizeOnLaunch = minimizeOnLaunch;
            g.autoUpdateClients = autoUpdateClients; g.lightMode = lightMode;
            g.autoLaunch = autoLaunch; g.hasCompletedOnboarding = hasCompletedOnboarding;
            Files.createDirectories(SETTINGS_PATH.getParent());
            Files.writeString(SETTINGS_PATH, new Gson().toJson(g));
        } catch (Exception e) { System.err.println("Failed to save global settings: " + e.getMessage()); }

        saveUserSettings();
    }

    /** Save only per-user settings (called when username is set). */
    public static void saveUserSettings() {
        Path p = userSettingsPath();
        if (p == null) return;
        try {
            UserSettings u = new UserSettings();
            u.statusMessage = statusMessage; u.accentColor = accentColor;
            u.profilePrivacy = profilePrivacy; u.avatarImagePath = avatarImagePath;
            u.favouriteServers = new ArrayList<>(favouriteServers);
            u.serverNotes = serverNotes; u.preferredTags = new ArrayList<>(preferredTags);
            u.groups = new ArrayList<>(groups); u.groupMembers = new HashMap<>(groupMembers);
            u.friendActivityNotifications = friendActivityNotifications;
            u.notifFriendRequests = notifFriendRequests; u.notifFriendOnline = notifFriendOnline;
            u.notifServerUpdates = notifServerUpdates; u.notifSystem = notifSystem;
            u.notifStreakReminder = notifStreakReminder;
            Files.createDirectories(p.getParent());
            Files.writeString(p, new Gson().toJson(u));
        } catch (Exception e) { System.err.println("Failed to save user settings: " + e.getMessage()); }
    }

    /** Load global (machine-wide) settings — call before login. */
    public static void loadSettings() {
        try {
            if (!Files.exists(SETTINGS_PATH)) return;
            GlobalSettings g = new Gson().fromJson(Files.readString(SETTINGS_PATH), GlobalSettings.class);
            if (g.downloadPath != null) downloadPath = g.downloadPath;
            minimizeOnLaunch = g.minimizeOnLaunch; autoUpdateClients = g.autoUpdateClients;
            lightMode = g.lightMode; autoLaunch = g.autoLaunch;
            hasCompletedOnboarding = g.hasCompletedOnboarding;
        } catch (Exception e) { System.err.println("Failed to load global settings: " + e.getMessage()); }
    }

    /** Load per-user settings — call after currentUsername is set. */
    public static void loadUserSettings() {
        // Reset user-specific fields to defaults first so old account's data doesn't bleed through
        statusMessage = ""; accentColor = "#9b5de5"; profilePrivacy = "public";
        avatarImagePath = null; favouriteServers = new LinkedHashSet<>();
        serverNotes = new HashMap<>(); preferredTags = new ArrayList<>();
        groups = new ArrayList<>(); groupMembers = new HashMap<>();
        friendActivityNotifications = true; notifFriendRequests = true;
        notifFriendOnline = true; notifServerUpdates = true;
        notifSystem = true; notifStreakReminder = true;

        Path p = userSettingsPath();
        if (p == null || !Files.exists(p)) return;
        try {
            UserSettings u = new Gson().fromJson(Files.readString(p), UserSettings.class);
            if (u.statusMessage  != null) statusMessage  = u.statusMessage;
            if (u.accentColor    != null) accentColor    = u.accentColor;
            if (u.profilePrivacy != null) profilePrivacy = u.profilePrivacy;
            if (u.avatarImagePath != null) avatarImagePath = u.avatarImagePath;
            if (u.favouriteServers != null) favouriteServers = new LinkedHashSet<>(u.favouriteServers);
            if (u.serverNotes    != null) serverNotes    = u.serverNotes;
            if (u.preferredTags  != null) preferredTags  = u.preferredTags;
            if (u.groups         != null) groups         = new ArrayList<>(u.groups);
            if (u.groupMembers   != null) groupMembers   = new HashMap<>(u.groupMembers);
            if (u.friendActivityNotifications != null) friendActivityNotifications = u.friendActivityNotifications;
            if (u.notifFriendRequests != null) notifFriendRequests = u.notifFriendRequests;
            if (u.notifFriendOnline   != null) notifFriendOnline   = u.notifFriendOnline;
            if (u.notifServerUpdates  != null) notifServerUpdates  = u.notifServerUpdates;
            if (u.notifSystem         != null) notifSystem         = u.notifSystem;
            if (u.notifStreakReminder  != null) notifStreakReminder  = u.notifStreakReminder;
        } catch (Exception e) { System.err.println("Failed to load user settings: " + e.getMessage()); }
    }

    public static List<String> getStylesheets(Class<?> cls) {
        List<String> sheets = new ArrayList<>();
        sheets.add(cls.getResource("style.css").toExternalForm());
        if (lightMode) sheets.add(cls.getResource("style-light.css").toExternalForm());
        if (Files.exists(ACCENT_CSS_PATH)) sheets.add(getAccentCssUrl());
        return sheets;
    }

    public static String getAccentCssUrl() { return ACCENT_CSS_PATH.toUri().toString() + "#" + System.currentTimeMillis(); }

    public static void setAccentColor(String hex) { accentColor = hex; writeAccentCss(); saveUserSettings(); }

    public static String darkenHex(String hex, double factor) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("#%02x%02x%02x", (int)(r * factor), (int)(g * factor), (int)(b * factor));
        } catch (Exception e) { return hex; }
    }

    public static void writeAccentCss() {
        try {
            Files.createDirectories(ACCENT_CSS_PATH.getParent());
            String a = accentColor;
            String d = darkenHex(a, 0.8);
            StringBuilder css = new StringBuilder();
            css.append(".nav-tab-active { -fx-text-fill: ").append(a).append("; -fx-border-color: ").append(a).append("; }\n");
            css.append(".search-field:focused { -fx-border-color: ").append(a).append("; }\n");
            css.append(".filter-btn-active { -fx-border-color: ").append(a).append("; }\n");
            css.append(".server-card-wrapper:hover { -fx-border-color: ").append(a).append("; }\n");
            css.append(".play-button { -fx-background-color: ").append(a).append("; }\n");
            css.append(".play-button:hover { -fx-background-color: ").append(d).append("; }\n");
            css.append(".detail-icon-placeholder { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".detail-stars-display { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".review-avatar { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".review-star-selected { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".msg-bubble-me { -fx-background-color: ").append(a).append("; }\n");
            css.append(".msg-sender-name { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".nav-avatar { -fx-background-color: ").append(a).append("; }\n");
            css.append(".nav-account-widget:hover .nav-username { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".settings-section-header { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".settings-checkbox:selected .box { -fx-background-color: ").append(a).append("; -fx-border-color: ").append(a).append("; }\n");
            css.append(".settings-avatar { -fx-background-color: ").append(a).append("; }\n");
            css.append(".dev-textarea { -fx-highlight-fill: ").append(a).append("; }\n");
            css.append(".dev-textarea:focused { -fx-border-color: ").append(a).append("; }\n");
            css.append(".dev-tag-check:selected .box { -fx-background-color: ").append(a).append("; -fx-border-color: ").append(a).append("; }\n");
            css.append(".auth-field:focused { -fx-border-color: ").append(a).append("; }\n");
            css.append(".auth-btn { -fx-background-color: ").append(a).append("; }\n");
            css.append(".auth-btn:hover { -fx-background-color: ").append(d).append("; }\n");
            css.append(".auth-link { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".auth-link:hover { -fx-text-fill: ").append(d).append("; }\n");
            css.append(".friends-subtab-active { -fx-border-color: ").append(a).append("; }\n");
            css.append(".splash-logo { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".splash-progress .bar { -fx-background-color: ").append(a).append("; }\n");
            css.append(".dialog-pane .button-bar .button:default { -fx-background-color: ").append(a).append("; }\n");
            css.append(".dialog-pane .button-bar .button:default:hover { -fx-background-color: ").append(d).append("; }\n");
            css.append(".dialog-pane .text-field:focused { -fx-border-color: ").append(a).append("; }\n");
            css.append(".nav-bell-badge { -fx-background-color: ").append(a).append("; }\n");
            css.append(".dark-menu-btn:hover { -fx-border-color: ").append(a).append("; }\n");
            css.append(".fav-btn:hover { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".fav-btn-active { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".server-note-text { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".pinned-header { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".skill-level-badge { -fx-text-fill: ").append(a).append("; }\n");
            css.append(".xp-bar-fill { -fx-background-color: ").append(a).append("; }\n");
            css.append(".profile-avatar { -fx-background-color: ").append(a).append("; }\n");
            Files.writeString(ACCENT_CSS_PATH, css.toString());
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

    /** Returns the local filename for the downloaded client (jar or exe). */
    public static String clientFileName(ServerProfile server) {
        if (server.jarUrl == null || server.jarUrl.isEmpty()) return "client.jar";
        String raw  = server.jarUrl.replaceAll("\\?.*", "");
        String name = raw.substring(raw.lastIndexOf('/') + 1);
        if (name.endsWith(".jar") || name.endsWith(".exe")) return name;
        return "client.jar";
    }

    /** @deprecated Use clientFileName */
    @Deprecated
    public static String jarFileName(ServerProfile server) { return clientFileName(server); }

    /** True if the server distributes a native .exe launcher rather than a JAR. */
    public static boolean isExeLauncher(ServerProfile server) {
        return clientFileName(server).endsWith(".exe");
    }

    public static boolean downloadClient(ServerProfile server) { return downloadClient(server, null, null); }

    public static boolean downloadClient(ServerProfile server, DoubleConsumer onProgress, boolean[] cancelledFlag) {
        Path jarPath = null;
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            Files.createDirectories(serverFolder);
            jarPath = serverFolder.resolve(clientFileName(server));

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

    /** Launches the game client — auto-detects JAR vs native EXE from the URL. */
    public static Process launchGame(ServerProfile server) {
        try {
            Path baseFolder  = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            Path clientPath  = baseFolder.resolve(clientFileName(server));
            ProcessBuilder pb;

            if (isExeLauncher(server)) {
                // Native launcher (.exe) — run directly, no JVM flags needed.
                // The descendant tracker in beginSession() will still catch any
                // child java.exe processes the launcher spawns.
                pb = new ProcessBuilder(clientPath.toAbsolutePath().toString());
                pb.directory(baseFolder.toFile());
            } else {
                // JAR — sandbox via -Duser.home so each server stores its cache
                // in its own Instance/ folder, keeping clients isolated.
                Path sandboxDir = baseFolder.resolve("Instance");
                if (!Files.exists(sandboxDir)) Files.createDirectories(sandboxDir);

                pb = new ProcessBuilder(
                    "java",
                    "-Duser.home=" + sandboxDir.toAbsolutePath().toString(),
                    "-jar",
                    clientPath.getFileName().toString()
                );
                pb.directory(baseFolder.toFile());
            }

            return pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isDownloaded(ServerProfile server) {
        Path clientPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), clientFileName(server));
        return Files.exists(clientPath);
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
            Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), clientFileName(server));
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