import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.function.Consumer;
import java.io.*;
import java.nio.file.*;

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

        // --- PASSWORD WITH UNHIDER ---
        VBox passGroup = new VBox(6);
        Label passLbl = new Label("PASSWORD");
        passLbl.getStyleClass().add("auth-label");

        StackPane passStack = new StackPane();
        
        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("auth-field");
        passwordField.setPromptText("Enter your password");

        TextField visiblePasswordField = new TextField();
        visiblePasswordField.getStyleClass().add("auth-field");
        visiblePasswordField.setPromptText("Enter your password");
        visiblePasswordField.setVisible(false);

        // This magically keeps both fields synced up!
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        Button togglePassBtn = new Button("SHOW");
        togglePassBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b92a5; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 0 10 0 0;");
        StackPane.setAlignment(togglePassBtn, Pos.CENTER_RIGHT);

        togglePassBtn.setOnAction(e -> {
            boolean isHidden = passwordField.isVisible();
            passwordField.setVisible(!isHidden);
            visiblePasswordField.setVisible(isHidden);
            togglePassBtn.setText(isHidden ? "HIDE" : "SHOW");
        });

        passStack.getChildren().addAll(passwordField, visiblePasswordField, togglePassBtn);
        passGroup.getChildren().addAll(passLbl, passStack);

        // --- OPTIONS ROW (Remember Me & Forgot Password) ---
        BorderPane optionsRow = new BorderPane();
        
        CheckBox rememberMe = new CheckBox("Remember me");
        rememberMe.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 12px;");
        rememberMe.setSelected(true);
        
        Label forgotPassword = new Label("Forgot password?");
        forgotPassword.getStyleClass().add("auth-link");
        forgotPassword.setStyle("-fx-font-size: 12px;");
        forgotPassword.setOnMouseClicked(e -> {
            // TODO: Route this to a forgot password screen or open a web browser link!
            System.out.println("Forgot password clicked!"); 
        });

        optionsRow.setLeft(rememberMe);
        optionsRow.setRight(forgotPassword);

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
            // Because they are bound bidirectionally, passwordField always has the correct text
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            
            if (username.isEmpty() || password.isEmpty()) {
                showError(errorLabel, "Please fill in all fields.");
            } else {
                loginBtn.setText("LOGGING IN...");
                loginBtn.setDisable(true);
                errorLabel.setVisible(false);

                // Safe JSON Builder
                com.google.gson.JsonObject jsonNode = new com.google.gson.JsonObject();
                jsonNode.addProperty("username", username);
                jsonNode.addProperty("password", password);
                String payload = jsonNode.toString();

                ApiClient.postJson("login.php", payload).thenAccept(response -> {
                    javafx.application.Platform.runLater(() -> {
                        loginBtn.setText("LOGIN");
                        loginBtn.setDisable(false);
                        
                        if (response != null && response.contains("\"token\"")) {
                            try {
                                com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(response, com.google.gson.JsonObject.class);
                                String token = obj.get("token").getAsString();
                                LauncherEngine.sessionToken = token;
                                if (rememberMe.isSelected()) {
                                    saveSession(username, token);
                                }
                            } catch (Exception ignored) {}
                            onLoginSuccess.accept(username);
                        } else if (response != null && response.contains("\"error\"")) {
                            try {
                                com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(response, com.google.gson.JsonObject.class);
                                showError(errorLabel, obj.get("error").getAsString());
                            } catch (Exception e2) {
                                showError(errorLabel, "Invalid username or password.");
                            }
                        } else {
                            showError(errorLabel, "Could not connect to server.");
                        }
                    });
                }).exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        loginBtn.setText("LOGIN");
                        loginBtn.setDisable(false);
                        showError(errorLabel, "Could not connect to server.");
                    });
                    return null;
                });
            }
        });

        // Allow Enter key to submit on both visible and hidden fields
        passwordField.setOnAction(e -> loginBtn.fire());
        visiblePasswordField.setOnAction(e -> loginBtn.fire());
        usernameField.setOnAction(e -> {
            if (passwordField.isVisible()) passwordField.requestFocus();
            else visiblePasswordField.requestFocus();
        });

        // Register link
        HBox registerRow = new HBox(5);
        registerRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Don't have an account?");
        noAccount.getStyleClass().add("auth-muted");
        Label registerLink = new Label("Register here");
        registerLink.getStyleClass().add("auth-link");
        registerLink.setOnMouseClicked(e -> onShowRegister.run());
        registerRow.getChildren().addAll(noAccount, registerLink);

        card.getChildren().addAll(brand, subtitle, sep, userGroup, passGroup, optionsRow, errorLabel, loginBtn, registerRow);
        centered.getChildren().add(card);
        root.setCenter(centered);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(LoginScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
        return scene;
    }

    private static final Path SESSION_FILE = Paths.get(System.getProperty("user.home"), ".rsps_hub", "session.dat");

    public static void saveSession(String username, String token) {
        try {
            Files.createDirectories(SESSION_FILE.getParent());
            Files.writeString(SESSION_FILE, username + "\n" + token);
        } catch (Exception ignored) {}
    }

    public static String[] loadSession() {
        try {
            if (Files.exists(SESSION_FILE)) {
                String[] parts = Files.readString(SESSION_FILE).split("\n");
                if (parts.length == 2) return parts;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void clearSession() {
        try { Files.deleteIfExists(SESSION_FILE); } catch (Exception ignored) {}
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