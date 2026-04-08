import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class LoginScreen {

    public static Scene create(Stage stage, Consumer<String> onLoginSuccess, Runnable onShowRegister) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("auth-root");
        root.setTop(TitleBar.create(stage));

        VBox centered = new VBox();
        centered.setAlignment(Pos.CENTER);

        VBox card = new VBox(16);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER_LEFT);

        // Branding
        Label brand = new Label("RSPS HUB");
        brand.getStyleClass().add("auth-brand");

        Label subtitle = new Label("Sign in to your account");
        subtitle.getStyleClass().add("auth-subtitle");

        Separator sep = new Separator();
        sep.getStyleClass().add("auth-sep");

        // Username
        VBox userGroup = fieldGroup("USERNAME", false);
        TextField usernameField = (TextField) userGroup.getChildren().get(1);
        usernameField.setPromptText("Enter your username");

        // Password
        VBox passGroup = fieldGroup("PASSWORD", true);
        PasswordField passwordField = (PasswordField) passGroup.getChildren().get(1);
        passwordField.setPromptText("Enter your password");

        // Error
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        // Login button
        Button loginBtn = new Button("LOGIN");
        loginBtn.getStyleClass().add("auth-btn");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                showError(errorLabel, "Please fill in all fields.");
            } else {
                onLoginSuccess.accept(username);
            }
        });

        // Allow Enter key to submit
        passwordField.setOnAction(e -> loginBtn.fire());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Register link
        HBox registerRow = new HBox(5);
        registerRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Don't have an account?");
        noAccount.getStyleClass().add("auth-muted");
        Label registerLink = new Label("Register here");
        registerLink.getStyleClass().add("auth-link");
        registerLink.setOnMouseClicked(e -> onShowRegister.run());
        registerRow.getChildren().addAll(noAccount, registerLink);

        card.getChildren().addAll(brand, subtitle, sep, userGroup, passGroup, errorLabel, loginBtn, registerRow);
        centered.getChildren().add(card);
        root.setCenter(centered);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(LoginScreen.class.getResource("style.css").toExternalForm());
        SceneUtils.applyRoundedCorners(scene, root);
        return scene;
    }

    private static VBox fieldGroup(String labelText, boolean isPassword) {
        VBox group = new VBox(6);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("auth-label");
        Control field = isPassword ? new PasswordField() : new TextField();
        field.getStyleClass().add("auth-field");
        group.getChildren().addAll(lbl, field);
        return group;
    }

    private static void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
