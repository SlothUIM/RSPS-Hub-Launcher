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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.*;

public class ServerDetailScreen {

    // Local review storage per server (keyed by server ID)
    private static final Map<Integer, List<Review>> reviewStore = new HashMap<>();

    static {
        reviewStore.put(1, new ArrayList<>(List.of(
            new Review("Aceplayer147",  5, "NPCs and bosses are well put together.",                                                                              "2026-04-08"),
            new Review("0din",          5, "Awesome game, really enjoying my time here.",                                                                         "2026-04-07"),
            new Review("Zap",           5, "Yeah, I plan to keep playing for sure.",                                                                              "2026-04-06"),
            new Review("coorsman412",   5, "Enjoying raids, they are pretty fun.",                                                                                "2026-04-01"),
            new Review("daeth",         5, "Trading feels fair, items are priced well.",                                                                          "2026-03-24"),
            new Review("dark prince",   5, "It was clear what to do when I started, which made it easier to get into the game.",                                  "2026-03-23"),
            new Review("royalnikolas",  5, "I see people everywhere while I'm training or exploring.",                                                            "2026-03-23"),
            new Review("shintosaa",     5, "Yeah, there are plenty of updates.",                                                                                  "2026-03-20"),
            new Review("shintosaa",     5, "I'm extremely motivated to level up.",                                                                                "2026-03-19"),
            new Review("yuluthu",       5, "Drop catcher, bottomless aggression, exodus staff are the items I'm most proud of in my bank.",                       "2026-03-17"),
            new Review("Azazo",         5, "Smooth & instant during fast fights.",                                                                                "2026-03-17"),
            new Review("lian",          4, "Sunday is the most active day.",                                                                                      "2026-03-17")
        )));
    }

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
            buildDivider(),
            buildDescriptionSection(server),
            buildScreenshotsSection(stage, server),
            buildChangelogSection(server),
            buildDivider(),
            buildReviewsSection(server, content)
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

        // Banner — fit-height mode: image is always exactly HERO_H px tall at its
        // natural aspect ratio, centred horizontally. Wide landscape images fill the
        // hero edge-to-edge; logos/square images sit centred with dark bg on sides.
        // For a full-bleed look upload a ~1280×360 (or wider) landscape image.
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

    private static VBox buildDescriptionSection(ServerProfile server) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(10, 40, 30, 40));

        Label header = sectionHeader("ABOUT THIS SERVER");

        String fullText = server.description != null ? server.description : "No description provided.";

        Label desc = new Label(fullText);
        desc.getStyleClass().add("detail-description");
        desc.setWrapText(true);

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
        List<Review> reviews = reviewStore.computeIfAbsent(server.id, k -> new ArrayList<>());

        VBox section = new VBox(20);
        section.setPadding(new Insets(30, 40, 60, 40));

        // Header + average rating
        double avg = reviews.isEmpty() ? 0 : reviews.stream().mapToInt(r -> r.stars).average().orElse(0);
        HBox ratingHeader = new HBox(16);
        ratingHeader.setAlignment(Pos.CENTER_LEFT);

        Label header = sectionHeader("REVIEWS");
        Label avgLabel = new Label(String.format("%.1f / 5", avg));
        avgLabel.getStyleClass().add("detail-avg-rating");
        Label starsLabel = new Label(buildStarString((int) Math.round(avg)));
        starsLabel.getStyleClass().add("detail-stars-display");
        Label countLabel = new Label("(" + reviews.size() + " reviews)");
        countLabel.getStyleClass().add("auth-muted");

        ratingHeader.getChildren().addAll(header, starsLabel, avgLabel, countLabel);

        // Review list
        VBox reviewList = new VBox(12);
        refreshReviewList(reviewList, reviews);

        // Submit form
        VBox submitForm = buildSubmitForm(server, reviews, reviewList, avg, avgLabel, starsLabel, countLabel);

        section.getChildren().addAll(ratingHeader, reviewList, buildDivider(), submitForm);
        return section;
    }

    private static void refreshReviewList(VBox reviewList, List<Review> reviews) {
        reviewList.getChildren().clear();
        List<Review> visible = reviews.stream().filter(r -> !r.pending).toList();
        if (visible.isEmpty()) {
            Label empty = new Label("No reviews yet. Be the first!");
            empty.getStyleClass().add("auth-muted");
            reviewList.getChildren().add(empty);
            return;
        }
        for (Review r : visible) {
            reviewList.getChildren().add(buildReviewCard(r));
        }
    }

    // ── MODERATION API (used by DeveloperPortalScreen) ───────────────────────

    /** Returns all pending reviews across every server, as server-name → review pairs. */
    public static Map<Integer, List<Review>> getPendingReviews() {
        Map<Integer, List<Review>> result = new HashMap<>();
        for (var entry : reviewStore.entrySet()) {
            List<Review> pending = entry.getValue().stream().filter(r -> r.pending).toList();
            if (!pending.isEmpty()) result.put(entry.getKey(), pending);
        }
        return result;
    }

    public static void approveReview(int serverId, Review review) {
        review.pending = false;
    }

    public static void rejectReview(int serverId, Review review) {
        List<Review> list = reviewStore.get(serverId);
        if (list != null) list.remove(review);
    }

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

    private static VBox buildSubmitForm(ServerProfile server, List<Review> reviews,
                                        VBox reviewList, double avg,
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
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            String text = commentField.getText().trim();
            if (text.isEmpty()) {
                errorLabel.setText("Please write a comment.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            Review newReview = new Review(
                LauncherEngine.currentUsername.isEmpty() ? "Anonymous" : LauncherEngine.currentUsername,
                selectedRating[0],
                text,
                java.time.LocalDate.now().toString()
            );

            if (containsProfanity(text)) {
                newReview.pending = true;
                reviews.add(0, newReview);
                errorLabel.setText("⚠  Your review contains inappropriate language and is under review by the server owner.");
                errorLabel.setStyle("-fx-text-fill: #e09020; -fx-font-size: 12px;");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            } else {
                reviews.add(0, newReview);
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }

            refreshReviewList(reviewList, reviews);

            // Update average (only count approved reviews)
            double newAvg = reviews.stream().filter(r -> !r.pending).mapToInt(r -> r.stars).average().orElse(0);
            long approved = reviews.stream().filter(r -> !r.pending).count();
            avgLabel.setText(String.format("%.1f / 5", newAvg));
            starsLabel.setText(buildStarString((int) Math.round(newAvg)));
            countLabel.setText("(" + approved + " reviews)");

            commentField.clear();
            selectedRating[0] = 0;
            updateStarPicker(starLabels, 0, 0);
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