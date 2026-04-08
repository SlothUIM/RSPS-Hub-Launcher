import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

public class DeveloperPortalScreen {

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

        Label pageTitle = new Label("DEVELOPER PORTAL");
        pageTitle.getStyleClass().add("settings-page-title");

        topBar.getChildren().addAll(backBtn, leftSpacer, pageTitle, rightSpacer);
        root.setTop(topBar);

        // --- CONTENT ---
        VBox content = new VBox(40);
        content.setPadding(new Insets(40, 60, 60, 60));
        content.setMaxWidth(760);

        Label introTitle = new Label("Submit Your Server");
        introTitle.getStyleClass().add("dev-intro-title");

        Label introSub = new Label("Fill out the form below to list your server on RSPS Hub. Your server will be reviewed before appearing in the store. You are responsible for hosting your own client JAR.");
        introSub.getStyleClass().add("dev-intro-sub");
        introSub.setWrapText(true);

        VBox introSection = new VBox(8, introTitle, introSub);

        content.getChildren().addAll(introSection, buildFormSection(onBack));

        VBox contentWrapper = new VBox(content);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setPadding(new Insets(0, 0, 40, 0));

        ScrollPane scrollPane = new ScrollPane(contentWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("main-scroll");
        root.setCenter(scrollPane);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(DeveloperPortalScreen.class.getResource("style.css").toExternalForm());
        return scene;
    }

    private static VBox buildFormSection(Runnable onBack) {
        // --- Basic fields ---
        TextField nameField = styledField("e.g. SlothScape");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe your server — game mode, XP rates, unique features...");
        descArea.getStyleClass().add("dev-textarea");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);
        descArea.setMaxWidth(Double.MAX_VALUE);

        TextField jarField = styledField("https://yourserver.com/client.jar");
        TextField websiteField = styledField("https://yourserver.com  (optional)");
        TextField discordField = styledField("https://discord.gg/yourserver  (optional)");

        // --- Server Icon ---
        TextField iconField = styledField("https://yourserver.com/icon.png  (optional)");

        StackPane iconPreviewPane = new StackPane();
        iconPreviewPane.getStyleClass().add("dev-icon-preview-box");
        iconPreviewPane.setPrefSize(64, 64);
        iconPreviewPane.setMinSize(64, 64);
        iconPreviewPane.setMaxSize(64, 64);

        Label iconPlaceholder = new Label("ICON");
        iconPlaceholder.getStyleClass().add("dev-preview-placeholder");

        ImageView iconImageView = new ImageView();
        iconImageView.setFitWidth(64);
        iconImageView.setFitHeight(64);
        iconImageView.setPreserveRatio(false);
        iconImageView.setVisible(false);

        iconPreviewPane.getChildren().addAll(iconPlaceholder, iconImageView);

        HBox iconRow = new HBox(12, iconField, iconPreviewPane);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(iconField, Priority.ALWAYS);

        wireImagePreview(iconField, iconImageView, iconPlaceholder, "ICON");

        // --- Banner ---
        TextField bannerField = styledField("https://yourserver.com/banner.png  (optional)");

        StackPane bannerPreviewPane = new StackPane();
        bannerPreviewPane.getStyleClass().add("dev-banner-preview-box");
        bannerPreviewPane.setMinHeight(130);
        bannerPreviewPane.setMaxHeight(130);
        bannerPreviewPane.setPrefHeight(130);
        bannerPreviewPane.setMaxWidth(Double.MAX_VALUE);

        Label bannerPlaceholder = new Label("Banner Preview");
        bannerPlaceholder.getStyleClass().add("dev-preview-placeholder");

        ImageView bannerImageView = new ImageView();
        bannerImageView.setPreserveRatio(false);
        bannerImageView.setVisible(false);
        bannerImageView.setManaged(false); // keeps it out of layout calculations
        bannerImageView.fitWidthProperty().bind(bannerPreviewPane.widthProperty());
        bannerImageView.setFitHeight(130);

        bannerPreviewPane.getChildren().addAll(bannerPlaceholder, bannerImageView);

        VBox bannerSection = new VBox(8, bannerField, bannerPreviewPane);

        wireImagePreview(bannerField, bannerImageView, bannerPlaceholder, "Banner Preview");

        // --- Tags ---
        String[] tagOptions = {"Custom", "PvP", "Economy", "OSRS", "Hardcore", "Leagues", "Vanilla", "Ironman", "1x XP", "High XP", "Group Ironman", "Skilling"};
        List<CheckBox> tagBoxes = new ArrayList<>();
        FlowPane tagPane = new FlowPane(10, 10);
        for (String tag : tagOptions) {
            CheckBox cb = new CheckBox(tag);
            cb.getStyleClass().add("dev-tag-check");
            tagBoxes.add(cb);
            tagPane.getChildren().add(cb);
        }

