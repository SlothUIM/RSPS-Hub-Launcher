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

        Label brand    = new Label("RSPS HUB");
        brand.getStyleClass().add("auth-brand");
        Label subtitle = new Label("Create your account");
        subtitle.getStyleClass().add("auth-subtitle");
        Separator sep  = new Separator();
        sep.getStyleClass().add("auth-sep");

        VBox userGroup = fieldGroup("USERNAME", false);
        TextField usernameField = (TextField) userGroup.getChildren().get(1);
        usernameField.setPromptText("Choose a username");

        VBox emailGroup = fieldGroup("EMAIL", false);
        TextField emailField = (TextField) emailGroup.getChildren().get(1);
        emailField.setPromptText("Enter your email");

        VBox passGroup = fieldGroup("PASSWORD", true);
        PasswordField passwordField = (PasswordField) passGroup.getChildren().get(1);
        passwordField.setPromptText("Choose a password");

        // -- strength meter --
        VBox strengthBox = buildStrengthMeter(passwordField);

        VBox confirmGroup = fieldGroup("CONFIRM PASSWORD", true);
        PasswordField confirmField = (PasswordField) confirmGroup.getChildren().get(1);
        confirmField.setPromptText("Confirm your password");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Button registerBtn = new Button("CREATE ACCOUNT");
        registerBtn.getStyleClass().add("auth-btn");
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        registerBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String email    = emailField.getText().trim();
            String password = passwordField.getText();
            String confirm  = confirmField.getText();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                showError(errorLabel, "Please fill in all fields.");
            } else if (password.length() < 8) {
                showError(errorLabel, "Password must be at least 8 characters.");
            } else if (!password.matches(".*[A-Z].*")) {
                showError(errorLabel, "Password must contain at least one uppercase letter.");
            } else if (!password.matches(".*[0-9].*")) {
                showError(errorLabel, "Password must contain at least one number.");
            } else if (password.matches("[a-zA-Z0-9]*")) {
                showError(errorLabel, "Password must contain at least one special character.");
            } else if (!password.equals(confirm)) {
                showError(errorLabel, "Passwords do not match.");
            } else {
                registerBtn.setText("CREATING ACCOUNT...");
                registerBtn.setDisable(true);
                errorLabel.setVisible(false);

<<<<<<< HEAD
                com.google.gson.JsonObject jsonNode = new com.google.gson.JsonObject();
                jsonNode.addProperty("username", username);
                jsonNode.addProperty("email", email);
                jsonNode.addProperty("password", password);
                String payload = jsonNode.toString();
=======
                String payload = String.format(
                    "{\"username\":\"%s\", \"email\":\"%s\", \"password\":\"%s\"}",
                    username, email, password
                );
>>>>>>> branch 'main' of https://github.com/SlothUIM/RSPS-Hub-Launcher.git

                ApiClient.postJson("register.php", payload).thenAccept(response -> {
                    javafx.application.Platform.runLater(() -> {
                        registerBtn.setText("CREATE ACCOUNT");
                        registerBtn.setDisable(false);
                        if (response != null && response.contains("\"success\"")) {
                            onRegisterSuccess.run();
                        } else if (response != null && response.contains("\"error\"")) {
                            try {
                                com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(response, com.google.gson.JsonObject.class);
                                showError(errorLabel, obj.get("error").getAsString());
                            } catch (Exception e2) {
                                showError(errorLabel, "Registration failed.");
                            }
                        } else {
                            showError(errorLabel, "Could not connect to server.");
                        }
                    });
                }).exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        registerBtn.setText("CREATE ACCOUNT");
                        registerBtn.setDisable(false);
                        showError(errorLabel, "Could not connect to server.");
                    });
                    return null;
                });
            }
        });

        confirmField.setOnAction(e -> registerBtn.fire());

        HBox loginRow = new HBox(5);
        loginRow.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Already have an account?");
        hasAccount.getStyleClass().add("auth-muted");
        Label loginLink = new Label("Sign in");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnMouseClicked(e -> onShowLogin.run());
        loginRow.getChildren().addAll(hasAccount, loginLink);

        card.getChildren().addAll(
            brand, subtitle, sep,
            userGroup, emailGroup,
            passGroup, strengthBox,
            confirmGroup,
            errorLabel, registerBtn, loginRow
        );
        centered.getChildren().add(card);
        root.setCenter(centered);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(LauncherEngine.getStylesheets(RegisterScreen.class));
        SceneUtils.applyRoundedCorners(scene, root, stage);
        return scene;
    }

    private static VBox buildStrengthMeter(PasswordField passwordField) {
        // Four segment bar
        HBox bar = new HBox(4);
        bar.setMaxWidth(Double.MAX_VALUE);
        Region[] segs = new Region[4];
        for (int i = 0; i < 4; i++) {
            segs[i] = new Region();
            segs[i].setPrefHeight(4);
            segs[i].setMinHeight(4);
            segs[i].setMaxHeight(4);
            HBox.setHgrow(segs[i], Priority.ALWAYS);
            segs[i].setStyle("-fx-background-color: #2a2e39; -fx-background-radius: 2;");
            bar.getChildren().add(segs[i]);
        }

        // Requirement chips
        Label reqLength  = reqChip("8+ chars");
        Label reqUpper   = reqChip("A–Z");
        Label reqNumber  = reqChip("0–9");
        Label reqSpecial = reqChip("!@#...");
        HBox chips = new HBox(6, reqLength, reqUpper, reqNumber, reqSpecial);

        Label strengthLabel = new Label("");
        strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555d6e;");

        VBox box = new VBox(6, bar, chips, strengthLabel);
        box.setPadding(new Insets(0, 0, 4, 0));

        passwordField.textProperty().addListener((obs, old, pw) -> {
            boolean hasLength  = pw.length() >= 8;
            boolean hasUpper   = pw.matches(".*[A-Z].*");
            boolean hasNumber  = pw.matches(".*[0-9].*");
            boolean hasSpecial = !pw.matches("[a-zA-Z0-9]*") && !pw.isEmpty();

            updateChip(reqLength,  hasLength);
            updateChip(reqUpper,   hasUpper);
            updateChip(reqNumber,  hasNumber);
            updateChip(reqSpecial, hasSpecial);

            int score = (hasLength ? 1 : 0) + (hasUpper ? 1 : 0) + (hasNumber ? 1 : 0) + (hasSpecial ? 1 : 0);

            String color;
            String label;
            if (pw.isEmpty()) {
                color = "#2a2e39"; label = "";
            } else if (score <= 1) {
                color = "#e05252"; label = "Weak";
            } else if (score == 2) {
                color = "#e09020"; label = "Fair";
            } else if (score == 3) {
                color = "#4a9eff"; label = "Good";
            } else {
                color = "#4caf50"; label = "Strong";
            }

            for (int i = 0; i < 4; i++) {
                String c = (pw.isEmpty() || i >= score) ? "#2a2e39" : color;
                segs[i].setStyle("-fx-background-color: " + c + "; -fx-background-radius: 2;");
            }
            strengthLabel.setText(label);
            strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (pw.isEmpty() ? "#555d6e" : color) + ";");
        });

        return box;
    }

    private static Label reqChip(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-background-color: #2a2e39; -fx-text-fill: #555d6e;" +
            "-fx-background-radius: 10; -fx-padding: 2 8;" +
            "-fx-font-size: 10px;"
        );
        return lbl;
    }

    private static void updateChip(Label chip, boolean met) {
        if (met) {
            chip.setStyle(
                "-fx-background-color: #1e3a1e; -fx-text-fill: #4caf50;" +
                "-fx-background-radius: 10; -fx-padding: 2 8;" +
                "-fx-font-size: 10px;"
            );
        } else {
            chip.setStyle(
                "-fx-background-color: #2a2e39; -fx-text-fill: #555d6e;" +
                "-fx-background-radius: 10; -fx-padding: 2 8;" +
                "-fx-font-size: 10px;"
            );
        }
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
