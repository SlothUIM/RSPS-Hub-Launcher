import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnboardingScreen {

    private static final String[] TAGS = {
        "PvP", "Economy", "OSRS", "Hardcore", "Leagues",
        "Vanilla", "Ironman", "Skilling", "Custom", "Minigames"
    };

    public static Scene create(Stage stage, Runnable onComplete) {
        int[] step = {0};
        Set<String> selectedTags = new HashSet<>();

        // Root: VBox — content in center, dots pinned at bottom
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #0f1115;");
        root.setAlignment(Pos.CENTER);

        // Flexible spacer pushes content to center
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        // Step content lives here
        StackPane stepHolder = new StackPane();
        stepHolder.setMaxWidth(520);
        stepHolder.setAlignment(Pos.CENTER);

        // Fixed bottom area: dots + padding
        HBox dotsRow = buildDots(step[0]);
        dotsRow.setAlignment(Pos.CENTER);

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        VBox bottomBar = new VBox(dotsRow);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(24, 0, 36, 0));

        root.getChildren().addAll(topSpacer, stepHolder, bottomSpacer, bottomBar);

        Runnable[] showStep = {null};

        Runnable next = () -> {
            step[0]++;
            dotsRow.getChildren().setAll(buildDots(step[0]).getChildren());
            showStep[0].run();
        };

        showStep[0] = () -> {
            VBox content = switch (step[0]) {
                case 0  -> buildWelcome(next);
                case 1  -> buildInterests(next, selectedTags);
                case 2  -> buildNotifPrefs(next);
                default -> buildFinish(() -> {
                    LauncherEngine.hasCompletedOnboarding = true;
                    LauncherEngine.preferredTags = new ArrayList<>(selectedTags);
                    LauncherEngine.saveSettings();
                    onComplete.run();
                });
            };

            content.setOpacity(0);
            content.setTranslateX(40);
            stepHolder.getChildren().setAll(content);

            new ParallelTransition(
                fade(content, 0, 1, 220),
                slide(content, 40, 0, 220)
            ).play();
        };

        showStep[0].run();

        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(OnboardingScreen.class));
        return scene;
    }

    // ── STEP 1: WELCOME ──────────────────────────────────────────────────────

    private static VBox buildWelcome(Runnable next) {
        Label logo = new Label("RSPS HUB");
        logo.setStyle("-fx-text-fill: #ff981f; -fx-font-size: 36px; -fx-font-weight: 800;");

        Label headline = new Label("Welcome" + (LauncherEngine.currentUsername.isEmpty()
            ? "!" : ", " + LauncherEngine.currentUsername + "!"));
        headline.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        headline.setWrapText(true);
        headline.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label sub = new Label("The ultimate hub for Old School RuneScape private servers.\nLet's get you set up in a few quick steps.");
        sub.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 14px;");
        sub.setWrapText(true);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button btn = primaryBtn("Get Started →");
        btn.setOnAction(e -> next.run());

        VBox box = new VBox(20, logo, headline, sub, btn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 40, 40, 40));
        return box;
    }

    // ── STEP 2: INTERESTS ─────────────────────────────────────────────────────

    private static VBox buildInterests(Runnable next, Set<String> selected) {
        Label headline = new Label("What do you enjoy?");
        headline.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label sub = new Label("Pick the server types you're into. We'll show those first.");
        sub.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");

        FlowPane tags = new FlowPane(10, 10);
        tags.setAlignment(Pos.CENTER);
        tags.setPrefWrapLength(440);

        for (String tag : TAGS) {
            Button btn = new Button(tag);
            btn.setStyle(tagStyle(false));
            btn.setOnAction(e -> {
                boolean on = selected.contains(tag);
                if (on) selected.remove(tag); else selected.add(tag);
                btn.setStyle(tagStyle(!on));
            });
            tags.getChildren().add(btn);
        }

        Button nextBtn = primaryBtn("Continue →");
        nextBtn.setOnAction(e -> next.run());

        Label skip = new Label("Skip");
        skip.setStyle("-fx-text-fill: #555b6e; -fx-font-size: 12px; -fx-cursor: hand;");
        skip.setOnMouseClicked(e -> next.run());

        VBox box = new VBox(16, headline, sub, tags, nextBtn, skip);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 40, 40, 40));
        return box;
    }

    // ── STEP 3: NOTIFICATIONS ─────────────────────────────────────────────────

    private static VBox buildNotifPrefs(Runnable next) {
        Label headline = new Label("Stay in the loop");
        headline.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label sub = new Label("Choose what you want to be notified about.\nYou can change this anytime in Settings.");
        sub.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 13px;");
        sub.setWrapText(true);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        record Toggle(String label, boolean[] flag) {}
        List<Toggle> toggles = List.of(
            new Toggle("Friend Requests",  new boolean[]{LauncherEngine.notifFriendRequests}),
            new Toggle("Friends Online",   new boolean[]{LauncherEngine.notifFriendOnline}),
            new Toggle("Server Updates",   new boolean[]{LauncherEngine.notifServerUpdates}),
            new Toggle("Streak Reminders", new boolean[]{LauncherEngine.notifStreakReminder}),
            new Toggle("System Messages",  new boolean[]{LauncherEngine.notifSystem})
        );

        VBox toggleBox = new VBox(8);
        toggleBox.setMaxWidth(360);
        for (Toggle t : toggles) {
            Label lbl = new Label(t.label());
            lbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

            CheckBox cb = new CheckBox();
            cb.getStyleClass().add("settings-checkbox");
            cb.setSelected(t.flag()[0]);
            cb.selectedProperty().addListener((obs, old, val) -> {
                t.flag()[0] = val;
                switch (t.label()) {
                    case "Friend Requests"  -> LauncherEngine.notifFriendRequests  = val;
                    case "Friends Online"   -> LauncherEngine.notifFriendOnline    = val;
                    case "Server Updates"   -> LauncherEngine.notifServerUpdates   = val;
                    case "Streak Reminders" -> LauncherEngine.notifStreakReminder   = val;
                    case "System Messages"  -> LauncherEngine.notifSystem          = val;
                }
                LauncherEngine.saveSettings();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(lbl, spacer, cb);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 20, 10, 16));
            row.setStyle(
                "-fx-background-color: #1a1d24; -fx-background-radius: 8;" +
                "-fx-border-color: #2a2e39; -fx-border-radius: 8; -fx-border-width: 1;"
            );
            toggleBox.getChildren().add(row);
        }

        Button nextBtn = primaryBtn("Continue →");
        nextBtn.setOnAction(e -> next.run());

        VBox box = new VBox(14, headline, sub, toggleBox, nextBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 40, 40, 40));
        return box;
    }

    // ── STEP 4: FINISH ───────────────────────────────────────────────────────

    private static VBox buildFinish(Runnable onComplete) {
        Label emoji = new Label("🎮");
        emoji.setStyle("-fx-font-size: 48px;");

        Label headline = new Label("You're all set!");
        headline.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label sub = new Label("Browse servers, track your playtime, and level up.\nGood luck out there.");
        sub.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 14px;");
        sub.setWrapText(true);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button btn = primaryBtn("Start Exploring →");
        btn.setOnAction(e -> onComplete.run());

        VBox box = new VBox(20, emoji, headline, sub, btn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 40, 40, 40));
        return box;
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private static HBox buildDots(int current) {
        int total = 4;
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);
        for (int i = 0; i < total; i++) {
            Circle dot = new Circle(i == current ? 5 : 3.5);
            dot.setFill(i == current ? Color.web("#ff981f") : Color.web("#2a2e39"));
            row.getChildren().add(dot);
        }
        return row;
    }

    private static Button primaryBtn(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color: #ff981f; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-padding: 12 32; -fx-cursor: hand;";
        String hover = "-fx-background-color: #e8871a; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-padding: 12 32; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private static String tagStyle(boolean selected) {
        return selected
            ? "-fx-background-color: #ff981f22; -fx-text-fill: #ff981f; -fx-border-color: #ff981f;" +
              "-fx-border-radius: 20; -fx-background-radius: 20; -fx-border-width: 1.5;" +
              "-fx-padding: 6 16; -fx-cursor: hand; -fx-font-size: 13px;"
            : "-fx-background-color: #1a1d24; -fx-text-fill: #8b92a5; -fx-border-color: #2a2e39;" +
              "-fx-border-radius: 20; -fx-background-radius: 20; -fx-border-width: 1;" +
              "-fx-padding: 6 16; -fx-cursor: hand; -fx-font-size: 13px;";
    }

    private static FadeTransition fade(javafx.scene.Node node, double from, double to, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(from);
        ft.setToValue(to);
        return ft;
    }

    private static TranslateTransition slide(javafx.scene.Node node, double from, double to, int ms) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        tt.setFromX(from);
        tt.setToX(to);
        return tt;
    }
}
