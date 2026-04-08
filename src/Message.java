public class Message {
    public String sender;
    public String content;
    public String timestamp;
    public boolean isOwn;

    public Message(String sender, String content, String timestamp, boolean isOwn) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.isOwn = isOwn;
    }
}
