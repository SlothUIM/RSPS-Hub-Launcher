import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ApiClient { 
    // CHANGE THIS to your actual DuckDNS domain!
    private static final String BASE_URL = "https://slothscape.duckdns.org/api/";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<String> postJson(String endpoint, String jsonPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
}