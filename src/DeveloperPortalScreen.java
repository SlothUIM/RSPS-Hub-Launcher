import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeveloperPortalScreen {

    // --- Critical Input Fields (Scoped here so the submit button can read them) ---
    private static TextField nameField;
    private static TextArea descArea;
    private static TextField jarField;
    private static ComboBox<String> xpRate;

    public static Scene create(Stage stage, Runnable onBack) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        HBox topBar = new HBox();
        topBar.getStyleClass().add("settings-topbar");
        topBar.setPadding(new Insets(20, 40, 20, 40));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("settings-back-btn");
        backBtn.setOnAction(e -> onBack.run());

        Region ls = new Region(); HBox.setHgrow(ls, Priority.ALWAYS);
        Region rs = new Region(); HBox.setHgrow(rs, Priority.ALWAYS);

        Label pageTitle = new Label("DEVELOPER PORTAL");
        pageTitle.getStyleClass().add("settings-page-title");

        topBar.getChildren().addAll(backBtn, ls, pageTitle, rs);
        root.setTop(new VBox(TitleBar.create(stage), topBar));

        VBox content = new VBox(36);
        content.setPadding(new Insets(40, 60, 60, 60));
        content.setMaxWidth(800);

        Label introTitle = new Label("Submit Your Server");
        introTitle.getStyleClass().add("dev-intro-title");
        Label introSub = new Label("Fill out the form below to list your server on RSPS Hub. Your server will be reviewed before appearing in the store.");
        introSub.getStyleClass().add("dev-intro-sub");
        introSub.setWrapText(true);

        content.getChildren().addAll(new VBox(8, introTitle, introSub),
            buildBrandingSection(),
            buildAboutSection(),
            buildScreenshotsSection(),
            buildServerSection(),
            buildTagsSection(onBack),
            buildModerationSection());

        VBox wrapper = new VBox(content);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(0, 0, 40, 0));

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        root.setCenter(scroll);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(DeveloperPortalScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
        return scene;
    }

    // ── SECTION 1: BRANDING ──────────────────────────────────────────────────

    private static VBox buildBrandingSection() {
        // Initializing the class-level variable here
        nameField = styledField("e.g. SlothScape");
        
        TextField taglineField = styledField("e.g. The #1 OSRS economy server — 500+ players online");

        // Accent color picker
        String[] colors = {"#ff981f", "#4a9eff", "#4caf50", "#e05252", "#9b5de5", "#00bcd4", "#e91e8c", "#ffd700"};
        final String[] picked = {colors[0]};

        // Preview swatch
        Region preview = new Region();
        preview.setPrefSize(32, 32);
        preview.setMinSize(32, 32);
        preview.setStyle("-fx-background-color: " + picked[0] + "; -fx-background-radius: 6;");

        // Custom hex field
        TextField hexField = new TextField(picked[0]);
        hexField.getStyleClass().add("auth-field");
        hexField.setPrefWidth(110);
        hexField.setPromptText("#rrggbb");

        HBox swatches = new HBox(8);
        swatches.setAlignment(Pos.CENTER_LEFT);
        List<Region> dots = new ArrayList<>();

        for (int i = 0; i < colors.length; i++) {
            final String hex = colors[i];
            Region dot = new Region();
            dot.setPrefSize(26, 26);
            dot.setMinSize(26, 26);
            dot.setCursor(javafx.scene.Cursor.HAND);
            applyDot(dot, hex, hex.equals(picked[0]));
            dots.add(dot);
            swatches.getChildren().add(dot);

            dot.setOnMouseClicked(e -> {
                picked[0] = hex;
                hexField.setText(hex);
                preview.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 6;");
                for (int j = 0; j < dots.size(); j++) applyDot(dots.get(j), colors[j], colors[j].equals(hex));
            });
        }

        hexField.textProperty().addListener((obs, old, val) -> {
            if (val.matches("#[0-9a-fA-F]{6}")) {
                picked[0] = val;
                preview.setStyle("-fx-background-color: " + val + "; -fx-background-radius: 6;");
                for (int j = 0; j < dots.size(); j++) applyDot(dots.get(j), colors[j], colors[j].equalsIgnoreCase(val));
            }
        });

        HBox colorRow = new HBox(12, swatches, hexField, preview);
        colorRow.setAlignment(Pos.CENTER_LEFT);

        // Icon
        TextField iconField = styledField("https://yourserver.com/icon.png  (optional)");
        StackPane iconPreview = new StackPane();
        iconPreview.getStyleClass().add("dev-icon-preview-box");
        iconPreview.setPrefSize(64, 64); iconPreview.setMinSize(64, 64); iconPreview.setMaxSize(64, 64);
        Label iconLabel = new Label("ICON"); iconLabel.getStyleClass().add("dev-preview-placeholder");
        ImageView iconIV = new ImageView(); iconIV.setFitWidth(64); iconIV.setFitHeight(64);
        iconIV.setPreserveRatio(false); iconIV.setVisible(false);
        iconPreview.getChildren().addAll(iconLabel, iconIV);
        wireImagePreview(iconField, iconIV, iconLabel, "ICON");
        HBox iconRow = new HBox(12, iconField, iconPreview);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(iconField, Priority.ALWAYS);

        // Banner
        TextField bannerField = styledField("https://yourserver.com/banner.png  (optional)");
        StackPane bannerPreview = new StackPane();
        bannerPreview.getStyleClass().add("dev-banner-preview-box");
        bannerPreview.setMinHeight(130); bannerPreview.setMaxHeight(130); bannerPreview.setPrefHeight(130);
        bannerPreview.setMaxWidth(Double.MAX_VALUE);
        Label bannerLabel = new Label("Banner Preview (1200×400 recommended)");
        bannerLabel.getStyleClass().add("dev-preview-placeholder");
        ImageView bannerIV = new ImageView();
        bannerIV.setPreserveRatio(false); bannerIV.setVisible(false); bannerIV.setManaged(false);
        bannerIV.fitWidthProperty().bind(bannerPreview.widthProperty()); bannerIV.setFitHeight(130);
        bannerPreview.getChildren().addAll(bannerLabel, bannerIV);
        wireImagePreview(bannerField, bannerIV, bannerLabel, "Banner Preview (1200×400 recommended)");

        return devSection("BRANDING",
            devRow("Server Name *",  nameField),
            devRow("Tagline",        taglineField),
            devRow("Accent Color",   colorRow),
            devRow("Server Icon",    iconRow),
            devRow("Banner Image",   new VBox(8, bannerField, bannerPreview))
        );
    }

    private static void applyDot(Region dot, String hex, boolean selected) {
        dot.setStyle(
            "-fx-background-color: " + hex + ";" +
            "-fx-background-radius: 13;" +
            "-fx-border-color: " + (selected ? "white" : "transparent") + ";" +
            "-fx-border-radius: 13;" +
            "-fx-border-width: 2.5;"
        );
    }

    // ── SECTION 2: ABOUT ─────────────────────────────────────────────────────

    private static VBox buildAboutSection() {
        // Initializing the class-level variable here
        descArea = new TextArea();
        descArea.setPromptText("Describe your server — game mode, XP rates, unique features, what makes it special...");
        descArea.getStyleClass().add("dev-textarea");
        descArea.setWrapText(true); descArea.setPrefRowCount(5); descArea.setMaxWidth(Double.MAX_VALUE);

        TextArea changelogArea = new TextArea();
        changelogArea.setPromptText("v1.3.0 \u2014 April 2025\n- Added new wilderness boss\n- Fixed PvP exploit\n\nv1.2.0 \u2014 March 2025\n- Launch");
        changelogArea.getStyleClass().add("dev-textarea");
        changelogArea.setWrapText(true); changelogArea.setPrefRowCount(6); changelogArea.setMaxWidth(Double.MAX_VALUE);

        Label changelogHint = new Label("Version headers start with v (e.g. v1.3.0 \u2014 April 2025). Bullet points start with -");
        changelogHint.getStyleClass().add("dev-visibility-hint");
        changelogHint.setWrapText(true);

        return devSection("ABOUT",
            devRow("Description *",  descArea),
            devRow("Patch Notes",    new VBox(6, changelogArea, changelogHint))
        );
    }

    // ── SECTION 3: SCREENSHOTS ───────────────────────────────────────────────

    private static VBox buildScreenshotsSection() {
        VBox rowsBox = new VBox(10);

        Button addBtn = new Button("+ Add Screenshot");
        addBtn.getStyleClass().add("settings-secondary-btn");

        Label hint = new Label("PNG or JPG, 1280×720 recommended. Shown in a horizontal gallery on your server page.");
        hint.getStyleClass().add("dev-visibility-hint");
        hint.setWrapText(true);

        // Start with two empty rows
        rowsBox.getChildren().add(buildScreenshotRow(rowsBox));
        rowsBox.getChildren().add(buildScreenshotRow(rowsBox));

        addBtn.setOnAction(e -> rowsBox.getChildren().add(buildScreenshotRow(rowsBox)));

        VBox inner = new VBox(12, hint, rowsBox, addBtn);
        return devSection("SCREENSHOTS", inner);
    }

    private static HBox buildScreenshotRow(VBox parent) {
        TextField urlField = styledField("https://yourserver.com/screenshot1.png");
        HBox.setHgrow(urlField, Priority.ALWAYS);

        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("dev-icon-preview-box");
        thumb.setPrefSize(96, 54); thumb.setMinSize(96, 54); thumb.setMaxSize(96, 54);
        Label thumbLabel = new Label("Preview"); thumbLabel.getStyleClass().add("dev-preview-placeholder");
        thumbLabel.setStyle("-fx-font-size: 10px;");
        ImageView iv = new ImageView(); iv.setFitWidth(96); iv.setFitHeight(54);
        iv.setPreserveRatio(false); iv.setVisible(false);
        thumb.getChildren().addAll(thumbLabel, iv);
        wireImagePreview(urlField, iv, thumbLabel, "Preview");

        Button removeBtn = new Button("\u00D7");
        removeBtn.getStyleClass().add("settings-secondary-btn");
        removeBtn.setStyle("-fx-text-fill: #e05252; -fx-font-weight: bold;");

        HBox row = new HBox(10, urlField, thumb, removeBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        removeBtn.setOnAction(e -> {
            if (parent.getChildren().size() > 1)
                parent.getChildren().remove(row);
        });

        return row;
    }

    // ── SECTION 4: SERVER DETAILS ────────────────────────────────────────────

    private static VBox buildServerSection() {
        // Initializing the class-level variables here
        jarField = styledField("https://yourserver.com/client.jar");
        xpRate = new ComboBox<>();
        
        TextField websiteField = styledField("https://yourserver.com  (optional)");
        TextField discordField = styledField("https://discord.gg/yourserver  (optional)");

        xpRate.getItems().addAll("1x (Vanilla)", "5x", "10x", "25x", "50x", "100x", "Custom / Varies");
        xpRate.setValue("Custom / Varies");
        xpRate.setMaxWidth(Double.MAX_VALUE);
        xpRate.setStyle(
            "-fx-background-color: #1a1d24; -fx-border-color: #2a2e39; -fx-text-fill: white;" +
            "-fx-prompt-text-fill: #8b92a5;"
        );

        return devSection("SERVER DETAILS",
            devRow("JAR Download URL *", jarField),
            devRow("XP Rate",            xpRate),
            devRow("Website",            websiteField),
            devRow("Discord",            discordField)
        );
    }

    // ── SECTION 5: TAGS, VISIBILITY & SUBMIT ─────────────────────────────────

    private static VBox buildTagsSection(Runnable onBack) {
        String[] tagOptions = {"Custom", "PvP", "Economy", "OSRS", "Hardcore", "Leagues",
            "Vanilla", "Ironman", "1x XP", "High XP", "Group Ironman", "Skilling", "Minigames", "Raids"};
        List<CheckBox> tagBoxes = new ArrayList<>();
        FlowPane tagPane = new FlowPane(10, 10);
        for (String tag : tagOptions) {
            CheckBox cb = new CheckBox(tag);
            cb.getStyleClass().add("dev-tag-check");
            tagBoxes.add(cb);
            tagPane.getChildren().add(cb);
        }

        // Custom tag input
        TextField customTagField = styledField("Add custom tag...");
        customTagField.setPrefWidth(180);
        customTagField.setMaxWidth(180);
        Button addTagBtn = new Button("+ Add");
        addTagBtn.getStyleClass().add("settings-secondary-btn");
        Runnable addCustomTag = () -> {
            String t = customTagField.getText().trim();
            if (t.isEmpty() || t.length() > 20) return;
            CheckBox cb = new CheckBox(t);
            cb.getStyleClass().add("dev-tag-check");
            cb.setSelected(true);
            tagBoxes.add(cb);
            tagPane.getChildren().add(cb);
            customTagField.clear();
        };
        addTagBtn.setOnAction(e -> addCustomTag.run());
        customTagField.setOnAction(e -> addCustomTag.run());
        HBox customTagRow = new HBox(8, customTagField, addTagBtn);
        customTagRow.setAlignment(Pos.CENTER_LEFT);
        customTagRow.setPadding(new Insets(6, 0, 0, 0));

        CheckBox visibleCheck = new CheckBox("Visible in store immediately");
        visibleCheck.getStyleClass().add("settings-checkbox");
        visibleCheck.setSelected(true);
        Label visHint = new Label("Uncheck to submit as hidden — useful if your server is still in development.");
        visHint.getStyleClass().add("dev-visibility-hint");
        visHint.setWrapText(true);

        CheckBox featuredCheck = new CheckBox("Request featured placement");
        featuredCheck.getStyleClass().add("settings-checkbox");
        Label featHint = new Label("Featured servers appear at the top of the store. Subject to review and approval.");
        featHint.getStyleClass().add("dev-visibility-hint");
        featHint.setWrapText(true);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setVisible(false); errorLabel.setManaged(false);

        Button submitBtn = new Button("SUBMIT SERVER");
        submitBtn.getStyleClass().add("auth-btn");
        submitBtn.setPrefWidth(220);

        VBox successPanel = buildSuccessPanel(onBack);
        successPanel.setVisible(false); successPanel.setManaged(false);

        // --- NEW API SUBMISSION LOGIC ---
        submitBtn.setOnAction(e -> {
            String serverName = nameField.getText().trim();
            String description = descArea.getText().trim();
            String jarUrl = jarField.getText().trim();
            String xp = xpRate.getValue();

            if (serverName.isEmpty() || jarUrl.isEmpty() || description.isEmpty()) {
                errorLabel.setText("Please fill in all required fields (*).");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            submitBtn.setText("SUBMITTING...");
            submitBtn.setDisable(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            String payload = String.format(
                "{\"name\":\"%s\", \"description\":\"%s\", \"jar_url\":\"%s\", \"xp_rate\":\"%s\"}", 
                serverName, description, jarUrl, xp
            );

            ApiClient.postJson("submit_server", payload).thenAccept(response -> {
                javafx.application.Platform.runLater(() -> {
                    submitBtn.setText("SUBMIT SERVER");
                    submitBtn.setDisable(false);
                    
                    if (response != null && !response.contains("error")) {
                        successPanel.setVisible(true); 
                        successPanel.setManaged(true);
                        submitBtn.setVisible(false);
                    } else {
                        errorLabel.setText("Failed to submit server to backend.");
                        errorLabel.setVisible(true);
                        errorLabel.setManaged(true);
                    }
                });
            }).exceptionally(ex -> {
                javafx.application.Platform.runLater(() -> {
                    submitBtn.setText("SUBMIT SERVER");
                    submitBtn.setDisable(false);
                    errorLabel.setText("Could not connect to the Hub API.");
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                });
                return null;
            });
        });

        return devSection("TAGS & SUBMISSION",
            devRow("Tags",        new VBox(8, tagPane, customTagRow)),
            devRow("Visibility",  new VBox(6, visibleCheck, visHint)),
            devRow("Featured",    new VBox(6, featuredCheck, featHint)),
            errorLabel,
            submitBtn,
            successPanel
        );
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

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
                            imageView.setVisible(true); placeholder.setVisible(false);
                        }
                    });
                    img.errorProperty().addListener((o, ov, error) -> {
                        if (error) { imageView.setVisible(false); placeholder.setText("Could not load image"); placeholder.setVisible(true); }
                    });
                } else {
                    imageView.setVisible(false); placeholder.setText(placeholderText); placeholder.setVisible(true);
                }
            });
            pause.playFromStart();
        });
    }

    private static VBox buildSuccessPanel(Runnable onBack) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("dev-success-panel");
        panel.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("\u2713  Server Submitted!");
        icon.getStyleClass().add("dev-success-title");
        Label msg = new Label("Your server has been submitted for review. Once approved it will appear in the RSPS Hub store.");
        msg.getStyleClass().add("dev-success-msg"); msg.setWrapText(true);
        Button backBtn = new Button("Back to Settings");
        backBtn.getStyleClass().add("settings-secondary-btn");
        backBtn.setOnAction(e -> onBack.run());

        panel.getChildren().addAll(icon, msg, backBtn);
        return panel;
    }

    private static TextField styledField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("auth-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static VBox devSection(String title, Node... rows) {
        VBox box = new VBox(18);
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
        lbl.setMinWidth(160);
        lbl.setWrapText(true);
        HBox row = new HBox(20, lbl, control);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }

    private static VBox buildModerationSection() {
        VBox list = new VBox(10);

        Runnable refresh = () -> {
            list.getChildren().clear();
            Map<Integer, List<Review>> pending = ServerDetailScreen.getPendingReviews();

            if (pending.isEmpty()) {
                Label none = new Label("No reviews pending moderation.");
                none.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
                list.getChildren().add(none);
                return;
            }

            for (var entry : pending.entrySet()) {
                int serverId = entry.getKey();
                for (Review r : new java.util.ArrayList<>(entry.getValue())) {
                    VBox card = new VBox(6);
                    card.setStyle(
                        "-fx-background-color: #1a1d24; -fx-background-radius: 8;" +
                        "-fx-border-color: #e09020; -fx-border-radius: 8; -fx-border-width: 1;" +
                        "-fx-padding: 12 16;"
                    );

                    Label stars = new Label("★".repeat(r.stars) + "☆".repeat(5 - r.stars));
                    stars.setStyle("-fx-text-fill: #ff981f; -fx-font-size: 13px;");

                    Label author = new Label(r.username + "  •  " + r.date);
                    author.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px;");

                    Label comment = new Label(r.comment);
                    comment.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                    comment.setWrapText(true);

                    Label flaggedNote = new Label("⚠  Flagged for inappropriate language");
                    flaggedNote.setStyle("-fx-text-fill: #e09020; -fx-font-size: 11px; -fx-font-style: italic;");

                    Button approveBtn = new Button("✓  Approve");
                    approveBtn.setStyle(
                        "-fx-background-color: #2a4a2a; -fx-text-fill: #4caf50;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;"
                    );

                    Button rejectBtn = new Button("✕  Reject");
                    rejectBtn.setStyle(
                        "-fx-background-color: #4a2a2a; -fx-text-fill: #e05252;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;"
                    );

                    Runnable[] doRefresh = {null};
                    approveBtn.setOnAction(e -> { ServerDetailScreen.approveReview(serverId, r); doRefresh[0].run(); });
                    rejectBtn.setOnAction(e ->  { ServerDetailScreen.rejectReview(serverId, r);  doRefresh[0].run(); });

                    HBox btnRow = new HBox(8, approveBtn, rejectBtn);
                    card.getChildren().addAll(author, stars, comment, flaggedNote, btnRow);
                    list.getChildren().add(card);

                    Runnable refreshRef = () -> {
                        list.getChildren().clear();
                        buildModerationSection(); 
                    };
                    doRefresh[0] = () -> {
                        Map<Integer, List<Review>> stillPending = ServerDetailScreen.getPendingReviews();
                        list.getChildren().clear();
                        if (stillPending.isEmpty()) {
                            Label none = new Label("No reviews pending moderation.");
                            none.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
                            list.getChildren().add(none);
                        } else {
                            for (var e2 : stillPending.entrySet()) {
                                for (Review r2 : e2.getValue()) {
                                    Label l = new Label(r2.username + ": " + r2.comment);
                                    l.setStyle("-fx-text-fill: #8b92a5;");
                                    list.getChildren().add(l);
                                }
                            }
                        }
                    };
                }
            }
        };

        refresh.run();

        return devSection("REVIEW MODERATION", list);
    }
}