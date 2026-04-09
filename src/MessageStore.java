import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MessageStore {

    private static final Path MESSAGES_PATH =
        Paths.get(System.getProperty("user.home"), ".rsps_hub", "messages.json");

    private static final Map<String, List<Message>> store = new HashMap<>();

    static {
        load();
    }

    public static List<Message> getMessages(String conversationId) {
        return store.computeIfAbsent(conversationId, k -> new ArrayList<>());
    }

    public static void addMessage(String conversationId, Message message) {
        getMessages(conversationId).add(message);
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(MESSAGES_PATH.getParent());
            Files.writeString(MESSAGES_PATH, new Gson().toJson(store));
        } catch (Exception e) {
            System.err.println("Failed to save messages: " + e.getMessage());
        }
    }

    public static void load() {
        try {
            if (!Files.exists(MESSAGES_PATH)) return;
            Type type = new TypeToken<Map<String, List<Message>>>(){}.getType();
            Map<String, List<Message>> loaded = new Gson().fromJson(Files.readString(MESSAGES_PATH), type);
            if (loaded != null) {
                store.clear();
                store.putAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Failed to load messages: " + e.getMessage());
        }
    }
}