        // --- Visibility toggle ---
        VBox visibilityBox = new VBox(6);
        CheckBox visibleCheck = new CheckBox("Visible in store");
        visibleCheck.getStyleClass().add("settings-checkbox");
        visibleCheck.setSelected(true);

        Label visibilityHint = new Label("Uncheck to submit as hidden — useful if your server is still in development. You can make it visible later from your dashboard.");
        visibilityHint.getStyleClass().add("dev-visibility-hint");
        visibilityHint.setWrapText(true);

        visibilityBox.getChildren().addAll(visibleCheck, visibilityHint);

        // --- Error + Submit ---
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button submitBtn = new Button("SUBMIT SERVER");
        submitBtn.getStyleClass().add("auth-btn");
        submitBtn.setPrefWidth(220);

        VBox successPanel = buildSuccessPanel(onBack);
        successPanel.setVisible(false);
        successPanel.setManaged(false);

        submitBtn.setOnAction(e -> {
            String name   = nameField.getText().trim();
            String desc   = descArea.getText().trim();
            String jar    = jarField.getText().trim();

            if (name.isEmpty() || desc.isEmpty() || jar.isEmpty()) {
                showError(errorLabel, "Server name, description, and JAR URL are required.");
                return;
            }
            if (!jar.startsWith("http")) {
                showError(errorLabel, "JAR URL must start with http:// or https://");
                return;
            }

            // Mock submission — replace with real API call later
            successPanel.setVisible(true);
            successPanel.setManaged(true);
            submitBtn.setDisable(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        });

        return devSection("SERVER DETAILS",
            devRow("Server Name *",      nameField),
            devRow("Description *",      descArea),
            devRow("JAR Download URL *", jarField),
            devRow("Server Icon",        iconRow),
            devRow("Banner Image",       bannerSection),
            devRow("Website",            websiteField),
            devRow("Discord",            discordField),
            devRow("Tags",               tagPane),
            devRow("Visibility",         visibilityBox),
            errorLabel,
            submitBtn,
            successPanel
        );
    }

    /**
     * Wires a text field to update an ImageView preview after a short delay.
     */
    private static void wireImagePreview(TextField urlField, ImageView imageView, Label placeholder, String placeholderText) {
        PauseTransition pause = new PauseTransition(Duration.millis(700));

        urlField.textProperty().addListener((obs, old, url) -> {
            pause.setOnFinished(e -> {
                String trimmed = url.trim();
                if (trimmed.startsWith("http")) {
                    Image img = new Image(trimmed, true);
                    imageView.setImage(img);

                    img.progressProperty().addListener((o, ov, progress) -> {
                        if (progress.doubleValue() >= 1.0 && !img.isError()) {
                            imageView.setVisible(true);
                            placeholder.setVisible(false);
                        }
                    });

                    img.errorProperty().addListener((o, ov, error) -> {
                        if (error) {
                            imageView.setVisible(false);
                            placeholder.setText("Could not load image");
                            placeholder.setVisible(true);
                        }
                    });
                } else {
                    imageView.setVisible(false);
                    placeholder.setText(placeholderText);
                    placeholder.setVisible(true);
                }
            });
            pause.playFromStart();
        });
    }

    private static VBox buildSuccessPanel(Runnable onBack) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("dev-success-panel");
        panel.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("✓  Server Submitted!");
        icon.getStyleClass().add("dev-success-title");

        Label msg = new Label("Your server has been submitted for review. Once approved it will appear in the RSPS Hub store. Keep an eye on your email.");
        msg.getStyleClass().add("dev-success-msg");
        msg.setWrapText(true);

        Button backBtn = new Button("Back to Settings");
        backBtn.getStyleClass().add("settings-secondary-btn");
        backBtn.setOnAction(e -> onBack.run());

        panel.getChildren().addAll(icon, msg, backBtn);
        return panel;
    }

    // --- Helpers ---

    private static TextField styledField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("auth-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static VBox devSection(String title, Node... rows) {
        VBox box = new VBox(20);
        Label header = new Label(title);
        header.getStyleClass().add("settings-section-header");
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2e39;");
        box.getChildren().addAll(header, sep);
        box.getChildren().addAll(rows);
        return box;
    }

    private static HBox devRow(String labelText, Node control) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("settings-row-label");
        lbl.setMinWidth(180);
        lbl.setWrapText(true);
        HBox row = new HBox(20, lbl, control);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }
}
