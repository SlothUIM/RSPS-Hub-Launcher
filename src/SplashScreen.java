import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashScreen {

    public static void show(Stage stage, Runnable onFinished) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("splash-root");

        Label logo = new Label("RSPS HUB");
        logo.getStyleClass().add("splash-logo");
        logo.setOpacity(0);

        Label tagline = new Label("Your gateway to every RSPS");
        tagline.getStyleClass().add("splash-tagline");
        tagline.setOpacity(0);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("splash-progress");
        progressBar.setPrefWidth(360);
        progressBar.setMaxWidth(360);
        progressBar.setOpacity(0);

        root.getChildren().addAll(logo, tagline, progressBar);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(SplashScreen.class.getResource("style.css").toExternalForm());
        SceneUtils.applyRoundedCorners(scene, root);
        stage.setScene(scene);

        // Fade in logo + tagline together
        FadeTransition logoFade = new FadeTransition(Duration.millis(700), logo);
        logoFade.setFromValue(0); logoFade.setToValue(1);

        FadeTransition tagFade = new FadeTransition(Duration.millis(700), tagline);
        tagFade.setFromValue(0); tagFade.setToValue(1);

        FadeTransition barFade = new FadeTransition(Duration.millis(400), progressBar);
        barFade.setFromValue(0); barFade.setToValue(1);

        ParallelTransition fadeIn = new ParallelTransition(logoFade, tagFade);

        // Progress fills over 3.5s
        Timeline progress = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(progressBar.progressProperty(), 0)),
            new KeyFrame(Duration.millis(3500), new KeyValue(progressBar.progressProperty(), 1))
        );

        // Fade out everything before switching
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(
            fadeIn,
            new PauseTransition(Duration.millis(200)),
            barFade,
            progress,
            new PauseTransition(Duration.millis(400)),
            fadeOut
        );

        seq.setOnFinished(e -> Platform.runLater(onFinished));
        seq.play();
    }
}
