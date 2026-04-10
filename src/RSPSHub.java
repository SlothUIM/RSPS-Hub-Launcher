import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RSPSHub extends Application {

    // Data
    private List<ServerProfile> allServers;
    private List<Friend> friends = new ArrayList<>();
    // groups and groupMembers are stored in LauncherEngine (per-user, persisted to disk)

    // UI state
    private String activeTag                   = "All";
    private boolean applyPreferredTagsOnNextLoad = false;
    private String searchText       = "";
    private boolean showingLibrary     = false;
    private boolean showingFriends     = false;
    private boolean showingMessaging   = false;
    private boolean showingStats       = false;
    private boolean showingLeaderboard = false;
    private String  leaderboardServer  = "All Servers";
    private String  activeConversation = null;
    private boolean isGroupConversation = false;

    // Hub layout refs (needed to swap center content for chat)
    private BorderPane hubRoot;
    private VBox       hubTopVBox;   // outer VBox holding titleBar+navbar+topControls
    private ScrollPane hubScrollPane;
    private VBox serverGrid;

    // Navbar tab refs
    private Button storeTab, libraryTab, friendsTab, statsTab, leaderboardTab;

    // Session timer — static so ServerDetailScreen can also drive them
    static Label    sessionTimerLabel;
    static Timeline sessionTimeline;
    static Stage    sessionStage;   // stored so endSession can un-minimize
    static long     sessionStart;   // epoch ms when current session began

    // Top control refs (toggled per tab)
    private VBox topControls;
    private HBox filterBar;
    private HBox sortRow;
    private TextField searchBar;

    // Notification badge (updated in updateDisplay)
    private Label notifBadge;

    // Notification store
    enum NotifType { FRIEND_REQUEST, FRIEND_ONLINE, SERVER_UPDATE, SYSTEM }
    static class AppNotif {
        NotifType type; String title; String body; String time; boolean read;
        String friendUsername; // for FRIEND_REQUEST actions
        AppNotif(NotifType t, String title, String body, String time) {
            this.type = t; this.title = title; this.body = body; this.time = time;
        }
    }
    private final List<AppNotif> notifications = new ArrayList<>();
    private Popup notifPopup = null;

    // Download badge
    private Label downloadBadge;

    // Active downloads list
    public static final ObservableList<DownloadItem> activeDownloads = FXCollections.observableArrayList();

    // Sort state
    private String sortOrder = "Players (High → Low)";

    // Social state
    private String friendsSubTab = "ONLINE";
    private List<FriendRequest> friendRequests = new ArrayList<>();
    private List<String> blockedUsers = new ArrayList<>();

    // Heartbeat timer
    private Timeline heartbeatTimeline;

    // Debounce flag — prevents stacked fade animations from multiple rapid updateDisplay() calls
    private boolean displayUpdatePending = false;

    // ── LIFECYCLE ────────────────────────────────────────────────────────────

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setTitle("RSPS Hub Launcher");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(600);

        // Attach resize support and maximize corner fix to every new scene
        primaryStage.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) ResizeHelper.addTo(primaryStage, scene);
        });

        SplashScreen.show(primaryStage, () -> {
            // Check for saved session — auto login if found
            String[] saved = LoginScreen.loadSession();
            if (saved != null) {
                LauncherEngine.currentUsername = saved[0];
                LauncherEngine.sessionToken    = saved[1];
                // If they have a saved session they've already been through onboarding — skip it.
                // NOTE: do NOT call saveSettings() here — init() hasn't run yet so all settings
                // are still at their in-memory defaults (including accentColor = #9b5de5).
                // Saving now would overwrite the real settings.json and wipe the player's saved accent.
                // showHub() will call init() → loadSettings() → writeAccentCss() with the real values.
                LauncherEngine.hasCompletedOnboarding = true;
                showHub(primaryStage);
            } else {
                showLoginScreen(primaryStage);
            }
        });
        primaryStage.show();
    }

    // ── SCREEN NAVIGATION ────────────────────────────────────────────────────

    private void showLoginScreen(Stage stage) {
        transitionTo(stage, () -> LoginScreen.create(
            stage,
            username -> {
                LauncherEngine.currentUsername = username;
                
                // Check if they've already done onboarding
                if (LauncherEngine.hasCompletedOnboarding) {
                    showHub(stage);
                } else {
                    // If not, show it, but make sure the finish button SAVES the state
                    stage.setScene(OnboardingScreen.create(stage, () -> {
                        LauncherEngine.hasCompletedOnboarding = true; // Set to true
                        LauncherEngine.saveSettings();                // CRITICAL: This writes to your PC
                        applyPreferredTagsOnNextLoad = true;
                        showHub(stage);
                    }));
                }
            },
            () -> showRegisterScreen(stage)
        ));
    }

    private void showRegisterScreen(Stage stage) {
        transitionTo(stage, () -> RegisterScreen.create(
            stage,
            () -> showLoginScreen(stage),
            () -> showLoginScreen(stage)
        ));
    }

    private void showSettings(Stage stage) {
        stage.setScene(AccountSettingsScreen.create(
            stage,
            () -> showHub(stage),
            () -> { allServers = null; LauncherEngine.currentUsername = ""; LauncherEngine.sessionToken = ""; LauncherEngine.avatarImagePath = null; friends.clear(); friendRequests.clear(); showLoginScreen(stage); },
            () -> showDevPortal(stage)
        ));
    }

    private void showDevPortal(Stage stage) {
        stage.setScene(DeveloperPortalScreen.create(stage, () -> showSettings(stage)));
    }

    private void showServerDetail(Stage stage, ServerProfile server) {
        Scene next = ServerDetailScreen.create(stage, server, () -> showHub(stage));
        stage.setScene(next);
    }

    private void showStats(Stage stage) {
        transitionTo(stage, () -> PlaytimeScreen.create(stage, () -> showHub(stage)));
    }

    private void showProfile(Stage stage, String username) {
        boolean isOwnProfile = username.equals(LauncherEngine.currentUsername);
        if (isOwnProfile) {
            stage.setScene(ProfileScreen.create(stage, username, true, LauncherEngine.statusMessage,
                blockedUsers, LauncherEngine.profilePrivacy, false,
                () -> { showHub(stage); showingFriends = true; setActiveTab(friendsTab); updateDisplay(); },
                () -> { showHub(stage); openConversation(username, false); }
            ));
            return;
        }
        // Fetch real privacy + friend status from backend before showing profile
        ApiClient.getUserProfile(username).thenAccept(obj -> Platform.runLater(() -> {
            String privacy  = obj.has("privacy")   ? obj.get("privacy").getAsString()   : "public";
            boolean isFriend= obj.has("is_friend") && obj.get("is_friend").getAsBoolean();
            boolean online  = obj.has("online")    && obj.get("online").getAsBoolean();
            Friend f = friends.stream().filter(fr -> fr.username.equals(username)).findFirst().orElse(null);
            String status = f != null && f.statusMessage != null ? f.statusMessage : "";
            stage.setScene(ProfileScreen.create(stage, username, online, status, blockedUsers,
                privacy, isFriend,
                () -> { showHub(stage); showingFriends = true; setActiveTab(friendsTab); updateDisplay(); },
                () -> { showHub(stage); openConversation(username, false); }
            ));
        }));
    }

    private void openConversation(String conversationId, boolean isGroup) {
        showingMessaging    = true;
        activeConversation  = conversationId;
        isGroupConversation = isGroup;
        showingFriends      = true;
        setActiveTab(friendsTab);
        buildChatView();
    }

    private void closeConversation() {
        showingMessaging    = false;
        activeConversation  = null;
        isGroupConversation = false;
        hubRoot.setCenter(hubScrollPane);
        updateDisplay();
    }

    // ── HUB SCENE ────────────────────────────────────────────────────────────

    private void showHub(Stage stage) {
        // Reload all per-user data whenever we enter the hub
        // (covers both first login and account switching)
        LauncherEngine.loadUserSettings();
        LauncherEngine.writeAccentCss();
        PlaytimeStore.reload();
        SessionHistoryStore.reload();
        StreakStore.reload();
        MessageStore.reload();
        ApiClient.getBlockedUsers().thenAccept(list -> Platform.runLater(() -> { blockedUsers.clear(); blockedUsers.addAll(list); updateDisplaySilent(); }));

        showingLibrary     = false;
        showingFriends     = false;
        showingMessaging   = false;
        showingStats       = false;
        showingLeaderboard = false;
        activeTag          = "All";
        if (applyPreferredTagsOnNextLoad && !LauncherEngine.preferredTags.isEmpty()) {
            activeTag = "For You";
            applyPreferredTagsOnNextLoad = false;
        }
        searchText         = "";

        if (allServers == null) {
            LauncherEngine.init();
            allServers = LauncherEngine.fetchServers();
            DiscordRPC.connectAsync();
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                DiscordRPC.setBrowsing("Browsing the store");
            }, "discord-idle").start();
            refreshFriendsFromApi();
            startHeartbeat();
            ApiClient.checkStaff().thenAccept(staff -> Platform.runLater(() -> {
                LauncherEngine.isStaff = staff;
                if (staff) updateDisplaySilent();
            }));
            // Sync privacy setting from server
            ApiClient.loadMyPrivacy().thenAccept(p -> Platform.runLater(() -> {
                LauncherEngine.profilePrivacy = p;
                LauncherEngine.saveSettings();
            }));
        }

        hubRoot = new BorderPane();
        hubRoot.getStyleClass().add("root-pane");

        // --- NAVBAR ---
        HBox navbar = new HBox(30);
        navbar.setPadding(new Insets(20, 40, 20, 40));
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("RSPS HUB");
        brand.getStyleClass().add("nav-brand");

        storeTab       = navTab("STORE",       true);
        libraryTab     = navTab("LIBRARY",     false);
        friendsTab     = navTab("FRIENDS",     false);
        statsTab       = navTab("STATS",       false);
        leaderboardTab = navTab("LEADERBOARD", false);

        storeTab.setOnAction(e -> {
            showingLibrary = false; showingFriends = false; showingStats = false; showingLeaderboard = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(storeTab);
            if (LauncherEngine.activeServer == null) DiscordRPC.setBrowsing("Browsing the store");
            updateDisplay();
        });
        libraryTab.setOnAction(e -> {
            showingLibrary = true; showingFriends = false; showingStats = false; showingLeaderboard = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(libraryTab);
            if (LauncherEngine.activeServer == null) DiscordRPC.setBrowsing("Viewing their library");
            updateDisplay();
        });
        friendsTab.setOnAction(e -> {
            showingFriends = true; showingLibrary = false; showingStats = false; showingLeaderboard = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(friendsTab);
            if (LauncherEngine.activeServer == null) DiscordRPC.setBrowsing("Hanging out in Friends");
            updateDisplay();
        });
        statsTab.setOnAction(e -> {
            showingStats = true; showingLibrary = false; showingFriends = false; showingLeaderboard = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(statsTab);
            if (LauncherEngine.activeServer == null) DiscordRPC.setBrowsing("Checking their stats");
            updateDisplay();
        });
        leaderboardTab.setOnAction(e -> {
            showingLeaderboard = true; showingLibrary = false; showingFriends = false; showingStats = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(leaderboardTab);
            if (LauncherEngine.activeServer == null) DiscordRPC.setBrowsing("Checking the leaderboard");
            updateDisplay();
        });

        // Account widget
        String initial = LauncherEngine.currentUsername.isEmpty() ? "?"
            : String.valueOf(LauncherEngine.currentUsername.charAt(0)).toUpperCase();
        Label avatarLetter = new Label(initial);
        avatarLetter.getStyleClass().add("nav-avatar");
        StackPane avatarCircle = new StackPane(avatarLetter);
        avatarCircle.setPrefSize(34, 34); avatarCircle.setMinSize(34, 34); avatarCircle.setMaxSize(34, 34);
        if (LauncherEngine.avatarImagePath != null) {
            java.io.File imgFile = new java.io.File(LauncherEngine.avatarImagePath);
            if (imgFile.exists()) {
                ImageView iv = new ImageView(new Image(imgFile.toURI().toString(), true));
                iv.setFitWidth(34); iv.setFitHeight(34); iv.setPreserveRatio(false);
                iv.setClip(new javafx.scene.shape.Circle(17, 17, 17));
                avatarLetter.setVisible(false);
                avatarCircle.getChildren().add(iv);
            }
        }
        Label usernameLabel = new Label(LauncherEngine.currentUsername);
        usernameLabel.getStyleClass().add("nav-username");
        VBox userInfo = new VBox(1, usernameLabel);
        if (LauncherEngine.statusMessage != null && !LauncherEngine.statusMessage.isEmpty()) {
            Label statusLbl = new Label(LauncherEngine.statusMessage);
            statusLbl.getStyleClass().add("nav-status-message");
            userInfo.getChildren().add(statusLbl);
        }
        HBox accountWidget = new HBox(10, avatarCircle, userInfo);
        accountWidget.setAlignment(Pos.CENTER);
        accountWidget.getStyleClass().add("nav-account-widget");
        accountWidget.setCursor(Cursor.HAND);
        accountWidget.setOnMouseClicked(e -> showSettings(stage));

        // Notification bell
        Button bellBtn = new Button("🔔");
        bellBtn.getStyleClass().add("nav-bell-btn");
        notifBadge = new Label("0");
        notifBadge.getStyleClass().add("nav-bell-badge");
        notifBadge.setVisible(false);
        notifBadge.setManaged(false);
        StackPane bellPane = new StackPane(bellBtn, notifBadge);
        bellPane.setAlignment(Pos.CENTER);
        StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);
        bellBtn.setOnAction(e -> showNotificationPopup(bellBtn));

        // Downloads button
        Button downloadBtn = new Button("⬇");
        downloadBtn.getStyleClass().add("nav-download-btn");
        downloadBadge = new Label("0");
        downloadBadge.getStyleClass().add("nav-download-badge");
        downloadBadge.setVisible(false);
        downloadBadge.setManaged(false);
        StackPane downloadPane = new StackPane(downloadBtn, downloadBadge);
        downloadPane.setAlignment(Pos.CENTER);
        StackPane.setAlignment(downloadBadge, Pos.TOP_RIGHT);
        downloadBtn.setOnAction(e -> showDownloadsPopup(downloadBtn));

        // Session timer (shown while a game is running).
        // showHub() is called every time we navigate back, so we must resume
        // the timer if a session is already in progress.
        sessionTimerLabel = new Label();
        sessionTimerLabel.getStyleClass().add("session-timer-label");

        if (LauncherEngine.activeServer != null && sessionStart > 0) {
            // Session is live — restore timer with correct elapsed time
            if (sessionTimeline != null) sessionTimeline.stop();
            long alreadyElapsed = (System.currentTimeMillis() - sessionStart) / 1000;
            final long[] elapsed = {alreadyElapsed};
            long h = elapsed[0] / 3600, m = (elapsed[0] % 3600) / 60, s = elapsed[0] % 60;
            sessionTimerLabel.setText(String.format("\u25B6 %d:%02d:%02d", h, m, s));
            sessionTimerLabel.setVisible(true);
            sessionTimerLabel.setManaged(true);
            sessionTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                elapsed[0]++;
                long hh = elapsed[0] / 3600, mm = (elapsed[0] % 3600) / 60, ss = elapsed[0] % 60;
                sessionTimerLabel.setText(String.format("\u25B6 %d:%02d:%02d", hh, mm, ss));
            }));
            sessionTimeline.setCycleCount(Timeline.INDEFINITE);
            sessionTimeline.play();
        } else {
            sessionTimerLabel.setVisible(false);
            sessionTimerLabel.setManaged(false);
        }

        // Discord logo button
        ImageView discordIv = new ImageView();
        try {
            java.io.InputStream dis = RSPSHub.class.getResourceAsStream("/discord_logo.png");
            if (dis != null) {
                discordIv.setImage(new Image(dis));
            } else {
                // fallback: load from file next to jar
                java.io.File df = new java.io.File("discord_logo.png");
                if (df.exists()) discordIv.setImage(new Image(df.toURI().toString()));
            }
        } catch (Exception ignored) {}
        discordIv.setFitWidth(28);
        discordIv.setFitHeight(28);
        discordIv.setPreserveRatio(true);
        discordIv.setSmooth(true);
        // Clip to circle
        javafx.scene.shape.Circle discordClip = new javafx.scene.shape.Circle(14, 14, 14);
        discordIv.setClip(discordClip);

        Button discordJoinBtn = new Button();
        discordJoinBtn.setGraphic(discordIv);
        discordJoinBtn.getStyleClass().add("nav-discord-btn");
        discordJoinBtn.setTooltip(new Tooltip("Join our Discord"));
        discordJoinBtn.setOnAction(e -> {
            try { java.awt.Desktop.getDesktop().browse(new java.net.URI("https://discord.gg/grt9C4GJcj")); }
            catch (Exception ex) { System.err.println("Failed to open Discord: " + ex.getMessage()); }
        });

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);
        navbar.getChildren().addAll(brand, storeTab, libraryTab, friendsTab, statsTab, leaderboardTab,
            navSpacer, sessionTimerLabel, discordJoinBtn, downloadPane, bellPane, accountWidget);

        // --- SEARCH & FILTERS ---
        topControls = new VBox(15);
        topControls.setPadding(new Insets(20, 40, 20, 40));

        searchBar = new TextField();
        searchBar.setPromptText("Search for a server...");
        searchBar.getStyleClass().add("search-field");
        HBox.setHgrow(searchBar, Priority.ALWAYS);
        searchBar.textProperty().addListener((obs, old, val) -> { searchText = val; updateDisplay(); });

        // Sort menu
        Label sortLabel = new Label("Sort by:");
        sortLabel.getStyleClass().add("auth-muted");
        MenuButton sortBtn = new MenuButton(sortOrder);
        sortBtn.getStyleClass().add("dark-menu-btn");
        for (String opt : new String[]{"Players (High → Low)", "Name A–Z", "Name Z–A", "Most Played (You)"}) {
            MenuItem sortItem = new MenuItem(opt);
            sortItem.setOnAction(e -> { sortOrder = opt; sortBtn.setText(opt); updateDisplay(); });
            sortBtn.getItems().add(sortItem);
        }
        sortRow = new HBox(10, searchBar, sortLabel, sortBtn);
        sortRow.setAlignment(Pos.CENTER_LEFT);

        filterBar = new HBox(10);
        List<String> tagList = new java.util.ArrayList<>();
        if (!LauncherEngine.preferredTags.isEmpty()) tagList.add("For You");
        tagList.addAll(java.util.Arrays.asList("All", "Custom", "PvP", "Economy", "OSRS", "Hardcore", "Leagues", "Vanilla", "Ironman", "Skilling"));
        String[] tags = tagList.toArray(new String[0]);
        for (String tag : tags) {
            Button tagBtn = new Button(tag);
            tagBtn.getStyleClass().add(tag.equals(activeTag) ? "filter-btn-active" : "filter-btn");
            tagBtn.setOnAction(e -> {
                filterBar.getChildren().forEach(b -> b.getStyleClass().setAll("filter-btn"));
                tagBtn.getStyleClass().setAll("filter-btn-active");
                activeTag = tag;
                updateDisplay();
            });
            filterBar.getChildren().add(tagBtn);
        }
        topControls.getChildren().addAll(sortRow, filterBar);

        hubTopVBox = new VBox(TitleBar.create(stage), navbar, topControls);
        hubRoot.setTop(hubTopVBox);

        // --- CONTENT ---
        serverGrid = new VBox(20);
        serverGrid.setPadding(new Insets(30));
        hubScrollPane = new ScrollPane(serverGrid);
        hubScrollPane.setFitToWidth(true);
        hubScrollPane.getStyleClass().add("main-scroll");
        boostScrollSpeed(hubScrollPane);
        hubRoot.setCenter(hubScrollPane);

        updateDisplay();

        Scene scene = new Scene(hubRoot);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(getClass()));
        SceneUtils.applyRoundedCorners(scene, hubRoot, stage);
        stage.setScene(scene);

    }

    private Button navTab(String text, boolean active) {
        Button btn = new Button(text);
        btn.getStyleClass().add(active ? "nav-tab-active" : "nav-tab");
        return btn;
    }

    private void setActiveTab(Button active) {
        for (Button b : new Button[]{storeTab, libraryTab, friendsTab, statsTab, leaderboardTab})
            b.getStyleClass().setAll("nav-tab");
        active.getStyleClass().setAll("nav-tab-active");
    }

    // ── API HELPERS ──────────────────────────────────────────────────────────────

    private void refreshFriendsFromApi() {
        ApiClient.getFriends().thenAccept(list -> Platform.runLater(() -> {
            friends.clear();
            friends.addAll(list);
        }));
        ApiClient.getFriendRequests().thenAccept(list -> Platform.runLater(() -> {
            // Keep outgoing requests (we track those locally until confirmed)
            List<FriendRequest> outgoing = new ArrayList<>();
            for (FriendRequest r : friendRequests) if (!r.incoming) outgoing.add(r);
            friendRequests.clear();
            friendRequests.addAll(list);
            friendRequests.addAll(outgoing);
        }));
    }

    private void startHeartbeat() {
        if (heartbeatTimeline != null) heartbeatTimeline.stop();
        heartbeatTimeline = new Timeline(new KeyFrame(Duration.seconds(60), e -> ApiClient.heartbeat()));
        heartbeatTimeline.setCycleCount(Timeline.INDEFINITE);
        heartbeatTimeline.play();
        // Send one immediately
        ApiClient.heartbeat();
    }

    // ── TRAY ICON ─────────────────────────────────────────────────────────────

    // ── SCREEN TRANSITION ─────────────────────────────────────────────────────

    private void transitionTo(Stage stage, java.util.function.Supplier<Scene> sceneBuilder) {
        Scene current = stage.getScene();
        if (current == null) {
            Scene next = sceneBuilder.get();
            stage.setScene(next);
            return;
        }
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), current.getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            Scene next = sceneBuilder.get();
            next.getRoot().setOpacity(0.0);
            stage.setScene(next);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), next.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void showLeaderboard(Stage stage) {
        transitionTo(stage, () -> LeaderboardScreen.create(stage, () -> showHub(stage)));
    }

    // ── DISPLAY ROUTING ──────────────────────────────────────────────────────

    private void updateDisplay() {
        // If an update is already queued, skip — the pending one will pick up latest state
        if (displayUpdatePending) return;
        displayUpdatePending = true;

        // Fade out → rebuild → fade in (only one animation in flight at a time)
        FadeTransition contentOut = new FadeTransition(Duration.millis(60), hubScrollPane);
        contentOut.setFromValue(hubScrollPane.getOpacity());
        contentOut.setToValue(0.0);
        contentOut.setOnFinished(ev -> {
            displayUpdatePending = false;
            rebuildDisplay();
            FadeTransition contentIn = new FadeTransition(Duration.millis(120), hubScrollPane);
            contentIn.setFromValue(0.0);
            contentIn.setToValue(1.0);
            contentIn.play();
        });
        contentOut.play();
    }

    /** Silently rebuild the display without the fade animation.
     *  Use for background data refreshes that shouldn't flash the UI. */
    private void updateDisplaySilent() {
        if (hubScrollPane == null) return;
        rebuildDisplay();
    }

    private void rebuildDisplay() {
        serverGrid.getChildren().clear();
        serverGrid.setSpacing(20);
        serverGrid.setPadding(new Insets(30));

        // Update notification badge
        long unread = notifications.stream().filter(n -> !n.read).count();
        if (notifBadge != null) {
            notifBadge.setText(String.valueOf(unread));
            notifBadge.setVisible(unread > 0);
            notifBadge.setManaged(unread > 0);
        }

        // Update download badge
        if (downloadBadge != null) {
            int dlCount = activeDownloads.size();
            downloadBadge.setText(String.valueOf(dlCount));
            downloadBadge.setVisible(dlCount > 0);
            downloadBadge.setManaged(dlCount > 0);
        }

        topControls.getChildren().clear();
        boolean showSearch = !showingFriends && !showingStats && !showingLeaderboard;
        if (showSearch) {
            topControls.setPadding(new Insets(20, 40, 20, 40));
            topControls.setAlignment(Pos.TOP_LEFT);

            // Staff-only refresh button — added/removed dynamically after async staff check resolves
            sortRow.getChildren().removeIf(n -> "staff-refresh".equals(n.getUserData()));
            if (LauncherEngine.isStaff) {
                Button refreshBtn = new Button("↻ Refresh");
                refreshBtn.setUserData("staff-refresh");
                refreshBtn.getStyleClass().add("settings-secondary-btn");
                refreshBtn.setOnAction(e -> {
                    refreshBtn.setDisable(true);
                    refreshBtn.setText("Refreshing...");
                    ApiClient.getLiveServers().thenAccept(servers -> Platform.runLater(() -> {
                        allServers = servers;
                        refreshBtn.setDisable(false);
                        refreshBtn.setText("↻ Refresh");
                        updateDisplay();
                    }));
                });
                sortRow.getChildren().add(refreshBtn);
            }

            topControls.getChildren().add(sortRow);
            boolean showFilters = !showingLibrary;
            filterBar.setVisible(showFilters);
            filterBar.setManaged(showFilters);
            if (showFilters) topControls.getChildren().add(filterBar);
        } else {
            topControls.setPadding(new Insets(22, 40, 0, 40));
            topControls.setAlignment(Pos.TOP_LEFT);
            String title    = showingFriends ? "FRIENDS" : showingStats ? "STATS" : "LEADERBOARD";
            String subtitle = showingFriends ? "Manage your friends and messages"
                            : showingStats   ? "Your playtime across all servers"
                            :                  "Top players by total playtime";
            Label titleLbl = new Label(title);
            titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
            Label subLbl = new Label(subtitle);
            subLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
            topControls.getChildren().addAll(titleLbl, subLbl);
        }

        if (showingFriends) { buildFriendsContent(); return; }
        if (showingStats)       { buildStatsContent();       return; }
        if (showingLeaderboard) { buildLeaderboardContent(); return; }

        List<ServerProfile> filtered = allServers.stream()
            .filter(s -> s.name.toLowerCase().contains(searchText.toLowerCase()))
            .filter(s -> activeTag.equals("All")
                || (activeTag.equals("For You") && s.tags != null && s.tags.stream().anyMatch(LauncherEngine.preferredTags::contains))
                || (s.tags != null && s.tags.contains(activeTag)))
            .filter(s -> !showingLibrary || LauncherEngine.isDownloaded(s))
            .collect(Collectors.toList());

        // Apply sort (Updated to use playersOnline)
        switch (sortOrder) {
            case "Name A–Z"       -> filtered.sort(Comparator.comparing(s -> s.name));
            case "Name Z–A"       -> filtered.sort(Comparator.comparing((ServerProfile s) -> s.name).reversed());
            case "Most Played (You)" -> filtered.sort(Comparator.comparingLong((ServerProfile s) -> PlaytimeStore.getMinutes(s.name)).reversed());
            default               -> filtered.sort(Comparator.comparingInt((ServerProfile s) -> s.playersOnline).reversed());
        }

        if (filtered.isEmpty()) {
            Label oops = new Label(showingLibrary ? "You haven't installed any servers yet!" : "No servers match your search.");
            oops.getStyleClass().add("empty-label");
            serverGrid.getChildren().add(oops);
            return;
        }

        List<ServerProfile> pinned   = filtered.stream().filter(s -> LauncherEngine.favouriteServers.contains(s.name)).collect(Collectors.toList());
        List<ServerProfile> unpinned = filtered.stream().filter(s -> !LauncherEngine.favouriteServers.contains(s.name)).collect(Collectors.toList());

        int cardIndex = 0;
        if (!pinned.isEmpty()) {
            Label pinnedHdr = new Label("\u2605  FAVOURITES");
            pinnedHdr.getStyleClass().add("pinned-header");
            serverGrid.getChildren().add(pinnedHdr);
            for (ServerProfile s : pinned) {
                VBox card = createServerCard(s);
                serverGrid.getChildren().add(card);
                animateCard(card, cardIndex++);
            }
            if (!unpinned.isEmpty()) {
                Label allHdr = new Label("ALL SERVERS");
                allHdr.getStyleClass().add("pinned-header");
                serverGrid.getChildren().add(allHdr);
            }
        }
        for (ServerProfile s : unpinned) {
            VBox card = createServerCard(s);
            serverGrid.getChildren().add(card);
            animateCard(card, cardIndex++);
        }
    }

    // ── FRIENDS CONTENT ──────────────────────────────────────────────────────

    private void buildFriendsContent() {
        serverGrid.setSpacing(6);
        serverGrid.setPadding(new Insets(16, 40, 40, 40));
        serverGrid.getChildren().add(buildFriendsSubTabBar());
        switch (friendsSubTab) {
            case "REQUESTS" -> buildRequestsContent();
            case "ACTIVITY" -> buildActivityContent();
            default         -> buildOnlineFriendsContent();
        }
    }

    private HBox buildFriendsSubTabBar() {
        HBox bar = new HBox(4);
        bar.setPadding(new Insets(0, 0, 12, 0));
        long incoming = friendRequests.stream().filter(r -> r.incoming).count();
        bar.getChildren().addAll(
            subTab("FRIENDS",  "ONLINE"),
            subTab("REQUESTS" + (incoming > 0 ? "  " + incoming : ""), "REQUESTS"),
            subTab("ACTIVITY", "ACTIVITY")
        );
        return bar;
    }

    private Button subTab(String label, String tab) {
        Button btn = new Button(label);
        btn.getStyleClass().add(friendsSubTab.equals(tab) ? "friends-subtab-active" : "friends-subtab");
        btn.setOnAction(e -> { friendsSubTab = tab; updateDisplay(); });
        return btn;
    }

    private void buildOnlineFriendsContent() {
        // Add friend row
        TextField addField = new TextField();
        addField.setPromptText("Search by username...");
        addField.getStyleClass().add("search-field");
        HBox.setHgrow(addField, Priority.ALWAYS);

        Button addBtn = new Button("Send Request");
        addBtn.getStyleClass().add("auth-btn");
        addBtn.setMinWidth(140);
        addBtn.setPrefHeight(38);

        Label feedbackLbl = new Label();
        feedbackLbl.setStyle("-fx-font-size: 12px;");
        feedbackLbl.setVisible(false);
        feedbackLbl.setManaged(false);

        Runnable sendRequest = () -> {
            String u = addField.getText().trim();
            feedbackLbl.setVisible(true);
            feedbackLbl.setManaged(true);

            if (u.isEmpty()) {
                feedbackLbl.setText("Enter a username first.");
                feedbackLbl.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                return;
            }
            if (u.equalsIgnoreCase(LauncherEngine.currentUsername)) {
                feedbackLbl.setText("You can't add yourself.");
                feedbackLbl.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                return;
            }
            if (friends.stream().anyMatch(f -> f.username.equalsIgnoreCase(u))) {
                feedbackLbl.setText("You're already friends with " + u + ".");
                feedbackLbl.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                return;
            }
            if (friendRequests.stream().anyMatch(r -> r.username.equalsIgnoreCase(u) && !r.incoming)) {
                feedbackLbl.setText("Request already sent to " + u + ".");
                feedbackLbl.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                return;
            }
            if (blockedUsers.contains(u)) {
                feedbackLbl.setText("Unblock " + u + " before sending a request.");
                feedbackLbl.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                return;
            }

            feedbackLbl.setText("Sending...");
            feedbackLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");
            String uFinal = u;
            ApiClient.addFriend(uFinal).thenAccept(result -> Platform.runLater(() -> {
                if ("ok".equals(result)) {
                    friendRequests.add(new FriendRequest(uFinal, false, "Just now"));
                    addField.clear();
                    feedbackLbl.setText("✓  Friend request sent to " + uFinal + "!");
                    feedbackLbl.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px;");
                } else {
                    feedbackLbl.setText(result);
                    feedbackLbl.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                }
            }));
        };

        addBtn.setOnAction(e -> sendRequest.run());
        addField.setOnAction(e -> sendRequest.run());

        HBox addRow = new HBox(10, addField, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox addBox = new VBox(6, addRow, feedbackLbl);
        addBox.setPadding(new Insets(0, 0, 16, 0));
        serverGrid.getChildren().add(addBox);

        List<Friend> visible = friends.stream()
            .filter(f -> !blockedUsers.contains(f.username))
            .collect(Collectors.toList());

        List<Friend> online  = visible.stream().filter(f ->  f.online).collect(Collectors.toList());
        List<Friend> offline = visible.stream().filter(f -> !f.online).collect(Collectors.toList());

        if (!online.isEmpty()) {
            serverGrid.getChildren().add(friendsGroupHeader("ONLINE — " + online.size()));
            for (Friend f : online) serverGrid.getChildren().add(createFriendRow(f));
        }
        if (!offline.isEmpty()) {
            serverGrid.getChildren().add(friendsGroupHeader("OFFLINE — " + offline.size()));
            for (Friend f : offline) serverGrid.getChildren().add(createFriendRow(f));
        }
        if (visible.isEmpty()) {
            Label empty = new Label("No friends yet. Send someone a request above!");
            empty.getStyleClass().add("empty-label");
            serverGrid.getChildren().add(empty);
        }

        serverGrid.getChildren().add(friendsGroupHeader("GROUP CHATS"));
        for (String group : LauncherEngine.groups) serverGrid.getChildren().add(createGroupRow(group));
        serverGrid.getChildren().add(buildNewGroupForm());
    }

    private void buildRequestsContent() {
        List<FriendRequest> incoming = friendRequests.stream().filter(r ->  r.incoming).collect(Collectors.toList());
        List<FriendRequest> outgoing = friendRequests.stream().filter(r -> !r.incoming).collect(Collectors.toList());

        if (incoming.isEmpty() && outgoing.isEmpty()) {
            Label empty = new Label("No pending friend requests.");
            empty.getStyleClass().add("empty-label");
            serverGrid.getChildren().add(empty);
            return;
        }

        if (!incoming.isEmpty()) {
            serverGrid.getChildren().add(friendsGroupHeader("INCOMING — " + incoming.size()));
            for (FriendRequest req : new ArrayList<>(incoming)) {
                HBox card = new HBox(14);
                card.getStyleClass().add("request-card");
                card.setAlignment(Pos.CENTER_LEFT);

                Label avatar = new Label(req.username.substring(0, 1).toUpperCase());
                avatar.getStyleClass().add("friend-avatar-offline");

                VBox info = new VBox(3);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label name = new Label(req.username);  name.getStyleClass().add("friend-name");
                Label time = new Label("Sent " + req.timestamp); time.getStyleClass().add("friend-status-offline");
                info.getChildren().addAll(name, time);

                Button viewBtn = new Button("View Profile");
                viewBtn.getStyleClass().add("settings-secondary-btn");
                viewBtn.setOnAction(e -> showProfile((Stage) card.getScene().getWindow(), req.username));

                Button acceptBtn = new Button("Accept");
                acceptBtn.getStyleClass().add("auth-btn");
                acceptBtn.setMinWidth(100);
                acceptBtn.setPrefHeight(38);
                acceptBtn.setOnAction(e -> {
                    ApiClient.acceptFriend(req.username).thenAccept(ok -> Platform.runLater(() -> {
                        if (ok) {
                            friends.add(new Friend(req.username, false, null));
                            friendRequests.remove(req);
                        }
                        updateDisplay();
                    }));
                });

                Button declineBtn = new Button("Decline");
                declineBtn.getStyleClass().add("settings-secondary-btn");
                declineBtn.setOnAction(e -> {
                    ApiClient.declineFriend(req.username).thenAccept(ok -> Platform.runLater(() -> {
                        friendRequests.remove(req);
                        updateDisplay();
                    }));
                });

                card.getChildren().addAll(avatar, info, viewBtn, acceptBtn, declineBtn);
                serverGrid.getChildren().add(card);
            }
        }

        if (!outgoing.isEmpty()) {
            serverGrid.getChildren().add(friendsGroupHeader("SENT — " + outgoing.size()));
            for (FriendRequest req : new ArrayList<>(outgoing)) {
                HBox card = new HBox(14);
                card.getStyleClass().add("request-card");
                card.setAlignment(Pos.CENTER_LEFT);

                Label avatar = new Label(req.username.substring(0, 1).toUpperCase());
                avatar.getStyleClass().add("friend-avatar-offline");

                VBox info = new VBox(3);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label name = new Label(req.username);  name.getStyleClass().add("friend-name");
                Label time = new Label("Pending • " + req.timestamp); time.getStyleClass().add("friend-status-offline");
                info.getChildren().addAll(name, time);

                Button viewBtn2 = new Button("View Profile");
                viewBtn2.getStyleClass().add("settings-secondary-btn");
                viewBtn2.setOnAction(e -> showProfile((Stage) card.getScene().getWindow(), req.username));

                Button cancelBtn = new Button("Cancel");
                cancelBtn.getStyleClass().add("settings-secondary-btn");
                cancelBtn.setOnAction(e -> {
                    ApiClient.declineFriend(req.username).thenAccept(ok -> Platform.runLater(() -> {
                        friendRequests.remove(req);
                        updateDisplay();
                    }));
                });

                card.getChildren().addAll(avatar, info, viewBtn2, cancelBtn);
                serverGrid.getChildren().add(card);
            }
        }
    }

    private void buildActivityContent() {
        Label loading = new Label("Loading activity...");
        loading.getStyleClass().add("auth-muted");
        serverGrid.getChildren().add(loading);

        ApiClient.getActivityFeed().thenAccept(feed -> Platform.runLater(() -> {
            serverGrid.getChildren().remove(loading);
            if (feed.isEmpty()) {
                Label empty = new Label("No activity yet.");
                empty.getStyleClass().add("empty-label");
                serverGrid.getChildren().add(empty);
                return;
            }
            for (ActivityItem item : feed) {
                HBox row = new HBox(12);
                row.getStyleClass().add("activity-item");
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 0, 10, 0));

                Label avatar = new Label(item.username.substring(0, 1).toUpperCase());
                avatar.getStyleClass().add("friend-avatar-offline");
                avatar.setMinWidth(36); avatar.setMaxWidth(36);
                avatar.setMinHeight(36); avatar.setMaxHeight(36);

                String actionText = item.target.isEmpty()
                    ? item.username + " " + item.action
                    : item.username + " " + item.action + " " + item.target;

                Label action = new Label(actionText);
                action.getStyleClass().add("activity-action");
                HBox.setHgrow(action, Priority.ALWAYS);

                Label time = new Label(item.timestamp);
                time.getStyleClass().add("activity-time");

                row.getChildren().addAll(avatar, action, time);
                serverGrid.getChildren().add(row);
            }
        }));
    }

    private Label friendsGroupHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("friends-section-header");
        lbl.setPadding(new Insets(14, 0, 6, 0));
        return lbl;
    }

    private HBox createFriendRow(Friend friend) {
        HBox row = new HBox(14);
        row.getStyleClass().add("friend-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(friend.username.substring(0, 1).toUpperCase());
        avatar.getStyleClass().add(friend.online ? "friend-avatar-online" : "friend-avatar-offline");
        avatar.setCursor(Cursor.HAND);
        avatar.setOnMouseClicked(e -> showProfile((Stage) row.getScene().getWindow(), friend.username));

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        boolean hasNick = friend.nickname != null && !friend.nickname.isEmpty();
        Label name = new Label(hasNick ? friend.nickname : friend.username);
        name.getStyleClass().add("friend-name");
        name.setCursor(Cursor.HAND);
        name.setOnMouseClicked(e -> showProfile((Stage) row.getScene().getWindow(), friend.username));
        info.getChildren().add(name);

        if (hasNick) {
            Label realName = new Label(friend.username);
            realName.getStyleClass().add("friend-status-offline");
            info.getChildren().add(realName);
        }

        Label status;
        if (friend.online && friend.playingServer != null) {
            status = new Label("Playing " + friend.playingServer);
            status.getStyleClass().add("friend-status-playing");
        } else {
            status = new Label(friend.online ? "Online" : "Offline");
            status.getStyleClass().add(friend.online ? "friend-status-online" : "friend-status-offline");
        }
        info.getChildren().add(status);

        if (friend.statusMessage != null && !friend.statusMessage.isEmpty()) {
            Label statusMsg = new Label("\"" + friend.statusMessage + "\"");
            statusMsg.getStyleClass().add("friend-status-msg");
            info.getChildren().add(statusMsg);
        }

        Button msgBtn = new Button("Message");
        msgBtn.getStyleClass().add("settings-secondary-btn");
        msgBtn.setOnAction(e -> openConversation(friend.username, false));

        Button discordBtn = new Button("📞");
        discordBtn.getStyleClass().add("friend-discord-btn");
        discordBtn.setTooltip(new Tooltip("Call on Discord"));

        // ··· more menu
        MenuItem setNickname = new MenuItem("Set Nickname");
        setNickname.setOnAction(e -> {
            Stage owner = (Stage) row.getScene().getWindow();
            DarkDialog.showInput(owner, "Nickname for " + friend.username + ":",
                friend.nickname != null ? friend.nickname : "",
                nick -> { friend.nickname = nick.trim().isEmpty() ? null : nick.trim(); updateDisplay(); });
        });

        MenuItem removeFriend = new MenuItem("Remove Friend");
        removeFriend.setOnAction(e -> {
            ApiClient.declineFriend(friend.username).thenAccept(ok -> Platform.runLater(() -> {
                friends.remove(friend);
                updateDisplay();
            }));
        });

        MenuItem reportItem = new MenuItem("Report");
        reportItem.setOnAction(e -> {
            Stage owner = (Stage) row.getScene().getWindow();
            DarkDialog.showAlert(owner, "Report submitted for " + friend.username + ". Our team will review it.");
        });

        MenuItem blockItem = new MenuItem("Block");
        blockItem.setOnAction(e -> ApiClient.blockUser(friend.username).thenAccept(ok -> Platform.runLater(() -> {
            if (ok) { blockedUsers.add(friend.username); updateDisplay(); }
        })));

        MenuButton moreBtn = new MenuButton("···");
        moreBtn.getStyleClass().add("friend-more-btn");
        moreBtn.getItems().addAll(setNickname, removeFriend, reportItem, blockItem);

        row.getChildren().addAll(avatar, info, msgBtn, discordBtn, moreBtn);
        return row;
    }

    private HBox createGroupRow(String groupName) {
        HBox row = new HBox(14);
        row.getStyleClass().add("friend-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("#");
        avatar.getStyleClass().add("group-avatar");

        List<String> members = LauncherEngine.groupMembers.getOrDefault(groupName, new ArrayList<>());
        Label name = new Label(groupName);
        name.getStyleClass().add("friend-name");

        String memberSummary = members.isEmpty() ? "No members yet"
            : String.join(", ", members) + (members.size() == 1 ? " (1 member)" : " (" + members.size() + " members)");
        Label memberLabel = new Label(memberSummary);
        memberLabel.getStyleClass().add("friend-status-offline");

        VBox info = new VBox(3, name, memberLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button openBtn = new Button("Open");
        openBtn.getStyleClass().add("settings-secondary-btn");
        openBtn.setOnAction(e -> openConversation(groupName, true));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("settings-secondary-btn");
        deleteBtn.setStyle("-fx-text-fill: #ff4444;");
        deleteBtn.setOnAction(e -> {
            LauncherEngine.groups.remove(groupName);
            LauncherEngine.groupMembers.remove(groupName);
            LauncherEngine.saveUserSettings();
            if (groupName.equals(activeConversation) && isGroupConversation) {
                activeConversation = null;
                isGroupConversation = false;
                hubRoot.setCenter(hubScrollPane);
            }
            updateDisplay();
        });

        row.getChildren().addAll(avatar, info, openBtn, deleteBtn);
        return row;
    }

    private VBox buildNewGroupForm() {
        Button createBtn = new Button("+ Create Group");
        createBtn.getStyleClass().add("settings-secondary-btn");
        createBtn.setOnAction(e -> openCreateGroupDialog((Stage) serverGrid.getScene().getWindow()));

        VBox form = new VBox(10);
        form.setPadding(new Insets(10, 0, 0, 0));
        form.getChildren().add(createBtn);
        return form;
    }

    private void openCreateGroupDialog(Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setWidth(440);
        dialog.setHeight(540);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(TitleBar.create(dialog));

        VBox content = new VBox(14);
        content.setPadding(new Insets(24, 24, 24, 24));

        Label title = new Label("CREATE GROUP CHAT");
        title.getStyleClass().add("settings-page-title");

        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Group name...");
        groupNameField.getStyleClass().add("auth-field");
        groupNameField.setMaxWidth(Double.MAX_VALUE);

        TextField searchField = new TextField();
        searchField.setPromptText("Search friends...");
        searchField.getStyleClass().add("search-field");

        // Friends listed newest first (reverse of insertion order)
        List<Friend> ordered = new ArrayList<>(friends);
        Collections.reverse(ordered);

        VBox friendsList = new VBox(6);
        List<CheckBox> checkBoxes = new ArrayList<>();
        List<HBox> rows = new ArrayList<>();

        for (Friend f : ordered) {
            Label avatar = new Label(f.username.substring(0, 1).toUpperCase());
            avatar.getStyleClass().add(f.online ? "friend-avatar-online" : "friend-avatar-offline");

            String displayName = (f.nickname != null && !f.nickname.isEmpty())
                ? f.nickname + "  (" + f.username + ")"
                : f.username;

            CheckBox cb = new CheckBox(displayName);
            cb.getStyleClass().add("settings-checkbox");
            cb.setUserData(f.username);
            cb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cb, Priority.ALWAYS);
            checkBoxes.add(cb);

            HBox row = new HBox(12, avatar, cb);
            row.setAlignment(Pos.CENTER_LEFT);
            rows.add(row);
            friendsList.getChildren().add(row);
        }

        // Filter by search
        searchField.textProperty().addListener((obs, old, val) -> {
            String q = val.toLowerCase();
            for (int i = 0; i < ordered.size(); i++) {
                Friend f = ordered.get(i);
                boolean match = f.username.toLowerCase().contains(q)
                    || (f.nickname != null && f.nickname.toLowerCase().contains(q));
                rows.get(i).setVisible(match);
                rows.get(i).setManaged(match);
            }
        });

        ScrollPane scroll = new ScrollPane(friendsList);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        scroll.setPrefHeight(260);
        scroll.setMaxHeight(260);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        boostScrollSpeed(scroll);

        Button createGroupBtn = new Button("Create Group");
        createGroupBtn.getStyleClass().add("auth-btn");
        createGroupBtn.setMaxWidth(Double.MAX_VALUE);
        createGroupBtn.setOnAction(e -> {
            String name = groupNameField.getText().trim();
            if (name.isEmpty()) return;
            List<String> selected = checkBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (String) cb.getUserData())
                .collect(Collectors.toList());
            LauncherEngine.groups.add(name);
            LauncherEngine.groupMembers.put(name, selected);
            LauncherEngine.saveUserSettings();
            MessageStore.getMessages(name);
            dialog.close();
            updateDisplay();
        });

        content.getChildren().addAll(title, groupNameField, searchField, scroll, createGroupBtn);
        root.setCenter(content);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(getClass()));
        SceneUtils.applyRoundedCorners(scene, root);
        dialog.setScene(scene);
        dialog.show();
    }

    // ── CHAT VIEW ────────────────────────────────────────────────────────────

    private void buildChatView() {
        // For group chats we still use local MessageStore; DMs use the API
        List<Message> messages = isGroupConversation
            ? MessageStore.getMessages(activeConversation)
            : new ArrayList<>();

        BorderPane chatPane = new BorderPane();
        chatPane.getStyleClass().add("root-pane");

        // Top bar
        HBox topBar = new HBox(14);
        topBar.getStyleClass().add("chat-topbar");
        topBar.setPadding(new Insets(14, 20, 14, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Friends");
        backBtn.getStyleClass().add("settings-back-btn");
        backBtn.setOnAction(e -> closeConversation());

        Label convLabel = new Label(activeConversation);
        convLabel.getStyleClass().add("chat-conv-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button discordCallBtn = new Button("📞 Call on Discord");
        discordCallBtn.getStyleClass().add("friend-discord-btn");
        discordCallBtn.setTooltip(new Tooltip("Wire this to a Discord server link when backend is ready"));

        topBar.getChildren().addAll(backBtn, convLabel, spacer, discordCallBtn);

        // Group chat: show members + Add Member button below top bar
        if (isGroupConversation) {
            List<String> members = LauncherEngine.groupMembers.computeIfAbsent(activeConversation, k -> new ArrayList<>());

            FlowPane memberChips = new FlowPane(6, 6);
            memberChips.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(memberChips, Priority.ALWAYS);

            Runnable rebuildChips = () -> {
                memberChips.getChildren().clear();
                for (String m : members) {
                    String init = m.isEmpty() ? "?" : String.valueOf(m.charAt(0)).toUpperCase();
                    Label chip = new Label(init + "  " + m);
                    chip.setStyle(
                        "-fx-background-color: #1a1d24; -fx-border-color: #2a2e39;" +
                        "-fx-border-radius: 12; -fx-background-radius: 12;" +
                        "-fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 3 10;"
                    );
                    memberChips.getChildren().add(chip);
                }
                if (members.isEmpty()) {
                    Label none = new Label("No members yet");
                    none.getStyleClass().add("auth-muted");
                    memberChips.getChildren().add(none);
                }
            };
            rebuildChips.run();

            // Add member dropdown
            MenuButton addMemberBtn = new MenuButton("+ Add");
            addMemberBtn.getStyleClass().add("settings-secondary-btn");
            addMemberBtn.setStyle("-fx-background-color: #d0d4de; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-background-radius: 4;");
            for (Friend f : friends) {
                if (!members.contains(f.username)) {
                    MenuItem item = new MenuItem(f.username);
                    item.setOnAction(e -> {
                        members.add(f.username);
                        rebuildChips.run();
                        addMemberBtn.getItems().remove(item);
                    });
                    addMemberBtn.getItems().add(item);
                }
            }

            HBox membersBar = new HBox(10, memberChips, addMemberBtn);
            membersBar.getStyleClass().add("chat-members-bar");
            membersBar.setPadding(new Insets(8, 20, 8, 20));
            membersBar.setAlignment(Pos.CENTER_LEFT);

            chatPane.setTop(new VBox(topBar, membersBar));
        } else {
            chatPane.setTop(topBar);
        }

        // Messages
        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(16, 20, 16, 20));
        for (Message msg : messages)
            messagesBox.getChildren().add(buildBubble(msg));

        ScrollPane msgScroll = new ScrollPane(messagesBox);
        msgScroll.setFitToWidth(true);
        msgScroll.getStyleClass().add("main-scroll");
        boostScrollSpeed(msgScroll);
        // Auto-scroll to bottom when new messages added
        messagesBox.heightProperty().addListener((obs, old, h) -> msgScroll.setVvalue(1.0));
        msgScroll.setVvalue(1.0);
        chatPane.setCenter(msgScroll);

        // Load DM history from API
        if (!isGroupConversation) {
            ApiClient.getMessages(activeConversation).thenAccept(apiMessages -> Platform.runLater(() -> {
                messagesBox.getChildren().clear();
                for (Message msg : apiMessages) messagesBox.getChildren().add(buildBubble(msg));
            }));
        }

        // Input bar
        HBox inputBar = new HBox(10);
        inputBar.getStyleClass().add("chat-input-bar");
        inputBar.setPadding(new Insets(12, 20, 16, 20));
        inputBar.setAlignment(Pos.CENTER);

        TextField inputField = new TextField();
        inputField.setPromptText("Message " + activeConversation + "...");
        inputField.getStyleClass().add("search-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("auth-btn");
        sendBtn.setPrefWidth(80);

        String convId = activeConversation;
        boolean isGroup = isGroupConversation;
        Runnable send = () -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            String sender = LauncherEngine.currentUsername.isEmpty() ? "You" : LauncherEngine.currentUsername;
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
            Message msg = new Message(sender, text, time, true);
            inputField.clear();
            if (isGroup) {
                MessageStore.addMessage(convId, msg);
                messagesBox.getChildren().add(buildBubble(msg));
            } else {
                // Optimistic UI — add bubble immediately, send async
                messagesBox.getChildren().add(buildBubble(msg));
                ApiClient.sendMessage(convId, text);
            }
        };

        inputField.setOnAction(e -> send.run());
        sendBtn.setOnAction(e -> send.run());
        inputBar.getChildren().addAll(inputField, sendBtn);
        chatPane.setBottom(inputBar);

        hubRoot.setCenter(chatPane);
    }

    private HBox buildBubble(Message msg) {
        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);

        VBox bubble = new VBox(3);

        if (isGroupConversation && !msg.isOwn) {
            Label senderLbl = new Label(msg.sender);
            senderLbl.getStyleClass().add("msg-sender-name");
            bubble.getChildren().add(senderLbl);
        }

        Label content = new Label(msg.content);
        content.setWrapText(true);
        content.setMaxWidth(420);
        content.getStyleClass().add(msg.isOwn ? "msg-bubble-me" : "msg-bubble-them");

        Label time = new Label(msg.timestamp);
        time.getStyleClass().add("msg-timestamp");

        if (msg.isOwn) {
            Label receipt = new Label("✓ Sent");
            receipt.getStyleClass().add("msg-read-receipt");
            bubble.getChildren().addAll(content, time, receipt);
        } else {
            bubble.getChildren().addAll(content, time);
        }

        if (msg.isOwn) {
            row.setAlignment(Pos.CENTER_RIGHT);
            time.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }

        row.getChildren().add(bubble);
        return row;
    }

    // ── SERVER CARD ──────────────────────────────────────────────────────────

    private VBox createServerCard(ServerProfile server) {
        int    skillLevel    = ServerSkillSystem.getLevel(server.name);
        double skillProgress = ServerSkillSystem.getLevelProgress(server.name);

        HBox card = new HBox(20);
        card.getStyleClass().add("server-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane bannerPane = new StackPane();
        bannerPane.setPrefSize(300, 150);
        bannerPane.setMinSize(300, 150);
        bannerPane.setMaxSize(300, 150);
        bannerPane.getStyleClass().add("card-banner");
        // Clip so the image never bleeds outside the thumbnail area
        javafx.scene.shape.Rectangle bannerClip = new javafx.scene.shape.Rectangle(300, 150);
        bannerClip.setArcWidth(8); bannerClip.setArcHeight(8);
        bannerPane.setClip(bannerClip);

        Label bannerLabel = new Label(server.name);
        bannerLabel.getStyleClass().add("card-banner-placeholder");
        bannerPane.getChildren().add(bannerLabel);

        // Card banner — prefer dedicated card_banner_url, fall back to banner_url
        String cardBannerSrc = (server.cardBannerUrl != null && !server.cardBannerUrl.isEmpty())
                ? server.cardBannerUrl : server.bannerUrl;
        if (cardBannerSrc != null && !cardBannerSrc.isEmpty()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(300); iv.setFitHeight(150);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            iv.setVisible(false);
            bannerPane.getChildren().add(iv);
            ImageCache.load(cardBannerSrc, img -> {
                iv.setImage(img); iv.setVisible(true); bannerLabel.setVisible(false);
            });
        }

        if (server.isNew) {
            Label newBadge = new Label("NEW");
            newBadge.getStyleClass().add("new-badge");
            StackPane.setAlignment(newBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(newBadge, new Insets(6));
            bannerPane.getChildren().add(newBadge);
        }

        int streakVal = StreakStore.getStreak(server.name);
        if (streakVal >= 1) {
            Label streakOverlay = new Label("\uD83D\uDD25 " + streakVal);
            streakOverlay.setStyle("-fx-text-fill: #e05252; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.65); -fx-background-radius: 6; -fx-padding: 2 7;");
            StackPane.setAlignment(streakOverlay, Pos.BOTTOM_LEFT);
            StackPane.setMargin(streakOverlay, new Insets(6));
            bannerPane.getChildren().add(streakOverlay);
        }

        VBox info = new VBox(5);
        Label title = new Label(server.name);
        title.getStyleClass().add("card-title");

        String rawDesc = server.description != null ? server.description : "";
        // Flatten newlines, cap at 240 chars (~3 lines at card width)
        String flatDesc = rawDesc.replace("\n", " ").replace("\r", "").replaceAll("\\s+", " ").trim();
        String shortDesc = flatDesc.length() > 240 ? flatDesc.substring(0, 240).trim() + "…" : flatDesc;
        Label desc = new Label(shortDesc);
        desc.setWrapText(true);
        desc.getStyleClass().add("card-desc");
        desc.setMaxWidth(Double.MAX_VALUE);

        HBox tagBox = new HBox(5);
        if (server.tags != null)
            for (String t : server.tags) { Label p = new Label(t.toUpperCase()); p.getStyleClass().add("tag-pill"); tagBox.getChildren().add(p); }

        // Spacer pushes tags to the bottom of the card
        Region infoSpacer = new Region();
        VBox.setVgrow(infoSpacer, Priority.ALWAYS);

        info.getChildren().addAll(title, desc, infoSpacer, tagBox);
        HBox.setHgrow(info, Priority.ALWAYS);

        String existingNote = LauncherEngine.serverNotes.get(server.name);
        if (existingNote != null && !existingNote.isEmpty()) {
            Label noteLbl = new Label("📝  " + existingNote);
            noteLbl.getStyleClass().add("server-note-text");
            info.getChildren().add(noteLbl);
        }

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        boolean isFav = LauncherEngine.favouriteServers.contains(server.name);
        Button starBtn = new Button(isFav ? "★" : "☆");
        starBtn.getStyleClass().add(isFav ? "fav-btn-active" : "fav-btn");
        starBtn.setOnAction(e -> {
            e.consume();
            if (LauncherEngine.favouriteServers.contains(server.name)) LauncherEngine.favouriteServers.remove(server.name);
            else LauncherEngine.favouriteServers.add(server.name);
            LauncherEngine.saveSettings();
            updateDisplay();
        });

        Button noteBtn = new Button("📝");
        noteBtn.getStyleClass().add("note-btn");
        String currentNote = LauncherEngine.serverNotes.get(server.name);
        noteBtn.setTooltip(new Tooltip(currentNote != null ? currentNote : "Add a note"));
        noteBtn.setOnAction(e -> {
            e.consume();
            String note = LauncherEngine.serverNotes.get(server.name);
            DarkDialog.showInput((Stage) card.getScene().getWindow(),
                "Note for " + server.name + ":", note != null ? note : "",
                result -> {
                    if (result.trim().isEmpty()) LauncherEngine.serverNotes.remove(server.name);
                    else LauncherEngine.serverNotes.put(server.name, result.trim());
                    LauncherEngine.saveSettings();
                    updateDisplay();
                });
        });

        HBox cardTools = new HBox(4, starBtn, noteBtn);
        cardTools.setAlignment(Pos.CENTER_RIGHT);

        String milestoneColor = ServerSkillSystem.getMilestoneColor(skillLevel);
        Label levelBadge = new Label("Lv. " + skillLevel);
        levelBadge.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-text-fill: " + milestoneColor + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 10; -fx-cursor: hand;");

        boolean[] overPopup = {false};
        Popup[]   popupRef  = {null};
        levelBadge.setOnMouseEntered(e -> {
            if (popupRef[0] != null && popupRef[0].isShowing()) return;
            Popup p = buildLevelPopup(server.name, skillLevel, skillProgress, milestoneColor, overPopup, popupRef);
            popupRef[0] = p;
            Bounds b = levelBadge.localToScreen(levelBadge.getBoundsInLocal());
            p.show(levelBadge, b.getMinX(), b.getMaxY() + 6);
        });
        levelBadge.setOnMouseExited(e -> {
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));
            delay.setOnFinished(ev -> { if (!overPopup[0] && popupRef[0] != null) { popupRef[0].hide(); popupRef[0] = null; } });
            delay.play();
        });

        // Updated to use playersOnline
        Label players = new Label("\uD83D\uDFE2 " + server.playersOnline + " Online");
        players.getStyleClass().add("player-count");

        boolean downloaded = LauncherEngine.isDownloaded(server);
        Button playBtn = new Button(downloaded ? "PLAY" : "INSTALL");
        playBtn.getStyleClass().add("play-button");
        playBtn.setOnAction(e -> {
            e.consume();
            handlePlayAction(server, (Stage) card.getScene().getWindow(), playBtn, () -> { if (showingLibrary) updateDisplay(); });
        });

        actions.getChildren().addAll(levelBadge, cardTools, players, playBtn);

        if (showingLibrary && downloaded) {
            Button uninstallBtn = new Button("Uninstall");
            uninstallBtn.getStyleClass().add("settings-logout-btn");
            uninstallBtn.setOnAction(e -> {
                e.consume();
                Stage owner = (Stage) card.getScene().getWindow();
                DarkDialog.showAlert(owner, "Uninstalling " + server.name + "...");
                new Thread(() -> {
                    LauncherEngine.uninstallServer(server);
                    javafx.application.Platform.runLater(this::updateDisplay);
                }).start();
            });
            actions.getChildren().add(uninstallBtn);
        }

        card.getChildren().addAll(bannerPane, info, actions);
        card.setOnMouseClicked(e -> showServerDetail((Stage) card.getScene().getWindow(), server));

        Region xpFill = new Region();
        xpFill.getStyleClass().add("xp-bar-fill");
        xpFill.setPrefHeight(4);
        xpFill.setMaxWidth(Double.MAX_VALUE);

        StackPane xpTrack = new StackPane();
        xpTrack.getStyleClass().add("xp-bar-track");
        xpTrack.setPrefHeight(4);
        xpTrack.setAlignment(Pos.CENTER_LEFT);
        xpTrack.widthProperty().addListener((obs, old, w) -> xpFill.setPrefWidth(w.doubleValue() * skillProgress));
        xpTrack.getChildren().add(xpFill);

        VBox wrapper = new VBox(card, xpTrack);
        wrapper.getStyleClass().add("server-card-wrapper");
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    // ── LEVEL POPUP ──────────────────────────────────────────────────────────

    private Popup buildLevelPopup(String serverName, int level, double progress, String color,
                                   boolean[] overPopup, Popup[] popupRef) {
        Popup popup = new Popup();
        popup.setAutoHide(false);

        VBox box = new VBox(10);
        box.setStyle(
            "-fx-background-color: #1a1d24;" +
            "-fx-border-color: #2a2e39;" +
            "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 4);"
        );
        box.setPadding(new Insets(16, 20, 16, 20));
        box.setPrefWidth(290);

        // Header row: server name + level
        Label nameLbl = new Label(serverName);
        nameLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px;");
        Label lvlLbl = new Label("Lv. " + level);
        lvlLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(nameLbl, hSpacer, lvlLbl);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Big level number + rank side by side — unconstrained
        String rank = ServerSkillSystem.getRankName(level);
        Label bigLvl = new Label(String.valueOf(level));
        bigLvl.setStyle("-fx-text-fill: white; -fx-font-size: 48px; -fx-font-weight: bold;");
        bigLvl.setMinWidth(Region.USE_PREF_SIZE);

        Label rankLbl = new Label(rank);
        rankLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        rankLbl.setMinWidth(Region.USE_PREF_SIZE);

        VBox rankStack = new VBox(2, rankLbl);
        rankStack.setAlignment(Pos.BOTTOM_LEFT);

        HBox levelRow = new HBox(14, bigLvl, rankStack);
        levelRow.setAlignment(Pos.CENTER_LEFT);

        // XP bar track
        StackPane track = new StackPane();
        track.setStyle("-fx-background-color: #0f1115; -fx-background-radius: 4;");
        track.setPrefHeight(8);
        track.setMaxWidth(Double.MAX_VALUE);
        track.setAlignment(Pos.CENTER_LEFT);

        Region fill = new Region();
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        fill.setPrefHeight(8);
        fill.setPrefWidth(0);
        track.getChildren().add(fill);

        // XP numbers
        long played = PlaytimeStore.getMinutes(serverName);
        long toNext = ServerSkillSystem.minutesToNextLevel(serverName);
        String playedStr = played < 60 ? played + "m" : (played / 60) + "h " + (played % 60) + "m";
        String nextStr   = level >= 99 ? "MAX" : (toNext < 60 ? toNext + "m to next level" : (toNext / 60) + "h " + (toNext % 60) + "m to next level");
        Label xpLbl = new Label(playedStr + " played");
        xpLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px;");
        Label nextLbl = new Label(level >= 99 ? "MAX LEVEL" : nextStr);
        nextLbl.setStyle("-fx-text-fill: " + (level >= 99 ? color : "#c8cdd8") + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Region xpSpacer = new Region(); HBox.setHgrow(xpSpacer, Priority.ALWAYS);
        HBox xpRow = new HBox(xpLbl, xpSpacer, nextLbl);

        box.getChildren().addAll(headerRow, levelRow, track, xpRow);

        box.setOnMouseEntered(e -> overPopup[0] = true);
        box.setOnMouseExited(e -> {
            overPopup[0] = false;
            popup.hide();
            popupRef[0] = null;
        });

        popup.getContent().add(box);

        // Animate XP bar fill after popup shows — use actual track width
        popup.setOnShown(e -> Platform.runLater(() -> {
            double trackW  = track.getWidth();
            double targetW = trackW * Math.min(1.0, progress);
            Timeline barAnim = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(fill.prefWidthProperty(), 0)),
                new KeyFrame(Duration.millis(500), new KeyValue(fill.prefWidthProperty(), targetW))
            );
            barAnim.play();
        }));

        return popup;
    }

    // ── CARD ANIMATION ───────────────────────────────────────────────────────

    private void animateCard(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(18);
        FadeTransition fade = new FadeTransition(Duration.millis(250), card);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), card);
        slide.setFromY(18); slide.setToY(0);
        ParallelTransition anim = new ParallelTransition(fade, slide);
        anim.setDelay(Duration.millis(Math.min(index, 6) * 55L));
        anim.play();
    }

    // ── UTILITIES ────────────────────────────────────────────────────────────

    /** Fast, smooth scrolling for a ScrollPane. */
    private static void boostScrollSpeed(ScrollPane sp) {
        final double[] velocity = {0};
        final Timeline[] momentum = {null};

        sp.getContent().setOnScroll(e -> {
            velocity[0] += e.getDeltaY() * 1.5; // pixels of momentum

            if (momentum[0] != null) momentum[0].stop();
            Timeline anim = new Timeline();
            momentum[0] = anim;

            for (int i = 1; i <= 20; i++) {
                anim.getKeyFrames().add(new KeyFrame(Duration.millis(i * 16), ev -> {
                    double contentH  = sp.getContent().getBoundsInLocal().getHeight();
                    double viewportH = sp.getViewportBounds().getHeight();
                    double scrollable = contentH - viewportH;
                    if (scrollable <= 0) return;
                    sp.setVvalue(sp.getVvalue() - velocity[0] / scrollable);
                    velocity[0] *= 0.82; // friction / deceleration
                }));
            }
            anim.setOnFinished(ev -> velocity[0] = 0);
            anim.play();
            e.consume();
        });
    }

    // ── STATS CONTENT ────────────────────────────────────────────────────────

    private void buildStatsContent() {
        serverGrid.setSpacing(20);
        serverGrid.setPadding(new Insets(30, 40, 40, 40));

        long totalMins   = PlaytimeStore.getTotalMinutes();
        int  serversPlayed = PlaytimeStore.getTotalServersPlayed();
        String mostPlayed  = PlaytimeStore.getMostPlayed();

        // Stat boxes row
        HBox statBoxes = new HBox(16);
        statBoxes.getChildren().addAll(
            statBox(formatMinutes(totalMins), "Total Playtime"),
            statBox(String.valueOf(serversPlayed), "Servers Played"),
            statBox(mostPlayed != null ? mostPlayed : "—", "Most Played")
        );
        serverGrid.getChildren().add(statBoxes);

        // Per-server bars
        Map<String, Long> all = PlaytimeStore.getAllMinutes();
        if (!all.isEmpty()) {
            Label hdr = new Label("PER SERVER");
            hdr.getStyleClass().add("friends-section-header");
            hdr.setPadding(new Insets(10, 0, 4, 0));
            serverGrid.getChildren().add(hdr);

            long max = all.values().stream().mapToLong(Long::longValue).max().orElse(1);
            all.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> {
                    Label name = new Label(e.getKey());
                    name.getStyleClass().add("stats-server-name");
                    name.setMinWidth(160);

                    Region fill = new Region();
                    fill.getStyleClass().add("play-button");
                    fill.setPrefHeight(8);
                    double ratio = (double) e.getValue() / max;
                    fill.prefWidthProperty().bind(serverGrid.widthProperty().multiply(ratio * 0.5));

                    Label time = new Label(formatMinutes(e.getValue()));
                    time.getStyleClass().add("stats-time-label");

                    StackPane track = new StackPane(fill);
                    track.getStyleClass().add("stats-bar-track");
                    track.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(track, Priority.ALWAYS);

                    HBox row = new HBox(12, name, track, time);
                    row.setAlignment(Pos.CENTER_LEFT);
                    serverGrid.getChildren().add(row);
                });
        }

        // Session history
        List<SessionHistoryStore.SessionRecord> sessions = SessionHistoryStore.getHistory();
        if (!sessions.isEmpty()) {
            Label hdr2 = new Label("RECENT SESSIONS");
            hdr2.getStyleClass().add("friends-section-header");
            hdr2.setPadding(new Insets(14, 0, 4, 0));
            serverGrid.getChildren().add(hdr2);

            for (SessionHistoryStore.SessionRecord rec : sessions) {
                HBox row = new HBox(16);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 16, 10, 16));
                row.setStyle("-fx-background-color: #1a1d24; -fx-background-radius: 8;");

                // Colour dot
                Label dot = new Label("▶");
                dot.setStyle("-fx-text-fill: " + LauncherEngine.accentColor + "; -fx-font-size: 11px;");

                Label serverName = new Label(rec.serverName);
                serverName.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                HBox.setHgrow(serverName, Priority.ALWAYS);

                Label duration = new Label(formatMinutes(rec.minutes));
                duration.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");

                Label date = new Label(rec.date);
                date.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px; -fx-min-width: 90;");
                date.setAlignment(Pos.CENTER_RIGHT);

                row.getChildren().addAll(dot, serverName, duration, date);
                serverGrid.getChildren().add(row);
            }
        }
    }

    private VBox statBox(String value, String label) {
        Label val = new Label(value);
        val.getStyleClass().add("profile-stat-value");
        val.setWrapText(true);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("profile-stat-label");
        VBox box = new VBox(4, val, lbl);
        box.getStyleClass().add("profile-stat-box");
        box.setPadding(new Insets(16, 20, 16, 20));
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String formatMinutes(long mins) {
        if (mins < 60) return mins + "m";
        return (mins / 60) + "h " + (mins % 60) + "m";
    }

    // ── LEADERBOARD CONTENT ──────────────────────────────────────────────────

    private void buildLeaderboardContent() {
        serverGrid.setSpacing(10);
        serverGrid.setPadding(new Insets(30, 40, 40, 40));

        Label sub = new Label("Top players by total playtime");
        sub.getStyleClass().add("settings-about-sub");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2e39;");

        serverGrid.getChildren().addAll(sub, sep);
        buildLeaderboardRows();
    }

    private void buildLeaderboardRows() {
        serverGrid.getChildren().removeIf(n -> n.getUserData() != null && n.getUserData().equals("row"));

        String yourName = LauncherEngine.currentUsername.isEmpty() ? "You" : LauncherEngine.currentUsername;

        record LeaderEntry(int rank, String username, String topServer, long minutes, boolean isYou) {}

        List<LeaderEntry> entries = new ArrayList<>();
        // Add yourself
        long yourMin = PlaytimeStore.getTotalMinutes();
        String yourTop = PlaytimeStore.getMostPlayed();
        entries.add(new LeaderEntry(0, yourName, yourTop != null ? yourTop : "—", yourMin, true));
        // Add friends (playtime not yet synced from server — shown as 0 until backend supports it)
        for (Friend f : friends) {
            entries.add(new LeaderEntry(0, f.username, "—", 0, false));
        }

        entries.sort(java.util.Comparator.comparingLong(LeaderEntry::minutes).reversed());
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            entries.set(i, new LeaderEntry(i + 1, e.username(), e.topServer(), e.minutes(), e.isYou()));
        }

        boolean anyRows = false;
        for (var entry : entries) {
            anyRows = true;

            HBox row = new HBox(16);
            row.setUserData("row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 18, 14, 18));

            String rankColor = switch (entry.rank()) {
                case 1 -> "#ffd700"; case 2 -> "#c0c0c0"; case 3 -> "#cd7f32"; default -> "#8b92a5";
            };
            Label rankLbl = new Label("#" + entry.rank());
            rankLbl.setStyle("-fx-text-fill: " + rankColor + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-min-width: 44;");

            String initial = entry.username().isEmpty() ? "?" : String.valueOf(entry.username().charAt(0)).toUpperCase();
            Label avatar = new Label(initial);
            avatar.setStyle(
                "-fx-background-color: " + (entry.isYou() ? LauncherEngine.accentColor : "#2a2e39") + ";" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center;" +
                "-fx-min-width: 38; -fx-min-height: 38; -fx-max-width: 38; -fx-max-height: 38;" +
                "-fx-background-radius: 20;"
            );
            avatar.setAlignment(Pos.CENTER);

            Label name = new Label(entry.username() + (entry.isYou() ? "  (you)" : ""));
            name.setStyle("-fx-text-fill: " + (entry.isYou() ? LauncherEngine.accentColor : "white") +
                "; -fx-font-size: 14px; -fx-font-weight: bold;");

            int lvl = ServerSkillSystem.minutesToLevel(entry.minutes());
            String lvlColor = ServerSkillSystem.getMilestoneColor(lvl);
            Label levelLbl = new Label("Lv." + lvl + " " + ServerSkillSystem.getRankName(lvl));
            levelLbl.setStyle("-fx-text-fill: " + lvlColor + "; -fx-font-size: 11px;");

            VBox nameBox = new VBox(2, name, levelLbl);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            long h = entry.minutes() / 60, m2 = entry.minutes() % 60;
            Label time = new Label(entry.minutes() == 0 ? "No time yet"
                : h > 0 ? h + "h " + m2 + "m" : m2 + "m");
            time.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px; -fx-font-weight: bold;");

            row.getChildren().addAll(rankLbl, avatar, nameBox, time);
            row.setStyle(entry.isYou()
                ? "-fx-background-color: rgba(255,152,31,0.08); -fx-background-radius: 8; -fx-border-color: rgba(255,152,31,0.25); -fx-border-radius: 8;"
                : "-fx-background-color: #1a1d24; -fx-background-radius: 8;");

            serverGrid.getChildren().add(row);
        }

        if (!anyRows) {
            Label empty = new Label("No players ranked for this server yet.");
            empty.getStyleClass().add("settings-about-sub");
            empty.setUserData("row");
            serverGrid.getChildren().add(empty);
        }
    }

    // ── NOTIFICATION POPUP ───────────────────────────────────────────────────

    private void refreshNotifBadge() {
        long unread = notifications.stream().filter(n -> !n.read).count();
        if (notifBadge != null) {
            notifBadge.setText(String.valueOf(unread));
            notifBadge.setVisible(unread > 0);
            notifBadge.setManaged(unread > 0);
        }
    }

    private void showNotificationPopup(Button anchor) {
        // Toggle off if already showing
        if (notifPopup != null && notifPopup.isShowing()) { notifPopup.hide(); notifPopup = null; return; }

        Popup popup = new Popup();
        popup.setAutoHide(true);
        notifPopup = popup;
        popup.setOnHidden(e -> notifPopup = null);

        VBox box = new VBox(0);
        box.setStyle(
            "-fx-background-color: #1a1d24; -fx-border-color: #2a2e39; -fx-border-width: 1;" +
            "-fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 6);"
        );
        box.setPrefWidth(340);

        // Header
        Label title = new Label("NOTIFICATIONS");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        String btnStyle = "-fx-background-color: transparent; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0;";
        Button markRead = new Button("Mark all read");
        markRead.setStyle(btnStyle + "-fx-text-fill: #ff981f;");
        markRead.setOnAction(e -> { notifications.forEach(n -> n.read = true); popup.hide(); refreshNotifBadge(); });

        Label sep = new Label("·");
        sep.setStyle("-fx-text-fill: #3a3e4a;");

        Button clearAll = new Button("Clear all");
        clearAll.setStyle(btnStyle + "-fx-text-fill: #8b92a5;");
        clearAll.setOnAction(e -> { notifications.clear(); popup.hide(); refreshNotifBadge(); });

        Region hSp = new Region(); HBox.setHgrow(hSp, Priority.ALWAYS);
        HBox header = new HBox(6, title, hSp, markRead, sep, clearAll);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 12, 16));
        header.setStyle("-fx-border-color: #2a2e39; -fx-border-width: 0 0 1 0;");
        box.getChildren().add(header);

        if (notifications.isEmpty()) {
            Label none = new Label("No notifications");
            none.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
            VBox empty = new VBox(none);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(28, 0, 28, 0));
            box.getChildren().add(empty);
        } else {
            ScrollPane scroll = new ScrollPane();
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            scroll.setMaxHeight(420);

            VBox list = new VBox(0);
            for (int i = 0; i < notifications.size(); i++) {
                AppNotif n = notifications.get(i);
                list.getChildren().add(buildNotifRow(n, popup));
                if (i < notifications.size() - 1) {
                    Region div = new Region();
                    div.setPrefHeight(1);
                    div.setStyle("-fx-background-color: #22252e;");
                    list.getChildren().add(div);
                }
            }
            scroll.setContent(list);
            scroll.setFitToWidth(true);
            box.getChildren().add(scroll);
        }

        popup.getContent().add(box);

        // Position below the bell button
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor, b.getMaxX() - 340, b.getMaxY() + 8);
    }

    private VBox buildNotifRow(AppNotif n, Popup popup) {
        String icon = switch (n.type) {
            case FRIEND_REQUEST -> "👤";
            case FRIEND_ONLINE  -> "🟢";
            case SERVER_UPDATE  -> "🎮";
            case SYSTEM         -> "🔔";
        };
        String iconColor = switch (n.type) {
            case FRIEND_REQUEST -> "#ff981f";
            case FRIEND_ONLINE  -> "#4caf50";
            case SERVER_UPDATE  -> "#4a9eff";
            case SYSTEM         -> "#8b92a5";
        };

        Label iconLbl = new Label(icon);
        iconLbl.setStyle(
            "-fx-background-color: " + iconColor + "22; -fx-text-fill: " + iconColor + ";" +
            "-fx-font-size: 16px; -fx-background-radius: 50; -fx-padding: 6;" +
            "-fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center;"
        );

        Label titleLbl = new Label(n.title);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label bodyLbl = new Label(n.body);
        bodyLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(220);

        Label timeLbl = new Label(n.time);
        timeLbl.setStyle("-fx-text-fill: #555b6e; -fx-font-size: 11px;");

        VBox textCol = new VBox(2, titleLbl, bodyLbl, timeLbl);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox row = new HBox(12, iconLbl, textCol);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        String baseBg = n.read ? "transparent" : "#1e2333";
        row.setStyle("-fx-background-color: " + baseBg + "; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #22252e; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: " + baseBg + "; -fx-cursor: hand;"));

        // For friend requests: add accept/decline buttons
        if (n.type == NotifType.FRIEND_REQUEST && n.friendUsername != null) {
            Button acceptBtn = new Button("Accept");
            acceptBtn.setStyle(
                "-fx-background-color: #ff981f; -fx-text-fill: white; -fx-font-size: 11px;" +
                "-fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;"
            );
            Button declineBtn = new Button("Decline");
            declineBtn.setStyle(
                "-fx-background-color: #2a2e39; -fx-text-fill: #8b92a5; -fx-font-size: 11px;" +
                "-fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;"
            );
            final String uname = n.friendUsername;
            acceptBtn.setOnAction(e -> {
                friends.add(new Friend(uname, true, null));
                friendRequests.removeIf(r -> r.username.equals(uname));
                notifications.remove(n);
                ActivityStore.add(new ActivityItem(uname, "joined as your friend", "", "Just now"));
                popup.hide();
                refreshNotifBadge();
            });
            declineBtn.setOnAction(e -> {
                friendRequests.removeIf(r -> r.username.equals(uname));
                notifications.remove(n);
                popup.hide();
                refreshNotifBadge();
            });
            HBox actions = new HBox(6, acceptBtn, declineBtn);
            actions.setPadding(new Insets(6, 0, 0, 0));
            textCol.getChildren().add(actions);
        }

        VBox wrapper = new VBox(row);
        return wrapper;
    }

    // ── DOWNLOADS POPUP ──────────────────────────────────────────────────────

    private void showDownloadsPopup(Button anchor) {
        ContextMenu menu = new ContextMenu();

        if (activeDownloads.isEmpty()) {
            Label none = new Label("No active downloads");
            none.getStyleClass().add("auth-muted");
            none.setPadding(new Insets(8, 16, 8, 16));
            if (LauncherEngine.lightMode) none.setStyle("-fx-text-fill: #5a6070;");
            menu.getItems().add(new CustomMenuItem(none, false));
        } else {
            for (DownloadItem item : new ArrayList<>(activeDownloads)) {
                VBox row = new VBox(4);
                row.setPadding(new Insets(8, 14, 8, 14));
                row.setPrefWidth(320);
                if (LauncherEngine.lightMode) row.setStyle("-fx-background-color: #ffffff;");

                Label nameLbl = new Label(item.serverName);
                nameLbl.getStyleClass().add("download-row-name");
                if (LauncherEngine.lightMode) nameLbl.setStyle("-fx-text-fill: #1a1a2e;");

                ProgressBar bar = new ProgressBar();
                bar.progressProperty().bind(item.progress);
                bar.setPrefWidth(260);

                Label statusLbl = new Label();
                statusLbl.getStyleClass().add("download-row-status");
                statusLbl.textProperty().bind(item.status);
                if (LauncherEngine.lightMode) statusLbl.setStyle("-fx-text-fill: #5a6070;");

                Button cancelBtn = new Button("Cancel");
                cancelBtn.getStyleClass().add("settings-secondary-btn");
                cancelBtn.setOnAction(e -> {
                    item.cancelled = true;
                    menu.hide();
                });

                HBox bottomRow = new HBox(10, statusLbl, cancelBtn);
                bottomRow.setAlignment(Pos.CENTER_LEFT);

                row.getChildren().addAll(nameLbl, bar, bottomRow);
                menu.getItems().add(new CustomMenuItem(row, false));
            }
        }

        menu.show(anchor, Side.BOTTOM, 0, 4);
    }

    // ── PLAY HANDLER ─────────────────────────────────────────────────────────

    private void launchAndTrack(ServerProfile server, Stage stage) {
        if (LauncherEngine.minimizeOnLaunch) stage.setIconified(true);
        Process proc = LauncherEngine.launchGame(server);
        if (proc != null) {
            long startEpoch = System.currentTimeMillis() / 1000;
            DiscordRPC.setActivity(server.name, startEpoch);
            beginSession(server.name, proc, stage);
        }
    }

    /**
     * Starts session tracking for a launched game process.
     * Uses ProcessHandle descendant tracking so that self-updating launcher JARs
     * (which spawn the actual game as a child then exit) are tracked correctly.
     * The timer only stops when every spawned process has exited — it cannot be
     * manually extended, making it safe for reward-based playtime systems.
     */
    static void beginSession(String serverName, Process proc, Stage stage) {
        LauncherEngine.activeServer = serverName;
        sessionStage = stage;
        long start = System.currentTimeMillis();
        sessionStart = start;

        // Start the navbar timer on the FX thread
        Platform.runLater(() -> {
            if (sessionTimerLabel != null) {
                sessionTimerLabel.setText("\u25B6 0:00:00");
                sessionTimerLabel.setVisible(true);
                sessionTimerLabel.setManaged(true);
            }
            if (sessionTimeline != null) sessionTimeline.stop();
            final long[] elapsed = {0};
            sessionTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                elapsed[0]++;
                long h = elapsed[0] / 3600, m = (elapsed[0] % 3600) / 60, s = elapsed[0] % 60;
                if (sessionTimerLabel != null)
                    sessionTimerLabel.setText(String.format("\u25B6 %d:%02d:%02d", h, m, s));
            }));
            sessionTimeline.setCycleCount(Timeline.INDEFINITE);
            sessionTimeline.play();
        });

        new Thread(() -> {
            // ── Descendant tracking ────────────────────────────────────────────
            // Many RSPS clients are self-updating launcher JARs: they download cache,
            // spawn the real game as a child process, then exit themselves.
            // We collect all descendant ProcessHandles while the initial proc is alive,
            // so we can keep waiting even after the launcher JAR exits.
            java.util.Set<ProcessHandle> watched = new java.util.LinkedHashSet<>();
            watched.add(proc.toHandle());

            // Poll every 500 ms while the launcher process is alive
            while (proc.isAlive()) {
                proc.toHandle().descendants().forEach(watched::add);
                try { Thread.sleep(500); } catch (InterruptedException ignored) { break; }
            }
            // One final snapshot after exit (child may appear in the last moment)
            try { proc.toHandle().descendants().forEach(watched::add); } catch (Exception ignored) {}

            // Now wait for every tracked process (initial + all descendants) to exit
            for (ProcessHandle ph : watched) {
                if (ph.isAlive()) {
                    try { ph.onExit().get(); } catch (Exception ignored) {}
                }
            }
            // ──────────────────────────────────────────────────────────────────

            long mins = (System.currentTimeMillis() - start) / 60000;

            // Only record sessions of at least 1 minute to filter out accidents
            if (mins >= 1) {
                PlaytimeStore.recordSession(serverName, mins);
                SessionHistoryStore.add(serverName, mins);
                StreakStore.recordPlay(serverName);
                ApiClient.updateStats(
                    PlaytimeStore.getTotalMinutes(),
                    PlaytimeStore.getTotalServersPlayed(),
                    PlaytimeStore.getMostPlayed()
                );
            }

            Platform.runLater(() -> {
                LauncherEngine.activeServer = null;
                DiscordRPC.setBrowsing("Browsing the store");
                if (sessionTimeline != null) { sessionTimeline.stop(); sessionTimeline = null; }
                if (sessionTimerLabel != null) {
                    sessionTimerLabel.setVisible(false);
                    sessionTimerLabel.setManaged(false);
                }
                if (LauncherEngine.minimizeOnLaunch && stage != null) stage.setIconified(false);
            });
        }, "playtime-tracker").start();
    }

    private void refreshDownloadBadge() {
        if (downloadBadge == null) return;
        int n = activeDownloads.size();
        downloadBadge.setText(String.valueOf(n));
        downloadBadge.setVisible(n > 0);
        downloadBadge.setManaged(n > 0);
    }

    void handlePlayAction(ServerProfile server, Stage stage, Button playBtn, Runnable onDownloadComplete) {
        if (!LauncherEngine.isDownloaded(server)) {
            playBtn.setDisable(true);
            playBtn.setText("DOWNLOADING...");

            DownloadItem item = new DownloadItem(server.name);
            activeDownloads.add(item);
            Platform.runLater(this::refreshDownloadBadge);
            item.status.set("Downloading");

            // cancelledFlag[0] mirrors item.cancelled so the download loop can read it
            boolean[] cancelledFlag = new boolean[]{false};
            new Thread(() -> {
                boolean success = LauncherEngine.downloadClient(server,
                    progress -> {
                        cancelledFlag[0] = item.cancelled; // keep flag in sync
                        Platform.runLater(() -> item.progress.set(progress));
                    },
                    cancelledFlag
                );
                Platform.runLater(() -> {
                    if (item.cancelled) {
                        item.status.set("Cancelled");
                        playBtn.setDisable(false);
                        playBtn.setText("INSTALL");
                    } else if (success) {
                        item.status.set("Complete");
                        playBtn.setDisable(false);
                        playBtn.setText("PLAY");
                        if (onDownloadComplete != null) onDownloadComplete.run();
                    } else {
                        item.status.set("Failed");
                        playBtn.setDisable(false);
                        playBtn.setText("INSTALL");
                    }
                    // Remove from active list after a short delay
                    new Thread(() -> {
                        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            activeDownloads.remove(item);
                            refreshDownloadBadge();
                        });
                    }).start();
                    updateDisplay();
                });
            }).start();

        } else if (LauncherEngine.autoUpdateClients) {
            playBtn.setDisable(true);
            playBtn.setText("CHECKING...");
            new Thread(() -> {
                if (LauncherEngine.isUpdateAvailable(server)) {
                    Platform.runLater(() -> playBtn.setText("UPDATING..."));
                    LauncherEngine.downloadClient(server);
                }
                Platform.runLater(() -> {
                    playBtn.setDisable(false);
                    playBtn.setText("PLAY");
                    launchAndTrack(server, stage);
                });
            }).start();
        } else {
            launchAndTrack(server, stage);
        }
    }

    public static void main(String[] args) { launch(args); }
}
