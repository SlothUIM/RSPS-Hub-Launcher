import javafx.beans.property.*;

public class DownloadItem {
    public final String serverName;
    public final DoubleProperty progress = new SimpleDoubleProperty(0);
    public final StringProperty status   = new SimpleStringProperty("Queued");
    public volatile boolean cancelled    = false;

    public DownloadItem(String serverName) { this.serverName = serverName; }
}
