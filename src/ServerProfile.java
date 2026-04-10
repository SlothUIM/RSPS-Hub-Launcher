import java.util.List;
import com.google.gson.annotations.SerializedName;

public class ServerProfile {
    
    public int id;
    public String name;
    public String tagline;
    public String description;
    
    // We added this in the Developer Portal!
    @SerializedName("xp_rate")
    public String xpRate;

    @SerializedName("jar_url")
    public String jarUrl;

    @SerializedName("banner_url")
    public String bannerUrl;

    @SerializedName("icon_url")
    public String iconUrl;

    @SerializedName("discord_url")
    public String discordUrl;

    @SerializedName("website_url")
    public String websiteUrl;

    @SerializedName("accent_color")
    public String accentColor;

    public String changelog;

    @SerializedName("players_online")
    public int playersOnline;

    public List<String> tags;
    public List<String> screenshots;
    public boolean isNew;

    public int approved;
    public int visible;

    @SerializedName("submitted_by")
    public String submittedBy;

    @Override
    public String toString() {
        return name + " (" + playersOnline + " Online)";
    }
}