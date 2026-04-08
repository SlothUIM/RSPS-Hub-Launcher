import java.util.List;

public class ServerProfile {
    public int id;
    public String name;
    public String description;
    public String jar_url;
    public String banner_url;
    public int players_online;
    public List<String> tags; // <--- THIS was missing!

    @Override
    public String toString() {
        return name + " (" + players_online + " Online)";
    }
}