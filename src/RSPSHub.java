import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private boolean showingLibrary  = false;
    private boolean showingFriends  = false;
    private boolean showingMessaging = false;
    private String  activeConversation = null;
    private boolean isGroupConversation = false;

    // Hub layout refs (needed to swap center content for chat)
    private BorderPane hubRoot;
    private ScrollPane hubScrollPane;
    private VBox serverGrid;

    // Navbar tab refs
    private Button storeTab, libraryTab, friendsTab;

    // ── LIFECYCLE ────────────────────────────────────────────────────────────

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RSPS Hub Launcher");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(800);
        showLoginScreen(primaryStage);
        primaryStage.show();

        friends.add(new Friend("PKMaster99",  true,  "SlothLite"));
        friends.add(new Friend("IronmanJoe",  true,  "MythicPS"));
        friends.add(new Friend("ZulrahGrind", false, null));
        friends.add(new Friend("Sasqu",       false, null));

        groups.add("RSPS Gang");
        groupMembers.put("RSPS Gang", new ArrayList<>(List.of("PKMaster99", "IronmanJoe")));
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
        showingLibrary   = false;
        showingFriends   = false;
        showingMessaging = false;
        activeTag        = "All";
        searchText       = "";

        if (allServers == null) {
            LauncherEngine.init();
            allServers = LauncherEngine.fetchServers();
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

        storeTab   = navTab("STORE",   true);
        libraryTab = navTab("LIBRARY", false);
        friendsTab = navTab("FRIENDS", false);

        storeTab.setOnAction(e -> {
            showingLibrary = false; showingFriends = false; showingMessaging = false;
            if (showingMessaging) hubRoot.setCenter(hubScrollPane);
            setActiveTab(storeTab);
            updateDisplay();
        });
        libraryTab.setOnAction(e -> {
            showingLibrary = true; showingFriends = false; showingMessaging = false;
            if (showingMessaging) hubRoot.setCenter(hubScrollPane);
            setActiveTab(libraryTab);
            updateDisplay();
        });
        friendsTab.setOnAction(e -> {
            showingFriends = true; showingLibrary = false;
            if (showingMessaging) { hubRoot.setCenter(hubScrollPane); showingMessaging = false; }
            setActiveTab(friendsTab);
            updateDisplay();
        });

        // Account widget
        String initial = LauncherEngine.currentUsername.isEmpty() ? "?"
            : String.valueOf(LauncherEngine.currentUsername.charAt(0)).toUpperCase();
        Label avatarCircle = new Label(initial);
        avatarCircle.getStyleClass().add("nav-avatar");
        Label usernameLabel = new Label(LauncherEngine.currentUsername);
        usernameLabel.getStyleClass().add("nav-username");
        HBox accountWidget = new HBox(10, avatarCircle, usernameLabel);
        accountWidget.setAlignment(Pos.CENTER);
        accountWidget.getStyleClass().add("nav-account-widget");
        accountWidget.setCursor(Cursor.HAND);
        accountWidget.setOnMouseClicked(e -> showSettings(stage));

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);
        navbar.getChildren().addAll(brand, storeTab, libraryTab, friendsTab, navSpacer, accountWidget);

        // --- SEARCH & FILTERS ---
        VBox topControls = new VBox(15);
        topControls.setPadding(new Insets(20, 40, 0, 40));

        TextField searchBar = new TextField();
        searchBar.setPromptText("Search for a server...");
        searchBar.getStyleClass().add("search-field");
        searchBar.textProperty().addListener((obs, old, val) -> { searchText = val; updateDisplay(); });

        HBox filterBar = new HBox(10);
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
        topControls.getChildren().addAll(searchBar, filterBar);

        hubRoot.setTop(new VBox(navbar, topControls));

        // --- CONTENT ---
        serverGrid = new VBox(20);
        serverGrid.setPadding(new Insets(30));
        hubScrollPane = new ScrollPane(serverGrid);
        hubScrollPane.setFitToWidth(true);
        hubScrollPane.getStyleClass().add("main-scroll");
        hubRoot.setCenter(hubScrollPane);

        updateDisplay();

        Scene scene = new Scene(hubRoot);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    private Button navTab(String text, boolean active) {
        Button btn = new Button(text);
        btn.getStyleClass().add(active ? "nav-tab-active" : "nav-tab");
        return btn;
    }

    private void setActiveTab(Button active) {
        for (Button b : new Button[]{storeTab, libraryTab, friendsTab})
            b.getStyleClass().setAll("nav-tab");
        active.getStyleClass().setAll("nav-tab-active");
    }

    // ── DISPLAY ROUTING ──────────────────────────────────────────────────────

    private void updateDisplay() {
        serverGrid.getChildren().clear();
        serverGrid.setSpacing(20);
        serverGrid.setPadding(new Insets(30));

        if (showingFriends) {
            buildFriendsContent();
            return;
        }

        List<ServerProfile> filtered = allServers.stream()
            .filter(s -> s.name.toLowerCase().contains(searchText.toLowerCase()))
            .filter(s -> activeTag.equals("All") || (s.tags != null && s.tags.contains(activeTag)))
            .filter(s -> !showingLibrary || LauncherEngine.isDownloaded(s))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label oops = new Label(showingLibrary ? "You haven't installed any servers yet!" : "No servers match your search.");
            oops.getStyleClass().add("empty-label");
            serverGrid.getChildren().add(oops);
        } else {
            for (ServerProfile server : filtered)
                serverGrid.getChildren().add(createServerCard(server));
        }
    }

    // ── FRIENDS CONTENT ──────────────────────────────────────────────────────

    private void buildFriendsContent() {
        serverGrid.setSpacing(6);
        serverGrid.setPadding(new Insets(24, 40, 40, 40));

        // Add friend
        TextField addField = new TextField();
        addField.setPromptText("Add friend by username...");
        addField.getStyleClass().add("search-field");
        addField.setPrefWidth(260);

        Button addBtn = new Button("Add Friend");
        addBtn.getStyleClass().add("settings-secondary-btn");
        addBtn.setOnAction(e -> {
            String u = addField.getText().trim();
            if (!u.isEmpty()) { friends.add(new Friend(u, false, null)); addField.clear(); updateDisplay(); }
        });
        addField.setOnAction(e -> addBtn.fire());

        HBox addRow = new HBox(10, addField, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setPadding(new Insets(0, 0, 16, 0));
        serverGrid.getChildren().add(addRow);

        // Online / offline
        List<Friend> online  = friends.stream().filter(f ->  f.online).collect(Collectors.toList());
        List<Friend> offline = friends.stream().filter(f -> !f.online).collect(Collectors.toList());

        if (!online.isEmpty()) {
            serverGrid.getChildren().add(friendsGroupHeader("ONLINE — " + online.size()));
            for (Friend f : online) serverGrid.getChildren().add(createFriendRow(f));
        }
        if (!offline.isEmpty()) {
            serverGrid.getChildren().add(friendsGroupHeader("OFFLINE — " + offline.size()));
            for (Friend f : offline) serverGrid.getChildren().add(createFriendRow(f));
        }
        if (friends.isEmpty()) {
            Label empty = new Label("No friends yet. Add someone above!");
            empty.getStyleClass().add("empty-label");
            serverGrid.getChildren().add(empty);
        }

        // Groups
        serverGrid.getChildren().add(friendsGroupHeader("GROUP CHATS"));

        for (String group : groups)
            serverGrid.getChildren().add(createGroupRow(group));

        // New group form
        serverGrid.getChildren().add(buildNewGroupForm());
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

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(friend.username);
        name.getStyleClass().add("friend-name");

        Label status;
        if (friend.online && friend.playingServer != null) {
            status = new Label("Playing " + friend.playingServer);
            status.getStyleClass().add("friend-status-playing");
        } else {
            status = new Label(friend.online ? "Online" : "Offline");
            status.getStyleClass().add(friend.online ? "friend-status-online" : "friend-status-offline");
        }
        info.getChildren().addAll(name, status);

        // Buttons
        Button msgBtn = new Button("Message");
        msgBtn.getStyleClass().add("settings-secondary-btn");
        msgBtn.setOnAction(e -> openConversation(friend.username, false));

        Button discordBtn = new Button("📞 Call");
        discordBtn.getStyleClass().add("friend-discord-btn");
        discordBtn.setTooltip(new Tooltip("Opens Discord to call this friend"));
        // Wire to real Discord URL when backend provides it

        row.getChildren().addAll(avatar, info, msgBtn, discordBtn);
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
        VBox form = new VBox(10);
        form.setPadding(new Insets(10, 0, 0, 0));

        // Group name field
        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Group name...");
        groupNameField.getStyleClass().add("search-field");
        groupNameField.setMaxWidth(Double.MAX_VALUE);

        // Friend checkboxes
        Label membersLabel = new Label("ADD MEMBERS");
        membersLabel.getStyleClass().add("auth-label");

        FlowPane checkboxPane = new FlowPane(10, 8);
        List<CheckBox> memberBoxes = new ArrayList<>();
        for (Friend f : friends) {
            CheckBox cb = new CheckBox(f.username);
            cb.getStyleClass().add("settings-checkbox");
            memberBoxes.add(cb);
            checkboxPane.getChildren().add(cb);
        }

        Button createBtn = new Button("Create Group");
        createBtn.getStyleClass().add("settings-secondary-btn");
        createBtn.setOnAction(e -> {
            String name = groupNameField.getText().trim();
            if (name.isEmpty()) return;
            List<String> selected = memberBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());
            groups.add(name);
            groupMembers.put(name, selected);
            MessageStore.getMessages(name);
            groupNameField.clear();
            memberBoxes.forEach(cb -> cb.setSelected(false));
            updateDisplay();
        });

        form.getChildren().addAll(groupNameField, membersLabel, checkboxPane, createBtn);
        return form;
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

        bubble.getChildren().addAll(content, time);

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

    private HBox createServerCard(ServerProfile server) {
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

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label players = new Label("🟢 " + server.players_online + " Online");
        players.getStyleClass().add("player-count");

        Button playBtn = new Button(LauncherEngine.isDownloaded(server) ? "PLAY" : "INSTALL");
        playBtn.getStyleClass().add("play-button");
        playBtn.setOnAction(e -> {
            e.consume();
            handlePlayAction(server, (Stage) card.getScene().getWindow(), playBtn, () -> { if (showingLibrary) updateDisplay(); });
        });

        actions.getChildren().addAll(players, playBtn);
        card.getChildren().addAll(bannerPane, info, actions);
        card.setOnMouseClicked(e -> showServerDetail((Stage) card.getScene().getWindow(), server));
        return card;
    }

    // ── PLAY HANDLER ─────────────────────────────────────────────────────────

    static void handlePlayAction(ServerProfile server, Stage stage, Button playBtn, Runnable onDownloadComplete) {
        if (!LauncherEngine.isDownloaded(server)) {
            playBtn.setDisable(true);
            playBtn.setText("DOWNLOADING...");
            new Thread(() -> {
                if (LauncherEngine.downloadClient(server)) {
                    javafx.application.Platform.runLater(() -> {
                        playBtn.setDisable(false);
                        playBtn.setText("PLAY");
                        if (onDownloadComplete != null) onDownloadComplete.run();
                    });
                } else {
                    javafx.application.Platform.runLater(() -> { playBtn.setDisable(false); playBtn.setText("INSTALL"); });
                }
            }).start();
        } else if (LauncherEngine.autoUpdateClients) {
            playBtn.setDisable(true);
            playBtn.setText("CHECKING...");
            new Thread(() -> {
                if (LauncherEngine.isUpdateAvailable(server)) {
                    javafx.application.Platform.runLater(() -> playBtn.setText("UPDATING..."));
                    LauncherEngine.downloadClient(server);
                }
                javafx.application.Platform.runLater(() -> {
                    playBtn.setDisable(false);
                    playBtn.setText("PLAY");
                    if (LauncherEngine.minimizeOnLaunch) stage.setIconified(true);
                    LauncherEngine.launchGame(server);
                });
            }).start();
        } else {
            if (LauncherEngine.minimizeOnLaunch) stage.setIconified(true);
            LauncherEngine.launchGame(server);
        }
    }

    public static void main(String[] args) { launch(args); }
}
