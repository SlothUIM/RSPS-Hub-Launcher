import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * For Server owners and developers as a drop in class
 * Just add HubConnector.init("YourServerName"); to your server startup and we handle the rest.
 */
public class HubConnector {
    public static void init(String serverName) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                /*
                 *  Swap 0 with 'PlayerHandler.getCount()' or your servers variable
                 */
                int count = 0;
                
                URL url = new URL("https://api.rspshub.gg/api/update_players?name="
                                   + serverName + "&count=" + count);
                url.openStream().close();
            } catch (Exception ignored) {}
        }, 1, 1, TimeUnit.MINUTES);
    }
}