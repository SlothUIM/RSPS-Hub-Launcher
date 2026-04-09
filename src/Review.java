public class Review {
    public String username;
    public int stars;
    public String comment;
    public String date;
    public boolean pending; // true = flagged for moderation, not shown publicly

    public Review(String username, int stars, String comment, String date) {
        this.username = username;
        this.stars    = stars;
        this.comment  = comment;
        this.date     = date;
        this.pending  = false;
    }
}
