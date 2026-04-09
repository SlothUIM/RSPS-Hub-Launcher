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
        // Mock reviews so the page isn't empty on first load
        reviewStore.put(1, new ArrayList<>(List.of(
            new Review("PKMaster99",  5, "Best server I've played in years. Active community and great updates.", "2024-03-10"),
            new Review("IronmanJoe",  4, "Really fun, love the ironman mode. Could use more end-game content.", "2024-02-28"),
            new Review("ZulrahGrind", 3, "Good server but the economy needs some work. Still worth trying.", "2024-02-14")
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
            buildInfoBar(stage, server),
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

    private static StackPane buildHero(ServerProfile server) {
        StackPane hero = new StackPane();
        hero.setMinHeight(220);
        hero.setMaxHeight(220);
        hero.setPrefHeight(220);
        hero.getStyleClass().add("detail-hero");

        // Banner image
        if (server.banner_url != null && !server.banner_url.isEmpty()) {
            ImageView bannerImg = new ImageView();
            bannerImg.setPreserveRatio(false);
            bannerImg.setFitHeight(220);
            bannerImg.setManaged(false);
            bannerImg.fitWidthProperty().bind(hero.widthProperty());
            Image img = new Image(server.banner_url, true);
            img.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) bannerImg.setImage(img);
            });
            hero.getChildren().add(bannerImg);
        }

        // Gradient overlay so text is readable over banner
        Region overlay = new Region();
        overlay.setManaged(false);
        String gradientEnd = LauncherEngine.lightMode ? "#f0f2f5" : "#0f1115";
        overlay.setStyle("-fx-background-color: linear-gradient(to bottom, transparent 30%, " + gradientEnd + " 100%);");
        overlay.prefWidthProperty().bind(hero.widthProperty());
        overlay.prefHeightProperty().bind(hero.heightProperty());
        hero.getChildren().add(overlay);

        // Server icon bottom-left
        StackPane iconPane = new StackPane();
        iconPane.getStyleClass().add("detail-icon-box");
        iconPane.setPrefSize(80, 80);
        iconPane.setMinSize(80, 80);
        iconPane.setMaxSize(80, 80);

        Label iconPlaceholder = new Label(server.name.substring(0, 1).toUpperCase());
        iconPlaceholder.getStyleClass().add("detail-icon-placeholder");

        if (server.icon_url != null && !server.icon_url.isEmpty()) {
            ImageView iconImg = new ImageView();
            iconImg.setFitWidth(80);
            iconImg.setFitHeight(80);
            iconImg.setPreserveRatio(false);
            iconImg.setManaged(false);
            Image img = new Image(server.icon_url, true);
            img.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0 && !img.isError()) {
                    iconImg.setImage(img);
                    iconPlaceholder.setVisible(false);
                }
            });
            iconPane.getChildren().addAll(iconPlaceholder, iconImg);
        } else {
            iconPane.getChildren().add(iconPlaceholder);
        }

        VBox heroBottom = new VBox(iconPane);
        heroBottom.setAlignment(Pos.BOTTOM_LEFT);
        heroBottom.setPadding(new Insets(0, 0, -30, 40));
        heroBottom.setMouseTransparent(true);
        hero.getChildren().add(heroBottom);

        return hero;
    }

    // ── INFO BAR (name, play button, links) ─────────────────────────────────

    private static HBox buildInfoBar(Stage stage, ServerProfile server) {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(40, 40, 24, 140)); // left padding to clear icon
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("detail-info-bar");

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

        Label players = new Label("🟢 " + server.players_online + " Online");
        players.getStyleClass().add("player-count");

        String accent = (server.accent_color != null && !server.accent_color.isEmpty())
            ? server.accent_color : LauncherEngine.accentColor;

        Button playBtn = new Button(LauncherEngine.isDownloaded(server) ? "PLAY" : "INSTALL");
        playBtn.getStyleClass().add("play-button");
        playBtn.setStyle("-fx-background-color: " + accent + ";");
        playBtn.setPrefWidth(160);
        playBtn.setOnAction(e -> {
            if (!LauncherEngine.isDownloaded(server)) {
                playBtn.setDisable(true);
                playBtn.setText("DOWNLOADING...");
                new Thread(() -> {
                    boolean ok = LauncherEngine.downloadClient(server);
                    javafx.application.Platform.runLater(() -> {
                        playBtn.setDisable(false);
                        playBtn.setText(ok ? "PLAY" : "INSTALL");
                    });
                }).start();
            } else {
                LauncherEngine.activeServer = server.name;
                if (LauncherEngine.minimizeOnLaunch) stage.setIconified(true);
                Process proc = LauncherEngine.launchGame(server);
                if (proc != null) {
                    long start = System.currentTimeMillis();
                    new Thread(() -> {
                        try { proc.waitFor(); } catch (InterruptedException ignored) {}
                        long mins = (System.currentTimeMillis() - start) / 60000;
                        PlaytimeStore.recordSession(server.name, mins);
                    }, "playtime-tracker").start();
                }
            }
        });

        HBox socialRow = new HBox(8);
        socialRow.setAlignment(Pos.CENTER_RIGHT);
        if (server.discord_url != null && !server.discord_url.isEmpty()) {
            Button discordBtn = new Button("Discord");
            discordBtn.getStyleClass().add("detail-social-btn");
            socialRow.getChildren().add(discordBtn);
        }
        if (server.website_url != null && !server.website_url.isEmpty()) {
            Button webBtn = new Button("Website");
            webBtn.getStyleClass().add("detail-social-btn");
            socialRow.getChildren().add(webBtn);
        }

        right.getChildren().addAll(players, playBtn, socialRow);
        bar.getChildren().addAll(left, right);
        return bar;
    }

    // ── DESCRIPTION ──────────────────────────────────────────────────────────

    private static VBox buildDescriptionSection(ServerProfile server) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(10, 40, 30, 40));

        Label header = sectionHeader("ABOUT THIS SERVER");
        Label desc = new Label(server.description != null ? server.description : "No description provided.");
        desc.getStyleClass().add("detail-description");
        desc.setWrapText(true);

        section.getChildren().addAll(header, desc);
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
        if (reviews.isEmpty()) {
            Label empty = new Label("No reviews yet. Be the first!");
            empty.getStyleClass().add("auth-muted");
            reviewList.getChildren().add(empty);
            return;
        }
        for (Review r : reviews) {
            reviewList.getChildren().add(buildReviewCard(r));
        }
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
            if (commentField.getText().trim().isEmpty()) {
                errorLabel.setText("Please write a comment.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            Review newReview = new Review(
                LauncherEngine.currentUsername.isEmpty() ? "Anonymous" : LauncherEngine.currentUsername,
                selectedRating[0],
                commentField.getText().trim(),
                java.time.LocalDate.now().toString()
            );
            reviews.add(0, newReview);
            refreshReviewList(reviewList, reviews);

            // Update average
            double newAvg = reviews.stream().mapToInt(r -> r.stars).average().orElse(0);
            avgLabel.setText(String.format("%.1f / 5", newAvg));
            starsLabel.setText(buildStarString((int) Math.round(newAvg)));
            countLabel.setText("(" + reviews.size() + " reviews)");

            commentField.clear();
            selectedRating[0] = 0;
            updateStarPicker(starLabels, 0, 0);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
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
}
