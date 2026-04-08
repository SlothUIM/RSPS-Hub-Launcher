import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;
import java.util.stream.Collectors;

public class RSPSHub extends Application {

    private VBox serverGrid;
    private List<ServerProfile> allServers;
    private String activeTag = "All";
    private String searchText = "";
    private boolean showingLibrary = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RSPS Hub Launcher");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(800);
        showLoginScreen(primaryStage);
        primaryStage.show();
    }

    private void showLoginScreen(Stage stage) {
        stage.setScene(LoginScreen.create(
            stage,
            username -> {
                LauncherEngine.currentUsername = username;
                showHub(stage);
            },
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
            () -> {
                allServers = null;
                LauncherEngine.currentUsername = "";
                LauncherEngine.avatarImagePath = null;
                showLoginScreen(stage);
            },
            () -> showDevPortal(stage)
        ));
    }

    private void showDevPortal(Stage stage) {
        stage.setScene(DeveloperPortalScreen.create(
            stage,
            () -> showSettings(stage)
        ));
    }

    private void showHub(Stage stage) {
        // Reset tab state when returning from settings
        showingLibrary = false;
        activeTag = "All";
        searchText = "";

        if (allServers == null) {
            LauncherEngine.init();
            allServers = LauncherEngine.fetchServers();
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // --- NAVBAR ---
        HBox navbar = new HBox(30);
        navbar.setPadding(new Insets(20, 40, 20, 40));
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("RSPS HUB");
        brand.getStyleClass().add("nav-brand");

        Button storeTab = new Button("STORE");
        storeTab.getStyleClass().add("nav-tab-active");

        Button libraryTab = new Button("LIBRARY");
        libraryTab.getStyleClass().add("nav-tab");

        storeTab.setOnAction(e -> {
            showingLibrary = false;
            storeTab.getStyleClass().setAll("nav-tab-active");
            libraryTab.getStyleClass().setAll("nav-tab");
            updateDisplay();
        });

        libraryTab.setOnAction(e -> {
            showingLibrary = true;
            libraryTab.getStyleClass().setAll("nav-tab-active");
            storeTab.getStyleClass().setAll("nav-tab");
            updateDisplay();
        });

        // Account widget (right side)
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

        navbar.getChildren().addAll(brand, storeTab, libraryTab, navSpacer, accountWidget);

        // --- SEARCH & FILTERS ---
        VBox topControls = new VBox(15);
        topControls.setPadding(new Insets(20, 40, 0, 40));

        TextField searchBar = new TextField();
        searchBar.setPromptText("Search for a server...");
        searchBar.getStyleClass().add("search-field");
        searchBar.textProperty().addListener((obs, oldText, newText) -> {
            searchText = newText;
            updateDisplay();
        });

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

        VBox headerArea = new VBox(navbar, topControls);
        root.setTop(headerArea);

        // --- SERVER LIST ---
        serverGrid = new VBox(20);
        serverGrid.setPadding(new Insets(30));
        ScrollPane scrollPane = new ScrollPane(serverGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("main-scroll");
        root.setCenter(scrollPane);

        updateDisplay();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    private void updateDisplay() {
        serverGrid.getChildren().clear();

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
            for (ServerProfile server : filtered) {
                serverGrid.getChildren().add(createServerCard(server));
            }
        }
    }

    private HBox createServerCard(ServerProfile server) {
        HBox card = new HBox(20);
        card.getStyleClass().add("server-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane banner = new StackPane(new Label(server.name));
        banner.setPrefSize(200, 100);
        banner.getStyleClass().add("card-banner");

        VBox info = new VBox(5);
        Label title = new Label(server.name);
        title.getStyleClass().add("card-title");

        Label desc = new Label(server.description);
        desc.setWrapText(true);
        desc.getStyleClass().add("card-desc");
        desc.setMaxWidth(400);

        HBox tagBox = new HBox(5);
        if (server.tags != null) {
            for (String t : server.tags) {
                Label p = new Label(t.toUpperCase());
                p.getStyleClass().add("tag-pill");
                tagBox.getChildren().add(p);
            }
        }

        info.getChildren().addAll(title, desc, tagBox);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label players = new Label("🟢 " + server.players_online + " Online");
        players.getStyleClass().add("player-count");

        Button playBtn = new Button(LauncherEngine.isDownloaded(server) ? "PLAY" : "INSTALL");
        playBtn.getStyleClass().add("play-button");

        playBtn.setOnAction(e -> {
            if (!LauncherEngine.isDownloaded(server)) {
                playBtn.setDisable(true);
                playBtn.setText("DOWNLOADING...");
                new Thread(() -> {
                    if (LauncherEngine.downloadClient(server)) {
                        javafx.application.Platform.runLater(() -> {
                            playBtn.setDisable(false);
                            playBtn.setText("PLAY");
                            if (showingLibrary) updateDisplay();
                        });
                    }
                }).start();
            } else {
                LauncherEngine.launchGame(server);
            }
        });

        actions.getChildren().addAll(players, playBtn);
        card.getChildren().addAll(banner, info, actions);
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
