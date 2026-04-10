import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.HashMap;
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

        if (LauncherEngine.isStaff) {
            content.getChildren().add(buildPendingSubmissionsSection(stage));
            content.getChildren().add(buildStaffManageSection(stage));
        }

        content.getChildren().add(buildOwnerManageSection(stage));

        content.getChildren().addAll(new VBox(8, introTitle, introSub),
            buildClaimSection(),
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

    // ── OWNER: MY SERVER EDITOR ──────────────────────────────────────────────

    private static VBox buildOwnerManageSection(Stage stage) {
        return buildEditorFor(stage, ApiClient::getMyServers, false,
            "✏  MY SERVER",
            "Your server will appear here once it's been approved. You can update branding, descriptions, and links at any time.");
    }

    // ── SECTION STAFF: PENDING SUBMISSIONS ───────────────────────────────────

    private static VBox buildPendingSubmissionsSection(Stage stage) {
        VBox section = new VBox(14);
        section.setPadding(new Insets(24, 28, 24, 28));
        section.setStyle("-fx-background-color: #1a1d24; -fx-background-radius: 10; -fx-border-color: #2a2e39; -fx-border-radius: 10; -fx-border-width: 1;");

        Label title = new Label("📋  PENDING SERVER SUBMISSIONS  (STAFF ONLY)");
        title.setStyle("-fx-text-fill: #ff981f; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox list = new VBox(10);
        Label loading = new Label("Loading submissions...");
        loading.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
        list.getChildren().add(loading);

        Runnable[] refreshRef = {null};

        Runnable refresh = () -> {
            list.getChildren().clear();
            ApiClient.getPendingServers().thenAccept(servers -> Platform.runLater(() -> {
                if (servers.isEmpty()) {
                    Label none = new Label("No pending submissions.");
                    none.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
                    list.getChildren().add(none);
                    return;
                }
                for (ServerProfile s : servers) {
                    VBox card = new VBox(6);
                    card.setPadding(new Insets(12, 16, 12, 16));
                    card.setStyle("-fx-background-color: #21252e; -fx-background-radius: 8; -fx-border-color: #2a2e39; -fx-border-radius: 8; -fx-border-width: 1;");

                    Label nameL = new Label(s.name != null ? s.name : "(unnamed)");
                    nameL.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label byL = new Label("Submitted by: " + (s.submittedBy != null ? s.submittedBy : "unknown"));
                    byL.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");

                    Label descL = new Label(s.description != null && s.description.length() > 120
                        ? s.description.substring(0, 120) + "..." : (s.description != null ? s.description : ""));
                    descL.setStyle("-fx-text-fill: #c0c5d0; -fx-font-size: 12px;");
                    descL.setWrapText(true);

                    Label xpL = new Label("XP Rate: " + (s.xpRate != null ? s.xpRate : "—") +
                        "  |  JAR: " + (s.jarUrl != null && !s.jarUrl.isEmpty() ? s.jarUrl : "—"));
                    xpL.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px;");
                    xpL.setWrapText(true);

                    Button approveBtn = new Button("✓ Approve");
                    approveBtn.setStyle("-fx-background-color: #2a7a2a; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 14 6 14;");

                    Button rejectBtn = new Button("✗ Reject");
                    rejectBtn.setStyle("-fx-background-color: #7a2a2a; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 14 6 14;");

                    Label statusL = new Label("");
                    statusL.setStyle("-fx-font-size: 11px;");

                    approveBtn.setOnAction(e -> {
                        approveBtn.setDisable(true); rejectBtn.setDisable(true);
                        ApiClient.approveServer(s.id).thenAccept(ok -> Platform.runLater(() -> {
                            if (ok) {
                                statusL.setText("✓ Approved");
                                statusL.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px;");
                                if (refreshRef[0] != null) refreshRef[0].run();
                            } else {
                                statusL.setText("Failed to approve.");
                                statusL.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px;");
                                approveBtn.setDisable(false); rejectBtn.setDisable(false);
                            }
                        }));
                    });

                    rejectBtn.setOnAction(e -> {
                        approveBtn.setDisable(true); rejectBtn.setDisable(true);
                        ApiClient.rejectServer(s.id).thenAccept(ok -> Platform.runLater(() -> {
                            if (ok) {
                                statusL.setText("✗ Rejected");
                                statusL.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px;");
                                if (refreshRef[0] != null) refreshRef[0].run();
                            } else {
                                statusL.setText("Failed to reject.");
                                statusL.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px;");
                                approveBtn.setDisable(false); rejectBtn.setDisable(false);
                            }
                        }));
                    });

                    HBox btnRow = new HBox(8, approveBtn, rejectBtn, statusL);
                    btnRow.setAlignment(Pos.CENTER_LEFT);

                    card.getChildren().addAll(nameL, byL, descL, xpL, btnRow);
                    list.getChildren().add(card);
                }
            }));
        };
        refreshRef[0] = refresh;
        refresh.run();

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.setStyle("-fx-background-color: #2a2e39; -fx-text-fill: #c0c5d0; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 5 12 5 12;");
        refreshBtn.setOnAction(e -> refresh.run());

        section.getChildren().addAll(title, refreshBtn, list);
        return section;
    }

    // ── SECTION STAFF: MANAGE SERVERS ────────────────────────────────────────

    private static VBox buildStaffManageSection(Stage stage) {
        return buildEditorFor(stage, ApiClient::getAllServers, true,
            "⚙  MANAGE SERVERS  (STAFF ONLY)", null);
    }

    @SuppressWarnings("unchecked")
    private static VBox buildEditorFor(
            Stage stage,
            java.util.function.Supplier<java.util.concurrent.CompletableFuture<List<ServerProfile>>> loader,
            boolean isStaff,
            String sectionTitle,
            String emptyHint) {
        List<ServerProfile>[] serverList = new List[]{new ArrayList<>()};
        String[] localBanner = {null};
        String[] localIcon   = {null};

        Label loadingLbl = new Label("Loading servers...");
        loadingLbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");

        ComboBox<String> serverPicker = new ComboBox<>();
        serverPicker.setPromptText("Select a server to edit...");
        serverPicker.setMaxWidth(Double.MAX_VALUE);
        serverPicker.setStyle("-fx-background-color: #1a1d24; -fx-border-color: #2a2e39; -fx-text-fill: white; -fx-prompt-text-fill: #8b92a5;");

        // ── ALL EDIT FIELDS ───────────────────────────────────────────────────
        TextField editName    = styledField("Server name");
        TextField editTagline = styledField("Short tagline");
        TextField editTags    = styledField("Economy,Ironman,PvP");
        TextField editAccent  = styledField("#rrggbb");
        TextField editPlayers = styledField("0");
        editPlayers.setPrefWidth(100); editPlayers.setMaxWidth(100);
        CheckBox editVisible  = new CheckBox("Visible in store"); editVisible.getStyleClass().add("settings-checkbox");
        CheckBox editApproved = new CheckBox("Approved");         editApproved.getStyleClass().add("settings-checkbox");
        TextField editBanner  = styledField("Banner / hero image URL  (or pick a file)");
        TextField editIcon    = styledField("Icon image URL  (or pick a file)");
        TextArea  editDesc    = new TextArea();
        editDesc.getStyleClass().add("dev-textarea"); editDesc.setWrapText(true); editDesc.setPrefRowCount(5); editDesc.setMaxWidth(Double.MAX_VALUE);
        TextField editJar     = styledField("JAR download URL");
        TextField editWebsite = styledField("Website URL");
        TextField editDiscord = styledField("Discord invite URL");
        ComboBox<String> editXpRate = new ComboBox<>();
        editXpRate.getItems().addAll("1x (Vanilla)", "5x", "10x", "25x", "50x", "100x", "Custom / Varies");
        editXpRate.setValue("Custom / Varies");
        editXpRate.setMaxWidth(Double.MAX_VALUE);
        editXpRate.setStyle("-fx-background-color: #1a1d24; -fx-border-color: #2a2e39; -fx-text-fill: white;");

        // ══ STORE CARD PREVIEW components ═════════════════════════════════════
        Label cardBannerLbl = new Label(""); cardBannerLbl.getStyleClass().add("card-banner-placeholder");
        ImageView cardBannerIV = new ImageView();
        cardBannerIV.setFitWidth(200); cardBannerIV.setFitHeight(100); cardBannerIV.setPreserveRatio(false); cardBannerIV.setVisible(false);
        StackPane cardBannerPane = new StackPane(cardBannerLbl, cardBannerIV);
        cardBannerPane.setPrefSize(200, 100); cardBannerPane.setMinSize(200, 100); cardBannerPane.setMaxSize(200, 100);
        cardBannerPane.getStyleClass().add("card-banner");

        Label cardTitle   = new Label(); cardTitle.getStyleClass().add("card-title");
        Label cardDescLbl = new Label(); cardDescLbl.getStyleClass().add("card-desc"); cardDescLbl.setWrapText(true);
        FlowPane cardTagBox = new FlowPane(6, 4);
        Region cardSpacer = new Region(); VBox.setVgrow(cardSpacer, Priority.ALWAYS);
        VBox cardInfo = new VBox(5, cardTitle, cardDescLbl, cardSpacer, cardTagBox); HBox.setHgrow(cardInfo, Priority.ALWAYS);
        Label cardPlayers = new Label("🟢 0 Online"); cardPlayers.getStyleClass().add("player-count");
        Button cardPlayBtn = new Button("PLAY"); cardPlayBtn.getStyleClass().add("play-button"); cardPlayBtn.setMouseTransparent(true); cardPlayBtn.setPrefWidth(110);
        VBox cardActions = new VBox(8, cardPlayers, cardPlayBtn); cardActions.setAlignment(Pos.CENTER_RIGHT); cardActions.setMinWidth(120); cardActions.setMaxWidth(120);
        HBox cardRow = new HBox(20, cardBannerPane, cardInfo, cardActions);
        cardRow.getStyleClass().add("server-card"); cardRow.setPadding(new Insets(15)); cardRow.setAlignment(Pos.CENTER_LEFT); cardRow.setMaxWidth(Double.MAX_VALUE);
        VBox cardWrapper = new VBox(cardRow); cardWrapper.getStyleClass().add("server-card-wrapper"); cardWrapper.setMaxWidth(Double.MAX_VALUE);
        Label cardPreviewHdr = new Label("STORE CARD PREVIEW");
        cardPreviewHdr.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox storePreview = new VBox(10, cardPreviewHdr, cardWrapper);
        storePreview.setStyle("-fx-padding: 16 0 0 0; -fx-border-color: #2a2e39; -fx-border-width: 1 0 0 0;");

        // ══ DETAIL PAGE PREVIEW — mirrors ServerDetailScreen exactly ══════════
        // Hero: 130px tall, clipped, fitHeight banner (not cover-crop)
        final int PREV_HERO_H = 130;
        StackPane heroPreviewPane = new StackPane();
        heroPreviewPane.setMinHeight(PREV_HERO_H); heroPreviewPane.setPrefHeight(PREV_HERO_H); heroPreviewPane.setMaxHeight(PREV_HERO_H);
        heroPreviewPane.setMaxWidth(Double.MAX_VALUE);
        heroPreviewPane.setStyle("-fx-background-color: #0f1115; -fx-background-radius: 8 8 0 0;");
        Rectangle heroClip = new Rectangle(); heroClip.setHeight(PREV_HERO_H); heroClip.widthProperty().bind(heroPreviewPane.widthProperty());
        heroPreviewPane.setClip(heroClip);

        Label heroBannerPlaceholder = new Label("No banner yet — upload one in the Store Card tab");
        heroBannerPlaceholder.setStyle("-fx-text-fill: #555d6e; -fx-font-size: 12px;");

        // fitHeight mode — matches actual detail page rendering
        ImageView heroBannerIV = new ImageView();
        heroBannerIV.setPreserveRatio(true);
        heroBannerIV.setFitHeight(PREV_HERO_H);
        heroBannerIV.setSmooth(true);
        heroBannerIV.setVisible(false);

        Region heroGradient = new Region();
        heroGradient.setManaged(false);
        heroGradient.setStyle("-fx-background-color: linear-gradient(to bottom, transparent 30%, #0f1115 100%);");
        heroGradient.prefWidthProperty().bind(heroPreviewPane.widthProperty()); heroGradient.setPrefHeight(PREV_HERO_H);

        heroPreviewPane.getChildren().addAll(heroBannerPlaceholder, heroBannerIV, heroGradient);

        // Icon — lives in info bar with translateY(-36), same as actual detail page
        StackPane detailIconPane = new StackPane();
        detailIconPane.setStyle("-fx-background-color: #1a1d24; -fx-background-radius: 8; -fx-border-color: #2a2e39; -fx-border-radius: 8; -fx-border-width: 2;");
        detailIconPane.setPrefSize(72, 72); detailIconPane.setMinSize(72, 72); detailIconPane.setMaxSize(72, 72);
        Label detailIconLbl = new Label("?"); detailIconLbl.setStyle("-fx-text-fill: #9b5de5; -fx-font-weight: bold; -fx-font-size: 22px;");
        ImageView detailIconIV = new ImageView(); detailIconIV.setFitWidth(72); detailIconIV.setFitHeight(72); detailIconIV.setPreserveRatio(false); detailIconIV.setVisible(false);
        detailIconPane.getChildren().addAll(detailIconLbl, detailIconIV);
        detailIconPane.setTranslateY(-36);  // float up into the hero, matching actual page

        // Info bar below hero
        Label detailPreviewName    = new Label("Server Name"); detailPreviewName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");
        Label detailPreviewTagline = new Label(); detailPreviewTagline.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
        FlowPane detailTagBox = new FlowPane(6, 4);
        VBox detailTextCol = new VBox(4, detailPreviewName, detailPreviewTagline, detailTagBox); HBox.setHgrow(detailTextCol, Priority.ALWAYS);

        Label previewPlayers = new Label("🟢 0 Online"); previewPlayers.getStyleClass().add("player-count");
        Button previewPlayBtn = new Button("PLAY"); previewPlayBtn.getStyleClass().add("play-button"); previewPlayBtn.setMouseTransparent(true); previewPlayBtn.setPrefWidth(140);
        Button previewDiscordBtn = new Button("Discord"); previewDiscordBtn.setStyle("-fx-background-color: #2a2e39; -fx-text-fill: #c8cdd8; -fx-background-radius: 6; -fx-padding: 7 14;"); previewDiscordBtn.setMouseTransparent(true);
        Button previewWebsiteBtn = new Button("Website");  previewWebsiteBtn.setStyle("-fx-background-color: #2a2e39; -fx-text-fill: #c8cdd8; -fx-background-radius: 6; -fx-padding: 7 14;"); previewWebsiteBtn.setMouseTransparent(true);
        HBox previewLinks = new HBox(8, previewDiscordBtn, previewWebsiteBtn);
        VBox detailRightCol = new VBox(8, previewPlayers, previewPlayBtn, previewLinks); detailRightCol.setAlignment(Pos.CENTER_RIGHT);

        HBox detailInfoBar = new HBox(16, detailIconPane, detailTextCol, detailRightCol);
        detailInfoBar.setPadding(new Insets(12, 16, 16, 16)); detailInfoBar.setAlignment(Pos.CENTER_LEFT);
        detailInfoBar.setStyle("-fx-background-color: #0f1115;");

        VBox detailPreviewBox = new VBox(0, heroPreviewPane, detailInfoBar);
        detailPreviewBox.setStyle("-fx-background-color: #0f1115; -fx-background-radius: 8; -fx-border-color: #2a2e39; -fx-border-radius: 8; -fx-border-width: 1;");
        detailPreviewBox.setMaxWidth(Double.MAX_VALUE);

        Label detailPreviewHdr = new Label("DETAIL PAGE PREVIEW");
        detailPreviewHdr.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox detailPreview = new VBox(10, detailPreviewHdr, detailPreviewBox);
        detailPreview.setStyle("-fx-padding: 16 0 0 0; -fx-border-color: #2a2e39; -fx-border-width: 1 0 0 0;");

        // ── ICON small preview (for store card tab) ────────────────────────────
        StackPane iconPreviewPane = new StackPane();
        iconPreviewPane.setStyle("-fx-background-color: #2a2e39; -fx-background-radius: 8; -fx-border-color: #3a3f4e; -fx-border-radius: 8; -fx-border-width: 1;");
        iconPreviewPane.setPrefSize(72, 72); iconPreviewPane.setMinSize(72, 72); iconPreviewPane.setMaxSize(72, 72);
        Label iconSmallPlaceholder = new Label("ICON"); iconSmallPlaceholder.setStyle("-fx-text-fill: #555d6e; -fx-font-size: 11px;");
        ImageView iconSmallIV = new ImageView(); iconSmallIV.setFitWidth(72); iconSmallIV.setFitHeight(72); iconSmallIV.setPreserveRatio(false); iconSmallIV.setVisible(false);
        iconPreviewPane.getChildren().addAll(iconSmallPlaceholder, iconSmallIV);

        // ══ SHARED IMAGE CONSUMERS ═════════════════════════════════════════════
        java.util.function.Consumer<Image> applyBannerImage = img -> {
            cardBannerIV.setImage(img); heroBannerIV.setImage(img);
            img.progressProperty().addListener((o, ov, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) Platform.runLater(() -> {
                    cardBannerIV.setVisible(true); cardBannerLbl.setVisible(false);
                    heroBannerIV.setVisible(true); heroBannerPlaceholder.setVisible(false);
                });
            });
            img.errorProperty().addListener((o, ov, err) -> { if (err) Platform.runLater(() -> {
                cardBannerIV.setVisible(false); cardBannerLbl.setVisible(true);
                heroBannerIV.setVisible(false); heroBannerPlaceholder.setVisible(true);
            }); });
        };

        java.util.function.Consumer<Image> applyIconImage = img -> {
            iconSmallIV.setImage(img); detailIconIV.setImage(img);
            img.progressProperty().addListener((o, ov, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) Platform.runLater(() -> {
                    iconSmallIV.setVisible(true); iconSmallPlaceholder.setVisible(false);
                    detailIconIV.setVisible(true); detailIconLbl.setVisible(false);
                });
            });
            img.errorProperty().addListener((o, ov, err) -> { if (err) Platform.runLater(() -> {
                iconSmallIV.setVisible(false); iconSmallPlaceholder.setVisible(true);
                detailIconIV.setVisible(false); detailIconLbl.setVisible(true);
            }); });
        };

        // ── BANNER FILE PICKER ────────────────────────────────────────────────
        Button bannerPickBtn = new Button("📁  Choose File...");
        bannerPickBtn.getStyleClass().add("settings-secondary-btn");
        bannerPickBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser(); fc.setTitle("Choose Banner Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(stage); if (file == null) return;
            localBanner[0] = file.getAbsolutePath(); editBanner.setText("");
            editBanner.setPromptText("📁  " + file.getName() + "  (will upload on save)");
            applyBannerImage.accept(new Image(file.toURI().toString(), true));
        });
        PauseTransition bannerPause = new PauseTransition(Duration.millis(700));
        editBanner.textProperty().addListener((obs, old, url) -> {
            bannerPause.setOnFinished(ev -> { String u = url.trim(); if (!u.startsWith("http")) return; localBanner[0] = null; applyBannerImage.accept(new Image(u, true)); });
            bannerPause.playFromStart();
        });
        HBox bannerInputRow = new HBox(10, editBanner, bannerPickBtn); bannerInputRow.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(editBanner, Priority.ALWAYS);

        // ── ICON FILE PICKER ──────────────────────────────────────────────────
        Button iconPickBtn = new Button("📁  Choose File...");
        iconPickBtn.getStyleClass().add("settings-secondary-btn");
        iconPickBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser(); fc.setTitle("Choose Icon Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(stage); if (file == null) return;
            localIcon[0] = file.getAbsolutePath(); editIcon.setText("");
            editIcon.setPromptText("📁  " + file.getName() + "  (will upload on save)");
            applyIconImage.accept(new Image(file.toURI().toString(), true));
        });
        PauseTransition iconPause = new PauseTransition(Duration.millis(700));
        editIcon.textProperty().addListener((obs, old, url) -> {
            iconPause.setOnFinished(ev -> { String u = url.trim(); if (!u.startsWith("http")) return; localIcon[0] = null; applyIconImage.accept(new Image(u, true)); });
            iconPause.playFromStart();
        });
        HBox iconInputRow = new HBox(10, editIcon, iconPickBtn, iconPreviewPane); iconInputRow.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(editIcon, Priority.ALWAYS);

        // ── LIVE PREVIEW LISTENERS ────────────────────────────────────────────
        editName.textProperty().addListener((obs, old, v) -> {
            cardTitle.setText(v);
            detailPreviewName.setText(v.isEmpty() ? "Server Name" : v);
            if (!cardBannerIV.isVisible()) cardBannerLbl.setText(v);
            if (!detailIconIV.isVisible()) detailIconLbl.setText(v.isEmpty() ? "?" : v.substring(0, 1).toUpperCase());
        });
        editTagline.textProperty().addListener((obs, old, v) -> detailPreviewTagline.setText(v));
        editDesc.textProperty().addListener((obs, old, v) -> {
            String flat = v.replace("\n", " ").replace("\r", "").replaceAll("\\s+", " ").trim();
            cardDescLbl.setText(flat.length() > 240 ? flat.substring(0, 240).trim() + "…" : flat);
        });
        editTags.textProperty().addListener((obs, old, v) -> {
            cardTagBox.getChildren().clear(); detailTagBox.getChildren().clear();
            for (String t : v.split(",")) {
                String tag = t.trim(); if (tag.isEmpty()) continue;
                Label p1 = new Label(tag.toUpperCase()); p1.getStyleClass().add("tag-pill");
                Label p2 = new Label(tag.toUpperCase()); p2.getStyleClass().add("tag-pill");
                cardTagBox.getChildren().add(p1); detailTagBox.getChildren().add(p2);
            }
        });
        editPlayers.textProperty().addListener((obs, old, v) -> {
            String txt; try { txt = "🟢 " + Integer.parseInt(v.trim()) + " Online"; } catch (NumberFormatException ex2) { txt = "🟢 0 Online"; }
            cardPlayers.setText(txt); previewPlayers.setText(txt);
        });
        editAccent.textProperty().addListener((obs, old, v) -> {
            String safe = v.trim().matches("#[0-9a-fA-F]{6}") ? v.trim() : LauncherEngine.accentColor;
            cardPlayBtn.setStyle("-fx-background-color: " + safe + ";");
            previewPlayBtn.setStyle("-fx-background-color: " + safe + ";");
        });

        // ══ TAB BAR ════════════════════════════════════════════════════════════
        String tabActiveStyle   = "-fx-background-color: #9b5de5; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 9 26; -fx-cursor: hand; -fx-background-radius: 6 0 0 6;";
        String tabInactiveStyle = "-fx-background-color: #2a2e39; -fx-text-fill: #8b92a5; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 9 26; -fx-cursor: hand; -fx-background-radius: 0 6 6 0;";
        Button tabStore  = new Button("STORE CARD");
        Button tabDetail = new Button("DETAIL PAGE");
        tabStore.setStyle(tabActiveStyle);
        tabDetail.setStyle(tabInactiveStyle);

        // ── STORE CARD TAB FIELDS ─────────────────────────────────────────────
        VBox storeCardPane = new VBox(16);
        storeCardPane.getChildren().addAll(
            devRow("Name *",       editName),
            devRow("Tagline",      editTagline),
            devRow("Tags",         editTags),
            devRow("Accent Color", editAccent),
            devRow("Banner",       new VBox(8, bannerInputRow)),
            devRow("Icon",         iconInputRow)
        );
        if (isStaff) {
            storeCardPane.getChildren().addAll(
                devRow("Players",  editPlayers),
                devRow("Status",   new HBox(20, editVisible, editApproved))
            );
        }
        storeCardPane.getChildren().add(storePreview);

        // ── DETAIL PAGE TAB FIELDS ────────────────────────────────────────────
        VBox detailPagePane = new VBox(16,
            devRow("Description *", editDesc),
            devRow("JAR URL *",     editJar),
            devRow("Website",       editWebsite),
            devRow("Discord",       editDiscord),
            devRow("XP Rate",       editXpRate),
            detailPreview
        );
        detailPagePane.setVisible(false); detailPagePane.setManaged(false);

        tabStore.setOnAction(e -> {
            tabStore.setStyle(tabActiveStyle);
            tabDetail.setStyle(tabInactiveStyle);
            storeCardPane.setVisible(true);  storeCardPane.setManaged(true);
            detailPagePane.setVisible(false); detailPagePane.setManaged(false);
        });
        tabDetail.setOnAction(e -> {
            tabDetail.setStyle(tabActiveStyle.replace("6 0 0 6", "0 6 6 0"));
            tabStore.setStyle(tabInactiveStyle.replace("0 6 6 0", "6 0 0 6"));
            storeCardPane.setVisible(false); storeCardPane.setManaged(false);
            detailPagePane.setVisible(true);  detailPagePane.setManaged(true);
        });
        HBox tabBar = new HBox(0, tabStore, tabDetail);
        tabBar.setPadding(new Insets(0, 0, 4, 0));

        // ── SAVE + TOGGLE VISIBILITY (staff only) ────────────────────────────
        Label saveStatus = new Label(); saveStatus.setVisible(false); saveStatus.setManaged(false);
        Button saveBtn = new Button("SAVE CHANGES"); saveBtn.getStyleClass().add("auth-btn"); saveBtn.setPrefWidth(180);
        Button toggleVisBtn = new Button("HIDE FROM STORE");
        toggleVisBtn.setStyle("-fx-background-color: #4a2a2a; -fx-text-fill: #e05252; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        toggleVisBtn.setVisible(isStaff); toggleVisBtn.setManaged(isStaff);
        HBox actionRow = new HBox(12, saveBtn, toggleVisBtn); actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox formBox = new VBox(16, tabBar, storeCardPane, detailPagePane, saveStatus, actionRow);
        formBox.setVisible(false); formBox.setManaged(false);

        // ── POPULATE ON SERVER SELECT ─────────────────────────────────────────
        serverPicker.setOnAction(e -> {
            int idx = serverPicker.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= serverList[0].size()) return;
            ServerProfile s = serverList[0].get(idx);
            localBanner[0] = null; localIcon[0] = null;
            editBanner.setPromptText("Banner / hero image URL  (or pick a file)");
            editIcon.setPromptText("Icon image URL  (or pick a file)");
            editBanner.setText(s.bannerUrl   != null ? s.bannerUrl   : "");
            editIcon.setText(s.iconUrl       != null ? s.iconUrl     : "");
            editName.setText(s.name          != null ? s.name        : "");
            editTagline.setText(s.tagline    != null ? s.tagline     : "");
            editDesc.setText(s.description   != null ? s.description : "");
            editJar.setText(s.jarUrl         != null ? s.jarUrl      : "");
            editWebsite.setText(s.websiteUrl != null ? s.websiteUrl  : "");
            editDiscord.setText(s.discordUrl != null ? s.discordUrl  : "");
            editAccent.setText(s.accentColor != null ? s.accentColor : "");
            editTags.setText(s.tags != null ? String.join(",", s.tags) : "");
            editXpRate.setValue(s.xpRate     != null ? s.xpRate      : "Custom / Varies");
            editPlayers.setText(String.valueOf(s.playersOnline));
            editVisible.setSelected(s.visible  == 1);
            editApproved.setSelected(s.approved == 1);

            if (s.bannerUrl != null && !s.bannerUrl.isEmpty()) {
                applyBannerImage.accept(new Image(s.bannerUrl, true));
            } else {
                cardBannerIV.setVisible(false); cardBannerLbl.setText(s.name != null ? s.name : ""); cardBannerLbl.setVisible(true);
                heroBannerIV.setVisible(false); heroBannerPlaceholder.setVisible(true);
            }
            if (s.iconUrl != null && !s.iconUrl.isEmpty()) {
                applyIconImage.accept(new Image(s.iconUrl, true));
            } else {
                iconSmallIV.setVisible(false); iconSmallPlaceholder.setVisible(true);
                detailIconIV.setVisible(false); detailIconLbl.setVisible(true);
                detailIconLbl.setText(s.name != null && !s.name.isEmpty() ? s.name.substring(0, 1).toUpperCase() : "?");
            }

            boolean vis = s.visible == 1;
            toggleVisBtn.setText(vis ? "HIDE FROM STORE" : "SHOW IN STORE");
            toggleVisBtn.setStyle(vis
                ? "-fx-background-color: #4a2a2a; -fx-text-fill: #e05252; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"
                : "-fx-background-color: #2a4a2a; -fx-text-fill: #4caf50; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"
            );
            saveStatus.setVisible(false); saveStatus.setManaged(false);
            formBox.setVisible(true); formBox.setManaged(true);
        });

        // ── SAVE (all fields from both tabs) ──────────────────────────────────
        saveBtn.setOnAction(e -> {
            int idx = serverPicker.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= serverList[0].size()) return;
            ServerProfile s = serverList[0].get(idx);
            int players = 0; try { players = Integer.parseInt(editPlayers.getText().trim()); } catch (NumberFormatException ex2) {}

            Map<String, Object> fields = new HashMap<>();
            fields.put("name",           editName.getText().trim());
            fields.put("tagline",        editTagline.getText().trim());
            fields.put("description",    editDesc.getText().trim());
            fields.put("jar_url",        editJar.getText().trim());
            fields.put("banner_url",     editBanner.getText().trim());
            fields.put("icon_url",       editIcon.getText().trim());
            fields.put("website_url",    editWebsite.getText().trim());
            fields.put("discord_url",    editDiscord.getText().trim());
            fields.put("accent_color",   editAccent.getText().trim());
            fields.put("tags",           editTags.getText().trim());
            fields.put("xp_rate",        editXpRate.getValue());
            fields.put("players_online", players);
            fields.put("visible",        editVisible.isSelected()  ? 1 : 0);
            fields.put("approved",       editApproved.isSelected() ? 1 : 0);

            saveBtn.setDisable(true); saveStatus.setVisible(false); saveStatus.setManaged(false);
            String bannerPath = localBanner[0]; String iconPath = localIcon[0];
            java.util.concurrent.CompletableFuture<Void> chain = java.util.concurrent.CompletableFuture.completedFuture(null);

            if (bannerPath != null) {
                chain = chain.thenCompose(v -> {
                    Platform.runLater(() -> saveBtn.setText("UPLOADING BANNER..."));
                    return ApiClient.uploadBanner(s.id, bannerPath).thenAccept(url -> { if (url != null) fields.put("banner_url", url); localBanner[0] = null; });
                });
            }
            if (iconPath != null) {
                chain = chain.thenCompose(v -> {
                    Platform.runLater(() -> saveBtn.setText("UPLOADING ICON..."));
                    return ApiClient.uploadIcon(s.id, iconPath).thenAccept(url -> { if (url != null) fields.put("icon_url", url); localIcon[0] = null; });
                });
            }
            chain.thenAccept(v -> Platform.runLater(() -> {
                if (fields.get("banner_url") instanceof String bu && !bu.isEmpty()) editBanner.setText(bu);
                if (fields.get("icon_url")   instanceof String iu && !iu.isEmpty()) editIcon.setText(iu);
                saveBtn.setText("SAVING...");
                ApiClient.updateServer(s.id, fields).thenAccept(ok -> Platform.runLater(() -> {
                    saveBtn.setDisable(false); saveBtn.setText("SAVE CHANGES");
                    saveStatus.setText(ok ? "✓  All changes saved." : "✗  Save failed — check connection.");
                    saveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (ok ? "#4caf50" : "#e05252") + ";");
                    saveStatus.setVisible(true); saveStatus.setManaged(true);
                }));
            }));
        });

        toggleVisBtn.setOnAction(e -> {
            editVisible.setSelected(!editVisible.isSelected());
            boolean vis = editVisible.isSelected();
            toggleVisBtn.setText(vis ? "HIDE FROM STORE" : "SHOW IN STORE");
            toggleVisBtn.setStyle(vis
                ? "-fx-background-color: #4a2a2a; -fx-text-fill: #e05252; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"
                : "-fx-background-color: #2a4a2a; -fx-text-fill: #4caf50; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"
            );
            saveBtn.fire();
        });

        // ── LOAD SERVER LIST ──────────────────────────────────────────────────
        loader.get().thenAccept(servers -> Platform.runLater(() -> {
            serverList[0] = servers;
            loadingLbl.setVisible(false); loadingLbl.setManaged(false);
            for (ServerProfile sv : servers) {
                String label = sv.name != null ? sv.name : "(unnamed)";
                if (isStaff && sv.approved == 0) label += "  [PENDING]";
                if (isStaff && sv.visible  == 0) label += "  [HIDDEN]";
                serverPicker.getItems().add(label);
            }
            if (servers.isEmpty()) {
                loadingLbl.setText(emptyHint != null ? emptyHint : "No servers found.");
                loadingLbl.setWrapText(true);
                loadingLbl.setVisible(true); loadingLbl.setManaged(true);
                serverPicker.setVisible(false); serverPicker.setManaged(false);
            }
        }));

        return devSection(sectionTitle,
            loadingLbl,
            serverPicker,
            formBox
        );
    }

    // ── SECTION 0: CLAIM SERVER ──────────────────────────────────────────────

    private static VBox buildClaimSection() {
        Label desc = new Label("Already listed on RSPS Hub but didn't submit it yourself? Claim ownership to manage your server's page.");
        desc.getStyleClass().add("dev-intro-sub");
        desc.setWrapText(true);

        TextField claimNameField = styledField("Enter your server name exactly as listed...");

        TextField verifyField = styledField("Your website domain or Discord invite URL (for verification)");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button claimBtn = new Button("CLAIM OWNERSHIP");
        claimBtn.getStyleClass().add("auth-btn");
        claimBtn.setPrefWidth(200);

        claimBtn.setOnAction(e -> {
            String serverName = claimNameField.getText().trim();
            String verify = verifyField.getText().trim();
            if (serverName.isEmpty() || verify.isEmpty()) {
                statusLabel.setText("Please fill in both fields.");
                statusLabel.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                statusLabel.setVisible(true); statusLabel.setManaged(true);
                return;
            }
            claimBtn.setDisable(true);
            claimBtn.setText("SUBMITTING...");
            statusLabel.setVisible(false); statusLabel.setManaged(false);

            String safe1 = serverName.replace("\"", "\\\"");
            String safe2 = verify.replace("\"", "\\\"");
            String payload = "{\"server_name\":\"" + safe1 + "\",\"verify\":\"" + safe2
                + "\",\"username\":\"" + LauncherEngine.currentUsername + "\"}";

            ApiClient.postJson("claim_server.php", payload).thenAccept(response -> {
                javafx.application.Platform.runLater(() -> {
                    claimBtn.setDisable(false);
                    claimBtn.setText("CLAIM OWNERSHIP");
                    statusLabel.setVisible(true); statusLabel.setManaged(true);
                    // Show success regardless — backend will review it
                    statusLabel.setText("✓  Claim submitted! We'll review your request and contact you via Discord.");
                    statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px;");
                    claimNameField.clear(); verifyField.clear();
                });
            }).exceptionally(ex -> {
                javafx.application.Platform.runLater(() -> {
                    claimBtn.setDisable(false);
                    claimBtn.setText("CLAIM OWNERSHIP");
                    statusLabel.setText("✓  Claim submitted! We'll review your request and contact you via Discord.");
                    statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px;");
                    statusLabel.setVisible(true); statusLabel.setManaged(true);
                });
                return null;
            });
        });

        return devSection("CLAIM AN EXISTING LISTING",
            desc,
            devRow("Server Name", claimNameField),
            devRow("Verification", verifyField),
            statusLabel,
            claimBtn
        );
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

            ApiClient.postJson("servers/submit.php", payload).thenAccept(response -> {
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