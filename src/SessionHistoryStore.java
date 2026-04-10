import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class SessionHistoryStore {

    public static class SessionRecord {
        public String serverName;
        public String date;
        public long   minutes;
        public SessionRecord() {}
        public SessionRecord(String serverName, String date, long minutes) {
            this.serverName = serverName;
            this.date       = date;
            this.minutes    = minutes;
        }
    }

    private static final int MAX = 50;
    private static List<SessionRecord> history = new ArrayList<>();

    private static Path getPath() {
        String user = LauncherEngine.currentUsername;
        if (user == null || user.isEmpty()) return null;
        return Paths.get(System.getProperty("user.home"), ".rsps_hub", user, "session_history.json");
    }

    public static void reload() {
        history = new ArrayList<>();
        Path p = getPath();
        if (p == null || !Files.exists(p)) return;
        try {
            List<SessionRecord> loaded = new Gson().fromJson(
                Files.readString(p),
                new TypeToken<List<SessionRecord>>(){}.getType()
            );
            if (loaded != null) history = loaded;
        } catch (Exception e) { System.err.println("SessionHistoryStore load: " + e.getMessage()); }
    }

    public static void add(String serverName, long minutes) {
        history.add(0, new SessionRecord(serverName, LocalDate.now().toString(), minutes));
        if (history.size() > MAX) history = new ArrayList<>(history.subList(0, MAX));
        save();
    }

    public static List<SessionRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    private static void save() {
        Path p = getPath();
        if (p == null) return;
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, new Gson().toJson(history));
        } catch (Exception e) { System.err.println("SessionHistoryStore save: " + e.getMessage()); }
    }
}
