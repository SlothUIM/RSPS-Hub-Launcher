import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class MessageStore {

    private static final Map<String, List<Message>> store = new HashMap<>();

    private static Path getPath() {
        String user = LauncherEngine.currentUsername;
        if (user == null || user.isEmpty()) return null;
        return Paths.get(System.getProperty("user.home"), ".rsps_hub", user, "messages.json");
    }

    public static void reload() {
        store.clear();
        Path p = getPath();
        if (p == null || !Files.exists(p)) return;
        try {
            Type type = new TypeToken<Map<String, List<Message>>>(){}.getType();
            Map<String, List<Message>> loaded = new Gson().fromJson(Files.readString(p), type);
            if (loaded != null) store.putAll(loaded);
        } catch (Exception e) { System.err.println("MessageStore load: " + e.getMessage()); }
    }

    public static List<Message> getMessages(String conversationId) {
        return store.computeIfAbsent(conversationId, k -> new ArrayList<>());
    }

    public static void addMessage(String conversationId, Message message) {
        getMessages(conversationId).add(message);
        save();
    }

    public static void save() {
        Path p = getPath();
        if (p == null) return;
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, new Gson().toJson(store));
        } catch (Exception e) { System.err.println("MessageStore save: " + e.getMessage()); }
    }
}
