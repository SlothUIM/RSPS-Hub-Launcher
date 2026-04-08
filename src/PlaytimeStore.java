import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;

public class PlaytimeStore {
    private static final Path PATH = Paths.get(System.getProperty("user.home"), ".rsps_hub", "playtime.json");

    private static Map<String, Long> minutesPlayed = new HashMap<>();
    private static Map<String, Integer> launchCount = new HashMap<>();

    static { load(); }

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
        try {
            Files.createDirectories(PATH.getParent());
            Data d = new Data();
            d.minutesPlayed = minutesPlayed;
            d.launchCount = launchCount;
            Files.writeString(PATH, new Gson().toJson(d));
        } catch (Exception e) { System.err.println("PlaytimeStore save failed: " + e.getMessage()); }
    }

    public static void load() {
        try {
            if (!Files.exists(PATH)) return;
            Data d = new Gson().fromJson(Files.readString(PATH), Data.class);
            if (d.minutesPlayed != null) minutesPlayed = d.minutesPlayed;
            if (d.launchCount != null) launchCount = d.launchCount;
        } catch (Exception e) { System.err.println("PlaytimeStore load failed: " + e.getMessage()); }
    }
}
