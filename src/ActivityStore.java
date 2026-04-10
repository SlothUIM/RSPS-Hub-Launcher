import java.util.ArrayList;
import java.util.List;

public class ActivityStore {

    private static final List<ActivityItem> feed = new ArrayList<>();

    public static List<ActivityItem> getFeed() { return feed; }

    public static void add(ActivityItem item) { feed.add(0, item); }
}
