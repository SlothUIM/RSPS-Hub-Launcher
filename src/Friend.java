public class Friend {
    public String username;
    public boolean online;
    public String playingServer;
    public String statusMessage;
    public String nickname;

    public Friend(String username, boolean online, String playingServer) {
        this.username = username;
        this.online = online;
        this.playingServer = playingServer;
    }

    public Friend(String username, boolean online, String playingServer, String statusMessage) {
        this.username = username;
        this.online = online;
        this.playingServer = playingServer;
        this.statusMessage = statusMessage;
    }
}
