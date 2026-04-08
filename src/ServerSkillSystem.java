/**
 * Converts per-server playtime into a RuneScape-style skill level (1–99).
 *
 * Curve: square-root scaling so early levels come quickly and later ones take grinding.
 *   Level  1  =     0 minutes  (just started)
 *   Level 10  =   600 minutes  (10 hours)
 *   Level 32  =  6 000 minutes  (100 hours)
 *   Level 50  = 15 000 minutes  (250 hours)
 *   Level 70  = 30 000 minutes  (500 hours)
 *   Level 99  = 60 000 minutes  (1 000 hours)
 */
public class ServerSkillSystem {

    private static final long MAX_MINUTES = 60_000L; // 1 000 hours = level 99

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    public static int getLevel(String serverName) {
        return minutesToLevel(PlaytimeStore.getMinutes(serverName));
    }

    /** 0.0 → 1.0 progress within the current level (for XP bar). */
    public static double getLevelProgress(String serverName) {
        long minutes = PlaytimeStore.getMinutes(serverName);
        int level = minutesToLevel(minutes);
        if (level >= 99) return 1.0;
        long minCurrent = levelToMinutes(level);
        long minNext    = levelToMinutes(level + 1);
        if (minNext <= minCurrent) return 1.0;
        return (double)(minutes - minCurrent) / (minNext - minCurrent);
    }

    /** Minutes remaining until the next level (0 at level 99). */
    public static long minutesToNextLevel(String serverName) {
        long minutes = PlaytimeStore.getMinutes(serverName);
        int level = minutesToLevel(minutes);
        if (level >= 99) return 0;
        return levelToMinutes(level + 1) - minutes;
    }

    /** Human-readable XP tooltip text. */
    public static String getTooltip(String serverName) {
        int level = getLevel(serverName);
        if (level >= 99) return "Level 99 — MAX";
        long toNext = minutesToNextLevel(serverName);
        String time = toNext < 60 ? toNext + "m" : (toNext / 60) + "h " + (toNext % 60) + "m";
        return "Level " + level + " — " + time + " to next level";
    }

    public static String getRankName(int level) {
        if (level >= 99) return "LEGENDARY";
        if (level >= 90) return "GRANDMASTER";
        if (level >= 75) return "MASTER";
        if (level >= 60) return "ELITE";
        if (level >= 50) return "VETERAN";
        if (level >= 40) return "EXPERT";
        if (level >= 30) return "SKILLED";
        if (level >= 20) return "ADEPT";
        if (level >= 10) return "JOURNEYMAN";
        if (level >= 5)  return "APPRENTICE";
        return "NOVICE";
    }

    /** Accent color for the level badge based on milestone bracket. */
    public static String getMilestoneColor(int level) {
        if (level >= 99) return "#ffd700"; // gold
        if (level >= 75) return "#9b5de5"; // purple
        if (level >= 50) return "#ff981f"; // orange
        if (level >= 25) return "#4caf50"; // green
        if (level >= 10) return "#4a9eff"; // blue
        return "#8b92a5";                  // grey — no glow
    }

    public static boolean hasMilestoneGlow(int level) { return level >= 10; }

    // ── MATH ─────────────────────────────────────────────────────────────────

    static int minutesToLevel(long minutes) {
        if (minutes <= 0) return 1;
        double ratio = Math.min(1.0, (double) minutes / MAX_MINUTES);
        return (int) Math.min(99, 1 + Math.floor(98.0 * Math.sqrt(ratio)));
    }

    /** Inverse: minimum minutes required to reach the given level. */
    static long levelToMinutes(int level) {
        if (level <= 1) return 0;
        double ratio = (level - 1) / 98.0;
        return (long)(ratio * ratio * MAX_MINUTES);
    }
}
