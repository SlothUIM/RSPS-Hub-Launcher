import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfileScreen {

    /** Called when viewing someone else's profile — respects their privacy setting. */
    public static Scene create(Stage stage, String username, boolean isOnline,
                               String statusMessage, List<String> blockedUsers,
                               String targetPrivacy, boolean isFriend,
                               Runnable onBack, Runnable onMessage) {

        boolean isOwnProfile = username.equals(LauncherEngine.currentUsername);
        // Determine what the viewer can see
        boolean canSeeDetails = isOwnProfile
            || "public".equals(targetPrivacy)
            || ("friends".equals(targetPrivacy) && isFriend);
        boolean isPrivate = !isOwnProfile && "private".equals(targetPrivacy);
        boolean isFriendsOnly = !isOwnProfile && "friends".equals(targetPrivacy) && !isFriend;

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

        // ── Avatar + identity ────────────────────────────────────────────────
        String initial = username.isEmpty() ? "?" : String.valueOf(username.charAt(0)).toUpperCase();
        Label avatarLetter = new Label(initial);
        avatarLetter.getStyleClass().add("profile-avatar");
        StackPane avatar = new StackPane(avatarLetter);
        avatar.setPrefSize(100, 100); avatar.setMinSize(100, 100); avatar.setMaxSize(100, 100);

        // Try local file first (own profile), then fall back to server URL for everyone
        boolean loadedLocal = false;
        if (isOwnProfile && LauncherEngine.avatarImagePath != null) {
            File imgFile = new File(LauncherEngine.avatarImagePath);
            if (imgFile.exists()) {
                ImageView iv = new ImageView(new Image(imgFile.toURI().toString(), true));
                iv.setFitWidth(100); iv.setFitHeight(100); iv.setPreserveRatio(false);
                iv.setClip(new Circle(50, 50, 50));
                avatarLetter.setVisible(false);
                avatar.getChildren().add(iv);
                loadedLocal = true;
            }
        }
        if (!loadedLocal) {
            // Try loading from server (works for own and other users)
            Image serverImg = new Image(ApiClient.avatarUrl(username), true);
            serverImg.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0 && !serverImg.isError()) {
                    javafx.application.Platform.runLater(() -> {
                        ImageView iv = new ImageView(serverImg);
                        iv.setFitWidth(100); iv.setFitHeight(100); iv.setPreserveRatio(false);
                        iv.setClip(new Circle(50, 50, 50));
                        avatarLetter.setVisible(false);
                        avatar.getChildren().add(iv);
                    });
                }
            });
        }

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

        // ── Privacy badge (own profile only) ────────────────────────────────
        Label privacyBadge = null;
        if (isOwnProfile) {
            String privacy = LauncherEngine.profilePrivacy;
            String badgeText;
            if ("private".equals(privacy)) {
                badgeText = "\uD83D\uDD12  Private";
            } else if ("friends".equals(privacy)) {
                badgeText = "\uD83D\uDC65  Friends Only";
            } else {
                badgeText = "\uD83C\uDF0D  Public";
            }
            privacyBadge = new Label(badgeText);
            privacyBadge.setStyle(
                "-fx-background-color: #1a1d24;" +
                "-fx-border-color: #2a2e39;" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;" +
                "-fx-text-fill: #8b92a5;" +
                "-fx-font-size: 11px;" +
                "-fx-padding: 4 12;"
            );
        }

        // ── Stats row ────────────────────────────────────────────────────────
        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER);

        if (isOwnProfile) {
            long totalMins = PlaytimeStore.getTotalMinutes();
            String playtimeStr;
            if (totalMins == 0) {
                playtimeStr = "0m";
            } else {
                long h = totalMins / 60;
                long m = totalMins % 60;
                playtimeStr = (h > 0 ? h + "h " : "") + m + "m";
            }
            stats.getChildren().addAll(
                statBox("Total Playtime", playtimeStr),
                statBox("Servers Played", String.valueOf(PlaytimeStore.getTotalServersPlayed())),
                statBox("Most Played", PlaytimeStore.getMostPlayed())
            );
        } else {
            // Placeholders — filled in async once API responds
            VBox playtimeBox  = statBox("Total Playtime", "...");
            VBox serversBox   = statBox("Servers Played", "...");
            VBox mostBox      = statBox("Most Played",    "...");
            stats.getChildren().addAll(playtimeBox, serversBox, mostBox);

            ApiClient.getUserStats(username).thenAccept(obj -> javafx.application.Platform.runLater(() -> {
                if (obj == null || obj.has("error")) {
                    updateStatBox(playtimeBox, "—");
                    updateStatBox(serversBox,  "—");
                    updateStatBox(mostBox,     "—");
                    return;
                }
                long totalMins = obj.has("total_playtime_minutes") ? obj.get("total_playtime_minutes").getAsLong() : 0;
                int servers    = obj.has("servers_played")         ? obj.get("servers_played").getAsInt() : 0;
                String most    = obj.has("most_played_server")     ? obj.get("most_played_server").getAsString() : "";
                long h = totalMins / 60, m = totalMins % 60;
                String playtimeStr = totalMins == 0 ? "0m" : (h > 0 ? h + "h " : "") + m + "m";
                updateStatBox(playtimeBox, playtimeStr);
                updateStatBox(serversBox,  String.valueOf(servers));
                updateStatBox(mostBox,     most.isEmpty() ? "—" : most);
            }));
        }

        // ── Server Levels section ────────────────────────────────────────────
        VBox serverLevelsSection = new VBox(10);
        Label serverLevelsHeader = new Label("SERVER LEVELS");
        serverLevelsHeader.getStyleClass().add("friends-section-header");
        serverLevelsHeader.setPadding(new Insets(0, 0, 4, 0));
        serverLevelsSection.getChildren().add(serverLevelsHeader);

        if (isOwnProfile) {
            Map<String, Long> allMinutes = PlaytimeStore.getAllMinutes();

            if (allMinutes.isEmpty()) {
                Label noServers = new Label("No servers played yet.");
                noServers.getStyleClass().add("auth-muted");
                serverLevelsSection.getChildren().add(noServers);
            } else {
                List<Map.Entry<String, Long>> sorted = new ArrayList<>(allMinutes.entrySet());
                sorted.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()));
                List<Map.Entry<String, Long>> top3 = sorted.subList(0, Math.min(3, sorted.size()));

                int rank = 1;
                for (Map.Entry<String, Long> entry : top3) {
                    serverLevelsSection.getChildren().add(buildServerLevelRow(rank, entry.getKey(), true));
                    rank++;
                }

                if ("private".equals(LauncherEngine.profilePrivacy)) {
                    Label hiddenNote = new Label("Your server levels are hidden from others.");
                    hiddenNote.getStyleClass().add("auth-muted");
                    hiddenNote.setStyle("-fx-font-style: italic;");
                    serverLevelsSection.getChildren().add(hiddenNote);
                }
            }
        } else {
            Label noData = new Label("Stats are not available for other players yet.");
            noData.setStyle("-fx-text-fill: #555d6e; -fx-font-size: 13px; -fx-font-style: italic;");
            serverLevelsSection.getChildren().add(noData);
        }

        // ── Favourite Servers section ────────────────────────────────────────
        VBox favSection = new VBox(10);
        Label favHeader = new Label("FAVOURITE SERVERS");
        favHeader.getStyleClass().add("friends-section-header");
        favHeader.setPadding(new Insets(0, 0, 4, 0));
        favSection.getChildren().add(favHeader);

        if (isOwnProfile) {
            FlowPane favFlow = new FlowPane(8, 8);
            favFlow.setAlignment(Pos.CENTER_LEFT);

            if (LauncherEngine.favouriteServers.isEmpty()) {
                Label noFavs = new Label("No favourites yet.");
                noFavs.getStyleClass().add("auth-muted");
                favSection.getChildren().add(noFavs);
            } else {
                for (String server : LauncherEngine.favouriteServers) {
                    favFlow.getChildren().add(buildFavPill(server));
                }

                boolean isPublic = "public".equals(LauncherEngine.profilePrivacy);
                if (!isPublic) {
                    favFlow.setOpacity(0.5);
                    Label hiddenNote = new Label("Hidden from others based on your privacy settings.");
                    hiddenNote.getStyleClass().add("auth-muted");
                    hiddenNote.setStyle("-fx-font-style: italic;");
                    favSection.getChildren().add(favFlow);
                    favSection.getChildren().add(hiddenNote);
                } else {
                    favSection.getChildren().add(favFlow);
                }
            }
        } else {
            Label noFavs = new Label("Not available.");
            noFavs.setStyle("-fx-text-fill: #555d6e; -fx-font-size: 13px; -fx-font-style: italic;");
            favSection.getChildren().add(noFavs);
        }

        // ── Action buttons (other profile only) ─────────────────────────────
        HBox actions = null;
        if (!isOwnProfile) {
            actions = new HBox(12);
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
                    // Unblock
                    ApiClient.unblockUser(username).thenAccept(ok -> javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            blockedUsers.remove(username);
                            blockBtn.setText("Block User");
                            blockBtn.getStyleClass().setAll("settings-logout-btn");
                        }
                    }));
                } else {
                    // Block
                    ApiClient.blockUser(username).thenAccept(ok -> javafx.application.Platform.runLater(() -> {
                        if (ok) {
                            blockedUsers.add(username);
                            blockBtn.setText("Unblock");
                            blockBtn.getStyleClass().setAll("settings-secondary-btn");
                        }
                    }));
                }
            });

            actions.getChildren().addAll(msgBtn, blockBtn);
        }

        // ── Recent activity ──────────────────────────────────────────────────
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

        // ── Layout assembly ──────────────────────────────────────────────────
        VBox content = new VBox(28);
        content.setPadding(new Insets(50, 60, 60, 60));
        content.setMaxWidth(680);

        content.getChildren().add(profileHeader);

        if (isOwnProfile && privacyBadge != null) {
            HBox badgeRow = new HBox(privacyBadge);
            badgeRow.setAlignment(Pos.CENTER);
            content.getChildren().add(badgeRow);
        }

        if (isPrivate) {
            // Private — show only name + action buttons, nothing else
        } else if (isFriendsOnly) {
            // Partial lock — friends-only profile viewed by non-friend
            VBox lockBox = new VBox(10);
            lockBox.setAlignment(Pos.CENTER);
            Label lockIcon = new Label("👥");
            lockIcon.setStyle("-fx-font-size: 36px;");
            Label lockMsg = new Label("Friends Only Profile");
            lockMsg.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
            Label lockSub = new Label("Add them as a friend to see their stats, levels and favourites.");
            lockSub.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");
            lockSub.setWrapText(true);
            lockSub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            lockBox.getChildren().addAll(lockIcon, lockMsg, lockSub);
            content.getChildren().add(lockBox);
        } else {
            // Visible — show all sections
            content.getChildren().add(stats);
            content.getChildren().add(serverLevelsSection);
            content.getChildren().add(favSection);
        }

        if (actions != null) {
            content.getChildren().add(actions);
        }

        if (!isPrivate && !isFriendsOnly) {
            content.getChildren().add(activitySection);
        }

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

    // ── Helper: build a server-level row from real data ──────────────────────
    private static HBox buildServerLevelRow(int rank, String serverName, boolean isReal) {
        int level = ServerSkillSystem.getLevel(serverName);
        double progress = ServerSkillSystem.getLevelProgress(serverName);
        return buildServerLevelRowRaw(rank, serverName, level, progress);
    }

    private static HBox buildServerLevelRowRaw(int rank, String serverName, int level, double progress) {
        String milestoneColor = ServerSkillSystem.getMilestoneColor(level);
        String rankName = ServerSkillSystem.getRankName(level);

        Label rankLbl = new Label("#" + rank);
        rankLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-weight: bold; -fx-min-width: 28;");

        Label nameLbl = new Label(serverName);
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label levelBadge = new Label("Lvl " + level);
        levelBadge.setStyle(
            "-fx-text-fill: " + milestoneColor + ";" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;"
        );

        Label rankNameLbl = new Label(rankName);
        rankNameLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px;");

        ProgressBar bar = new ProgressBar(progress);
        bar.setStyle(
            "-fx-accent: " + milestoneColor + ";" +
            "-fx-pref-width: 120;" +
            "-fx-pref-height: 6;"
        );

        HBox row = new HBox(10, rankLbl, nameLbl, levelBadge, rankNameLbl, bar);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Helper: build a favourite server pill ───────────────────────────────
    private static Label buildFavPill(String serverName) {
        Label pill = new Label(serverName);
        pill.setStyle(
            "-fx-background-color: #1a1d24;" +
            "-fx-border-color: #2a2e39;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 4 12;"
        );
        return pill;
    }

    // ── Unchanged helpers ────────────────────────────────────────────────────

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

    /** Updates the value label inside a statBox created by statBox(). */
    private static void updateStatBox(VBox box, String newValue) {
        if (!box.getChildren().isEmpty() && box.getChildren().get(0) instanceof Label lbl) {
            lbl.setText(newValue);
        }
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
