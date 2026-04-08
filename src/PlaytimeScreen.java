import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.*;

public class PlaytimeScreen {

    public static Scene create(Stage stage, Runnable onBack) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // --- TOP BAR ---
        HBox topBar = new HBox();
        topBar.getStyleClass().add("settings-topbar");
        topBar.setPadding(new Insets(20, 40, 20, 40));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("settings-back-btn");
        backBtn.setOnAction(e -> onBack.run());

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        Label pageTitle = new Label("PLAYTIME STATS");
        pageTitle.getStyleClass().add("settings-page-title");

        topBar.getChildren().addAll(backBtn, leftSpacer, pageTitle, rightSpacer);
        root.setTop(new VBox(TitleBar.create(stage), topBar));

        // --- CONTENT ---
        VBox content = new VBox(32);
        content.setPadding(new Insets(40, 60, 60, 60));
        content.setMaxWidth(760);

        // Big stats row
        long totalMinutes = PlaytimeStore.getTotalMinutes();
        int serversPlayed = PlaytimeStore.getTotalServersPlayed();
        String mostPlayed = PlaytimeStore.getMostPlayed();

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
            statBox(formatMinutes(totalMinutes), "Total Time"),
            statBox(String.valueOf(serversPlayed), "Servers Played"),
            statBox(mostPlayed, "Most Played")
        );

        content.getChildren().add(statsRow);

        // Per-server section
        Label sectionHeader = new Label("PER SERVER");
        sectionHeader.getStyleClass().add("friends-section-header");
        content.getChildren().add(sectionHeader);

        Map<String, Long> allMinutes = PlaytimeStore.getAllMinutes();
        if (allMinutes.isEmpty()) {
            Label empty = new Label("No playtime recorded yet. Start playing!");
            empty.getStyleClass().add("auth-muted");
            content.getChildren().add(empty);
        } else {
            // Sort by minutes descending
            List<Map.Entry<String, Long>> sorted = new ArrayList<>(allMinutes.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            long maxMinutes = sorted.get(0).getValue();

            VBox serverList = new VBox(12);
            for (Map.Entry<String, Long> entry : sorted) {
                serverList.getChildren().add(buildServerRow(entry.getKey(), entry.getValue(), maxMinutes));
            }
            content.getChildren().add(serverList);
        }

        VBox contentWrapper = new VBox(content);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setPadding(new Insets(0, 0, 40, 0));

        ScrollPane scrollPane = new ScrollPane(contentWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("main-scroll");
        root.setCenter(scrollPane);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(PlaytimeScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
        return scene;
    }

    private static VBox statBox(String value, String label) {
        VBox box = new VBox(4);
        box.getStyleClass().add("profile-stat-box");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setPrefWidth(180);

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("profile-stat-value");
        valLbl.setWrapText(true);
        valLbl.setMaxWidth(160);
        valLbl.setAlignment(Pos.CENTER);

        Label lblLbl = new Label(label);
        lblLbl.getStyleClass().add("profile-stat-label");

        box.getChildren().addAll(valLbl, lblLbl);
        return box;
    }

    private static HBox buildServerRow(String serverName, long minutes, long maxMinutes) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        Label nameLbl = new Label(serverName);
        nameLbl.getStyleClass().add("stats-server-name");
        nameLbl.setMinWidth(160);
        nameLbl.setMaxWidth(160);

        ProgressBar bar = new ProgressBar(maxMinutes > 0 ? (double) minutes / maxMinutes : 0);
        bar.getStyleClass().add("stats-bar-track");
        bar.setStyle("-fx-accent: #ff981f;");
        bar.setPrefWidth(300);
        bar.setPrefHeight(8);
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label timeLbl = new Label(formatMinutes(minutes));
        timeLbl.getStyleClass().add("stats-time-label");
        timeLbl.setMinWidth(70);

        row.getChildren().addAll(nameLbl, bar, timeLbl);
        return row;
    }

    private static String formatMinutes(long minutes) {
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        long mins = minutes % 60;
        return mins > 0 ? hours + "h " + mins + "m" : hours + "h";
    }
}
