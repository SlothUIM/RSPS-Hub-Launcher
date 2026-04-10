import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class ApiClient {
<<<<<<< HEAD
	
	private static final String BASE_URL = "https://slothscape.duckdns.org/api/";
    private static final HttpClient client = HttpClient.newHttpClient();
=======

    private static final String BASE = "http://api.therspshub.com/api/";
    private static final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
>>>>>>> branch 'main' of https://github.com/SlothUIM/RSPS-Hub-Launcher.git
    private static final Gson gson = new Gson();

    // -- helpers --

    private static HttpRequest.Builder get(String endpoint) {
        return HttpRequest.newBuilder()
            .uri(URI.create(BASE + endpoint))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + LauncherEngine.sessionToken)
            .GET();
    }

    private static HttpRequest.Builder post(String endpoint, Object body) {
        return HttpRequest.newBuilder()
            .uri(URI.create(BASE + endpoint))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + LauncherEngine.sessionToken)
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)));
    }

    private static CompletableFuture<JsonObject> fetch(HttpRequest req) {
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(res -> {
                try { return new JsonParser().parse(res.body()).getAsJsonObject(); }
                catch (Exception e) { return new JsonObject(); }
            })
            .exceptionally(ex -> {
                System.err.println("API error: " + ex.getMessage());
                return new JsonObject();
            });
    }

<<<<<<< HEAD
    /**
     * Generic POST method for sending JSON payloads (unauthenticated — used by login/register).
     */
    public static CompletableFuture<String> postJson(String endpoint, String jsonPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // 👉 THIS IS THE MAGIC LINE: It prints exactly what the server says!
                    System.out.println("[API X-RAY] Response from " + endpoint + ": " + response.body());
                    return response.body();
                });
=======
    // unauthenticated POST — used by login/register screens
    public static CompletableFuture<String> postJson(String endpoint, String json) {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(BASE + endpoint))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body);
