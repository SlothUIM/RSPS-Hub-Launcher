import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Local HTTP API server — used by the Electron UI.
 * Start with: main --api-mode --port 7890
 */
public class ApiServer {

    private static final String API_SECRET = "RH0WrSd0q05j3RA0DwWpqMfiP4cYFQxQs7kw5RTjAbfcjOGf";
    private static final Gson GSON = new Gson();

    // Active game session — volatile so the tracker thread and HTTP thread see the same value
    private static volatile String activeSessionServer = null;
    private static volatile long   activeSessionStart  = 0;

    public static void start(int port) {
        LauncherEngine.loadSettings();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                rule.anyHost();
            }));
        });

        // ── AUTH CHECK (every route except /api/ping) ──────────────────────────
        app.before(ctx -> {
            if (ctx.path().equals("/api/ping")) return;
            String key = ctx.header("X-Api-Key");
            if (!API_SECRET.equals(key)) {
                ctx.status(401).json(Map.of("error", "Unauthorized"));
                throw new Exception("Unauthorized");
            }
        });

        // ── HEALTH CHECK ───────────────────────────────────────────────────────
        app.get("/api/ping", ctx -> ctx.json(Map.of("status", "ok")));

        // ── AUTH ───────────────────────────────────────────────────────────────

        app.post("/api/auth/login", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String username = body.get("username").getAsString();
            String password = body.get("password").getAsString();
            boolean remember = !body.has("remember") || body.get("remember").getAsBoolean();

            String result = ApiClient.postJson("auth/login.php",
                GSON.toJson(Map.of("username", username, "password", password))
            ).join();

            JsonObject res = JsonParser.parseString(result).getAsJsonObject();
            if (res.has("token")) {
                LauncherEngine.sessionToken    = res.get("token").getAsString();
                LauncherEngine.currentUsername = username;
                LauncherEngine.loadUserSettings();
                if (remember) LauncherEngine.saveSession();
                else          LauncherEngine.clearSession();
                ctx.json(Map.of(
                    "success", true,
                    "username", username,
                    "token", LauncherEngine.sessionToken
                ));
            } else {
                ctx.status(401).json(Map.of(
                    "error", res.has("error") ? res.get("error").getAsString() : "Login failed"
                ));
            }
        });

        app.post("/api/auth/register", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String result = ApiClient.postJson("auth/register.php", GSON.toJson(Map.of(
                "username", body.get("username").getAsString(),
                "password", body.get("password").getAsString(),
                "email",    body.has("email") ? body.get("email").getAsString() : ""
            ))).join();
            JsonObject res = JsonParser.parseString(result).getAsJsonObject();
            if (res.has("token")) {
                LauncherEngine.sessionToken    = res.get("token").getAsString();
                LauncherEngine.currentUsername = body.get("username").getAsString();
                LauncherEngine.loadUserSettings();
                LauncherEngine.saveSession();
                ctx.json(Map.of("success", true, "username", LauncherEngine.currentUsername, "token", LauncherEngine.sessionToken));
            } else {
                ctx.status(400).json(Map.of("error", res.has("error") ? res.get("error").getAsString() : "Registration failed"));
            }
        });

        app.post("/api/auth/logout", ctx -> {
            LauncherEngine.sessionToken    = "";
            LauncherEngine.currentUsername = "";
            LauncherEngine.clearSession();
            ctx.json(Map.of("success", true));
        });

        // ── CURRENT USER ───────────────────────────────────────────────────────

        app.get("/api/user", ctx -> {
            if (LauncherEngine.currentUsername.isEmpty()) {
                ctx.status(401).json(Map.of("error", "Not logged in"));
                return;
            }
            ctx.json(Map.of(
                "username",   LauncherEngine.currentUsername,
                "token",      LauncherEngine.sessionToken,
                "favourites", List.copyOf(LauncherEngine.favouriteServers)
            ));
        });

        // ── SERVERS ────────────────────────────────────────────────────────────

        app.get("/api/servers", ctx -> {
            List<ServerProfile> servers = ApiClient.getLiveServers().join();
            // Enrich each server with local install state
            List<Map<String, Object>> enriched = new java.util.ArrayList<>();
            for (ServerProfile s : servers) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id",            s.id);
                m.put("name",          s.name);
                m.put("tagline",       s.tagline);
                m.put("description",   s.description);
                m.put("xpRate",        s.xpRate);
                m.put("jarUrl",        s.jarUrl);
                m.put("bannerUrl",     s.bannerUrl);
                m.put("cardBannerUrl", s.cardBannerUrl);
                m.put("iconUrl",       s.iconUrl);
                m.put("discordUrl",    s.discordUrl);
                m.put("websiteUrl",    s.websiteUrl);
                m.put("accentColor",   s.accentColor);
                m.put("changelog",     s.changelog);
                m.put("hubPlayers",    s.hubPlayers);
                m.put("reviewCount",   s.reviewCount);
                m.put("avgRating",     s.avgRating);
                m.put("serverOnline",  s.serverOnline);
                m.put("tags",          s.tags);
                m.put("screenshots",   s.screenshots);
                m.put("isNew",         s.isNew);
                m.put("skillLevel",    1);
                m.put("downloaded", LauncherEngine.isDownloaded(s));
                enriched.add(m);
            }
            ctx.json(Map.of(
                "servers",    enriched,
                "favourites", List.copyOf(LauncherEngine.favouriteServers)
            ));
        });

        app.post("/api/servers/{name}/play", ctx -> {
            String name = ctx.pathParam("name");
            ServerProfile server = findServer(name, ctx);
            if (server == null) return;

            PlaytimeStore.reload();
            final long startMs = System.currentTimeMillis();
            Process proc = LauncherEngine.launchGame(server);
            ctx.json(Map.of("success", true));

            if (proc != null) {
                final String serverName = server.name;
                final int    serverId   = server.id;
                activeSessionServer = serverName;
                activeSessionStart  = startMs;

                // Notify VPS that a hub player started a session
                ApiClient.sessionStart(serverId);

                new Thread(() -> {
                    try {
                        // Collect all descendants while the initial process is alive.
                        // This handles self-updating launchers that spawn a child JAR then exit.
                        java.util.Set<ProcessHandle> watched = new java.util.LinkedHashSet<>();
                        watched.add(proc.toHandle());
                        long lastPing = System.currentTimeMillis();
                        while (proc.isAlive()) {
                            proc.toHandle().descendants().forEach(watched::add);
                            Thread.sleep(500);
                            // Ping the VPS every 60 s to keep hub_players count accurate
                            if (System.currentTimeMillis() - lastPing >= 60_000L) {
                                ApiClient.sessionPing(serverId);
                                lastPing = System.currentTimeMillis();
                            }
                        }
                        // Also grab any descendants of descendants added above
                        new java.util.ArrayList<>(watched).forEach(ph ->
                            ph.descendants().forEach(watched::add));

                        // Wait for every tracked process to exit
                        for (ProcessHandle ph : watched) {
                            try { ph.onExit().get(); } catch (Exception ignored) {}
                        }

                        // Session is over — notify VPS, then clear active state
                        ApiClient.sessionEnd(serverId);
                        activeSessionServer = null;

                        long minutes = (System.currentTimeMillis() - startMs) / 60_000L;
                        if (minutes >= 1) {
                            PlaytimeStore.recordSession(serverName, minutes);
                        }
                    } catch (Exception e) {
                        ApiClient.sessionEnd(serverId);
                        activeSessionServer = null;
                        System.err.println("[Playtime] tracker error: " + e.getMessage());
                    }
                }, "playtime-tracker").start();
            }
        });

        app.post("/api/servers/{name}/install", ctx -> {
            String name = ctx.pathParam("name");

            // Read jarUrl from request body — avoids a second live-server fetch
            String jarUrl = null;
            try {
                JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
                if (body.has("jarUrl") && !body.get("jarUrl").isJsonNull())
                    jarUrl = body.get("jarUrl").getAsString();
            } catch (Exception ignored) {}

            if (jarUrl == null || jarUrl.isEmpty()) {
                ctx.status(400).json(Map.of("error", "No jar URL provided for: " + name));
                return;
            }

            // Build a minimal ServerProfile just for the downloader
            ServerProfile server = new ServerProfile();
            server.name   = name;
            server.jarUrl = jarUrl;

            boolean[] result = {false};
            Thread dlThread = new Thread(() -> {
                result[0] = LauncherEngine.downloadClient(server);
            });
            dlThread.start();
            dlThread.join();
            if (result[0]) {
                ctx.json(Map.of("success", true, "downloaded", true));
            } else {
                ctx.status(500).json(Map.of("error", "Download failed — check server logs"));
            }
        });

        app.post("/api/servers/{name}/uninstall", ctx -> {
            String name = ctx.pathParam("name");
            ServerProfile server = new ServerProfile();
            server.name = name;
            boolean ok = LauncherEngine.uninstallServer(server);
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(500).json(Map.of("error", "Uninstall failed"));
        });

        app.post("/api/servers/{name}/favourite", ctx -> {
            String name = ctx.pathParam("name");
            if (LauncherEngine.favouriteServers.contains(name)) {
                LauncherEngine.favouriteServers.remove(name);
            } else {
                LauncherEngine.favouriteServers.add(name);
            }
            LauncherEngine.saveUserSettings();
            ctx.json(Map.of(
                "favourited", LauncherEngine.favouriteServers.contains(name),
                "favourites", List.copyOf(LauncherEngine.favouriteServers)
            ));
        });

        // ── PLAYTIME / STATS ───────────────────────────────────────────────────

        app.get("/api/playtime", ctx -> {
            PlaytimeStore.reload();
            ctx.json(Map.of(
                "totalMinutes", PlaytimeStore.getTotalMinutes(),
                "perServer",    PlaytimeStore.getAllMinutes()
            ));
        });

        app.get("/api/session/status", ctx -> {
            String s = activeSessionServer;
            if (s == null) {
                ctx.json(Map.of("active", false));
            } else {
                ctx.json(Map.of("active", true, "serverName", s,
                    "elapsedMs", System.currentTimeMillis() - activeSessionStart));
            }
        });

        // ── FRIENDS ────────────────────────────────────────────────────────────

        app.get("/api/friends", ctx -> {
            List<Friend> friends = ApiClient.getFriends().join();
            ctx.json(Map.of("friends", friends));
        });

        app.post("/api/friends", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String username = body.get("username").getAsString();
            String result = ApiClient.addFriend(username).join();
            ctx.json(Map.of("result", result));
        });

        app.get("/api/friends/requests", ctx -> {
            List<FriendRequest> requests = ApiClient.getFriendRequests().join();
            ctx.json(Map.of("requests", requests));
        });

        app.post("/api/friends/accept", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String username = body.get("username").getAsString();
            boolean ok = ApiClient.acceptFriend(username).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Failed to accept request"));
        });

        app.post("/api/friends/decline", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String username = body.get("username").getAsString();
            boolean ok = ApiClient.declineFriend(username).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Failed to decline request"));
        });

        app.delete("/api/friends/{username}", ctx -> {
            String username = ctx.pathParam("username");
            boolean ok = ApiClient.removeFriend(username).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Failed to remove friend"));
        });

        // ── MESSAGES ──────────────────────────────────────────────────────────

        app.get("/api/messages", ctx -> {
            List<com.google.gson.JsonObject> convos = ApiClient.getConversations().join();
            ctx.json(Map.of("conversations", convos));
        });

        app.get("/api/messages/{username}", ctx -> {
            String username = ctx.pathParam("username");
            List<Message> messages = ApiClient.getMessages(username).join();
            ctx.json(Map.of("messages", messages));
        });

        app.post("/api/messages/{username}", ctx -> {
            String username = ctx.pathParam("username");
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String content = body.get("content").getAsString();
            boolean ok = ApiClient.sendMessage(username, content).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Failed to send message"));
        });

        // ── HEARTBEAT ─────────────────────────────────────────────────────────

        app.post("/api/heartbeat", ctx -> {
            ApiClient.heartbeat();
            ctx.json(Map.of("ok", true));
        });

        // ── LEADERBOARD ────────────────────────────────────────────────────────

        app.get("/api/leaderboard", ctx -> {
            // Fetch VPS leaderboard entries
            List<Map<String, Object>> entries = new ArrayList<>(ApiClient.getLeaderboard().join());

            // Inject the current user's real local playtime so it's always accurate
            String me = LauncherEngine.currentUsername;
            if (me != null && !me.isEmpty()) {
                PlaytimeStore.reload();
                long myMinutes    = PlaytimeStore.getTotalMinutes();
                String myTop      = PlaytimeStore.getMostPlayed();
                boolean alreadyIn = entries.stream()
                    .anyMatch(e -> me.equalsIgnoreCase(String.valueOf(e.get("username"))));
                if (!alreadyIn) {
                    Map<String, Object> myEntry = new java.util.LinkedHashMap<>();
                    myEntry.put("username",     me);
                    myEntry.put("totalMinutes", myMinutes);
                    myEntry.put("topServer",    myTop != null ? myTop : "—");
                    myEntry.put("isYou",        true);
                    entries.add(myEntry);
                } else {
                    // Update existing entry with local playtime
                    entries.replaceAll(e -> {
                        if (me.equalsIgnoreCase(String.valueOf(e.get("username")))) {
                            Map<String, Object> updated = new java.util.LinkedHashMap<>(e);
                            updated.put("totalMinutes", myMinutes);
                            updated.put("topServer",    myTop != null ? myTop : "—");
                            updated.put("isYou",        true);
                            return updated;
                        }
                        return e;
                    });
                }
            }

            // Sort descending by totalMinutes and add rank numbers
            entries.sort((a, b) -> Long.compare(
                ((Number) b.getOrDefault("totalMinutes", 0)).longValue(),
                ((Number) a.getOrDefault("totalMinutes", 0)).longValue()
            ));
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).put("rank", i + 1);
            }

            ctx.json(Map.of("entries", entries));
        });

        // ── SETTINGS ──────────────────────────────────────────────────────────────

        app.get("/api/settings", ctx -> {
            Map<String, Object> s = new java.util.LinkedHashMap<>();
            s.put("downloadPath",        LauncherEngine.downloadPath);
            s.put("minimizeOnLaunch",    LauncherEngine.minimizeOnLaunch);
            s.put("autoUpdateClients",   LauncherEngine.autoUpdateClients);
            s.put("accentColor",         LauncherEngine.accentColor);
            s.put("statusMessage",       LauncherEngine.statusMessage);
            s.put("profilePrivacy",      LauncherEngine.profilePrivacy);
            s.put("preferredTags",       List.copyOf(LauncherEngine.preferredTags));
            s.put("notifFriendRequests", LauncherEngine.notifFriendRequests);
            s.put("notifFriendOnline",   LauncherEngine.notifFriendOnline);
            s.put("notifServerUpdates",  LauncherEngine.notifServerUpdates);
            s.put("notifStreakReminder",  LauncherEngine.notifStreakReminder);
            s.put("notifSystem",         LauncherEngine.notifSystem);
            ctx.json(s);
        });

        app.post("/api/settings", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            if (body.has("downloadPath"))        LauncherEngine.downloadPath        = body.get("downloadPath").getAsString();
            if (body.has("minimizeOnLaunch"))    LauncherEngine.minimizeOnLaunch    = body.get("minimizeOnLaunch").getAsBoolean();
            if (body.has("autoUpdateClients"))   LauncherEngine.autoUpdateClients   = body.get("autoUpdateClients").getAsBoolean();
            if (body.has("accentColor"))         LauncherEngine.accentColor         = body.get("accentColor").getAsString();
            if (body.has("statusMessage"))       LauncherEngine.statusMessage       = body.get("statusMessage").getAsString();
            if (body.has("profilePrivacy"))      LauncherEngine.profilePrivacy      = body.get("profilePrivacy").getAsString();
            if (body.has("preferredTags")) {
                LauncherEngine.preferredTags.clear();
                body.get("preferredTags").getAsJsonArray()
                    .forEach(e -> LauncherEngine.preferredTags.add(e.getAsString()));
            }
            if (body.has("notifFriendRequests")) LauncherEngine.notifFriendRequests = body.get("notifFriendRequests").getAsBoolean();
            if (body.has("notifFriendOnline"))   LauncherEngine.notifFriendOnline   = body.get("notifFriendOnline").getAsBoolean();
            if (body.has("notifServerUpdates"))  LauncherEngine.notifServerUpdates  = body.get("notifServerUpdates").getAsBoolean();
            if (body.has("notifStreakReminder"))  LauncherEngine.notifStreakReminder  = body.get("notifStreakReminder").getAsBoolean();
            if (body.has("notifSystem"))         LauncherEngine.notifSystem         = body.get("notifSystem").getAsBoolean();
            LauncherEngine.saveSettings();
            ctx.json(Map.of("success", true));
        });

        // ── ANNOUNCEMENTS ─────────────────────────────────────────────────────────

        app.get("/api/announcements", ctx -> {
            try {
                JsonObject obj = ApiClient.getAnnouncements().join();
                String json = (obj != null && obj.has("announcements"))
                    ? obj.toString()
                    : "{\"announcements\":[]}";
                ctx.result(json).contentType("application/json");
            } catch (Exception e) {
                System.err.println("[announcements] ERROR: " + e);
                ctx.result("{\"announcements\":[]}").contentType("application/json");
            }
        });

        app.post("/api/announcements", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String title   = body.has("title")   ? body.get("title").getAsString()   : "";
            String message = body.has("message") ? body.get("message").getAsString() : "";
            JsonObject res = ApiClient.postAnnouncement(title, message).join();
            if (res.has("error")) ctx.status(400).json(Map.of("error", res.get("error").getAsString()));
            else ctx.json(Map.of("success", true));
        });

        app.post("/api/announcements/delete", ctx -> {
            int id = JsonParser.parseString(ctx.body()).getAsJsonObject().get("id").getAsInt();
            JsonObject res = ApiClient.deleteAnnouncement(id).join();
            if (res.has("error")) ctx.status(400).json(Map.of("error", res.get("error").getAsString()));
            else ctx.json(Map.of("success", true));
        });

        // ── DEV PORTAL ────────────────────────────────────────────────────────────

        app.get("/api/dev/check", ctx -> {
            ctx.json(Map.of("isStaff", LauncherEngine.isStaff, "username", LauncherEngine.currentUsername));
        });

        app.get("/api/dev/my-servers", ctx -> {
            List<ServerProfile> servers = ApiClient.getMyServers().join();
            ctx.json(Map.of("servers", servers != null ? servers : List.of()));
        });

        app.get("/api/dev/all-servers", ctx -> {
            // Raw call so we can surface any error message to the UI
            String rawBody = "";
            List<ServerProfile> servers = new ArrayList<>();
            try {
                var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.therspshub.com/api/servers/all.php"))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + LauncherEngine.sessionToken)
                    .header("X-Staff-Secret", "RH_STAFF_7xKq2mNvL9pWdY4z")
                    .GET().build();
                var res = java.net.http.HttpClient.newBuilder().build()
                    .send(req, HttpResponse.BodyHandlers.ofString());
                rawBody = res.body();
                System.out.println("[dev/all-servers] HTTP " + res.statusCode() + ": " + rawBody);
                JsonObject obj = JsonParser.parseString(rawBody).getAsJsonObject();
                if (obj.has("servers")) {
                    for (var el : obj.getAsJsonArray("servers"))
                        servers.add(GSON.fromJson(el, ServerProfile.class));
                }
                // If the endpoint returns a plain array (like list.php)
                else if (JsonParser.parseString(rawBody).isJsonArray()) {
                    for (var el : JsonParser.parseString(rawBody).getAsJsonArray())
                        servers.add(GSON.fromJson(el, ServerProfile.class));
                }
            } catch (Exception e) {
                rawBody = "Exception: " + e.getMessage();
                System.err.println("[dev/all-servers] error: " + e.getMessage());
            }
            // Fallback: if all.php failed/missing, use the public list.php so staff can still see live servers
            if (servers.isEmpty()) {
                try {
                    List<ServerProfile> fallback = ApiClient.getLiveServers().join();
                    if (!fallback.isEmpty()) {
                        System.out.println("[dev/all-servers] falling back to list.php (" + fallback.size() + " servers). all.php said: " + rawBody);
                        ctx.json(Map.of("servers", fallback, "fallback", true, "fallbackReason", rawBody));
                        return;
                    }
                } catch (Exception fe) { System.err.println("[dev/all-servers] fallback failed: " + fe.getMessage()); }
                ctx.json(Map.of("servers", List.of(), "error", rawBody));
            } else {
                ctx.json(Map.of("servers", servers));
            }
        });

        app.get("/api/dev/pending", ctx -> {
            String rawBody = "";
            List<ServerProfile> servers = new ArrayList<>();
            try {
                var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.therspshub.com/api/servers/pending.php"))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + LauncherEngine.sessionToken)
                    .header("X-Staff-Secret", "RH_STAFF_7xKq2mNvL9pWdY4z")
                    .GET().build();
                var res = java.net.http.HttpClient.newBuilder().build()
                    .send(req, HttpResponse.BodyHandlers.ofString());
                rawBody = res.body();
                System.out.println("[dev/pending] HTTP " + res.statusCode() + ": " + rawBody);
                JsonObject obj = JsonParser.parseString(rawBody).getAsJsonObject();
                if (obj.has("servers"))
                    for (var el : obj.getAsJsonArray("servers"))
                        servers.add(GSON.fromJson(el, ServerProfile.class));
            } catch (Exception e) {
                rawBody = "Exception: " + e.getMessage();
                System.err.println("[dev/pending] error: " + e.getMessage());
            }
            if (!servers.isEmpty()) {
                ctx.json(Map.of("servers", servers));
            } else {
                ctx.json(Map.of("servers", List.of(), "error", rawBody));
            }
        });

        app.post("/api/dev/submit", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            Map<String, Object> fields = new java.util.LinkedHashMap<>();
            for (String key : new String[]{"name","description","jar_url","xp_rate","tagline","accent_color",
                    "icon_url","banner_url","card_banner_url","website_url","discord_url","tags","changelog"}) {
                if (body.has(key) && !body.get(key).isJsonNull())
                    fields.put(key, body.get(key).getAsString());
            }
            String result = ApiClient.submitServer(fields).join();
            ctx.json(Map.of("result", result != null ? result : "submitted"));
        });

        app.post("/api/dev/update", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            int id = body.get("id").getAsInt();
            Map<String, Object> fields = new java.util.LinkedHashMap<>();
            String[] strKeys = {"name","tagline","description","jar_url","xp_rate","accent_color",
                "icon_url","banner_url","card_banner_url","website_url","discord_url","tags","changelog"};
            for (String key : strKeys) {
                if (body.has(key) && !body.get(key).isJsonNull())
                    fields.put(key, body.get(key).getAsString());
            }
            for (String key : new String[]{"visible","approved","players_online"}) {
                if (body.has(key) && !body.get(key).isJsonNull())
                    fields.put(key, body.get(key).getAsInt());
            }
            boolean ok = ApiClient.updateServer(id, fields).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Update failed"));
        });

        app.post("/api/dev/delete", ctx -> {
            int id = JsonParser.parseString(ctx.body()).getAsJsonObject().get("id").getAsInt();
            boolean ok = ApiClient.deleteServer(id).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Delete failed"));
        });

        app.post("/api/dev/approve", ctx -> {
            int id = JsonParser.parseString(ctx.body()).getAsJsonObject().get("id").getAsInt();
            boolean ok = ApiClient.approveServer(id).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Approve failed"));
        });

        app.post("/api/dev/reject", ctx -> {
            int id = JsonParser.parseString(ctx.body()).getAsJsonObject().get("id").getAsInt();
            boolean ok = ApiClient.rejectServer(id).join();
            if (ok) ctx.json(Map.of("success", true));
            else ctx.status(400).json(Map.of("error", "Reject failed"));
        });

        app.post("/api/dev/upload/icon", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String url = ApiClient.uploadIcon(body.get("serverId").getAsInt(), body.get("base64").getAsString()).join();
            if (url != null) ctx.json(Map.of("url", url));
            else ctx.status(500).json(Map.of("error", "Upload failed"));
        });

        app.post("/api/dev/upload/banner", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String url = ApiClient.uploadBanner(body.get("serverId").getAsInt(), body.get("base64").getAsString()).join();
            if (url != null) ctx.json(Map.of("url", url));
            else ctx.status(500).json(Map.of("error", "Upload failed"));
        });

        app.post("/api/dev/upload/card-banner", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String url = ApiClient.uploadCardBanner(body.get("serverId").getAsInt(), body.get("base64").getAsString()).join();
            if (url != null) ctx.json(Map.of("url", url));
            else ctx.status(500).json(Map.of("error", "Upload failed"));
        });

        app.post("/api/dev/claim", ctx -> {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String result = ApiClient.postJson("claim_server.php",
                GSON.toJson(Map.of(
                    "server_name", body.get("server_name").getAsString(),
                    "verify",      body.get("verify").getAsString(),
                    "username",    LauncherEngine.currentUsername
                ))
            ).join();
            ctx.json(Map.of("result", result != null ? result : "submitted"));
        });

        app.start(port);
        System.out.println("[ApiServer] Listening on port " + port);
    }

    // ── HELPERS ────────────────────────────────────────────────────────────────

    private static ServerProfile findServer(String name, Context ctx) {
        try {
            List<ServerProfile> servers = ApiClient.getLiveServers().join();
            return servers.stream()
                .filter(s -> s.name != null && s.name.equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    ctx.status(404).json(Map.of("error", "Server not found: " + name));
                    return null;
                });
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
            return null;
        }
    }
}
