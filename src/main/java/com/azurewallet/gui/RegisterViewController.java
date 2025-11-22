package com.azurewallet.gui;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import com.azurewallet.models.UserAccount;
import java.util.Map;

public class RegisterViewController {
    private Scene scene;
    private AzureWalletApp app;
    private Map<String, UserAccount> users;

    public RegisterViewController(AzureWalletApp app) {
        this.app = app;
        this.users = app.getUsers();
        this.scene = createRegisterScene();
    }

    private Scene createRegisterScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + AzureWalletApp.getPrimaryColor() + ";");

        root.setTop(createHeader());
        root.setCenter(createRegisterPanel());

        return new Scene(root, 1000, 750);
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: " + AzureWalletApp.getSecondaryColor() + "; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");

        Label title = new Label("🔷 CREATE NEW ACCOUNT");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getAccentColor() + ";");
        header.getChildren().add(title);

        return header;
    }

    private VBox createRegisterPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(40));

        Label titleLabel = new Label("Register Your Account");
        titleLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(30));
        formBox.setStyle("-fx-background-color: " + AzureWalletApp.getSecondaryColor() + "; -fx-border-radius: 10;");
        formBox.setMaxWidth(400);

        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-text-fill: " + AzureWalletApp.getTextColor() + "; -fx-font-size: 12; -fx-font-weight: bold;");
        TextField userField = new TextField();
        userField.setPromptText("Choose a username");
        userField.setStyle(getTextFieldStyle());

        Label mobileLabel = new Label("Mobile Number (09xxxxxxxxx)");
        mobileLabel.setStyle("-fx-text-fill: " + AzureWalletApp.getTextColor() + "; -fx-font-size: 12; -fx-font-weight: bold;");
        TextField mobileField = new TextField();
        mobileField.setPromptText("09123456789");
        mobileField.setStyle(getTextFieldStyle());

        Label pinLabel = new Label("4-Digit PIN");
        pinLabel.setStyle("-fx-text-fill: " + AzureWalletApp.getTextColor() + "; -fx-font-size: 12; -fx-font-weight: bold;");
        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Enter 4 digits");
        pinField.setStyle(getTextFieldStyle());

        Button registerBtn = new Button("CREATE ACCOUNT");
        registerBtn.setPrefWidth(Double.MAX_VALUE);
        registerBtn.setStyle("-fx-padding: 12; -fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: " + AzureWalletApp.getSuccessColor() + "; -fx-text-fill: white; -fx-cursor: hand;");
        registerBtn.setOnAction(e -> handleRegister(userField.getText(), mobileField.getText(), pinField.getText()));

        Button backBtn = new Button("← Back to Login");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + AzureWalletApp.getAccentColor() + "; -fx-font-size: 12; -fx-cursor: hand;");
        backBtn.setOnAction(e -> {
            LoginViewController login = new LoginViewController(app);
            app.primaryStage.setScene(login.getScene());
        });

        formBox.getChildren().addAll(userLabel, userField, mobileLabel, mobileField, pinLabel, pinField, registerBtn, backBtn);
        panel.getChildren().addAll(titleLabel, formBox);

        return panel;
    }

    private void handleRegister(String username, String mobile, String pin) {
        username = username.trim().toLowerCase();

        if (username.isEmpty() || mobile.isEmpty() || pin.isEmpty()) {
            showAlert("Error", "Please fill all fields", Alert.AlertType.WARNING);
            return;
        }

        if (users.containsKey(username)) {
            showAlert("Error", "Username already exists", Alert.AlertType.ERROR);
            return;
        }

        if (!mobile.matches("^09\\d{9}$")) {
            showAlert("Error", "Invalid mobile number format (09xxxxxxxxx)", Alert.AlertType.ERROR);
            return;
        }

        if (!pin.matches("\\d{4}")) {
            showAlert("Error", "PIN must be 4 digits", Alert.AlertType.ERROR);
            return;
        }

        for (UserAccount u : users.values()) {
            if (u.getMobile().equals(mobile)) {
                showAlert("Error", "This mobile number is already registered", Alert.AlertType.ERROR);
                return;
            }
        }

        UserAccount newUser = new UserAccount(username, pin, mobile);
        users.put(username, newUser);
        app.getFileManager().saveUsers(users);

        showAlert("Success", "Account created! Please login.", Alert.AlertType.INFORMATION);
        LoginViewController login = new LoginViewController(app);
        app.primaryStage.setScene(login.getScene());
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
