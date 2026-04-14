import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;

public class PlaytimeStore {

    private static Map<String, Long>    minutesPlayed = new HashMap<>();
    private static Map<String, Integer> launchCount   = new HashMap<>();

    private static Path getPath() {
        String user = LauncherEngine.currentUsername;
        if (user == null || user.isEmpty())
            return Paths.get(System.getProperty("user.home"), ".rsps_hub", "playtime.json");
        return Paths.get(System.getProperty("user.home"), ".rsps_hub", user, "playtime.json");
    }

    public static void reload() {
        minutesPlayed = new HashMap<>();
        launchCount   = new HashMap<>();
        Path p = getPath();
        if (p == null || !Files.exists(p)) return;
        try {
            Data d = new Gson().fromJson(Files.readString(p), Data.class);
            if (d != null && d.minutesPlayed != null) minutesPlayed = d.minutesPlayed;
            if (d != null && d.launchCount   != null) launchCount   = d.launchCount;
        } catch (Exception e) { System.err.println("PlaytimeStore load: " + e.getMessage()); }
    }

    public static void recordSession(String serverName, long minutes) {
        if (minutes < 1) return;
        minutesPlayed.merge(serverName, minutes, Long::sum);
        launchCount.merge(serverName, 1, Integer::sum);
        save();
    }

    public static long getMinutes(String serverName) {
        return minutesPlayed.getOrDefault(serverName, 0L);
    }

    public static int getLaunchCount(String serverName) {
        return launchCount.getOrDefault(serverName, 0);
    }

    public static long getTotalMinutes() {
        return minutesPlayed.values().stream().mapToLong(Long::longValue).sum();
    }

    public static String getMostPlayed() {
        return minutesPlayed.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None yet");
    }

    public static Map<String, Long> getAllMinutes() {
        return Collections.unmodifiableMap(minutesPlayed);
    }

    public static int getTotalServersPlayed() {
        return minutesPlayed.size();
    }

    private static class Data { Map<String, Long> minutesPlayed; Map<String, Integer> launchCount; }

    public static void save() {
        Path p = getPath();
        if (p == null) return;
        try {
            Files.createDirectories(p.getParent());
            Data d = new Data();
            d.minutesPlayed = minutesPlayed;
            d.launchCount   = launchCount;
            Files.writeString(p, new Gson().toJson(d));
        } catch (Exception e) { System.err.println("PlaytimeStore save: " + e.getMessage()); }
    }
}
