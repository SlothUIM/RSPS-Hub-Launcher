import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class RegisterScreen {

    public static Scene create(Stage stage, Runnable onRegisterSuccess, Runnable onShowLogin) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("auth-root");
        root.setTop(TitleBar.create(stage));

        VBox centered = new VBox();
        centered.setAlignment(Pos.CENTER);

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER_LEFT);

        // Branding
        Label brand = new Label("RSPS HUB");
        brand.getStyleClass().add("auth-brand");

        Label subtitle = new Label("Create your account");
        subtitle.getStyleClass().add("auth-subtitle");

        Separator sep = new Separator();
        sep.getStyleClass().add("auth-sep");

        // Fields
        VBox userGroup = fieldGroup("USERNAME", false);
        TextField usernameField = (TextField) userGroup.getChildren().get(1);
        usernameField.setPromptText("Choose a username");

        VBox emailGroup = fieldGroup("EMAIL", false);
        TextField emailField = (TextField) emailGroup.getChildren().get(1);
        emailField.setPromptText("Enter your email");

        VBox passGroup = fieldGroup("PASSWORD", true);
        PasswordField passwordField = (PasswordField) passGroup.getChildren().get(1);
        passwordField.setPromptText("Choose a password");

        VBox confirmGroup = fieldGroup("CONFIRM PASSWORD", true);
        PasswordField confirmField = (PasswordField) confirmGroup.getChildren().get(1);
        confirmField.setPromptText("Confirm your password");

        // Error
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        // Register button
        Button registerBtn = new Button("CREATE ACCOUNT");
        registerBtn.getStyleClass().add("auth-btn");
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        registerBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                showError(errorLabel, "Please fill in all fields.");
            } else if (!password.equals(confirm)) {
                showError(errorLabel, "Passwords do not match.");
            } else {
                onRegisterSuccess.run();
            }
        });

        confirmField.setOnAction(e -> registerBtn.fire());

        // Login link
        HBox loginRow = new HBox(5);
        loginRow.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Already have an account?");
        hasAccount.getStyleClass().add("auth-muted");
        Label loginLink = new Label("Sign in");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnMouseClicked(e -> onShowLogin.run());
        loginRow.getChildren().addAll(hasAccount, loginLink);

        card.getChildren().addAll(brand, subtitle, sep, userGroup, emailGroup, passGroup, confirmGroup, errorLabel, registerBtn, loginRow);
        centered.getChildren().add(card);
        root.setCenter(centered);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(RegisterScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
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
