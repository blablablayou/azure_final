package com.azurewallet.gui;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import com.azurewallet.models.UserAccount;
import java.util.Map;

public class LoginViewController {
    private Scene scene;
    private AzureWalletApp app;
    private Map<String, UserAccount> users;

    public LoginViewController(AzureWalletApp app) {
        this.app = app;
        this.users = app.getUsers();
        this.scene = createLoginScene();
    }

    private Scene createLoginScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + AzureWalletApp.getPrimaryColor() + ";");

        root.setTop(createHeader());
        root.setCenter(createLoginPanel());

        return new Scene(root, 1000, 750);
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: " + AzureWalletApp.getSecondaryColor() + "; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.SPACE_BETWEEN);

        Label title = new Label("🔷 AZURE DIGITAL WALLET");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getAccentColor() + ";");

        Button themeBtn = new Button(AzureWalletApp.isDarkMode ? "☀️ Light" : "🌙 Dark");
        themeBtn.setStyle("-fx-padding: 8 15; -fx-font-size: 12; -fx-background-color: " + AzureWalletApp.getAccentColor() + "; -fx-text-fill: white; -fx-border-radius: 5;");
        themeBtn.setOnAction(e -> {
            app.toggleTheme();
            app.switchToLogin();
        });

        header.getChildren().addAll(title, themeBtn);
        return header;
    }

    private VBox createLoginPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40));

        Label welcomeLabel = new Label("Welcome Back");
        welcomeLabel.setStyle("-fx-font-size: 36; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        Label subLabel = new Label("Sign in to your account");
        subLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #888;");

        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(30));
        formBox.setStyle("-fx-background-color: " + AzureWalletApp.getSecondaryColor() + "; -fx-border-radius: 10;");
        formBox.setMaxWidth(350);

        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-text-fill: " + AzureWalletApp.getTextColor() + "; -fx-font-size: 12; -fx-font-weight: bold;");
        TextField userField = new TextField();
        userField.setPromptText("Enter username");
        userField.setStyle(getTextFieldStyle());

        Label pinLabel = new Label("PIN");
        pinLabel.setStyle("-fx-text-fill: " + AzureWalletApp.getTextColor() + "; -fx-font-size: 12; -fx-font-weight: bold;");
        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Enter 4-digit PIN");
        pinField.setStyle(getTextFieldStyle());

        Button loginBtn = new Button("LOGIN");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-padding: 12; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: " + AzureWalletApp.getAccentColor() + "; -fx-text-fill: white; -fx-cursor: hand;");
        loginBtn.setOnAction(e -> handleLogin(userField.getText(), pinField.getText()));

        Button registerBtn = new Button("Don't have an account? Register");
        registerBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + AzureWalletApp.getAccentColor() + "; -fx-font-size: 11; -fx-cursor: hand;");
        registerBtn.setOnAction(e -> {
            RegisterViewController register = new RegisterViewController(app);
            app.primaryStage.setScene(register.getScene());
        });

        formBox.getChildren().addAll(userLabel, userField, pinLabel, pinField, loginBtn, registerBtn);
        panel.getChildren().addAll(welcomeLabel, subLabel, formBox);

        return panel;
    }

    private void handleLogin(String username, String pin) {
        if (username.isEmpty() || pin.isEmpty()) {
            showAlert("Error", "Please fill all fields", Alert.AlertType.WARNING);
            return;
        }

        username = username.toLowerCase();
        if (!users.containsKey(username)) {
            showAlert("Error", "User not found", Alert.AlertType.ERROR);
            return;
        }

        UserAccount acc = users.get(username);
        if (acc.isLocked()) {
            long minsLeft = (acc.getLockEndTime() - System.currentTimeMillis()) / 60000;
            showAlert("Account Locked", "Try again in " + Math.max(minsLeft, 1) + " minute(s)", Alert.AlertType.ERROR);
            return;
        }

        if (!acc.verifyPin(pin)) {
            acc.registerFailedAttempt();
            app.getFileManager().saveUsers(users);
            showAlert("Error", "Incorrect PIN", Alert.AlertType.ERROR);
            return;
        }

        acc.resetLock();
        app.getFileManager().saveUsers(users);
        acc.viewVoucherNotification(app.getFileManager());
        app.switchToDashboard(acc);
    }

    private String getTextFieldStyle() {
        String bgColor = AzureWalletApp.isDarkMode ? "#3a3a3a" : "#e8e8e8";
        String textColor = AzureWalletApp.getTextColor();
        return "-fx-padding: 10; -fx-font-size: 13; -fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-border-color: " + AzureWalletApp.getAccentColor() + "; -fx-border-width: 0 0 2 0;";
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public Scene getScene() {
        return scene;
    }
}
