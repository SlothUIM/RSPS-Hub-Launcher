import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Adds mouse-drag resize support to an undecorated/transparent Stage.
 * Attach once per scene via RSPSHub's scene change listener.
 */
public class ResizeHelper {

    private static final int BORDER = 14;
    private static final int MIN_W  = 700;
    private static final int MIN_H  = 500;

    public static void addTo(Stage stage, Scene scene) {
        final double[] startX     = {0};
        final double[] startY     = {0};
        final double[] startW     = {0};
        final double[] startH     = {0};
        final double[] startStagX = {0};
        final double[] startStagY = {0};
        final Cursor[] dragCursor  = {Cursor.DEFAULT};

        // Use event filters (capture phase) so the scene intercepts events
        // before any child node (e.g. ScrollPane scrollbar) can consume them.

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (stage.isMaximized()) { scene.setCursor(Cursor.DEFAULT); return; }
            scene.setCursor(resizeCursor(e.getSceneX(), e.getSceneY(),
                    scene.getWidth(), scene.getHeight()));
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            dragCursor[0] = resizeCursor(e.getSceneX(), e.getSceneY(),
                    scene.getWidth(), scene.getHeight());
            if (dragCursor[0] == Cursor.DEFAULT) return;
            startX[0]     = e.getScreenX();
            startY[0]     = e.getScreenY();
            startW[0]     = stage.getWidth();
            startH[0]     = stage.getHeight();
            startStagX[0] = stage.getX();
            startStagY[0] = stage.getY();
            e.consume(); // prevent child from acting on this press
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            Cursor c = dragCursor[0];
            if (c == Cursor.DEFAULT) return;

            double dx = e.getScreenX() - startX[0];
            double dy = e.getScreenY() - startY[0];

            double newW = startW[0], newH = startH[0];
            double newX = startStagX[0], newY = startStagY[0];

            if (c == Cursor.E_RESIZE  || c == Cursor.NE_RESIZE || c == Cursor.SE_RESIZE)
                newW = Math.max(MIN_W, startW[0] + dx);

            if (c == Cursor.W_RESIZE  || c == Cursor.NW_RESIZE || c == Cursor.SW_RESIZE) {
                newW = Math.max(MIN_W, startW[0] - dx);
                newX = startStagX[0] + (startW[0] - newW);
            }
            if (c == Cursor.S_RESIZE  || c == Cursor.SE_RESIZE || c == Cursor.SW_RESIZE)
                newH = Math.max(MIN_H, startH[0] + dy);

            if (c == Cursor.N_RESIZE  || c == Cursor.NE_RESIZE || c == Cursor.NW_RESIZE) {
                newH = Math.max(MIN_H, startH[0] - dy);
                newY = startStagY[0] + (startH[0] - newH);
            }

            stage.setX(newX); stage.setY(newY);
            stage.setWidth(newW); stage.setHeight(newH);
            e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (dragCursor[0] != Cursor.DEFAULT) e.consume();
            dragCursor[0] = Cursor.DEFAULT;
        });
    }

    private static Cursor resizeCursor(double x, double y, double w, double h) {
        boolean left   = x < BORDER;
        boolean right  = x > w - BORDER;
        boolean top    = y < BORDER;
        boolean bottom = y > h - BORDER;

        if (left  && top)    return Cursor.NW_RESIZE;
        if (right && top)    return Cursor.NE_RESIZE;
        if (left  && bottom) return Cursor.SW_RESIZE;
        if (right && bottom) return Cursor.SE_RESIZE;
        if (left)            return Cursor.W_RESIZE;
        if (right)           return Cursor.E_RESIZE;
        if (top)             return Cursor.N_RESIZE;
        if (bottom)          return Cursor.S_RESIZE;
        return Cursor.DEFAULT;
    }
}
