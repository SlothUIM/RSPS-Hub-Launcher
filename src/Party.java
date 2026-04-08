import java.util.ArrayList;
import java.util.List;

public class Party {
    public String leader;
    public List<String> members = new ArrayList<>();

    public Party(String leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public void addMember(String username) {
        if (!members.contains(username)) members.add(username);
    }

    public void removeMember(String username) {
        members.remove(username);
    }
}
