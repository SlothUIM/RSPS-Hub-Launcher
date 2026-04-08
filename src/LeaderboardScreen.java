import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.*;

public class LeaderboardScreen {

    private record LeaderEntry(int rank, String username, String topServer, long minutes, boolean isYou) {}

    public static Scene create(Stage stage, Runnable onBack) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // --- TOP BAR ---
        HBox topBar = new HBox();
        topBar.getStyleClass().add("settings-topbar");
        topBar.setPadding(new Insets(20, 40, 20, 40));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("settings-back-btn");
        backBtn.setOnAction(e -> onBack.run());

        Region leftSpacer  = new Region(); HBox.setHgrow(leftSpacer,  Priority.ALWAYS);
        Region rightSpacer = new Region(); HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        Label pageTitle = new Label("LEADERBOARD");
        pageTitle.getStyleClass().add("settings-page-title");

        topBar.getChildren().addAll(backBtn, leftSpacer, pageTitle, rightSpacer);
        root.setTop(new VBox(TitleBar.create(stage), topBar));

        // --- CONTENT ---
        VBox content = new VBox(10);
        content.setPadding(new Insets(30, 50, 50, 50));
        content.setMaxWidth(700);

        Label sub = new Label("Top players by total playtime across all RSPS Hub servers");
        sub.getStyleClass().add("settings-about-sub");
        sub.setWrapText(true);
        content.getChildren().add(sub);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2e39;");
        content.getChildren().add(sep);

        // Mock leaderboard data (mix of friends + you)
        String yourName = LauncherEngine.currentUsername.isEmpty() ? "You" : LauncherEngine.currentUsername;
        Map<String, Long> real = PlaytimeStore.getAllMinutes();
        long yourMinutes = real.values().stream().mapToLong(Long::longValue).sum();
        String yourTop = PlaytimeStore.getMostPlayed() != null ? PlaytimeStore.getMostPlayed() : "—";

        List<LeaderEntry> entries = new ArrayList<>(List.of(
            new LeaderEntry(1, "PKMaster99",   "SlothLite",  2840, false),
            new LeaderEntry(2, "IronmanJoe",   "MythicPS",   1920, false),
            new LeaderEntry(3, "ZulrahGrind",  "SlothLite",   980, false),
            new LeaderEntry(4, "Sasqu",        "NightmarePS", 720, false)
        ));

        // Insert the real player at the right position
        LeaderEntry youEntry = new LeaderEntry(0, yourName, yourTop, yourMinutes, true);
        entries.add(youEntry);
        entries.sort(Comparator.comparingLong(LeaderEntry::minutes).reversed());
        for (int i = 0; i < entries.size(); i++) {
            LeaderEntry e = entries.get(i);
            entries.set(i, new LeaderEntry(i + 1, e.username(), e.topServer(), e.minutes(), e.isYou()));
        }

        for (LeaderEntry entry : entries) {
            content.getChildren().add(buildRow(entry));
        }

        VBox wrapper = new VBox(content);
        wrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        root.setCenter(scroll);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(LeaderboardScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
        return scene;
    }

    private static HBox buildRow(LeaderEntry entry) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setMaxWidth(640);

        String rankColor = switch (entry.rank()) {
            case 1 -> "#ffd700";
            case 2 -> "#c0c0c0";
            case 3 -> "#cd7f32";
            default -> "#8b92a5";
        };

        Label rankLbl = new Label("#" + entry.rank());
        rankLbl.setStyle("-fx-text-fill: " + rankColor + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-min-width: 44;");

        String initial = entry.username().isEmpty() ? "?" : String.valueOf(entry.username().charAt(0)).toUpperCase();
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color: " + (entry.isYou() ? LauncherEngine.accentColor : "#2a2e39") + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-alignment: center;" +
            "-fx-min-width: 38; -fx-min-height: 38;" +
            "-fx-max-width: 38; -fx-max-height: 38;" +
            "-fx-background-radius: 20;"
        );
        avatar.setAlignment(Pos.CENTER);

        Label name = new Label(entry.username() + (entry.isYou() ? "  (you)" : ""));
        name.setStyle("-fx-text-fill: " + (entry.isYou() ? LauncherEngine.accentColor : "white") +
            "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label server = new Label(entry.topServer().equals("—") ? "No games played" : "Top: " + entry.topServer());
        server.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");

        VBox nameBox = new VBox(2, name, server);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        long h = entry.minutes() / 60;
        long m = entry.minutes() % 60;
        String timeStr = h > 0 ? h + "h " + m + "m" : m + "m";
        Label time = new Label(timeStr);
        time.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px; -fx-font-weight: bold;");

        row.getChildren().addAll(rankLbl, avatar, nameBox, time);

        // Highlight your row
        if (entry.isYou()) {
            row.setStyle("-fx-background-color: rgba(255,152,31,0.08); -fx-background-radius: 8; -fx-border-color: rgba(255,152,31,0.25); -fx-border-radius: 8;");
        } else {
            row.setStyle("-fx-background-color: #1a1d24; -fx-background-radius: 8;");
        }

        return row;
    }
}
