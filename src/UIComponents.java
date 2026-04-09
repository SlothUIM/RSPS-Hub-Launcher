import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class UIComponents {

    public static HBox buildServerCard(ServerProfile server) {
        // --- 1. MAIN CARD CONTAINER ---
        HBox card = new HBox(20);
        card.setStyle(
            "-fx-background-color: #1a1d24; " +
            "-fx-background-radius: 8; " +
            "-fx-border-width: 0 0 4 0; " + // Thick border on the bottom only
            "-fx-border-color: " + (server.accentColor != null ? server.accentColor : "#ff981f") + "; " +
            "-fx-border-radius: 8;"
        );
        card.setPadding(new Insets(15));
        card.setMaxWidth(800);

        // --- 2. LEFT: BANNER/ICON BOX ---
        StackPane bannerBox = new StackPane();
        bannerBox.setPrefSize(180, 120);
        bannerBox.setStyle("-fx-background-color: #0f1115; -fx-background-radius: 6;");
        
        Label titleLogo = new Label(server.name);
        titleLogo.setStyle("-fx-text-fill: #555b6e; -fx-font-weight: bold;");
        
        if (server.isNew) {
            Label newBadge = new Label("NEW");
            newBadge.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
            StackPane.setAlignment(newBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(newBadge, new Insets(8));
            bannerBox.getChildren().add(newBadge);
        }

        // FIXED: Using server.name instead of server.id
        int currentStreak = StreakStore.getStreak(server.name);
        if (currentStreak > 0) {
            Label streakBadge = new Label("🔥 " + currentStreak);
            streakBadge.setStyle("-fx-background-color: #1a1d24; -fx-text-fill: #ff5252; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;");
            StackPane.setAlignment(streakBadge, Pos.BOTTOM_LEFT);
            StackPane.setMargin(streakBadge, new Insets(8));
            bannerBox.getChildren().add(streakBadge);
        }
        
        bannerBox.getChildren().add(titleLogo);

        // --- 3. MIDDLE: INFO SECTION ---
        VBox infoBox = new VBox(8);
        HBox.setHgrow(infoBox, Priority.ALWAYS); // Push everything else to the right

        Label title = new Label(server.name);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label desc = new Label(server.description);
        desc.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
        desc.setWrapText(true);

        HBox tagsBox = new HBox(6);
        if (server.tags != null) {
            for (String tag : server.tags) {
                Label t = new Label(tag.toUpperCase());
                t.setStyle("-fx-background-color: #2a2e39; -fx-text-fill: #8b92a5; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                tagsBox.getChildren().add(t);
            }
        }

        infoBox.getChildren().addAll(title, desc, tagsBox);

        // --- 4. RIGHT: ACTION SECTION ---
        VBox actionBox = new VBox(0);
        actionBox.setAlignment(Pos.TOP_RIGHT);
        actionBox.setPrefWidth(140);

        // Top Right: Level Badge & Icons
        HBox topRight = new HBox(10);
        topRight.setAlignment(Pos.CENTER_RIGHT);
        
        // FIXED: Using server.name instead of server.id
        int level = ServerSkillSystem.getLevel(server.name);
        Label lvlBadge = new Label("Lv. " + level);
        // Add a faint green glow if they are over level 10, just like your design!
        String glow = ServerSkillSystem.hasMilestoneGlow(level) ? "-fx-effect: dropshadow(three-pass-box, rgba(76, 175, 80, 0.4), 10, 0, 0, 0);" : "";
        lvlBadge.setStyle("-fx-background-color: rgba(76, 175, 80, 0.1); -fx-border-color: #4caf50; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #4caf50; -fx-padding: 2 8; " + glow);
        
        topRight.getChildren().addAll(lvlBadge);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS); // Pushes the bottom row down

        // Bottom Right: Online Count & Play Button
        VBox bottomRight = new VBox(8);
        bottomRight.setAlignment(Pos.BOTTOM_RIGHT);

        Label onlineCount = new Label("🟢 " + server.playersOnline + " Online");
        onlineCount.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px;");

        Button playBtn = new Button("PLAY");
        playBtn.setStyle("-fx-background-color: #9b5de5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 6; -fx-cursor: hand;");
        playBtn.setMaxWidth(Double.MAX_VALUE); // Fill width
        
        playBtn.setOnAction(e -> {
            // Disable button so they don't spam click
            playBtn.setText("LAUNCHING...");
            playBtn.setDisable(true);
            
            // Note: Since you integrated LauncherEngine in RSPSHub, 
            // you might handle the actual launching back in your main hub logic,
            // but if this is a standalone component, you'd trigger it here!
            
            // And log the playtime!
            // PlaytimeStore.recordSession(server.name, 1);
        });

        bottomRight.getChildren().addAll(onlineCount, playBtn);
        actionBox.getChildren().addAll(topRight, spacer, bottomRight);

        // --- 5. ASSEMBLE ---
        card.getChildren().addAll(bannerBox, infoBox, actionBox);
        return card;
    }
}