import com.google.gson.Gson;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class StreakStore {

    private static Map<String, String>  lastPlayed = new HashMap<>();
    private static Map<String, Integer> streaks    = new HashMap<>();

    private static Path getPath() {
        String user = LauncherEngine.currentUsername;
        if (user == null || user.isEmpty()) return null;
        return Paths.get(System.getProperty("user.home"), ".rsps_hub", user, "streaks.json");
    }

    public static void reload() {
        lastPlayed = new HashMap<>();
        streaks    = new HashMap<>();
        Path p = getPath();
        if (p == null || !Files.exists(p)) return;
        try {
            Data d = new Gson().fromJson(Files.readString(p), Data.class);
            if (d != null && d.lastPlayed != null) lastPlayed = d.lastPlayed;
            if (d != null && d.streaks    != null) streaks    = d.streaks;
        } catch (Exception e) { System.err.println("StreakStore load: " + e.getMessage()); }
    }

    public static void recordPlay(String serverName) {
        String today = LocalDate.now().toString();
        String last  = lastPlayed.get(serverName);

        if (last == null) {
            streaks.put(serverName, 1);
        } else if (last.equals(today)) {
            return;
        } else {
            LocalDate lastDate = LocalDate.parse(last);
            if (lastDate.plusDays(1).equals(LocalDate.now()))
                streaks.merge(serverName, 1, Integer::sum);
            else
                streaks.put(serverName, 1);
        }
        lastPlayed.put(serverName, today);
        save();
    }

    public static int getStreak(String serverName) {
        return streaks.getOrDefault(serverName, 0);
    }

    private static class Data { Map<String, String> lastPlayed; Map<String, Integer> streaks; }

    public static void save() {
        Path p = getPath();
        if (p == null) return;
        try {
            Files.createDirectories(p.getParent());
            Data d = new Data(); d.lastPlayed = lastPlayed; d.streaks = streaks;
            Files.writeString(p, new Gson().toJson(d));
        } catch (Exception e) { System.err.println("StreakStore save: " + e.getMessage()); }
    }
}
