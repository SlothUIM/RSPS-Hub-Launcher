public class FriendRequest {
    public String username;
    public boolean incoming; // true = they sent to us
    public String timestamp;

    public FriendRequest(String username, boolean incoming, String timestamp) {
        this.username = username;
        this.incoming = incoming;
        this.timestamp = timestamp;
    }
}
