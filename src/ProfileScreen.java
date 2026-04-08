import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;

public class ProfileScreen {

    public static Scene create(Stage stage, String username, boolean isOnline,
                               String statusMessage, List<String> blockedUsers,
                               Runnable onBack, Runnable onMessage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Top bar
        HBox topBar = new HBox();
        topBar.getStyleClass().add("settings-topbar");
        topBar.setPadding(new Insets(16, 40, 16, 40));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("settings-back-btn");
        backBtn.setOnAction(e -> onBack.run());
        topBar.getChildren().add(backBtn);

        root.setTop(new VBox(TitleBar.create(stage), topBar));

        // Avatar + identity
        String initial = username.isEmpty() ? "?" : String.valueOf(username.charAt(0)).toUpperCase();
        Label avatar = new Label(initial);
        avatar.getStyleClass().add("profile-avatar");

        Label nameLbl = new Label(username);
        nameLbl.getStyleClass().add("profile-username");

        Label onlineLbl = new Label(isOnline ? "● Online" : "○ Offline");
        onlineLbl.getStyleClass().add(isOnline ? "friend-status-online" : "friend-status-offline");

        VBox identity = new VBox(6, nameLbl, onlineLbl);
        identity.setAlignment(Pos.CENTER);

        if (statusMessage != null && !statusMessage.isEmpty()) {
            Label statusLbl = new Label("\"" + statusMessage + "\"");
            statusLbl.getStyleClass().add("profile-status-msg");
            identity.getChildren().add(statusLbl);
        }

        VBox profileHeader = new VBox(16, avatar, identity);
        profileHeader.setAlignment(Pos.CENTER);

        // Stats
        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
            statBox("Member Since", "2024"),
            statBox("Servers Played", "4"),
            statBox("Reviews Written", "2")
        );

        // Action buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Button msgBtn = new Button("Send Message");
        msgBtn.getStyleClass().add("auth-btn");
        msgBtn.setPrefWidth(160);
        msgBtn.setOnAction(e -> onMessage.run());

        boolean isBlocked = blockedUsers.contains(username);
        Button blockBtn = new Button(isBlocked ? "Unblock" : "Block User");
        blockBtn.getStyleClass().add(isBlocked ? "settings-secondary-btn" : "settings-logout-btn");
        blockBtn.setOnAction(e -> {
            if (blockedUsers.contains(username)) {
                blockedUsers.remove(username);
                blockBtn.setText("Block User");
                blockBtn.getStyleClass().setAll("settings-logout-btn");
            } else {
                blockedUsers.add(username);
                blockBtn.setText("Unblock");
                blockBtn.getStyleClass().setAll("settings-secondary-btn");
            }
        });

        actions.getChildren().addAll(msgBtn, blockBtn);

        // Recent activity
        VBox activitySection = new VBox(0);
        Label activityHeader = new Label("RECENT ACTIVITY");
        activityHeader.getStyleClass().add("friends-section-header");
        activityHeader.setPadding(new Insets(0, 0, 10, 0));
        activitySection.getChildren().add(activityHeader);

        boolean hasActivity = false;
        for (ActivityItem item : ActivityStore.getFeed()) {
            if (item.username.equals(username)) {
                activitySection.getChildren().add(buildActivityRow(item));
                hasActivity = true;
            }
        }
        if (!hasActivity) {
            Label none = new Label("No recent activity.");
            none.getStyleClass().add("auth-muted");
            activitySection.getChildren().add(none);
        }

        // Main content
        VBox content = new VBox(28, profileHeader, stats, actions, activitySection);
        content.setPadding(new Insets(50, 60, 60, 60));
        content.setMaxWidth(680);

        VBox wrapper = new VBox(content);
        wrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        root.setCenter(scroll);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(ProfileScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
        return scene;
    }

    private static VBox statBox(String label, String value) {
        Label val = new Label(value);
        val.getStyleClass().add("profile-stat-value");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("profile-stat-label");
        VBox box = new VBox(4, val, lbl);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("profile-stat-box");
        return box;
    }

    private static HBox buildActivityRow(ActivityItem item) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-item");
        row.setPadding(new Insets(10, 0, 10, 0));

        String actionText = item.target.isEmpty()
            ? item.action
            : item.action + " " + item.target;

        Label action = new Label(actionText);
        action.getStyleClass().add("activity-action");
        HBox.setHgrow(action, Priority.ALWAYS);

        Label time = new Label(item.timestamp);
        time.getStyleClass().add("activity-time");

        row.getChildren().addAll(action, time);
        return row;
    }
}
