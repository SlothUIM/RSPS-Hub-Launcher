public class ActivityItem {
    public String username;
    public String action;
    public String target;
    public String timestamp;

    public ActivityItem(String username, String action, String target, String timestamp) {
        this.username = username;
        this.action = action;
        this.target = target;
        this.timestamp = timestamp;
    }
}
