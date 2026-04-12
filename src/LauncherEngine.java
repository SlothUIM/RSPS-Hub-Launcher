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

    // ── Auto-update ───────────────────────────────────────────────────────────
    public static final String CURRENT_VERSION   = "1.0.0";
    public static final String VERSION_CHECK_URL = "https://therspshub.com/version.json";

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
    private static final Path RUNTIMES_PATH    = Paths.get(System.getProperty("user.home"), ".rsps_hub", "runtimes");

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

    /** Returns the running JVM's class file major version (52=Java8, 61=Java17, etc.) */
    public static int getCurrentJavaMajor() {
        try { return (int) Double.parseDouble(System.getProperty("java.class.version", "52.0")); }
        catch (Exception e) { return 52; }
    }

    /** Returns the bytecode major version required by this server's JAR, or getCurrentJavaMajor() if not applicable. */
    public static int getRequiredJavaMajor(ServerProfile server) {
        if (isExeLauncher(server)) return getCurrentJavaMajor();
        Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), clientFileName(server));
        if (!Files.exists(jarPath)) return getCurrentJavaMajor();
        return detectRequiredJavaMajor(jarPath);
    }

    /** Returns the managed java.exe Path for the given Java version (e.g. 11), or null if not downloaded yet. */
    public static Path getManagedJavaExe(int javaVersion) {
        Path exe = RUNTIMES_PATH.resolve("java-" + javaVersion).resolve("bin").resolve("java.exe");
        return Files.exists(exe) ? exe : null;
    }

    /**
     * Downloads and installs a JRE from Adoptium into ~/.rsps_hub/runtimes/java-{version}/.
     * Progress reports 0.0–1.0. Set cancelled[0]=true to abort mid-download.
     * Returns path to java.exe on success, null on failure or cancel.
     */
    public static Path downloadRuntime(int javaVersion, DoubleConsumer onProgress, boolean[] cancelled) throws Exception {
        Path runtimeDir = RUNTIMES_PATH.resolve("java-" + javaVersion);
        Path tmpZip     = RUNTIMES_PATH.resolve("java-" + javaVersion + ".zip.tmp");
        Path tmpExtract = RUNTIMES_PATH.resolve("java-" + javaVersion + "-extract.tmp");
        Files.createDirectories(RUNTIMES_PATH);

        try {
            // 1. Query Adoptium API for download link + size
            String apiUrl = "https://api.adoptium.net/v3/assets/latest/" + javaVersion
                + "/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse";
            HttpURLConnection apiConn = (HttpURLConnection) new URL(apiUrl).openConnection(java.net.Proxy.NO_PROXY);
            apiConn.setConnectTimeout(12000);
            apiConn.setReadTimeout(15000);
            apiConn.setRequestProperty("Accept", "application/json");
            apiConn.setRequestProperty("User-Agent", "Mozilla/5.0");
            String jsonResp;
            try (InputStreamReader r = new InputStreamReader(apiConn.getInputStream())) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096]; int n;
                while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
                jsonResp = sb.toString();
            }
            com.google.gson.JsonArray releases = new com.google.gson.Gson()
                .fromJson(jsonResp, com.google.gson.JsonArray.class);
            com.google.gson.JsonObject pkg = releases.get(0).getAsJsonObject()
                .getAsJsonArray("binaries").get(0).getAsJsonObject()
                .getAsJsonObject("package");
            String downloadUrl = pkg.get("link").getAsString();
            long   totalBytes  = pkg.get("size").getAsLong();

            if (cancelled != null && cancelled[0]) return null;

            // 2. Download ZIP (0 → 82% of overall progress)
            HttpURLConnection dlConn = openFollowingRedirects(downloadUrl, 8);
            try (InputStream in = dlConn.getInputStream();
                 java.io.OutputStream out = Files.newOutputStream(tmpZip)) {
                byte[] buf = new byte[65536];
                long downloaded = 0; int n;
                while ((n = in.read(buf)) != -1) {
                    if (cancelled != null && cancelled[0]) return null;
                    out.write(buf, 0, n);
                    downloaded += n;
                    if (onProgress != null && totalBytes > 0)
                        onProgress.accept((double) downloaded / totalBytes * 0.82);
                }
            }
            if (cancelled != null && cancelled[0]) return null;
            if (onProgress != null) onProgress.accept(0.84);

            // 3. Extract ZIP to temp dir (82 → 97%)
            Files.createDirectories(tmpExtract);
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(tmpZip))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (cancelled != null && cancelled[0]) return null;
                    Path target = tmpExtract.resolve(entry.getName()).normalize();
                    if (!target.startsWith(tmpExtract.normalize())) { zis.closeEntry(); continue; } // zip-slip guard
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        try (java.io.OutputStream out = Files.newOutputStream(target)) {
                            byte[] buf = new byte[65536]; int n;
                            while ((n = zis.read(buf)) != -1) out.write(buf, 0, n);
                        }
                    }
                    zis.closeEntry();
                }
            }
            if (onProgress != null) onProgress.accept(0.97);

            // 4. Move extracted JRE root to final location (97 → 100%)
            java.io.File[] kids = tmpExtract.toFile().listFiles();
            if (kids == null || kids.length == 0) throw new Exception("ZIP extraction produced no output");
            Path jreRoot = kids[0].toPath();
            if (Files.exists(runtimeDir)) {
                Files.walk(runtimeDir).sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile).forEach(java.io.File::delete);
            }
            Files.move(jreRoot, runtimeDir);
            if (onProgress != null) onProgress.accept(1.0);

            Path javaExe = runtimeDir.resolve("bin").resolve("java.exe");
            System.out.println("Managed Java " + javaVersion + " installed at: " + javaExe);
            return Files.exists(javaExe) ? javaExe : null;

        } finally {
            try { Files.deleteIfExists(tmpZip); } catch (Exception ignored) {}
            try {
                if (Files.exists(tmpExtract))
                    Files.walk(tmpExtract).sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile).forEach(java.io.File::delete);
            } catch (Exception ignored) {}
        }
    }

    /** Opens a connection following redirects across HTTP↔HTTPS (Java won't do this automatically). */
    private static HttpURLConnection openFollowingRedirects(String urlStr, int maxRedirects) throws Exception {
        String current = urlStr;
        for (int i = 0; i < maxRedirects; i++) {
            URL url = new URL(current);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) break;
                // Handle relative redirects
                if (!location.startsWith("http")) location = new URL(url, location).toString();
                current = location;
            } else {
                return conn;
            }
        }
        throw new Exception("Too many redirects or no valid response for: " + urlStr);
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

            // Follow redirects manually — HttpURLConnection won't cross HTTP→HTTPS
            HttpURLConnection conn = openFollowingRedirects(server.jarUrl, 8);
            long contentLength = conn.getContentLengthLong();

            // Sanity-check: if the server returned HTML instead of a JAR, bail out
            String ct = conn.getContentType();
            if (ct != null && ct.contains("text/html")) {
                System.err.println("Download returned HTML instead of JAR — server may require a browser or a direct link.");
                conn.disconnect();
                return false;
            }

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
                pb = new ProcessBuilder(clientPath.toAbsolutePath().toString());
                pb.directory(baseFolder.toFile());
            } else {
                // Detect required Java version from JAR bytecode, find matching install
                int requiredMajor = detectRequiredJavaMajor(clientPath);
                String javaExe    = findJavaExecutable(requiredMajor);
                System.out.println("JAR requires bytecode major " + requiredMajor + " → using: " + javaExe);

                // Use javaw (no console window) instead of java — this matches what Windows does
                // on double-click, and avoids pipe-buffer blocking where the child process fills
                // the 64 KB stdout pipe (which we never read) and hangs waiting for a consumer.
                String javaLauncher;
                if (javaExe.equals("java")) {
                    javaLauncher = "javaw"; // system javaw, same bin dir as java
                } else {
                    Path javawPath = Paths.get(javaExe.endsWith("java.exe")
                        ? javaExe.replace("java.exe", "javaw.exe") : javaExe);
                    javaLauncher = Files.exists(javawPath) ? javawPath.toString() : javaExe;
                }

                pb = new ProcessBuilder(
                    javaLauncher,
                    "-jar",
                    clientPath.getFileName().toString()
                );
                pb.directory(baseFolder.toFile());

                // When using a non-system java, set JAVA_HOME and PATH so that the game's
                // bootstrapper (which often does its own `java -version` check) finds the right JVM
                if (!javaExe.equals("java")) {
                    Path javaHome = Paths.get(javaExe).getParent().getParent(); // bin/java.exe → JRE root
                    pb.environment().put("JAVA_HOME", javaHome.toString());
                    String sysPATH = pb.environment().getOrDefault("PATH", System.getenv("PATH"));
                    pb.environment().put("PATH", javaHome.resolve("bin").toString() + java.io.File.pathSeparator + sysPATH);
                }

                // Discard child stdout/stderr — belt-and-suspenders against pipe blocking
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            }

            return pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads the bytecode major version from the first .class file inside a JAR.
     * Java 8=52, Java 11=55, Java 17=61. Returns 52 (Java 8) if undetectable.
     */
    private static int detectRequiredJavaMajor(Path jarPath) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath.toFile())) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        byte[] magic = new byte[8];
                        if (is.read(magic) == 8 && magic[0] == (byte)0xCA && magic[1] == (byte)0xFE) {
                            return ((magic[6] & 0xFF) << 8) | (magic[7] & 0xFF); // major version
                        }
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
        return 52; // default: Java 8
    }

    /**
     * Finds the right java.exe for the given bytecode major version.
     * Only switches away from system java if the system java is TOO OLD to run the JAR.
     * Never downgrades — Java is backwards-compatible so a newer JVM can always run older bytecode.
     * Major versions: 52=Java8, 55=Java11, 61=Java17
     */
    private static String findJavaExecutable(int requiredMajor) {
        // Get the current JVM's class file version
        String specVer = System.getProperty("java.class.version", "52.0");
        int currentMajor = 52;
        try { currentMajor = (int) Double.parseDouble(specVer); } catch (Exception ignored) {}

        // Current JVM is new enough — no need to switch
        if (currentMajor >= requiredMajor) return "java";

        // Current JVM is too old, search for a newer one
        int javaVersion = requiredMajor - 44; // 52→8, 55→11, 61→17

        // 1. Check managed runtimes downloaded by the hub
        Path managed = getManagedJavaExe(javaVersion);
        if (managed != null) {
            System.out.println("Using managed Java " + javaVersion + ": " + managed);
            return managed.toAbsolutePath().toString();
        }

        // 2. Fall back to system installs
        String[] roots = {
            "C:\\Program Files\\Java",
            "C:\\Program Files\\Eclipse Adoptium",
            "C:\\Program Files\\Microsoft",
            "C:\\Program Files\\Amazon Corretto",
            "C:\\Program Files\\Zulu",
            "C:\\Program Files\\BellSoft",
        };

        for (String root : roots) {
            java.io.File dir = new java.io.File(root);
            if (!dir.exists()) continue;
            java.io.File[] kids = dir.listFiles();
            if (kids == null) continue;
            for (java.io.File kid : kids) {
                String name = kid.getName().toLowerCase();
                if (name.contains("jdk") && (name.contains("-" + javaVersion + ".") || name.contains("-" + javaVersion + "-") || name.endsWith("-" + javaVersion) || name.contains("jdk" + javaVersion) || (javaVersion == 8 && (name.contains("1.8") || name.contains("jdk8"))))) {
                    java.io.File javaExe = new java.io.File(kid, "bin\\java.exe");
                    if (javaExe.exists()) {
                        System.out.println("Found Java " + javaVersion + " at: " + javaExe.getAbsolutePath());
                        return javaExe.getAbsolutePath();
                    }
                }
            }
        }

        System.out.println("No Java " + javaVersion + " found, falling back to system java");
        return "java";
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

    // ── Launcher self-update ──────────────────────────────────────────────────

    public static class UpdateInfo {
        public String version;
        public String downloadUrl;
        public String notes;
    }

    public static UpdateInfo checkForUpdate() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(VERSION_CHECK_URL).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "RSPSHub-Launcher/" + CURRENT_VERSION);
            if (conn.getResponseCode() != 200) return null;
            try (InputStreamReader r = new InputStreamReader(conn.getInputStream())) {
                UpdateInfo info = new Gson().fromJson(r, UpdateInfo.class);
                if (info == null || info.version == null) return null;
                if (isNewerVersion(info.version, CURRENT_VERSION)) return info;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isNewerVersion(String remote, String current) {
        try {
            String[] r = remote.split("\\.");
            String[] c = current.split("\\.");
            int len = Math.max(r.length, c.length);
            for (int i = 0; i < len; i++) {
                int rv = i < r.length ? Integer.parseInt(r[i].trim()) : 0;
                int cv = i < c.length ? Integer.parseInt(c[i].trim()) : 0;
                if (rv > cv) return true;
                if (rv < cv) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void downloadUpdateAndRestart(String downloadUrl, javafx.application.Application app) {
        try {
            Path updateJar = Paths.get(System.getProperty("user.home"), ".rsps_hub", "RSPSHub-update.jar");
            Files.createDirectories(updateJar.getParent());

            // Find current JAR location
            String currentJar = LauncherEngine.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            // On Windows the path starts with / — strip it
            if (System.getProperty("os.name").toLowerCase().contains("win") && currentJar.startsWith("/")) {
                currentJar = currentJar.substring(1);
            }
            currentJar = currentJar.replace("%20", " ");
            Path currentJarPath = Paths.get(currentJar);

            // Download new JAR
            HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, updateJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Write PowerShell restart script
            Path script = Paths.get(System.getProperty("java.io.tmpdir"), "rsps_hub_update.ps1");
            String javaw = ProcessHandle.current().info().command().orElse("javaw");
            // Prefer javaw over java
            javaw = javaw.replace("java.exe", "javaw.exe");

            String ps = String.join("\r\n",
                "Start-Sleep -Seconds 2",
                "Copy-Item -Path '" + updateJar.toString().replace("'", "''") + "' " +
                    "-Destination '" + currentJarPath.toString().replace("'", "''") + "' -Force",
                "Start-Process '" + javaw.replace("'", "''") + "' " +
                    "-ArgumentList '-jar','" + currentJarPath.toString().replace("'", "''") + "'",
                "Remove-Item -Path $MyInvocation.MyCommand.Path -Force"
            );
            Files.writeString(script, ps);

            // Launch script detached and exit
            new ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-File", script.toString())
                .start();
            javafx.application.Platform.exit();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isUpdateAvailable(ServerProfile server) {
        try {
            Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), clientFileName(server));
            if (!Files.exists(jarPath)) return false;
            long localSize = Files.size(jarPath);
            HttpURLConnection conn = (HttpURLConnection) new URL(server.jarUrl).openConnection(java.net.Proxy.NO_PROXY);
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            long remoteSize = conn.getContentLengthLong();
            conn.disconnect();
            return remoteSize > 0 && remoteSize != localSize;
        } catch (Exception e) { return false; }
    }
}