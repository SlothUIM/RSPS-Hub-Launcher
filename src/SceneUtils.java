import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SceneUtils {

    private static final double ARC = 10;

    /** Makes the scene transparent and clips the root pane to rounded corners. */
    public static void applyRoundedCorners(Scene scene, Pane root) {
        scene.setFill(Color.TRANSPARENT);
        Rectangle clip = new Rectangle();
        clip.setArcWidth(ARC * 2);
        clip.setArcHeight(ARC * 2);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);
    }
}
