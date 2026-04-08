import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class AccountSettingsScreen {

    public static Scene create(Stage stage, Runnable onBack, Runnable onLogout) {
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

        Label pageTitle = new Label("ACCOUNT SETTINGS");
        pageTitle.getStyleClass().add("settings-page-title");

        topBar.getChildren().addAll(backBtn, leftSpacer, pageTitle, rightSpacer);
        root.setTop(topBar);

        // --- CONTENT ---
        VBox content = new VBox(40);
        content.setPadding(new Insets(40, 60, 60, 60));
        content.setMaxWidth(700);

        content.getChildren().addAll(
            buildProfileSection(stage),
            buildLauncherSection(stage),
            buildAppearanceSection(),
            buildAboutSection(),
            buildSessionSection(onLogout)
        );

        VBox contentWrapper = new VBox(content);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setPadding(new Insets(0, 0, 40, 0));

        ScrollPane scrollPane = new ScrollPane(contentWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("main-scroll");
        root.setCenter(scrollPane);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(AccountSettingsScreen.class.getResource("style.css").toExternalForm());
        return scene;
    }

    private static VBox buildProfileSection(Stage stage) {
        String initial = LauncherEngine.currentUsername.isEmpty() ? "?"
            : String.valueOf(LauncherEngine.currentUsername.charAt(0)).toUpperCase();

        Label avatarCircle = new Label(initial);
        avatarCircle.getStyleClass().add("settings-avatar");

        Button changePhotoBtn = new Button("Change Photo");
        changePhotoBtn.getStyleClass().add("settings-secondary-btn");
        changePhotoBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Profile Photo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                LauncherEngine.avatarImagePath = file.getAbsolutePath();
                avatarCircle.setText("✓");
            }
        });

        VBox avatarBox = new VBox(10, avatarCircle, changePhotoBtn);
        avatarBox.setAlignment(Pos.CENTER);

        TextField nameField = new TextField(LauncherEngine.currentUsername);
        nameField.getStyleClass().add("auth-field");
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.textProperty().addListener((obs, old, val) -> LauncherEngine.currentUsername = val);

        TextField emailField = new TextField("Connect backend to save email");
        emailField.getStyleClass().add("auth-field");
        emailField.setMaxWidth(Double.MAX_VALUE);
        emailField.setEditable(false);
        emailField.setOpacity(0.4);

        return section("PROFILE", avatarBox, settingRow("Display Name", nameField), settingRow("Email", emailField));
    }

    private static VBox buildLauncherSection(Stage stage) {
        // Download location
        TextField downloadField = new TextField(LauncherEngine.downloadPath);
        downloadField.getStyleClass().add("auth-field");
        downloadField.setPrefWidth(260);
        downloadField.textProperty().addListener((obs, old, val) -> LauncherEngine.downloadPath = val);

        Button browseDownload = new Button("Browse");
        browseDownload.getStyleClass().add("settings-secondary-btn");
        browseDownload.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Download Folder");
            File dir = dc.showDialog(stage);
            if (dir != null) {
                LauncherEngine.downloadPath = dir.getAbsolutePath() + "/";
                downloadField.setText(LauncherEngine.downloadPath);
            }
        });

        HBox downloadRow = new HBox(10, downloadField, browseDownload);
        downloadRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(downloadField, Priority.ALWAYS);

        // Minimize on launch
        CheckBox minimizeCheck = new CheckBox("Minimize launcher when a game starts");
        minimizeCheck.getStyleClass().add("settings-checkbox");
        minimizeCheck.setSelected(LauncherEngine.minimizeOnLaunch);
        minimizeCheck.selectedProperty().addListener((obs, old, val) -> LauncherEngine.minimizeOnLaunch = val);

        // Auto-update
        CheckBox autoUpdateCheck = new CheckBox("Re-download client if a newer version is available");
        autoUpdateCheck.getStyleClass().add("settings-checkbox");
        autoUpdateCheck.setSelected(LauncherEngine.autoUpdateClients);
        autoUpdateCheck.selectedProperty().addListener((obs, old, val) -> LauncherEngine.autoUpdateClients = val);

        // Java path
        TextField javaField = new TextField(LauncherEngine.javaPath);
        javaField.getStyleClass().add("auth-field");
        javaField.setPrefWidth(260);
        javaField.textProperty().addListener((obs, old, val) -> LauncherEngine.javaPath = val);

        Button browseJava = new Button("Browse");
        browseJava.getStyleClass().add("settings-secondary-btn");
        browseJava.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Java Executable");
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                LauncherEngine.javaPath = file.getAbsolutePath();
                javaField.setText(LauncherEngine.javaPath);
            }
        });

        HBox javaRow = new HBox(10, javaField, browseJava);
        javaRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(javaField, Priority.ALWAYS);

        return section("LAUNCHER SETTINGS",
            settingRow("Download Location", downloadRow),
            settingRow("Minimize on Launch", minimizeCheck),
            settingRow("Auto-update Clients", autoUpdateCheck),
            settingRow("Java Path", javaRow)
        );
    }

    private static VBox buildAppearanceSection() {
        String[] colors   = {"#ff981f", "#4a9eff", "#4caf50", "#e05252", "#9b5de5"};
        String[] names    = {"Orange",  "Blue",    "Green",   "Red",     "Purple"};

        Label selectedLabel = new Label("Current: Orange");
        selectedLabel.getStyleClass().add("settings-color-selected");

        HBox colorRow = new HBox(12);
        colorRow.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < colors.length; i++) {
            final String name = names[i];
            Label dot = new Label();
            dot.getStyleClass().add("color-dot");
            dot.setStyle("-fx-background-color: " + colors[i] + ";");
            dot.setOnMouseClicked(e -> selectedLabel.setText("Current: " + name + "  (takes effect on restart)"));
            colorRow.getChildren().add(dot);
        }

        return section("APPEARANCE", settingRow("Accent Color", colorRow), selectedLabel);
    }

    private static VBox buildAboutSection() {
        Label version = new Label("RSPS Hub Launcher  v1.0.0");
        version.getStyleClass().add("settings-about-text");

        Label built = new Label("Built with JavaFX 17  •  Made for the RSPS community");
        built.getStyleClass().add("settings-about-sub");

        return section("ABOUT", version, built);
    }

    private static VBox buildSessionSection(Runnable onLogout) {
        Label info = new Label("Logged in as:  " + LauncherEngine.currentUsername);
        info.getStyleClass().add("settings-about-sub");

        Button logoutBtn = new Button("LOGOUT");
        logoutBtn.getStyleClass().add("settings-logout-btn");
        logoutBtn.setOnAction(e -> onLogout.run());

        return section("SESSION", info, logoutBtn);
    }

    // --- Helpers ---

    private static VBox section(String title, Node... rows) {
        VBox box = new VBox(16);

        Label header = new Label(title);
        header.getStyleClass().add("settings-section-header");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2e39;");

        box.getChildren().addAll(header, sep);
        box.getChildren().addAll(rows);
        return box;
    }

    private static HBox settingRow(String labelText, Node control) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("settings-row-label");
        lbl.setMinWidth(200);

        HBox row = new HBox(20, lbl, control);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }
}
