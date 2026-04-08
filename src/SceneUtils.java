import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class SceneUtils {

    private static final double ARC = 10;

    /**
     * Wraps the root in a StackPane, applies a rounded clip to it, and makes
     * the scene transparent. Using a StackPane wrapper ensures the clip covers
     * all four corners reliably regardless of what child nodes are present.
     */
    public static void applyRoundedCorners(Scene scene, Pane root, Stage stage) {
        scene.setFill(Color.TRANSPARENT);

        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color: transparent;");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(ARC * 2);
        clip.setArcHeight(ARC * 2);
        clip.widthProperty().bind(wrapper.widthProperty());
        clip.heightProperty().bind(wrapper.heightProperty());
        wrapper.setClip(clip);

        scene.setRoot(wrapper);

        if (stage != null) {
            stage.maximizedProperty().addListener((obs, old, maximized) -> {
                double arc = maximized ? 0 : ARC * 2;
                clip.setArcWidth(arc);
                clip.setArcHeight(arc);
            });
        }
    }

    /** Overload without stage — corners stay rounded always (use for dialogs). */
    public static void applyRoundedCorners(Scene scene, Pane root) {
        applyRoundedCorners(scene, root, null);
    }
}
