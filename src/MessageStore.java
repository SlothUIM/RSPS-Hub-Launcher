import java.util.*;

public class MessageStore {

    private static final Map<String, List<Message>> store = new HashMap<>();

    static {
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
    }

    public static List<Message> getMessages(String conversationId) {
        return store.computeIfAbsent(conversationId, k -> new ArrayList<>());
    }

    public static void addMessage(String conversationId, Message message) {
        getMessages(conversationId).add(message);
    }
}
