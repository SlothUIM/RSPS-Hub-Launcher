import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
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
    private List<String> groups  = new ArrayList<>();
    private Map<String, List<String>> groupMembers = new HashMap<>(); // groupName -> member usernames

    // UI state
    private String activeTag        = "All";
    private String searchText       = "";
    private boolean showingLibrary     = false;
    private boolean showingFriends     = false;
    private boolean showingMessaging   = false;
    private boolean showingStats       = false;
    private boolean showingLeaderboard = false;
    private String  activeConversation = null;
    private boolean isGroupConversation = false;

    // Hub layout refs (needed to swap center content for chat)
    private BorderPane hubRoot;
    private ScrollPane hubScrollPane;
    private VBox serverGrid;

    // Navbar tab refs
    private Button storeTab, libraryTab, friendsTab, statsTab, leaderboardTab;

    // Session timer
    private Label    sessionTimerLabel;
    private Timeline sessionTimeline;

    // Top control refs (toggled per tab)
    private HBox filterBar;
    private HBox sortRow;
    private TextField searchBar;

    // Notification badge (updated in updateDisplay)
    private Label notifBadge;

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

    // ── LIFECYCLE ────────────────────────────────────────────────────────────

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setTitle("RSPS Hub Launcher");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(800);

        Friend pk  = new Friend("PKMaster99",  true,  "SlothLite");  pk.statusMessage  = "Grinding slayer";
        Friend joe = new Friend("IronmanJoe",  true,  "MythicPS");   joe.statusMessage = "AFK - brb";
        friends.add(pk);
        friends.add(joe);
        friends.add(new Friend("ZulrahGrind", false, null));
        friends.add(new Friend("Sasqu",       false, null));

        groups.add("RSPS Gang");
        groupMembers.put("RSPS Gang", new ArrayList<>(List.of("PKMaster99", "IronmanJoe")));

        friendRequests.add(new FriendRequest("NightmarePS_Dev", true,  "5m ago"));
        friendRequests.add(new FriendRequest("Slayer_King",     true,  "2h ago"));
        friendRequests.add(new FriendRequest("CosmicRSPS",      false, "10m ago"));

        // Attach resize support and maximize corner fix to every new scene
        primaryStage.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) ResizeHelper.addTo(primaryStage, scene);
        });

        SplashScreen.show(primaryStage, () -> showLoginScreen(primaryStage));
        primaryStage.show();
    }

    // ── SCREEN NAVIGATION ────────────────────────────────────────────────────

    private void showLoginScreen(Stage stage) {
        stage.setScene(LoginScreen.create(
            stage,
            username -> { LauncherEngine.currentUsername = username; showHub(stage); },
            () -> showRegisterScreen(stage)
        ));
    }

    private void showRegisterScreen(Stage stage) {
        stage.setScene(RegisterScreen.create(
            stage,
            () -> showLoginScreen(stage),
            () -> showLoginScreen(stage)
        ));
    }

    private void showSettings(Stage stage) {
        stage.setScene(AccountSettingsScreen.create(
            stage,
            () -> showHub(stage),
            () -> { allServers = null; LauncherEngine.currentUsername = ""; LauncherEngine.avatarImagePath = null; showLoginScreen(stage); },
            () -> showDevPortal(stage)
        ));
    }

    private void showDevPortal(Stage stage) {
        stage.setScene(DeveloperPortalScreen.create(stage, () -> showSettings(stage)));
    }

    private void showServerDetail(Stage stage, ServerProfile server) {
        stage.setScene(ServerDetailScreen.create(stage, server, () -> showHub(stage)));
    }

    private void showStats(Stage stage) {
        stage.setScene(PlaytimeScreen.create(stage, () -> showHub(stage)));
    }

    private void showProfile(Stage stage, String username) {
        Friend friend = friends.stream().filter(f -> f.username.equals(username)).findFirst().orElse(null);
        boolean online = friend != null && friend.online;
        String status  = friend != null && friend.statusMessage != null ? friend.statusMessage : "";
        stage.setScene(ProfileScreen.create(stage, username, online, status, blockedUsers,
            () -> { showHub(stage); showingFriends = true; setActiveTab(friendsTab); updateDisplay(); },
            () -> { showHub(stage); openConversation(username, false); }
        ));
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
        showingLibrary     = false;
        showingFriends     = false;
        showingMessaging   = false;
        showingStats       = false;
        showingLeaderboard = false;
        activeTag          = "All";
        searchText         = "";

        if (allServers == null) {
            LauncherEngine.init();
            allServers = LauncherEngine.fetchServers();
            // Demo: seed visual test data so badges are visible
            if (allServers.size() > 0) {
                allServers.get(0).isNew = true;
                StreakStore.seedDemo(allServers.get(0).name, 7);
                PlaytimeStore.seedDemo(allServers.get(0).name, 4200); // ~70 hours → Lv 51
                if (allServers.get(0).changelog == null || allServers.get(0).changelog.isEmpty())
                    allServers.get(0).changelog = "v1.3.0 — April 2025\n- Added new wilderness boss\n- PvP balancing updates\n- Fixed client crash on login\n- New donator zone added\n\nv1.2.5 — March 2025\n- Economy rebalance\n- New skilling area: Zeah\n- Performance improvements\n\nv1.2.0 — February 2025\n- Launch";
            }
            if (allServers.size() > 1) {
                allServers.get(1).isNew = true;
                StreakStore.seedDemo(allServers.get(1).name, 3);
                PlaytimeStore.seedDemo(allServers.get(1).name, 900);  // ~15 hours → Lv 18
            }
            SessionHistoryStore.seedDemo();
            DiscordRPC.connectAsync();
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
            updateDisplay();
        });
        libraryTab.setOnAction(e -> {
            showingLibrary = true; showingFriends = false; showingStats = false; showingLeaderboard = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(libraryTab);
            updateDisplay();
        });
        friendsTab.setOnAction(e -> {
            showingFriends = true; showingLibrary = false; showingStats = false; showingLeaderboard = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(friendsTab);
            updateDisplay();
        });
        statsTab.setOnAction(e -> {
            showingStats = true; showingLibrary = false; showingFriends = false; showingLeaderboard = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(statsTab);
            updateDisplay();
        });
        leaderboardTab.setOnAction(e -> {
            showingLeaderboard = true; showingLibrary = false; showingFriends = false; showingStats = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(leaderboardTab);
            updateDisplay();
        });

        // Account widget
        String initial = LauncherEngine.currentUsername.isEmpty() ? "?"
            : String.valueOf(LauncherEngine.currentUsername.charAt(0)).toUpperCase();
        Label avatarCircle = new Label(initial);
        avatarCircle.getStyleClass().add("nav-avatar");
        Label usernameLabel = new Label(LauncherEngine.currentUsername);
        usernameLabel.getStyleClass().add("nav-username");
        VBox userInfo = new VBox(1, usernameLabel);
        if (LauncherEngine.statusMessage != null && !LauncherEngine.statusMessage.isEmpty()) {
            Label statusLbl = new Label(LauncherEngine.statusMessage);
            statusLbl.getStyleClass().add("nav-status-message");
            userInfo.getChildren().add(statusLbl);
        }
        if (LauncherEngine.activeServer != null && !LauncherEngine.activeServer.isEmpty()) {
            Label presenceLbl = new Label("▶ " + LauncherEngine.activeServer);
            presenceLbl.getStyleClass().add("nav-rich-presence");
            userInfo.getChildren().add(presenceLbl);
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

        // Session timer (shown while a game is running)
        sessionTimerLabel = new Label();
        sessionTimerLabel.getStyleClass().add("session-timer-label");
        sessionTimerLabel.setVisible(false);
        sessionTimerLabel.setManaged(false);

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);
        navbar.getChildren().addAll(brand, storeTab, libraryTab, friendsTab, statsTab, leaderboardTab,
            navSpacer, sessionTimerLabel, downloadPane, bellPane, accountWidget);

        // --- SEARCH & FILTERS ---
        VBox topControls = new VBox(15);
        topControls.setPadding(new Insets(20, 40, 0, 40));

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
        for (String opt : new String[]{"Players (High → Low)", "Name A–Z", "Name Z–A"}) {
            MenuItem sortItem = new MenuItem(opt);
            sortItem.setOnAction(e -> { sortOrder = opt; sortBtn.setText(opt); updateDisplay(); });
            sortBtn.getItems().add(sortItem);
        }
        sortRow = new HBox(10, searchBar, sortLabel, sortBtn);
        sortRow.setAlignment(Pos.CENTER_LEFT);

        filterBar = new HBox(10);
        String[] tags = {"All", "Custom", "PvP", "Economy", "OSRS", "Hardcore", "Leagues", "Vanilla", "Ironman", "Skilling"};
        for (String tag : tags) {
            Button tagBtn = new Button(tag);
            tagBtn.getStyleClass().add(tag.equals("All") ? "filter-btn-active" : "filter-btn");
            tagBtn.setOnAction(e -> {
                filterBar.getChildren().forEach(b -> b.getStyleClass().setAll("filter-btn"));
                tagBtn.getStyleClass().setAll("filter-btn-active");
                activeTag = tag;
                updateDisplay();
            });
            filterBar.getChildren().add(tagBtn);
        }
        topControls.getChildren().addAll(sortRow, filterBar);

        hubRoot.setTop(new VBox(TitleBar.create(stage), navbar, topControls));

        // --- CONTENT ---
        serverGrid = new VBox(20);
        serverGrid.setPadding(new Insets(30));
        hubScrollPane = new ScrollPane(serverGrid);
        hubScrollPane.setFitToWidth(true);
        hubScrollPane.getStyleClass().add("main-scroll");
        hubRoot.setCenter(hubScrollPane);

        updateDisplay();

        Scene scene = new Scene(hubRoot);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(getClass()));
        SceneUtils.applyRoundedCorners(scene, hubRoot, stage);
        stage.setScene(scene);

        // Mock friend activity toast on hub load
        PauseTransition toastDelay = new PauseTransition(Duration.seconds(1.5));
        toastDelay.setOnFinished(e -> ToastManager.show(stage, "PKMaster99 is online", "Playing SlothLite \u2022 Grinding slayer"));
        toastDelay.play();
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

    private void showLeaderboard(Stage stage) {
        stage.setScene(LeaderboardScreen.create(stage, () -> showHub(stage)));
    }

    // ── DISPLAY ROUTING ──────────────────────────────────────────────────────

    private void updateDisplay() {
        serverGrid.getChildren().clear();
        serverGrid.setSpacing(20);
        serverGrid.setPadding(new Insets(30));

        // Update notification badge
        long pending = friendRequests.stream().filter(r -> r.incoming).count();
        if (notifBadge != null) {
            notifBadge.setText(String.valueOf(pending));
            notifBadge.setVisible(pending > 0);
            notifBadge.setManaged(pending > 0);
        }

        // Update download badge
        if (downloadBadge != null) {
            int dlCount = activeDownloads.size();
            downloadBadge.setText(String.valueOf(dlCount));
            downloadBadge.setVisible(dlCount > 0);
            downloadBadge.setManaged(dlCount > 0);
        }

        // Hide search/sort/tags in friends, stats, leaderboard
        boolean showSearch = !showingFriends && !showingStats && !showingLeaderboard;
        sortRow.setVisible(showSearch);
        sortRow.setManaged(showSearch);
        boolean showFilters = showSearch && !showingLibrary;
        filterBar.setVisible(showFilters);
        filterBar.setManaged(showFilters);

        if (showingFriends) { buildFriendsContent(); return; }
        if (showingStats)       { buildStatsContent();       return; }
        if (showingLeaderboard) { buildLeaderboardContent(); return; }

        List<ServerProfile> filtered = allServers.stream()
            .filter(s -> s.name.toLowerCase().contains(searchText.toLowerCase()))
            .filter(s -> activeTag.equals("All") || (s.tags != null && s.tags.contains(activeTag)))
            .filter(s -> !showingLibrary || LauncherEngine.isDownloaded(s))
            .collect(Collectors.toList());

        // Apply sort
        switch (sortOrder) {
            case "Name A–Z" -> filtered.sort(Comparator.comparing(s -> s.name));
            case "Name Z–A" -> filtered.sort(Comparator.comparing((ServerProfile s) -> s.name).reversed());
            default         -> filtered.sort(Comparator.comparingInt((ServerProfile s) -> s.players_online).reversed());
        }

        if (filtered.isEmpty()) {
            Label oops = new Label(showingLibrary ? "You haven't installed any servers yet!" : "No servers match your search.");
            oops.getStyleClass().add("empty-label");
            serverGrid.getChildren().add(oops);
            return;
        }

        // Pinned first
        List<ServerProfile> pinned   = filtered.stream().filter(s -> LauncherEngine.favouriteServers.contains(s.name)).collect(Collectors.toList());
        List<ServerProfile> unpinned = filtered.stream().filter(s -> !LauncherEngine.favouriteServers.contains(s.name)).collect(Collectors.toList());

        if (!pinned.isEmpty()) {
            Label pinnedHdr = new Label("★  FAVOURITES");
            pinnedHdr.getStyleClass().add("pinned-header");
            serverGrid.getChildren().add(pinnedHdr);
            for (ServerProfile s : pinned) serverGrid.getChildren().add(createServerCard(s));
            if (!unpinned.isEmpty()) {
                Label allHdr = new Label("ALL SERVERS");
                allHdr.getStyleClass().add("pinned-header");
                serverGrid.getChildren().add(allHdr);
            }
        }
        for (ServerProfile s : unpinned) serverGrid.getChildren().add(createServerCard(s));
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
        // Add friend → sends request instead of adding directly
        TextField addField = new TextField();
        addField.setPromptText("Add friend by username...");
        addField.getStyleClass().add("search-field");
        addField.setPrefWidth(260);

        Button addBtn = new Button("Send Request");
        addBtn.getStyleClass().add("settings-secondary-btn");
        addBtn.setOnAction(e -> {
            String u = addField.getText().trim();
            if (!u.isEmpty()) {
                friendRequests.add(new FriendRequest(u, false, "Just now"));
                addField.clear();
                updateDisplay();
            }
        });
        addField.setOnAction(e -> addBtn.fire());

        HBox addRow = new HBox(10, addField, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setPadding(new Insets(0, 0, 16, 0));
        serverGrid.getChildren().add(addRow);

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
        for (String group : groups) serverGrid.getChildren().add(createGroupRow(group));
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

                Button acceptBtn = new Button("Accept");
                acceptBtn.getStyleClass().add("auth-btn");
                acceptBtn.setOnAction(e -> {
                    friends.add(new Friend(req.username, true, null));
                    friendRequests.remove(req);
                    ActivityStore.add(new ActivityItem(req.username, "joined as your friend", "", "Just now"));
                    updateDisplay();
                });

                Button declineBtn = new Button("Decline");
                declineBtn.getStyleClass().add("settings-secondary-btn");
                declineBtn.setOnAction(e -> { friendRequests.remove(req); updateDisplay(); });

                card.getChildren().addAll(avatar, info, acceptBtn, declineBtn);
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

                Button cancelBtn = new Button("Cancel");
                cancelBtn.getStyleClass().add("settings-secondary-btn");
                cancelBtn.setOnAction(e -> { friendRequests.remove(req); updateDisplay(); });

                card.getChildren().addAll(avatar, info, cancelBtn);
                serverGrid.getChildren().add(card);
            }
        }
    }

    private void buildActivityContent() {
        List<ActivityItem> feed = ActivityStore.getFeed();
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
        removeFriend.setOnAction(e -> { friends.remove(friend); updateDisplay(); });

        MenuItem reportItem = new MenuItem("Report");
        reportItem.setOnAction(e -> {
            Stage owner = (Stage) row.getScene().getWindow();
            DarkDialog.showAlert(owner, "Report submitted for " + friend.username + ". Our team will review it.");
        });

        MenuItem blockItem = new MenuItem("Block");
        blockItem.setOnAction(e -> { blockedUsers.add(friend.username); updateDisplay(); });

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

        List<String> members = groupMembers.getOrDefault(groupName, new ArrayList<>());
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

        row.getChildren().addAll(avatar, info, openBtn);
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
            groups.add(name);
            groupMembers.put(name, selected);
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
        List<Message> messages = MessageStore.getMessages(activeConversation);

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
            List<String> members = groupMembers.computeIfAbsent(activeConversation, k -> new ArrayList<>());

            HBox membersBar = new HBox(10);
            membersBar.getStyleClass().add("chat-members-bar");
            membersBar.setPadding(new Insets(8, 20, 8, 20));
            membersBar.setAlignment(Pos.CENTER_LEFT);

            Label membersLbl = new Label("Members: " + (members.isEmpty() ? "None" : String.join(", ", members)));
            membersLbl.getStyleClass().add("auth-muted");
            HBox.setHgrow(membersLbl, Priority.ALWAYS);

            // Add member dropdown
            MenuButton addMemberBtn = new MenuButton("+ Add Member");
            addMemberBtn.getStyleClass().add("settings-secondary-btn");
            for (Friend f : friends) {
                if (!members.contains(f.username)) {
                    MenuItem item = new MenuItem(f.username);
                    item.setOnAction(e -> {
                        members.add(f.username);
                        membersLbl.setText("Members: " + String.join(", ", members));
                        addMemberBtn.getItems().remove(item);
                    });
                    addMemberBtn.getItems().add(item);
                }
            }

            membersBar.getChildren().addAll(membersLbl, addMemberBtn);

            VBox topSection = new VBox(topBar, membersBar);
            chatPane.setTop(topSection);
        }
        chatPane.setTop(topBar);

        // Messages
        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(16, 20, 16, 20));
        for (Message msg : messages)
            messagesBox.getChildren().add(buildBubble(msg));

        ScrollPane msgScroll = new ScrollPane(messagesBox);
        msgScroll.setFitToWidth(true);
        msgScroll.getStyleClass().add("main-scroll");
        // Auto-scroll to bottom when new messages added
        messagesBox.heightProperty().addListener((obs, old, h) -> msgScroll.setVvalue(1.0));
        msgScroll.setVvalue(1.0);
        chatPane.setCenter(msgScroll);

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

        Runnable send = () -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            String sender = LauncherEngine.currentUsername.isEmpty() ? "You" : LauncherEngine.currentUsername;
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
            Message msg = new Message(sender, text, time, true);
            MessageStore.addMessage(activeConversation, msg);
            messagesBox.getChildren().add(buildBubble(msg));
            inputField.clear();
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
        String skillTooltip  = ServerSkillSystem.getTooltip(server.name);

        HBox card = new HBox(20);
        card.getStyleClass().add("server-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);

        StackPane bannerPane = new StackPane();
        bannerPane.setPrefSize(200, 100);
        bannerPane.getStyleClass().add("card-banner");

        Label bannerLabel = new Label(server.name);
        bannerLabel.getStyleClass().add("card-banner-placeholder");
        bannerPane.getChildren().add(bannerLabel);

        if (server.banner_url != null && !server.banner_url.isEmpty()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(200); iv.setFitHeight(100);
            iv.setPreserveRatio(false);
            iv.setManaged(false); iv.setVisible(false);
            Image img = new Image(server.banner_url, true);
            img.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) { iv.setImage(img); iv.setVisible(true); bannerLabel.setVisible(false); }
            });
            bannerPane.getChildren().add(iv);
        }

        // NEW badge overlay on banner (top-right corner)
        if (server.isNew) {
            Label newBadge = new Label("NEW");
            newBadge.getStyleClass().add("new-badge");
            StackPane.setAlignment(newBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(newBadge, new Insets(6));
            bannerPane.getChildren().add(newBadge);
        }

        // Streak badge overlay on banner (bottom-left corner)
        int streakVal = StreakStore.getStreak(server.name);
        if (streakVal >= 1) {
            Label streakOverlay = new Label("\uD83D\uDD25 " + streakVal);
            streakOverlay.setStyle(
                "-fx-text-fill: #e05252;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: rgba(0,0,0,0.65);" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 2 7;"
            );
            StackPane.setAlignment(streakOverlay, Pos.BOTTOM_LEFT);
            StackPane.setMargin(streakOverlay, new Insets(6));
            bannerPane.getChildren().add(streakOverlay);
        }

        VBox info = new VBox(5);
        Label title = new Label(server.name);
        title.getStyleClass().add("card-title");

        Label desc = new Label(server.description);
        desc.setWrapText(true);
        desc.getStyleClass().add("card-desc");
        desc.setMaxWidth(400);

        HBox tagBox = new HBox(5);
        if (server.tags != null)
            for (String t : server.tags) { Label p = new Label(t.toUpperCase()); p.getStyleClass().add("tag-pill"); tagBox.getChildren().add(p); }

        info.getChildren().addAll(title, desc, tagBox);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Note text below description
        String existingNote = LauncherEngine.serverNotes.get(server.name);
        if (existingNote != null && !existingNote.isEmpty()) {
            Label noteLbl = new Label("📝  " + existingNote);
            noteLbl.getStyleClass().add("server-note-text");
            info.getChildren().add(noteLbl);
        }

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // Star (favourite) + Note buttons
        boolean isFav = LauncherEngine.favouriteServers.contains(server.name);
        Button starBtn = new Button(isFav ? "★" : "☆");
        starBtn.getStyleClass().add(isFav ? "fav-btn-active" : "fav-btn");
        starBtn.setOnAction(e -> {
            e.consume();
            if (LauncherEngine.favouriteServers.contains(server.name))
                LauncherEngine.favouriteServers.remove(server.name);
            else
                LauncherEngine.favouriteServers.add(server.name);
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

        // Skill level badge
        Label levelBadge = new Label("Lv. " + skillLevel);
        levelBadge.getStyleClass().add("skill-level-badge");
        Tooltip.install(levelBadge, new Tooltip(skillTooltip));

        Label players = new Label("\uD83D\uDFE2 " + server.players_online + " Online");
        players.getStyleClass().add("player-count");


        boolean downloaded = LauncherEngine.isDownloaded(server);
        Button playBtn = new Button(downloaded ? "PLAY" : "INSTALL");
        playBtn.getStyleClass().add("play-button");
        playBtn.setOnAction(e -> {
            e.consume();
            handlePlayAction(server, (Stage) card.getScene().getWindow(), playBtn, () -> { if (showingLibrary) updateDisplay(); });
        });

        actions.getChildren().addAll(levelBadge, cardTools, players, playBtn);

        // Uninstall button shown only in library
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

        // XP bar at the bottom of the card
        Region xpFill = new Region();
        xpFill.getStyleClass().add("xp-bar-fill");
        xpFill.setPrefHeight(4);
        xpFill.setMaxWidth(Double.MAX_VALUE);

        StackPane xpTrack = new StackPane();
        xpTrack.getStyleClass().add("xp-bar-track");
        xpTrack.setPrefHeight(4);
        xpTrack.setAlignment(Pos.CENTER_LEFT);

        // Bind fill width to track width * progress
        xpTrack.widthProperty().addListener((obs, old, w) ->
            xpFill.setPrefWidth(w.doubleValue() * skillProgress));
        xpTrack.getChildren().add(xpFill);

        VBox wrapper = new VBox(card, xpTrack);
        wrapper.getStyleClass().add("server-card-wrapper");
        Tooltip.install(xpTrack, new Tooltip(skillTooltip));
        return wrapper;
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

        Label sub = new Label("Top players by total playtime across all RSPS Hub servers");
        sub.getStyleClass().add("settings-about-sub");
        sub.setWrapText(true);
        serverGrid.getChildren().add(sub);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2e39;");
        serverGrid.getChildren().add(sep);

        String yourName = LauncherEngine.currentUsername.isEmpty() ? "You" : LauncherEngine.currentUsername;
        long yourMinutes = PlaytimeStore.getTotalMinutes();
        String yourTop   = PlaytimeStore.getMostPlayed();

        record LeaderEntry(int rank, String username, String topServer, long minutes, boolean isYou) {}

        List<LeaderEntry> entries = new ArrayList<>(List.of(
            new LeaderEntry(1, "PKMaster99",  "SlothLite",   2840, false),
            new LeaderEntry(2, "IronmanJoe",  "MythicPS",    1920, false),
            new LeaderEntry(3, "ZulrahGrind", "SlothLite",    980, false),
            new LeaderEntry(4, "Sasqu",       "NightmarePS",  720, false)
        ));

        entries.add(new LeaderEntry(0, yourName, yourTop != null ? yourTop : "—", yourMinutes, true));
        entries.sort(java.util.Comparator.comparingLong(LeaderEntry::minutes).reversed());
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            entries.set(i, new LeaderEntry(i + 1, e.username(), e.topServer(), e.minutes(), e.isYou()));
        }

        for (var entry : entries) {
            HBox row = new HBox(16);
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

            Label server = new Label(entry.topServer().equals("—") ? "No games played" : "Top: " + entry.topServer());
            server.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");

            VBox nameBox = new VBox(2, name, server);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            long h = entry.minutes() / 60, m = entry.minutes() % 60;
            Label time = new Label(h > 0 ? h + "h " + m + "m" : m + "m");
            time.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px; -fx-font-weight: bold;");

            row.getChildren().addAll(rankLbl, avatar, nameBox, time);
            row.setStyle(entry.isYou()
                ? "-fx-background-color: rgba(255,152,31,0.08); -fx-background-radius: 8; -fx-border-color: rgba(255,152,31,0.25); -fx-border-radius: 8;"
                : "-fx-background-color: #1a1d24; -fx-background-radius: 8;");

            serverGrid.getChildren().add(row);
        }
    }

    // ── NOTIFICATION POPUP ───────────────────────────────────────────────────

    private void showNotificationPopup(Button anchor) {
        ContextMenu menu = new ContextMenu();

        List<FriendRequest> incoming = friendRequests.stream()
            .filter(r -> r.incoming).collect(Collectors.toList());

        String popupText = LauncherEngine.lightMode ? "-fx-text-fill: #1a1a2e;" : "";

        if (incoming.isEmpty()) {
            Label none = new Label("No new notifications");
            none.getStyleClass().add("auth-muted");
            none.setPadding(new Insets(8, 16, 8, 16));
            if (LauncherEngine.lightMode) none.setStyle("-fx-text-fill: #5a6070;");
            menu.getItems().add(new CustomMenuItem(none, false));
        } else {
            for (FriendRequest req : new ArrayList<>(incoming)) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 14, 8, 14));
                row.setPrefWidth(300);
                if (LauncherEngine.lightMode) row.setStyle("-fx-background-color: #ffffff;");

                Label avatar = new Label(req.username.substring(0, 1).toUpperCase());
                avatar.getStyleClass().add("friend-avatar-offline");

                Label msg = new Label(req.username + " sent you a friend request");
                msg.getStyleClass().add("friend-name");
                msg.setWrapText(true);
                msg.setMaxWidth(160);
                msg.setStyle(popupText);
                HBox.setHgrow(msg, Priority.ALWAYS);

                Button acceptBtn = new Button("✓");
                acceptBtn.getStyleClass().add("auth-btn");
                acceptBtn.setPrefSize(32, 28);
                acceptBtn.setOnAction(e -> {
                    friends.add(new Friend(req.username, true, null));
                    friendRequests.remove(req);
                    ActivityStore.add(new ActivityItem(req.username, "joined as your friend", "", "Just now"));
                    menu.hide();
                    updateDisplay();
                });

                Button declineBtn = new Button("✕");
                declineBtn.getStyleClass().add("settings-secondary-btn");
                declineBtn.setPrefSize(32, 28);
                declineBtn.setOnAction(e -> {
                    friendRequests.remove(req);
                    menu.hide();
                    updateDisplay();
                });

                row.getChildren().addAll(avatar, msg, acceptBtn, declineBtn);
                menu.getItems().add(new CustomMenuItem(row, false));
            }

            menu.getItems().add(new SeparatorMenuItem());

            Label viewAll = new Label("View all in Friends → Requests");
            viewAll.getStyleClass().add("auth-muted");
            viewAll.setPadding(new Insets(6, 14, 6, 14));
            if (LauncherEngine.lightMode) viewAll.setStyle("-fx-text-fill: #5a6070;");
            MenuItem viewAllItem = new CustomMenuItem(viewAll, true);
            viewAllItem.setOnAction(e -> {
                friendsSubTab = "REQUESTS";
                showingFriends = true;
                setActiveTab(friendsTab);
                updateDisplay();
            });
            menu.getItems().add(viewAllItem);
        }

        menu.show(anchor, Side.BOTTOM, 0, 4);
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
        LauncherEngine.activeServer = server.name;
        if (LauncherEngine.minimizeOnLaunch) stage.setIconified(true);
        Process proc = LauncherEngine.launchGame(server);
        if (proc != null) {
            long start = System.currentTimeMillis();
            long startEpoch = start / 1000;
            DiscordRPC.setActivity(server.name, startEpoch);

            // Start session timer in navbar
            if (sessionTimerLabel != null) {
                sessionTimerLabel.setText("\u25B6 0:00:00");
                sessionTimerLabel.setVisible(true);
                sessionTimerLabel.setManaged(true);
                final long[] elapsed = {0};
                sessionTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                    elapsed[0]++;
                    long h = elapsed[0] / 3600;
                    long m = (elapsed[0] % 3600) / 60;
                    long s = elapsed[0] % 60;
                    sessionTimerLabel.setText(String.format("\u25B6 %d:%02d:%02d", h, m, s));
                }));
                sessionTimeline.setCycleCount(Timeline.INDEFINITE);
                sessionTimeline.play();
            }

            new Thread(() -> {
                try { proc.waitFor(); } catch (InterruptedException ignored) {}
                long mins = (System.currentTimeMillis() - start) / 60000;
                PlaytimeStore.recordSession(server.name, mins);
                SessionHistoryStore.add(server.name, mins);
                StreakStore.recordPlay(server.name);
                Platform.runLater(() -> {
                    LauncherEngine.activeServer = null;
                    DiscordRPC.clearActivity();
                    if (sessionTimeline != null) { sessionTimeline.stop(); sessionTimeline = null; }
                    if (sessionTimerLabel != null) {
                        sessionTimerLabel.setVisible(false);
                        sessionTimerLabel.setManaged(false);
                    }
                    if (LauncherEngine.minimizeOnLaunch) stage.setIconified(false);
                });
            }, "playtime-tracker").start();
        }
    }

    void handlePlayAction(ServerProfile server, Stage stage, Button playBtn, Runnable onDownloadComplete) {
        if (!LauncherEngine.isDownloaded(server)) {
            playBtn.setDisable(true);
            playBtn.setText("DOWNLOADING...");

            DownloadItem item = new DownloadItem(server.name);
            activeDownloads.add(item);
            Platform.runLater(this::updateDisplay);
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
                            updateDisplay();
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
