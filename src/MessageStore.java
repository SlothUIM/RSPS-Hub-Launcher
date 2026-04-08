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
        // Seed mock data — overridden by persisted file if it exists
        store.put("PKMaster99", new ArrayList<>(List.of(
            new Message("PKMaster99", "yo you playing tonight?",      "10:32 AM", false),
            new Message("",           "yeah after work, around 8",    "10:35 AM", true),
            new Message("PKMaster99", "bet, meet me at edge",         "10:36 AM", false)
        )));

        store.put("IronmanJoe", new ArrayList<>(List.of(
            new Message("IronmanJoe", "nice server btw",              "Yesterday", false),
            new Message("",           "thanks man been grinding it",  "Yesterday", true)
        )));

        store.put("RSPS Gang", new ArrayList<>(List.of(
            new Message("PKMaster99", "anyone wanna boss?",           "9:15 AM", false),
            new Message("IronmanJoe", "im in",                        "9:16 AM", false),
            new Message("",           "give me 10 mins",              "9:20 AM", true)
        )));

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
