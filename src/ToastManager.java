import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ToastManager {

    private static final String BG      = "#1a1d24";
    private static final String BORDER  = "#2a2e39";
    private static final String ACCENT  = "#ff981f";
    private static final String TEXT    = "#ffffff";
    private static final String SUBTEXT = "#8b92a5";

    /** Show a toast anchored to the bottom-right corner of the given stage. */
    public static void show(Stage stage, String title, String message) {
        if (!LauncherEngine.friendActivityNotifications) return;

        Popup popup = new Popup();
        popup.setAutoHide(false);

        VBox box = new VBox(5);
        box.setStyle(
            "-fx-background-color: " + BG + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);"
        );
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setMaxWidth(280);
        box.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        titleLbl.setWrapText(true);

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill: " + SUBTEXT + "; -fx-font-size: 12px;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(244);

        box.getChildren().addAll(titleLbl, msgLbl);
        popup.getContent().add(box);

        // Position bottom-right of stage
        popup.setOnShown(e -> {
            popup.setX(stage.getX() + stage.getWidth()  - 310);
            popup.setY(stage.getY() + stage.getHeight() - 110);
        });

        popup.show(stage);

        FadeTransition fade = new FadeTransition(Duration.millis(500), box);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> popup.hide());

        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> fade.play());
        pause.play();
    }
}
