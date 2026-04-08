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

import com.google.gson.Gson;

public class LauncherEngine {

    private static final String API_URL = "http://slothscape.duckdns.org/mock_store.php?api=true";

    // Session
    public static String currentUsername = "";
    public static String avatarImagePath = null;

    // Settings
    public static String downloadPath = System.getProperty("user.home") + "/.rsps_hub/";
    public static boolean minimizeOnLaunch = false;
    public static boolean autoUpdateClients = false;
    public static String javaPath = "java";

    /**
     * Initializes the Hub directory on the user's PC.
     */
    public static void init() {
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
     * Downloads a specific server's client.
     * The UI will call this when 'Play' is clicked if the file is missing.
     */
    public static boolean downloadClient(ServerProfile server) {
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            Files.createDirectories(serverFolder);
            
            Path jarPath = serverFolder.resolve("SlothLite.jar");

            System.out.println("Downloading " + server.name + "...");
            
            URL jarUrl = new URL(server.jar_url);
            try (InputStream in = jarUrl.openStream()) {
                Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Launches the .jar for a server.
     */
    public static void launchGame(ServerProfile server) {
        try {
            Path serverFolder = Paths.get(downloadPath, server.name.replaceAll(" ", "_"));
            // Note: We use the serverFolder as the working directory so the game 
            // saves its own cache/settings in the right spot!
            ProcessBuilder pb = new ProcessBuilder(javaPath, "-jar", "SlothLite.jar");
            pb.directory(serverFolder.toFile());
            pb.start(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper to check if a server is already downloaded.
     */
    public static boolean isDownloaded(ServerProfile server) {
        Path jarPath = Paths.get(downloadPath, server.name.replaceAll(" ", "_"), "SlothLite.jar");
        return Files.exists(jarPath);
    }

    public static void main(String[] args) {
        // Now the main method just starts the Hub environment
        init();
        System.out.println("Engine ready. Waiting for UI controller...");
        
        // Next step: Start the UI!
    }
}