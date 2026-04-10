import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerDetailScreen {

    public static Scene create(Stage stage, ServerProfile server, Runnable onBack) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // --- TOP BAR ---
        HBox topBar = new HBox();
        topBar.getStyleClass().add("settings-topbar");
        topBar.setPadding(new Insets(16, 40, 16, 40));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Store");
        backBtn.getStyleClass().add("settings-back-btn");
        backBtn.setOnAction(e -> onBack.run());

        topBar.getChildren().add(backBtn);
        root.setTop(new VBox(TitleBar.create(stage), topBar));

        // --- SCROLLABLE CONTENT ---
        VBox content = new VBox(0);
        content.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(
            buildHero(server),
            buildInfoBar(stage, server, onBack),
            buildContentTabs(stage, server, content)
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("main-scroll");
        root.setCenter(scrollPane);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(ServerDetailScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
        return scene;
    }

    // ── HERO BANNER ──────────────────────────────────────────────────────────

    static final int HERO_H = 130;  // banner height — accessible by buildInfoBar

    private static StackPane buildHero(ServerProfile server) {
        StackPane hero = new StackPane();
        hero.setMinHeight(HERO_H);
        hero.setMaxHeight(HERO_H);
        hero.setPrefHeight(HERO_H);
        hero.getStyleClass().add("detail-hero");

        // Clip the whole hero — icon lives in the info bar below, so no overlap issue
        Rectangle heroClip = new Rectangle();
        heroClip.setHeight(HERO_H);
        heroClip.widthProperty().bind(hero.widthProperty());
        hero.setClip(heroClip);

        // Banner — same as Exora: natural ratio, height locked to HERO_H, centred.
        if (server.bannerUrl != null && !server.bannerUrl.isEmpty()) {
            ImageView bannerImg = new ImageView();
            bannerImg.setPreserveRatio(true);
            bannerImg.setFitHeight(HERO_H);
            bannerImg.setSmooth(true);
            Image img = new Image(server.bannerUrl, true);
            img.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) bannerImg.setImage(img);
            });
            hero.getChildren().add(bannerImg);
        }

        // Gradient fade at the bottom
        Region overlay = new Region();
        overlay.setManaged(false);
        String gradientEnd = LauncherEngine.lightMode ? "#f0f2f5" : "#0f1115";
        overlay.setStyle("-fx-background-color: linear-gradient(to bottom, transparent 30%, " + gradientEnd + " 100%);");
        overlay.prefWidthProperty().bind(hero.widthProperty());
        overlay.setPrefHeight(HERO_H);
        hero.getChildren().add(overlay);

        return hero;
    }

    /** Builds the icon pane (80×80). Called by buildInfoBar so the icon
     *  lives outside the clipped hero and can visually float upward via translateY. */
    private static StackPane buildIconPane(ServerProfile server) {
        StackPane iconPane = new StackPane();
        iconPane.getStyleClass().add("detail-icon-box");
        iconPane.setPrefSize(80, 80); iconPane.setMinSize(80, 80); iconPane.setMaxSize(80, 80);

        Label placeholder = new Label(server.name.substring(0, 1).toUpperCase());
        placeholder.getStyleClass().add("detail-icon-placeholder");

        if (server.iconUrl != null && !server.iconUrl.isEmpty()) {
            ImageView iconImg = new ImageView();
            iconImg.setFitWidth(80); iconImg.setFitHeight(80);
            iconImg.setPreserveRatio(false); iconImg.setManaged(false);
            Image img = new Image(server.iconUrl, true);
            img.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) {
                    iconImg.setImage(img); placeholder.setVisible(false);
                }
            });
            iconPane.getChildren().addAll(placeholder, iconImg);
        } else {
            iconPane.getChildren().add(placeholder);
        }
        return iconPane;
    }

    // ── INFO BAR (name, play button, links) ─────────────────────────────────

    private static HBox buildInfoBar(Stage stage, ServerProfile server, Runnable onBack) {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(12, 40, 24, 40));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("detail-info-bar");

        // Icon — lives here so it's outside the clipped hero.
        // Negative translateY pulls it up to visually straddle the hero boundary.
        StackPane iconPane = buildIconPane(server);
        iconPane.setTranslateY(-36);  // float up into the hero area
        bar.getChildren().add(iconPane);

        // Left: name + tagline + tags
        VBox left = new VBox(6);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label nameLabel = new Label(server.name);
        nameLabel.getStyleClass().add("detail-server-name");

        if (server.tagline != null && !server.tagline.isEmpty()) {
            Label tagline = new Label(server.tagline);
            tagline.getStyleClass().add("detail-tagline");
            left.getChildren().addAll(nameLabel, tagline);
        } else {
            left.getChildren().add(nameLabel);
        }

        // Tags
        HBox tagBox = new HBox(6);
        if (server.tags != null) {
            for (String t : server.tags) {
                Label pill = new Label(t.toUpperCase());
                pill.getStyleClass().add("tag-pill");
                tagBox.getChildren().add(pill);
            }
        }
        left.getChildren().add(tagBox);

        // Right: player count + play button + social links
        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER_RIGHT);

        // Updated to playersOnline
        Label players = new Label("🟢 " + server.playersOnline + " Online");
        players.getStyleClass().add("player-count");

        // Updated to accentColor
        String accent = (server.accentColor != null && !server.accentColor.isEmpty())
            ? server.accentColor : LauncherEngine.accentColor;

        boolean alreadyInstalled = LauncherEngine.isDownloaded(server);
        Button playBtn = new Button(alreadyInstalled ? "PLAY" : "INSTALL");
        playBtn.getStyleClass().add("play-button");
        playBtn.setStyle("-fx-background-color: " + accent + ";");
        playBtn.setPrefWidth(160);

        ProgressBar dlBar = new ProgressBar(0);
        dlBar.setPrefWidth(160);
        dlBar.setVisible(false);
        dlBar.setManaged(false);
        dlBar.setStyle("-fx-accent: " + accent + ";");

        Label dlLabel = new Label();
        dlLabel.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 11px;");
        dlLabel.setVisible(false);
        dlLabel.setManaged(false);

        // Update banner (shown async if update found)
        Label updateChip = new Label("⬆  Update available");
        updateChip.setStyle(
            "-fx-background-color: rgba(255,152,31,0.12); -fx-border-color: rgba(255,152,31,0.4);" +
            "-fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #ff981f;" +
            "-fx-font-size: 11px; -fx-padding: 4 10;"
        );
        updateChip.setVisible(false);
        updateChip.setManaged(false);

        Button updatePlayBtn = new Button("UPDATE & PLAY");
        updatePlayBtn.setStyle(
            "-fx-background-color: " + accent + "; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"
        );
        updatePlayBtn.setVisible(false);
        updatePlayBtn.setManaged(false);
        updatePlayBtn.setPrefWidth(160);

        // Shared launch logic — delegates to RSPSHub.beginSession for full tracking
        // with ProcessHandle descendant watching (handles launcher JARs that spawn
        // a child game process and then exit themselves).
        Runnable launchNow = () -> {
            if (LauncherEngine.minimizeOnLaunch) stage.setIconified(true);
            Process proc = LauncherEngine.launchGame(server);
            if (proc != null) {
                long startEpoch = System.currentTimeMillis() / 1000;
                DiscordRPC.setActivity(server.name, startEpoch);
                // Start tracking — timer will show in hub navbar
                RSPSHub.beginSession(server.name, proc, stage);
                // Navigate back to hub so the session timer is visible to the user
                javafx.application.Platform.runLater(onBack);
            }
        };

        // Shared download-then-action logic
        java.util.function.Consumer<Runnable> downloadThen = (afterDownload) -> {
            playBtn.setDisable(true);
            updatePlayBtn.setDisable(true);
            playBtn.setText("DOWNLOADING...");
            dlBar.setProgress(0);
            dlBar.setVisible(true);
            dlBar.setManaged(true);
            dlLabel.setText("0%");
            dlLabel.setVisible(true);
            dlLabel.setManaged(true);
            new Thread(() -> {
                boolean ok = LauncherEngine.downloadClient(server,
                    progress -> javafx.application.Platform.runLater(() -> {
                        dlBar.setProgress(progress);
                        dlLabel.setText((int)(progress * 100) + "%");
                    }), null);
                javafx.application.Platform.runLater(() -> {
                    dlBar.setVisible(false);  dlBar.setManaged(false);
                    dlLabel.setVisible(false); dlLabel.setManaged(false);
                    playBtn.setDisable(false); updatePlayBtn.setDisable(false);
                    playBtn.setText(ok ? "PLAY" : "INSTALL");
                    if (ok) afterDownload.run();
                });
            }).start();
        };

        playBtn.setOnAction(e -> {
            if (!LauncherEngine.isDownloaded(server)) {
                downloadThen.accept(launchNow);
            } else {
                launchNow.run();
            }
        });

        updatePlayBtn.setOnAction(e -> downloadThen.accept(launchNow));

        // Async update check (only if already installed)
        if (alreadyInstalled) {
            new Thread(() -> {
                boolean hasUpdate = LauncherEngine.isUpdateAvailable(server);
                javafx.application.Platform.runLater(() -> {
                    if (hasUpdate) {
                        if (LauncherEngine.autoUpdateClients) {
                            // Auto mode: swap play button for update+play directly
                            playBtn.setVisible(false);
                            playBtn.setManaged(false);
                            updatePlayBtn.setVisible(true);
                            updatePlayBtn.setManaged(true);
                            updateChip.setVisible(true);
                            updateChip.setManaged(true);
                        } else {
                            // Manual mode: show chip + extra button
                            updateChip.setVisible(true);
                            updateChip.setManaged(true);
                            updatePlayBtn.setVisible(true);
                            updatePlayBtn.setManaged(true);
                        }
                    }
                });
            }, "update-check").start();
        }

        HBox socialRow = new HBox(8);
        socialRow.setAlignment(Pos.CENTER_RIGHT);
        
        if (server.discordUrl != null && !server.discordUrl.isEmpty()) {
            Button discordBtn = new Button("Discord");
            discordBtn.getStyleClass().add("detail-social-btn");
            discordBtn.setOnAction(e -> openUrl(server.discordUrl));
            socialRow.getChildren().add(discordBtn);
        }
        if (server.websiteUrl != null && !server.websiteUrl.isEmpty()) {
            Button webBtn = new Button("Website");
            webBtn.getStyleClass().add("detail-social-btn");
            webBtn.setOnAction(e -> openUrl(server.websiteUrl));
            socialRow.getChildren().add(webBtn);
        }

        right.getChildren().addAll(players, updateChip, playBtn, updatePlayBtn, dlBar, dlLabel, socialRow);
        bar.getChildren().addAll(left, right);
        return bar;
    }

    // ── DESCRIPTION ──────────────────────────────────────────────────────────

    // ── ABOUT / GALLERY TAB SYSTEM ───────────────────────────────────────────

    private static VBox buildContentTabs(Stage stage, ServerProfile server, VBox content) {
        // Build both panes up front
        VBox aboutPane = new VBox(0);
        aboutPane.getChildren().addAll(
            buildDescriptionSection(server),
            buildChangelogSection(server),
            buildDivider(),
            buildReviewsSection(server, content)
        );

        VBox galleryPane = buildGallerySection(stage, server);
        galleryPane.setVisible(false);
        galleryPane.setManaged(false);

        // Tab buttons
        String activeStyle   = "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-color: transparent; -fx-border-color: transparent transparent " +
            LauncherEngine.accentColor + " transparent; -fx-border-width: 0 0 2 0;" +
            "-fx-padding: 12 20; -fx-cursor: hand;";
        String inactiveStyle = "-fx-text-fill: #8b92a5; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-color: transparent; -fx-border-color: transparent;" +
            "-fx-padding: 12 20; -fx-cursor: hand;";

        int galleryCount = (server.screenshots != null) ? server.screenshots.size() : 0;
        String galleryLabel = galleryCount > 0 ? "GALLERY  (" + galleryCount + ")" : "GALLERY";

        Button aboutBtn   = new Button("ABOUT THIS SERVER");
        Button galleryBtn = new Button(galleryLabel);
        aboutBtn.setStyle(activeStyle);
        galleryBtn.setStyle(inactiveStyle);

        aboutBtn.setOnAction(e -> {
            aboutPane.setVisible(true);   aboutPane.setManaged(true);
            galleryPane.setVisible(false); galleryPane.setManaged(false);
            aboutBtn.setStyle(activeStyle);
            galleryBtn.setStyle(inactiveStyle);
        });
        galleryBtn.setOnAction(e -> {
            galleryPane.setVisible(true);  galleryPane.setManaged(true);
            aboutPane.setVisible(false);   aboutPane.setManaged(false);
            galleryBtn.setStyle(activeStyle);
            aboutBtn.setStyle(inactiveStyle);
        });

        Region tabSpacer = new Region();
        HBox.setHgrow(tabSpacer, Priority.ALWAYS);
        HBox tabBar = new HBox(aboutBtn, galleryBtn, tabSpacer);
        tabBar.setStyle("-fx-border-color: transparent transparent #2a2e39 transparent; -fx-border-width: 0 0 1 0;");
        tabBar.setPadding(new Insets(0, 40, 0, 40));

        VBox result = new VBox(0, tabBar, aboutPane, galleryPane);
        return result;
    }

    private static VBox buildGallerySection(Stage stage, ServerProfile server) {
        VBox section = new VBox(20);
        section.setPadding(new Insets(30, 40, 30, 40));

        boolean hasScreenshots = server.screenshots != null && !server.screenshots.isEmpty();
        if (!hasScreenshots) {
            Label empty = new Label("No gallery content has been added for this server yet.");
            empty.getStyleClass().add("auth-muted");
            section.getChildren().add(empty);
            return section;
        }

        List<String> imageUrls = new ArrayList<>();
        List<String> videoUrls = new ArrayList<>();
        for (String url : server.screenshots) {
            if (isYouTubeUrl(url)) videoUrls.add(url);
            else imageUrls.add(url);
        }

        // ── Screenshots grid ──
        if (!imageUrls.isEmpty()) {
            Label imgHeader = sectionHeader("SCREENSHOTS");
            section.getChildren().add(imgHeader);

            List<Image> loadedImages = new ArrayList<>();
            FlowPane grid = new FlowPane(10, 10);
            grid.setPrefWrapLength(Double.MAX_VALUE);

            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                final int idx = i;

                StackPane thumb = new StackPane();
                thumb.setPrefSize(310, 175); thumb.setMinSize(310, 175); thumb.setMaxSize(310, 175);
                thumb.getStyleClass().add("screenshot-box");
                thumb.setStyle(thumb.getStyle() + "-fx-cursor: hand;");

                Label placeholder = new Label("Loading...");
                placeholder.getStyleClass().add("dev-preview-placeholder");

                ImageView iv = new ImageView();
                iv.setFitWidth(310); iv.setFitHeight(175);
                iv.setPreserveRatio(false);
                iv.setVisible(false); iv.setManaged(false);

                Label hoverHint = new Label("🔍  View");
                hoverHint.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                    "-fx-background-color: rgba(0,0,0,0.55); -fx-padding: 4 12; -fx-background-radius: 6;");
                hoverHint.setVisible(false);

                Image img = new Image(url, true);
                loadedImages.add(img);
                img.progressProperty().addListener((obs, old, p) -> {
                    if (p.doubleValue() >= 1.0 && !img.isError()) {
                        iv.setImage(img);
                        iv.setVisible(true); iv.setManaged(true);
                        placeholder.setVisible(false); placeholder.setManaged(false);
                    }
                });

                thumb.getChildren().addAll(placeholder, iv, hoverHint);
                thumb.setOnMouseEntered(e -> hoverHint.setVisible(true));
                thumb.setOnMouseExited(e -> hoverHint.setVisible(false));
                thumb.setOnMouseClicked(e -> openLightbox(stage, loadedImages, idx));
                grid.getChildren().add(thumb);
            }
            section.getChildren().add(grid);
        }

        // ── YouTube videos ──
        if (!videoUrls.isEmpty()) {
            Label vidHeader = sectionHeader("VIDEOS");
            section.getChildren().add(vidHeader);

            FlowPane vidGrid = new FlowPane(10, 10);
            vidGrid.setPrefWrapLength(Double.MAX_VALUE);

            for (String url : videoUrls) {
                String videoId = extractYouTubeId(url);
                String thumbUrl = videoId != null
                    ? "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg"
                    : null;

                StackPane thumb = new StackPane();
                thumb.setPrefSize(310, 175); thumb.setMinSize(310, 175); thumb.setMaxSize(310, 175);
                thumb.getStyleClass().add("screenshot-box");
                thumb.setStyle(thumb.getStyle() + "-fx-cursor: hand;");

                Label placeholder = new Label("Video");
                placeholder.getStyleClass().add("dev-preview-placeholder");

                if (thumbUrl != null) {
                    ImageView iv = new ImageView();
                    iv.setFitWidth(310); iv.setFitHeight(175);
                    iv.setPreserveRatio(false);
                    iv.setVisible(false); iv.setManaged(false);
                    Image img = new Image(thumbUrl, true);
                    img.progressProperty().addListener((obs, old, p) -> {
                        if (p.doubleValue() >= 1.0 && !img.isError()) {
                            iv.setImage(img);
                            iv.setVisible(true); iv.setManaged(true);
                            placeholder.setVisible(false); placeholder.setManaged(false);
                        }
                    });
                    thumb.getChildren().add(iv);
                }

                // Play button overlay
                Label playBtn = new Label("▶");
                playBtn.setStyle(
                    "-fx-text-fill: white; -fx-font-size: 28px;" +
                    "-fx-background-color: rgba(0,0,0,0.65);" +
                    "-fx-padding: 10 16; -fx-background-radius: 50;"
                );
                thumb.getChildren().addAll(placeholder, playBtn);
                thumb.setOnMouseClicked(e -> openUrl(url));
                vidGrid.getChildren().add(thumb);
            }
            section.getChildren().add(vidGrid);
        }

        return section;
    }

    private static boolean isYouTubeUrl(String url) {
        return url != null && (url.contains("youtube.com/watch") || url.contains("youtu.be/"));
    }

    private static String extractYouTubeId(String url) {
        try {
            if (url.contains("youtu.be/")) {
                String id = url.substring(url.indexOf("youtu.be/") + 9);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                return id;
            }
            if (url.contains("v=")) {
                String id = url.substring(url.indexOf("v=") + 2);
                if (id.contains("&")) id = id.substring(0, id.indexOf("&"));
                return id;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static VBox buildDescriptionSection(ServerProfile server) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(10, 40, 30, 40));

        Label header = sectionHeader("ABOUT THIS SERVER");

        String fullText = server.description != null ? server.description : "No description provided.";

        TextFlow desc = buildLinkedTextFlow(fullText);
        desc.getStyleClass().add("detail-description");

        // Collapsed: show ~4 lines (~90px). Expanded: show full text.
        final double COLLAPSED_H = 90;
        desc.setMaxHeight(COLLAPSED_H);
        desc.setMinHeight(COLLAPSED_H);
        desc.setPrefHeight(COLLAPSED_H);
        // Clip so text doesn't visually overflow when collapsed
        Rectangle descClip = new Rectangle();
        descClip.setWidth(9999);
        descClip.setHeight(COLLAPSED_H);
        desc.setClip(descClip);

        // Fade-out gradient at the bottom of collapsed text
        Region fadeOut = new Region();
        fadeOut.setStyle("-fx-background-color: linear-gradient(to bottom, transparent 0%, #0f1115 100%);");
        fadeOut.setPrefHeight(36);
        fadeOut.setMaxHeight(36);
        fadeOut.setMaxWidth(Double.MAX_VALUE);
        fadeOut.setMouseTransparent(true);

        StackPane descStack = new StackPane(desc, fadeOut);
        StackPane.setAlignment(fadeOut, Pos.BOTTOM_CENTER);
        descStack.setMaxHeight(COLLAPSED_H);
        descStack.setMinHeight(COLLAPSED_H);

        boolean[] expanded = {false};
        Button expandBtn = new Button("▼  Read more");
        expandBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #9b5de5;" +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;"
        );
        expandBtn.setOnAction(e -> {
            expanded[0] = !expanded[0];
            if (expanded[0]) {
                desc.setMaxHeight(Double.MAX_VALUE);
                desc.setMinHeight(0);
                desc.setPrefHeight(Region.USE_COMPUTED_SIZE);
                desc.setClip(null);
                descStack.setMaxHeight(Double.MAX_VALUE);
                descStack.setMinHeight(0);
                fadeOut.setVisible(false);
                expandBtn.setText("▲  Show less");
            } else {
                desc.setMaxHeight(COLLAPSED_H);
                desc.setMinHeight(COLLAPSED_H);
                desc.setPrefHeight(COLLAPSED_H);
                desc.setClip(descClip);
                descStack.setMaxHeight(COLLAPSED_H);
                descStack.setMinHeight(COLLAPSED_H);
                fadeOut.setVisible(true);
                expandBtn.setText("▼  Read more");
            }
        });

        section.getChildren().addAll(header, descStack, expandBtn);
        return section;
    }

    // ── URL-LINKIFIED TEXT FLOW ───────────────────────────────────────────────

    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE
    );

    private static TextFlow buildLinkedTextFlow(String text) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(3);

        Matcher m = URL_PATTERN.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                Text plain = new Text(text.substring(last, m.start()));
                plain.setStyle("-fx-fill: #c8cdd8; -fx-font-size: 13px;");
                flow.getChildren().add(plain);
            }
            String url = m.group();
            Hyperlink link = new Hyperlink(url);
            link.setStyle(
                "-fx-text-fill: #ff981f; -fx-border-color: transparent; " +
                "-fx-padding: 0; -fx-font-size: 13px; -fx-cursor: hand;"
            );
            link.setOnAction(e -> openUrl(url));
            flow.getChildren().add(link);
            last = m.end();
        }
        if (last < text.length()) {
            Text tail = new Text(text.substring(last));
            tail.setStyle("-fx-fill: #c8cdd8; -fx-font-size: 13px;");
            flow.getChildren().add(tail);
        }
        return flow;
    }

    // ── SCREENSHOTS ──────────────────────────────────────────────────────────

    private static VBox buildScreenshotsSection(Stage stage, ServerProfile server) {
        if (server.screenshots == null || server.screenshots.isEmpty()) return new VBox();

        VBox section = new VBox(12);
        section.setPadding(new Insets(0, 40, 30, 40));

        Label header = sectionHeader("SCREENSHOTS");

        ScrollPane hScroll = new ScrollPane();
        hScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        hScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        hScroll.getStyleClass().add("main-scroll");
        hScroll.setPrefHeight(200);

        HBox imgRow = new HBox(10);
        imgRow.setPadding(new Insets(4, 0, 4, 0));

        List<Image> loadedImages = new ArrayList<>();
        for (int i = 0; i < server.screenshots.size(); i++) {
            String url = server.screenshots.get(i);
            final int idx = i;

            StackPane imgBox = new StackPane();
            imgBox.getStyleClass().add("screenshot-box");
            imgBox.setPrefSize(300, 170);
            imgBox.setMinSize(300, 170);
            imgBox.setMaxSize(300, 170);
            imgBox.setStyle("-fx-cursor: hand;");

            Label placeholder = new Label("Loading...");
            placeholder.getStyleClass().add("dev-preview-placeholder");

            ImageView iv = new ImageView();
            iv.setFitWidth(300);
            iv.setFitHeight(170);
            iv.setPreserveRatio(false);
            iv.setManaged(false);
            iv.setVisible(false);

            // Hover overlay hint
            Label hoverHint = new Label("🔍 View");
            hoverHint.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 4 10 4 10; -fx-background-radius: 6;");
            hoverHint.setVisible(false);

            Image img = new Image(url, true);
            loadedImages.add(img);
            img.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) {
                    iv.setImage(img);
                    iv.setManaged(true);
                    iv.setVisible(true);
                    placeholder.setVisible(false);
                }
            });

            imgBox.getChildren().addAll(placeholder, iv, hoverHint);
            imgBox.setOnMouseEntered(e -> hoverHint.setVisible(true));
            imgBox.setOnMouseExited(e -> hoverHint.setVisible(false));
            imgBox.setOnMouseClicked(e -> openLightbox(stage, loadedImages, idx));
            imgRow.getChildren().add(imgBox);
        }

        hScroll.setContent(imgRow);
        section.getChildren().addAll(header, hScroll);
        return section;
    }

    // ── CHANGELOG ────────────────────────────────────────────────────────────

    private static VBox buildChangelogSection(ServerProfile server) {
        if (server.changelog == null || server.changelog.isEmpty()) return new VBox();

        VBox section = new VBox(12);
        section.setPadding(new Insets(0, 40, 30, 40));

        section.getChildren().add(sectionHeader("WHAT'S NEW"));
        section.getChildren().add(buildPatchNotes(server.changelog));
        return section;
    }

    private static VBox buildPatchNotes(String changelog) {
        VBox notes = new VBox(0);

        for (String raw : changelog.split("\n")) {
            String line = raw.stripTrailing();

            if (line.isEmpty()) {
                // Spacer between versions
                Region spacer = new Region();
                spacer.setPrefHeight(10);
                notes.getChildren().add(spacer);
                continue;
            }

            boolean isVersion = line.matches("(?i)^v\\d.*") || line.startsWith("[");
            boolean isBullet  = line.startsWith("-") || line.startsWith("•");

            if (isVersion) {
                // Version header row with a pill badge
                String[] parts = line.split("—", 2);
                String ver  = parts[0].trim();
                String date = parts.length > 1 ? parts[1].trim() : "";

                Label verLabel = new Label(ver);
                verLabel.setStyle(
                    "-fx-text-fill: " + LauncherEngine.accentColor + ";" +
                    "-fx-font-size: 13px; -fx-font-weight: bold;" +
                    "-fx-background-color: rgba(255,152,31,0.12);" +
                    "-fx-background-radius: 6; -fx-padding: 3 10;"
                );

                HBox row = new HBox(10, verLabel);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 0, 6, 0));

                if (!date.isEmpty()) {
                    Label dateLabel = new Label(date);
                    dateLabel.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");
                    row.getChildren().add(dateLabel);
                }

                notes.getChildren().add(row);

            } else if (isBullet) {
                String text = line.substring(1).trim();
                HBox row = new HBox(8);
                row.setPadding(new Insets(2, 0, 2, 12));
                row.setAlignment(javafx.geometry.Pos.TOP_LEFT);

                Label dot = new Label("•");
                dot.setStyle("-fx-text-fill: " + LauncherEngine.accentColor + "; -fx-font-size: 13px;");
                dot.setMinWidth(14);

                Label content = new Label(text);
                content.setStyle("-fx-text-fill: #c8cdd8; -fx-font-size: 13px;");
                content.setWrapText(true);
                HBox.setHgrow(content, Priority.ALWAYS);

                row.getChildren().addAll(dot, content);
                notes.getChildren().add(row);

            } else {
                // Plain text line
                Label lbl = new Label(line);
                lbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");
                lbl.setWrapText(true);
                lbl.setPadding(new Insets(2, 0, 2, 0));
                notes.getChildren().add(lbl);
            }
        }

        return notes;
    }

    // ── REVIEWS ──────────────────────────────────────────────────────────────

    private static VBox buildReviewsSection(ServerProfile server, VBox content) {
        VBox section = new VBox(20);
        section.setPadding(new Insets(30, 40, 60, 40));

        Label header   = sectionHeader("REVIEWS");
        Label avgLabel = new Label("—");
        avgLabel.getStyleClass().add("detail-avg-rating");
        Label starsLabel = new Label("☆☆☆☆☆");
        starsLabel.getStyleClass().add("detail-stars-display");
        Label countLabel = new Label("(loading...)");
        countLabel.getStyleClass().add("auth-muted");

        HBox ratingHeader = new HBox(16, header, starsLabel, avgLabel, countLabel);
        ratingHeader.setAlignment(Pos.CENTER_LEFT);

        VBox reviewList = new VBox(12);
        Label loadingLbl = new Label("Loading reviews...");
        loadingLbl.getStyleClass().add("auth-muted");
        reviewList.getChildren().add(loadingLbl);

        VBox submitForm = buildSubmitForm(server, reviewList, avgLabel, starsLabel, countLabel);

        section.getChildren().addAll(ratingHeader, reviewList, buildDivider(), submitForm);

        // Fetch from API
        ApiClient.getReviews(server.id).thenAccept(reviews ->
            javafx.application.Platform.runLater(() -> {
                refreshReviewList(reviewList, reviews);
                updateRatingHeader(avgLabel, starsLabel, countLabel, reviews);
            })
        );

        return section;
    }

    private static void refreshReviewList(VBox reviewList, List<Review> reviews) {
        reviewList.getChildren().clear();
        if (reviews.isEmpty()) {
            Label empty = new Label("No reviews yet. Be the first!");
            empty.getStyleClass().add("auth-muted");
            reviewList.getChildren().add(empty);
            return;
        }
        for (Review r : reviews) reviewList.getChildren().add(buildReviewCard(r));
    }

    private static void updateRatingHeader(Label avgLabel, Label starsLabel, Label countLabel, List<Review> reviews) {
        if (reviews.isEmpty()) {
            avgLabel.setText("—"); starsLabel.setText("☆☆☆☆☆");
            countLabel.setText("(no reviews yet)");
        } else {
            double avg = reviews.stream().mapToInt(r -> r.stars).average().orElse(0);
            avgLabel.setText(String.format("%.1f / 5", avg));
            starsLabel.setText(buildStarString((int) Math.round(avg)));
            countLabel.setText("(" + reviews.size() + " review" + (reviews.size() == 1 ? "" : "s") + ")");
        }
    }

    // ── MODERATION API (used by DeveloperPortalScreen) ───────────────────────
    public static Map<Integer, List<Review>> getPendingReviews() { return new java.util.HashMap<>(); }
    public static void approveReview(int serverId, Review review) {}
    public static void rejectReview(int serverId, Review review) {}

    private static HBox buildReviewCard(Review r) {
        HBox card = new HBox(16);
        card.getStyleClass().add("review-card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_LEFT);

        // Avatar
        Label avatar = new Label(r.username.substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("review-avatar");

        // Content
        VBox body = new VBox(6);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label username = new Label(r.username);
        username.getStyleClass().add("review-username");
        Label stars = new Label(buildStarString(r.stars));
        stars.getStyleClass().add("detail-stars-display");
        Label date = new Label(r.date);
        date.getStyleClass().add("auth-muted");
        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);
        topRow.getChildren().addAll(username, stars, rowSpacer, date);

        Label comment = new Label(r.comment);
        comment.getStyleClass().add("card-desc");
        comment.setWrapText(true);

        body.getChildren().addAll(topRow, comment);
        card.getChildren().addAll(avatar, body);
        return card;
    }

    private static VBox buildSubmitForm(ServerProfile server,
                                        VBox reviewList,
                                        Label avgLabel, Label starsLabel, Label countLabel) {
        VBox form = new VBox(14);

        Label formHeader = sectionHeader("LEAVE A REVIEW");

        // Star picker
        int[] selectedRating = {0};
        HBox starPicker = new HBox(6);
        starPicker.setAlignment(Pos.CENTER_LEFT);
        Label[] starLabels = new Label[5];

        for (int i = 0; i < 5; i++) {
            final int rating = i + 1;
            Label star = new Label("☆");
            star.getStyleClass().add("review-star-pick");
            star.setOnMouseClicked(e -> {
                selectedRating[0] = rating;
                updateStarPicker(starLabels, rating, selectedRating[0]);
            });
            star.setOnMouseEntered(e -> updateStarPicker(starLabels, rating, selectedRating[0]));
            star.setOnMouseExited(e -> updateStarPicker(starLabels, selectedRating[0], selectedRating[0]));
            starLabels[i] = star;
            starPicker.getChildren().add(star);
        }

        // Comment field
        TextArea commentField = new TextArea();
        commentField.setPromptText("Share your experience with this server...");
        commentField.getStyleClass().add("dev-textarea");
        commentField.setWrapText(true);
        commentField.setPrefRowCount(3);
        commentField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button submitBtn = new Button("SUBMIT REVIEW");
        submitBtn.getStyleClass().add("auth-btn");
        submitBtn.setPrefWidth(180);

        submitBtn.setOnAction(e -> {
            if (selectedRating[0] == 0) {
                errorLabel.setText("Please select a star rating.");
                errorLabel.setVisible(true); errorLabel.setManaged(true);
                return;
            }
            String text = commentField.getText().trim();
            if (text.isEmpty()) {
                errorLabel.setText("Please write a comment.");
                errorLabel.setVisible(true); errorLabel.setManaged(true);
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("SUBMITTING...");
            errorLabel.setVisible(false); errorLabel.setManaged(false);

            ApiClient.submitReview(server.id, selectedRating[0], text).thenAccept(result ->
                javafx.application.Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                    submitBtn.setText("SUBMIT REVIEW");
                    if ("ok".equals(result)) {
                        commentField.clear();
                        selectedRating[0] = 0;
                        updateStarPicker(starLabels, 0, 0);
                        // Refresh from API so the new review shows
                        ApiClient.getReviews(server.id).thenAccept(reviews ->
                            javafx.application.Platform.runLater(() -> {
                                refreshReviewList(reviewList, reviews);
                                updateRatingHeader(avgLabel, starsLabel, countLabel, reviews);
                            })
                        );
                    } else {
                        errorLabel.setText(result);
                        errorLabel.setStyle("-fx-text-fill: #e05252; -fx-font-size: 12px;");
                        errorLabel.setVisible(true); errorLabel.setManaged(true);
                    }
                })
            );
        });

        form.getChildren().addAll(formHeader, starPicker, commentField, errorLabel, submitBtn);
        return form;
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private static void updateStarPicker(Label[] stars, int highlightUpTo, int selected) {
        for (int i = 0; i < 5; i++) {
            stars[i].setText(i < highlightUpTo ? "★" : "☆");
            stars[i].getStyleClass().setAll(i < selected ? "review-star-selected" : "review-star-pick");
        }
    }

    private static String buildStarString(int rating) {
        return "★".repeat(Math.max(0, rating)) + "☆".repeat(Math.max(0, 5 - rating));
    }

    private static Label sectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("settings-section-header");
        return lbl;
    }

    private static Region buildDivider() {
        Region div = new Region();
        div.setMinHeight(1);
        div.setMaxHeight(1);
        div.setStyle("-fx-background-color: #2a2e39;");
        return div;
    }

    // ── SCREENSHOT LIGHTBOX ───────────────────────────────────────────────────

    private static void openLightbox(Stage owner, List<Image> images, int startIndex) {
        Stage lightbox = new Stage();
        lightbox.initOwner(owner);
        lightbox.initModality(Modality.APPLICATION_MODAL);
        lightbox.initStyle(StageStyle.TRANSPARENT);

        int[] current = {startIndex};

        // Main image
        ImageView mainImg = new ImageView();
        mainImg.setFitWidth(960);
        mainImg.setFitHeight(540);
        mainImg.setPreserveRatio(true);

        StackPane imagePane = new StackPane(mainImg);
        imagePane.setStyle("-fx-background-color: transparent;");

        // Counter label
        Label counter = new Label();
        counter.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");

        Runnable updateImage = () -> {
            Image img = images.get(current[0]);
            mainImg.setImage(img);
            counter.setText((current[0] + 1) + " / " + images.size());
        };

        // Nav buttons
        Button prevBtn = new Button("‹");
        Button nextBtn = new Button("›");
        for (Button b : new Button[]{prevBtn, nextBtn}) {
            b.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white;" +
                "-fx-font-size: 28px; -fx-font-weight: bold; -fx-background-radius: 50;" +
                "-fx-min-width: 48; -fx-min-height: 48; -fx-cursor: hand;");
        }
        prevBtn.setOnAction(e -> { current[0] = (current[0] - 1 + images.size()) % images.size(); updateImage.run(); });
        nextBtn.setOnAction(e -> { current[0] = (current[0] + 1) % images.size(); updateImage.run(); });
        prevBtn.setVisible(images.size() > 1);
        nextBtn.setVisible(images.size() > 1);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> lightbox.close());

        HBox navRow = new HBox(20, prevBtn, counter, nextBtn);
        navRow.setAlignment(Pos.CENTER);

        HBox topBar = new HBox(closeBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(0, 0, 12, 0));

        VBox content = new VBox(16, topBar, imagePane, navRow);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setStyle(
            "-fx-background-color: rgba(10,12,18,0.95);" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 40, 0, 0, 8);"
        );
        content.setMaxWidth(1040);
        content.setMaxHeight(660);

        // Dim overlay
        StackPane overlay = new StackPane(content);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.75);");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) lightbox.close(); });

        Scene scene = new Scene(overlay, owner.getWidth(), owner.getHeight());
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) lightbox.close();
            if (e.getCode() == KeyCode.LEFT)  { current[0] = (current[0] - 1 + images.size()) % images.size(); updateImage.run(); }
            if (e.getCode() == KeyCode.RIGHT) { current[0] = (current[0] + 1) % images.size(); updateImage.run(); }
        });

        updateImage.run();

        // Fade in
        overlay.setOpacity(0);
        lightbox.setScene(scene);
        lightbox.show();
        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), overlay);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // ── PROFANITY FILTER ─────────────────────────────────────────────────────

    private static final Set<String> BANNED_WORDS = Set.of(
        "fuck", "shit", "bitch", "asshole", "bastard", "cunt", "dick", "cock",
        "pussy", "faggot", "nigger", "nigga", "retard", "whore", "slut",
        "motherfucker", "fucker", "ass", "piss", "crap", "damn", "hell",
        "bollocks", "wanker", "twat", "prick", "arsehole", "arse"
    );

    private static boolean containsProfanity(String text) {
        String lower = text.toLowerCase().replaceAll("[^a-z ]", " ");
        for (String word : lower.split("\\s+")) {
            if (BANNED_WORDS.contains(word)) return true;
        }
        return false;
    }

    private static void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            System.err.println("Failed to open URL: " + e.getMessage());
        }
    }
}