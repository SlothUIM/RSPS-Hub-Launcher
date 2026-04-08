import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.function.Consumer;

/** Fully dark-themed replacement for Alert / TextInputDialog. */
public class DarkDialog {

    /** Single-line text input. onConfirm receives the entered text. */
    public static void showInput(Stage owner, String prompt, String defaultValue, Consumer<String> onConfirm) {
        Stage dialog = build(owner, 420, 230);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(TitleBar.create(dialog));

        VBox content = new VBox(12);
        content.setPadding(new Insets(28, 28, 24, 28));

        Label promptLabel = new Label(prompt);
        promptLabel.getStyleClass().add("dialog-prompt");

        TextField field = new TextField(defaultValue != null ? defaultValue : "");
        field.getStyleClass().add("auth-field");
        field.setMaxWidth(Double.MAX_VALUE);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(6, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("settings-secondary-btn");
        cancelBtn.setPrefWidth(90);
        cancelBtn.setOnAction(e -> dialog.close());

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("auth-btn");
        okBtn.setPrefWidth(90);
        okBtn.setOnAction(e -> { dialog.close(); onConfirm.accept(field.getText()); });

        field.setOnAction(e -> okBtn.fire());
        buttons.getChildren().addAll(cancelBtn, okBtn);
        content.getChildren().addAll(promptLabel, field, buttons);
        root.setCenter(content);

        show(dialog, root);
    }

    /** Simple message with an OK button. */
    public static void showAlert(Stage owner, String message) {
        Stage dialog = build(owner, 380, 200);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(TitleBar.create(dialog));

        VBox content = new VBox(18);
        content.setPadding(new Insets(28, 28, 24, 28));
        content.setAlignment(Pos.CENTER);

        Label msg = new Label(message);
        msg.getStyleClass().add("dialog-prompt");
        msg.setWrapText(true);

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("auth-btn");
        okBtn.setPrefWidth(100);
        okBtn.setOnAction(e -> dialog.close());

        content.getChildren().addAll(msg, okBtn);
        root.setCenter(content);

        show(dialog, root);
    }

    // ── internals ────────────────────────────────────────────────────────────

    private static Stage build(Stage owner, double w, double h) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL);
        d.initOwner(owner);
        d.initStyle(StageStyle.TRANSPARENT);
        d.setWidth(w);
        d.setHeight(h);
        return d;
    }

    private static void show(Stage dialog, BorderPane root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(DarkDialog.class));
        SceneUtils.applyRoundedCorners(scene, root);
        dialog.setScene(scene);
        dialog.show();
    }
}
