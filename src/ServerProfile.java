import java.util.List;

public class ServerProfile {
    public int id;
    public String name;
    public String tagline;
    public String description;
    public String jar_url;
    public String banner_url;
    public String icon_url;
    public String discord_url;
    public String website_url;
    public String accent_color;
    public String changelog;
    public int players_online;
    public List<String> tags;
    public List<String> screenshots;

    @Override
    public String toString() {
        return name + " (" + players_online + " Online)";
    }
}
