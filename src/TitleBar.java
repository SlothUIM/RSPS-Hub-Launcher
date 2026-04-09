import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class TitleBar {

    public static HBox create(Stage stage) {
        HBox bar = new HBox();
        bar.getStyleClass().add("custom-title-bar");

        // StackPane fills all space: label centered, buttons pinned right
        StackPane content = new StackPane();
        HBox.setHgrow(content, Priority.ALWAYS);

        Label appName = new Label("RSPS HUB");
        appName.getStyleClass().add("title-bar-app-name");
        StackPane.setAlignment(appName, Pos.CENTER);

        Button minimizeBtn = titleBtn("—");
        Button maximizeBtn = titleBtn("⬜");
        Button closeBtn    = titleBtn("✕");
        closeBtn.getStyleClass().add("title-bar-close");

        minimizeBtn.setOnAction(e -> stage.setIconified(true));
        maximizeBtn.setOnAction(e -> {
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                // Use visual bounds so the taskbar stays visible
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
                stage.setMaximized(true);
            }
        });
        closeBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(0, minimizeBtn, maximizeBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setFillHeight(true);
        StackPane.setAlignment(buttons, Pos.CENTER_RIGHT);

        content.getChildren().addAll(appName, buttons);
        bar.getChildren().add(content);

        // Make the bar drag the window
        final double[] dragDelta = new double[2];
        bar.setOnMousePressed(e -> {
            dragDelta[0] = e.getSceneX();
            dragDelta[1] = e.getSceneY();
        });
        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragDelta[0]);
            stage.setY(e.getScreenY() - dragDelta[1]);
        });

        return bar;
    }

    private static Button titleBtn(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("title-bar-btn");
        return btn;
    }
}