>>>>>>> branch 'main' of https://github.com/SlothUIM/RSPS-Hub-Launcher.git
    }

    // -- servers --

    public static CompletableFuture<List<ServerProfile>> getLiveServers() {
<<<<<<< HEAD
        HttpRequest request = HttpRequest.newBuilder()
        		.uri(URI.create("http://slothscape.duckdns.org:8080/api/servers"))
                .GET()
                .build();
=======
        var req = HttpRequest.newBuilder()
            .uri(URI.create(BASE + "servers/list.php"))
            .timeout(Duration.ofSeconds(15))
            .GET().build();
>>>>>>> branch 'main' of https://github.com/SlothUIM/RSPS-Hub-Launcher.git

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(res -> {
                List<ServerProfile> result = new ArrayList<>();
                try {
                    if (res.statusCode() == 200)
                        result = gson.fromJson(res.body(), new TypeToken<List<ServerProfile>>(){}.getType());
                } catch (Exception ignored) {}
                return result;
            })
            .exceptionally(ex -> new ArrayList<>());
    }

    // -- friends --

    public static CompletableFuture<List<Friend>> getFriends() {
        return fetch(get("friends/list.php").build()).thenApply(obj -> {
            List<Friend> list = new ArrayList<>();
            for (JsonElement el : arr(obj, "friends")) {
                JsonObject f = el.getAsJsonObject();
                boolean online = f.has("online_status") && f.get("online_status").getAsInt() == 1;
                String status  = str(f, "status_msg");
                list.add(new Friend(f.get("username").getAsString(), online, null, status));
            }
            return list;
        });
    }

    public static CompletableFuture<List<FriendRequest>> getFriendRequests() {
        return fetch(get("friends/requests.php").build()).thenApply(obj -> {
            List<FriendRequest> list = new ArrayList<>();
            for (JsonElement el : arr(obj, "requests")) {
                JsonObject r = el.getAsJsonObject();
                list.add(new FriendRequest(r.get("user_from").getAsString(), true, str(r, "created_at")));
            }
            return list;
        });
    }

    public static CompletableFuture<String> addFriend(String username) {
        return fetch(post("friends/add.php", Map.of("username", username)).build())
            .thenApply(obj -> obj.has("error") ? obj.get("error").getAsString() : "ok");
    }

    public static CompletableFuture<Boolean> acceptFriend(String username) {
        return fetch(post("friends/accept.php", Map.of("username", username)).build())
            .thenApply(obj -> obj.has("success") && obj.get("success").getAsBoolean());
    }

    public static CompletableFuture<Boolean> declineFriend(String username) {
        return fetch(post("friends/decline.php", Map.of("username", username)).build())
            .thenApply(obj -> !obj.has("error"));
    }

    public static CompletableFuture<Void> heartbeat() {
        return fetch(post("friends/heartbeat.php", Map.of()).build()).thenApply(o -> null);
    }

    // -- messages --

    public static CompletableFuture<Boolean> sendMessage(String to, String content) {
        return fetch(post("messages/send.php", Map.of("to", to, "content", content)).build())
            .thenApply(obj -> obj.has("success") && obj.get("success").getAsBoolean());
    }

    public static CompletableFuture<List<Message>> getMessages(String withUser) {
        return fetch(get("messages/list.php?with=" + withUser).build()).thenApply(obj -> {
            List<Message> list = new ArrayList<>();
            String me = LauncherEngine.currentUsername;
            for (JsonElement el : arr(obj, "messages")) {
                JsonObject m = el.getAsJsonObject();
                String sender = m.get("sender").getAsString();
                list.add(new Message(sender, m.get("content").getAsString(), str(m, "sent_at"), sender.equals(me)));
            }
            return list;
        });
    }

    public static CompletableFuture<List<JsonObject>> getConversations() {
        return fetch(get("messages/conversations.php").build()).thenApply(obj -> {
            List<JsonObject> list = new ArrayList<>();
            for (JsonElement el : arr(obj, "conversations")) list.add(el.getAsJsonObject());
            return list;
        });
    }

    // -- activity --

    public static CompletableFuture<List<ActivityItem>> getActivityFeed() {
        return fetch(get("activity/feed.php").build()).thenApply(obj -> {
            List<ActivityItem> list = new ArrayList<>();
            for (JsonElement el : arr(obj, "feed")) {
                JsonObject a = el.getAsJsonObject();
                list.add(new ActivityItem(
                    a.get("username").getAsString(),
                    a.get("action").getAsString(),
                    str(a, "target"),
                    str(a, "created_at")
                ));
            }
            return list;
        });
    }

    public static CompletableFuture<Void> logActivity(String action, String target) {
        return fetch(post("activity/log.php", Map.of("action", action, "target", target)).build())
            .thenApply(o -> null);
    }

    // -- users --

    public static CompletableFuture<List<String>> searchUsers(String query) {
        return fetch(get("users/search.php?q=" + query).build()).thenApply(obj -> {
            List<String> list = new ArrayList<>();
            for (JsonElement el : arr(obj, "users")) list.add(el.getAsString());
            return list;
        });
    }

    public static CompletableFuture<JsonObject> getUserStats(String username) {
        return fetch(get("users/stats.php?username=" + username).build());
    }

    public static CompletableFuture<Void> updateStats(long totalMinutes, int serversPlayed, String mostPlayed) {
        return fetch(post("users/update_stats.php", Map.of(
            "total_playtime_minutes", totalMinutes,
            "servers_played", serversPlayed,
            "most_played_server", mostPlayed
        )).build()).thenApply(o -> null);
    }

    public static CompletableFuture<String> uploadAvatar(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
                return java.util.Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                System.err.println("Avatar read failed: " + e.getMessage());
                return null;
            }
        }).thenCompose(b64 -> {
            if (b64 == null) return CompletableFuture.completedFuture(null);
            return fetch(post("users/upload_avatar.php", Map.of("image", b64)).build())
                .thenApply(obj -> obj.has("url") ? obj.get("url").getAsString() : null);
        });
    }

    // -- staff server management --

    public static CompletableFuture<List<ServerProfile>> getMyServers() {
        return fetch(get("servers/mine.php").build()).thenApply(obj -> {
            List<ServerProfile> list = new ArrayList<>();
            for (JsonElement el : arr(obj, "servers"))
                list.add(gson.fromJson(el, ServerProfile.class));
            return list;
        });
    }

    public static CompletableFuture<List<ServerProfile>> getAllServers() {
        return fetch(get("servers/all.php").build()).thenApply(obj -> {
            List<ServerProfile> list = new ArrayList<>();
            for (JsonElement el : arr(obj, "servers"))
                list.add(gson.fromJson(el, ServerProfile.class));
            return list;
        });
    }

    public static CompletableFuture<Boolean> updateServer(int id, Map<String, Object> fields) {
        Map<String, Object> payload = new HashMap<>(fields);
        payload.put("id", id);
        return fetch(post("servers/update.php", payload).build())
            .thenApply(obj -> !obj.has("error"));
    }

    public static CompletableFuture<String> uploadIcon(int serverId, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
                return java.util.Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                System.err.println("Icon read failed: " + e.getMessage());
                return null;
            }
        }).thenCompose(b64 -> {
            if (b64 == null) return CompletableFuture.completedFuture(null);
            Map<String, Object> payload = new HashMap<>();
            payload.put("image", b64);
            payload.put("server_id", serverId);
            return fetch(post("servers/upload_icon.php", payload).build())
                .thenApply(obj -> obj.has("url") ? obj.get("url").getAsString() : null);
        });
    }

    public static CompletableFuture<String> uploadBanner(int serverId, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
                return java.util.Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                System.err.println("Banner read failed: " + e.getMessage());
                return null;
            }
        }).thenCompose(b64 -> {
            if (b64 == null) return CompletableFuture.completedFuture(null);
            Map<String, Object> payload = new HashMap<>();
            payload.put("image", b64);
            payload.put("server_id", serverId);
            return fetch(post("servers/upload_banner.php", payload).build())
                .thenApply(obj -> obj.has("url") ? obj.get("url").getAsString() : null);
        });
    }

    public static CompletableFuture<Boolean> checkStaff() {
        return fetch(get("users/me.php").build())
            .thenApply(obj -> obj.has("is_staff") && obj.get("is_staff").getAsBoolean());
    }

    public static CompletableFuture<String> getMyEmail() {
        return fetch(get("users/me.php").build())
            .thenApply(obj -> str(obj, "email"));
    }

    public static CompletableFuture<String> updateEmail(String email) {
        return fetch(post("users/update_email.php", Map.of("email", email)).build())
            .thenApply(obj -> obj.has("error") ? obj.get("error").getAsString() : "ok");
    }

    public static String avatarUrl(String username) {
        return "http://api.therspshub.com/uploads/avatars/" + username + ".jpg";
    }

    // -- internal utils --

    private static JsonArray arr(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonArray() ? obj.getAsJsonArray(key) : new JsonArray();
    }

    private static String str(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
