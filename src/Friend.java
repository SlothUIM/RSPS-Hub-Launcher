public class Friend {
    public String username;
    public boolean online;
    public String playingServer; // null if offline

    public Friend(String username, boolean online, String playingServer) {
        this.username = username;
        this.online = online;
        this.playingServer = playingServer;
    }
}
