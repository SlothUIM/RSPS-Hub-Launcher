import java.util.ArrayList;
import java.util.List;

public class ActivityStore {

    private static final List<ActivityItem> feed = new ArrayList<>(List.of(
        new ActivityItem("PKMaster99",  "installed",        "MythicPS",  "2m ago"),
        new ActivityItem("IronmanJoe",  "left a review on", "SlothLite", "15m ago"),
        new ActivityItem("PKMaster99",  "is now playing",   "SlothLite", "1h ago"),
        new ActivityItem("ZulrahGrind", "joined RSPS Hub",  "",          "3h ago"),
        new ActivityItem("IronmanJoe",  "installed",        "MythicPS",  "Yesterday")
    ));

    public static List<ActivityItem> getFeed() { return feed; }

    public static void add(ActivityItem item) { feed.add(0, item); }
}
