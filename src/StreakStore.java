import com.google.gson.Gson;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class StreakStore {

    private static final Path PATH = Paths.get(System.getProperty("user.home"), ".rsps_hub", "streaks.json");

    private static Map<String, String>  lastPlayed = new HashMap<>();
    private static Map<String, Integer> streaks    = new HashMap<>();

    static { load(); }

    public static void recordPlay(String serverName) {
        String today = LocalDate.now().toString();
        String last  = lastPlayed.get(serverName);

        if (last == null) {
            streaks.put(serverName, 1);
        } else if (last.equals(today)) {
            return; // already played today
        } else {
            LocalDate lastDate = LocalDate.parse(last);
            if (lastDate.plusDays(1).equals(LocalDate.now()))
                streaks.merge(serverName, 1, Integer::sum); // consecutive
            else
                streaks.put(serverName, 1); // streak broken
        }
        lastPlayed.put(serverName, today);
        save();
    }

    public static int getStreak(String serverName) {
        return streaks.getOrDefault(serverName, 0);
    }

    /** Seeds demo streak data only if the server has no existing streak. */
    public static void seedDemo(String serverName, int days) {
        streaks.putIfAbsent(serverName, days);
        lastPlayed.putIfAbsent(serverName, LocalDate.now().toString());
    }

    private static class Data { Map<String, String> lastPlayed; Map<String, Integer> streaks; }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Data d = new Data(); d.lastPlayed = lastPlayed; d.streaks = streaks;
            Files.writeString(PATH, new Gson().toJson(d));
        } catch (Exception e) { System.err.println("StreakStore save: " + e.getMessage()); }
    }

    public static void load() {
        try {
            if (!Files.exists(PATH)) return;
            Data d = new Gson().fromJson(Files.readString(PATH), Data.class);
            if (d.lastPlayed != null) lastPlayed = d.lastPlayed;
            if (d.streaks    != null) streaks    = d.streaks;
        } catch (Exception e) { System.err.println("StreakStore load: " + e.getMessage()); }
    }
}
