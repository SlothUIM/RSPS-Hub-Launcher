import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HubConnector {

    // The endpoint pointing to your VPS
    private static final String API_URL = "https://slothscape.duckdns.org/api/update_players";

    /**
     * Starts the background heartbeat to update the RSPS Hub.
     * * @param serverName The exact name of your server as registered on the Hub.
     * @param apiKey     Your secret API key from the Developer Portal.
     */
    public static void start(String serverName, String apiKey) { 
        System.out.println("[RSPS Hub] Connector initialized for: " + serverName);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                // 👉 DEV INSTRUCTION: Change this line to however your server counts players!
                // Example: World.getPlayers().size() OR PlayerHandler.getPlayerCount()
                int currentPlayers = 0; //PlayerHandler.getPlayerCount(); 

                // Safely format the URL (handles spaces in server names)
                String safeName = serverName.replace(" ", "%20");
                String requestUrl = API_URL + "?name=" + safeName 
                                  + "&count=" + currentPlayers 
                                  + "&key=" + apiKey;

                // Send the heartbeat
                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000); // 3-second timeout limits lag
                conn.getResponseCode();       // Executes the ping

            } catch (Exception e) {
                // Fails silently to prevent console spam if the Hub is restarting
            }
        }, 0, 1, TimeUnit.MINUTES); // Pings once every 60 seconds
    }
}