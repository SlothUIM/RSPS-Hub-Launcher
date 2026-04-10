import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class ApiClient {
	
	private static final String BASE_URL = "https://slothscape.duckdns.org/api/";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    // ── REQUEST BUILDERS ────────────────────────────────────────────────────────

    private static HttpRequest.Builder authedGet(String endpoint) {
        return HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + endpoint))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + LauncherEngine.sessionToken)
            .GET();
    }

    private static HttpRequest.Builder authedPost(String endpoint, String json) {
        return HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + endpoint))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + LauncherEngine.sessionToken)
            .POST(HttpRequest.BodyPublishers.ofString(json));
    }

    private static CompletableFuture<JsonObject> sendAndParse(HttpRequest req) {
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(res -> {
                try {
                    return new JsonParser().parse(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    System.err.println("[ApiClient] Parse error: " + e.getMessage() + " | body: " + res.body());
                    return new JsonObject();
                }
            })
            .exceptionally(ex -> {
                System.err.println("[ApiClient] Request failed: " + ex.getMessage());
                return new JsonObject();
            });
    }

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
    }

    // ── SERVERS ──────────────────────────────────────────────────────────────────

    public static CompletableFuture<List<ServerProfile>> getLiveServers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "servers/list.php"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    List<ServerProfile> serverList = new ArrayList<>();
                    if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                        try {
                            serverList = gson.fromJson(response.body(), new TypeToken<List<ServerProfile>>(){}.getType());
                        } catch (Exception e) {
                            System.err.println("[ApiClient] Error parsing live servers JSON: " + e.getMessage());
                        }
                    }
                    return serverList;
                })
                .exceptionally(ex -> {
                    System.err.println("[ApiClient] Failed to fetch servers: " + ex.getMessage());
                    return new ArrayList<>();
                });
    }

    // ── FRIENDS ──────────────────────────────────────────────────────────────────

    /** GET friends/list.php — returns accepted friends with online status. */
    public static CompletableFuture<List<Friend>> getFriends() {
        return sendAndParse(authedGet("friends/list.php").build())
            .thenApply(obj -> {
                List<Friend> list = new ArrayList<>();
                JsonArray arr = obj.has("friends") ? obj.getAsJsonArray("friends") : new JsonArray();
                for (JsonElement el : arr) {
                    JsonObject f = el.getAsJsonObject();
                    String username   = f.get("username").getAsString();
                    boolean online    = f.has("online_status") && f.get("online_status").getAsInt() == 1;
                    String statusMsg  = f.has("status_msg") && !f.get("status_msg").isJsonNull()
                                        ? f.get("status_msg").getAsString() : "";
                    list.add(new Friend(username, online, null, statusMsg));
                }
                return list;
            });
    }

    /** GET friends/requests.php — returns incoming pending requests. */
    public static CompletableFuture<List<FriendRequest>> getFriendRequests() {
        return sendAndParse(authedGet("friends/requests.php").build())
            .thenApply(obj -> {
                List<FriendRequest> list = new ArrayList<>();
                JsonArray arr = obj.has("requests") ? obj.getAsJsonArray("requests") : new JsonArray();
                for (JsonElement el : arr) {
                    JsonObject r = el.getAsJsonObject();
                    String from = r.get("user_from").getAsString();
                    String at   = r.has("created_at") ? r.get("created_at").getAsString() : "";
                    list.add(new FriendRequest(from, true, at));
                }
                return list;
            });
    }

    /** POST friends/add.php — send a friend request. */
    public static CompletableFuture<String> addFriend(String username) {
        String json = "{\"username\":\"" + username + "\"}";
        return sendAndParse(authedPost("friends/add.php", json).build())
            .thenApply(obj -> {
                if (obj.has("error")) return obj.get("error").getAsString();
                return "ok";
            });
    }

    /** POST friends/accept.php — accept a pending request. */
    public static CompletableFuture<Boolean> acceptFriend(String username) {
        String json = "{\"username\":\"" + username + "\"}";
        return sendAndParse(authedPost("friends/accept.php", json).build())
            .thenApply(obj -> obj.has("success") && obj.get("success").getAsBoolean());
    }

    /** POST friends/decline.php — decline or cancel a friend request / remove friend. */
    public static CompletableFuture<Boolean> declineFriend(String username) {
        String json = "{\"username\":\"" + username + "\"}";
        return sendAndParse(authedPost("friends/decline.php", json).build())
            .thenApply(obj -> !obj.has("error"));
    }

    /** POST friends/heartbeat.php — keeps online_status alive (call every ~60s). */
    public static CompletableFuture<Void> heartbeat() {
        return sendAndParse(authedPost("friends/heartbeat.php", "{}").build())
            .thenApply(obj -> null);
    }

    // ── MESSAGES ─────────────────────────────────────────────────────────────────

    /** POST messages/send.php — send a message to another user. */
    public static CompletableFuture<Boolean> sendMessage(String to, String content) {
        String safe = content.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"to\":\"" + to + "\",\"content\":\"" + safe + "\"}";
        return sendAndParse(authedPost("messages/send.php", json).build())
            .thenApply(obj -> obj.has("success") && obj.get("success").getAsBoolean());
    }

    /** GET messages/list.php?with=username — load conversation. */
    public static CompletableFuture<List<Message>> getMessages(String withUser) {
        return sendAndParse(authedGet("messages/list.php?with=" + withUser).build())
            .thenApply(obj -> {
                List<Message> list = new ArrayList<>();
                JsonArray arr = obj.has("messages") ? obj.getAsJsonArray("messages") : new JsonArray();
                String me = LauncherEngine.currentUsername;
                for (JsonElement el : arr) {
                    JsonObject m = el.getAsJsonObject();
                    String sender  = m.get("sender").getAsString();
                    String content = m.get("content").getAsString();
                    String sentAt  = m.get("sent_at").getAsString();
                    boolean isOwn  = sender.equals(me);
                    list.add(new Message(sender, content, sentAt, isOwn));
                }
                return list;
            });
    }

    /** GET messages/conversations.php — list all conversations with last message + unread. */
    public static CompletableFuture<List<JsonObject>> getConversations() {
        return sendAndParse(authedGet("messages/conversations.php").build())
            .thenApply(obj -> {
                List<JsonObject> list = new ArrayList<>();
                JsonArray arr = obj.has("conversations") ? obj.getAsJsonArray("conversations") : new JsonArray();
                for (JsonElement el : arr) list.add(el.getAsJsonObject());
                return list;
            });
    }

    // ── ACTIVITY ─────────────────────────────────────────────────────────────────

    /** GET activity/feed.php — activity from me + friends. */
    public static CompletableFuture<List<ActivityItem>> getActivityFeed() {
        return sendAndParse(authedGet("activity/feed.php").build())
            .thenApply(obj -> {
                List<ActivityItem> list = new ArrayList<>();
                JsonArray arr = obj.has("feed") ? obj.getAsJsonArray("feed") : new JsonArray();
                for (JsonElement el : arr) {
                    JsonObject a = el.getAsJsonObject();
                    String username  = a.get("username").getAsString();
                    String action    = a.get("action").getAsString();
                    String target    = a.has("target") && !a.get("target").isJsonNull() ? a.get("target").getAsString() : "";
                    String createdAt = a.has("created_at") ? a.get("created_at").getAsString() : "";
                    list.add(new ActivityItem(username, action, target, createdAt));
                }
                return list;
            });
    }

    /** POST activity/log.php — record an action. */
    public static CompletableFuture<Void> logActivity(String action, String target) {
        String safeAction = action.replace("\"", "\\\"");
        String safeTarget = target.replace("\"", "\\\"");
        String json = "{\"action\":\"" + safeAction + "\",\"target\":\"" + safeTarget + "\"}";
        return sendAndParse(authedPost("activity/log.php", json).build())
            .thenApply(obj -> null);
    }

    // ── USER SEARCH ──────────────────────────────────────────────────────────────

    /** GET users/search.php?q=query — username prefix search. */
    public static CompletableFuture<List<String>> searchUsers(String query) {
        return sendAndParse(authedGet("users/search.php?q=" + query).build())
            .thenApply(obj -> {
                List<String> list = new ArrayList<>();
                JsonArray arr = obj.has("users") ? obj.getAsJsonArray("users") : new JsonArray();
                for (JsonElement el : arr) list.add(el.getAsString());
                return list;
            });
    }

    // ── USER STATS & AVATARS ─────────────────────────────────────────────────────

    /** GET users/stats.php?username=X — returns playtime stats for any user. */
    public static CompletableFuture<JsonObject> getUserStats(String username) {
        return sendAndParse(authedGet("users/stats.php?username=" + username).build());
    }

    /** POST users/update_stats.php — push local playtime totals to the server. */
    public static CompletableFuture<Void> updateStats(long totalMinutes, int serversPlayed, String mostPlayed) {
        String safe = mostPlayed.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"total_playtime_minutes\":" + totalMinutes
            + ",\"servers_played\":" + serversPlayed
            + ",\"most_played_server\":\"" + safe + "\"}";
        return sendAndParse(authedPost("users/update_stats.php", json).build())
            .thenApply(obj -> null);
    }

    /**
     * POST users/upload_avatar.php — upload profile picture as base64.
     * Returns the public URL of the saved image, or null on failure.
     */
    public static CompletableFuture<String> uploadAvatar(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
                String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
                return "{\"image\":\"" + b64 + "\"}";
            } catch (Exception e) {
                System.err.println("[ApiClient] uploadAvatar read failed: " + e.getMessage());
                return null;
            }
        }).thenCompose(json -> {
            if (json == null) return CompletableFuture.completedFuture((String) null);
            return sendAndParse(authedPost("users/upload_avatar.php", json).build())
                .thenApply(obj -> obj.has("url") ? obj.get("url").getAsString() : null);
        });
    }

    /** Returns the avatar URL for any username (no request needed — deterministic). */
    public static String avatarUrl(String username) {
        return "http://api.therspshub.com/uploads/avatars/" + username + ".jpg";
    }
}
