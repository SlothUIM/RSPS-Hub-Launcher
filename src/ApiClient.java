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
    // Make sure your BASE_URL ends with a slash!
	// Notice the :4567 added here!
	private static final String BASE_URL = "http://api.rspshub.gg/api/";
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Generic POST method for sending JSON payloads.
     */
    public static CompletableFuture<String> postJson(String endpoint, String jsonPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    // ── STORE & SERVERS ─────────────────────────────────────────────────────

    /**
     * Fetches the live list of approved servers and their current online player counts.
     */
    /**
     * Fetches the live list of approved servers and their current online player counts.
     */
    public static CompletableFuture<List<ServerProfile>> getLiveServers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "servers"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // explicitly declare as List to satisfy Java's strict generic types
                    List<ServerProfile> serverList = new ArrayList<>(); 
                    
                    if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                        try {
                            serverList = new Gson().fromJson(response.body(), new TypeToken<List<ServerProfile>>(){}.getType());
                        } catch (Exception e) {
                            System.err.println("[ApiClient] Error parsing live servers JSON: " + e.getMessage());
                        }
                    }
                    return serverList;
                })
                .exceptionally(ex -> {
                    System.err.println("[ApiClient] Failed to fetch servers: " + ex.getMessage());
                    // Cast the fallback to List as well
                    return (List<ServerProfile>) new ArrayList<ServerProfile>(); 
                });
    }

    // ── SOCIAL HUB METHODS ──────────────────────────────────────────────────

    /**
     * Fetches the user's friends list, including their live online status and current server.
     */
    public static CompletableFuture<List<Friend>> getFriends(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "friends/" + username))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    List<Friend> friendsList = new ArrayList<>();
                    
                    if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                        try {
                            // Fixed: Using the older Gson syntax to prevent undefined method errors
                            JsonArray arr = new JsonParser().parse(response.body()).getAsJsonArray();
                            for (JsonElement el : arr) {
                                JsonObject obj = el.getAsJsonObject();
                                friendsList.add(new Friend(
                                    obj.get("username").getAsString(),
                                    obj.get("online").getAsBoolean(),
                                    obj.has("server") && !obj.get("server").isJsonNull() ? obj.get("server").getAsString() : ""
                                ));
                            }
                        } catch (Exception e) {
                            System.err.println("[ApiClient] Error parsing friends JSON: " + e.getMessage());
                        }
                    }
                    return friendsList;
                })
                .exceptionally(ex -> {
                    System.err.println("[ApiClient] Failed to fetch friends: " + ex.getMessage());
                    return new ArrayList<>(); 
                });
    }

    /**
     * Pushes the user's custom status and privacy settings to the global Hub API.
     */
    public static CompletableFuture<Boolean> updateProfileSettings(String username, String status, String privacy) {
        String payload = String.format("{\"username\":\"%s\", \"status\":\"%s\", \"privacy\":\"%s\"}", 
                username, status, privacy);
        
        return postJson("profile/update", payload)
                .thenApply(response -> response != null && !response.contains("error"))
                .exceptionally(ex -> {
                    System.err.println("[ApiClient] Failed to update profile: " + ex.getMessage());
                    return false;
                });
    }
}