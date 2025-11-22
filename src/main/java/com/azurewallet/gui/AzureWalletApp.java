package com.azurewallet.gui;

import javafx.application.Application;
import javafx.stage.Stage;
import com.azurewallet.system.FileManager;
import com.azurewallet.models.UserAccount;
import com.azurewallet.main.BackgroundScheduler;
import java.util.Map;

public class AzureWalletApp extends Application {
    private FileManager fileManager;
    private Map<String, UserAccount> users;
    private BackgroundScheduler scheduler;
    public Stage primaryStage;
    public static boolean isDarkMode = true;
    private UserAccount currentUser;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        fileManager = new FileManager();
        users = fileManager.loadUsers();
        scheduler = new BackgroundScheduler(fileManager, users);
        scheduler.runScheduler();

        LoginViewController loginView = new LoginViewController(this);
        primaryStage.setScene(loginView.getScene());
        primaryStage.setTitle("Azure Digital Wallet");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(750);
        primaryStage.setOnCloseRequest(e -> {
            fileManager.saveUsers(users);
            System.exit(0);
        });
        primaryStage.show();
    }

    public void switchToDashboard(UserAccount account) {
        this.currentUser = account;
        DashboardController dashboard = new DashboardController(this, account);
        primaryStage.setScene(dashboard.getScene());
    }

    public void switchToLogin() {
        this.currentUser = null;
        LoginViewController loginView = new LoginViewController(this);
        primaryStage.setScene(loginView.getScene());
    }

    public void toggleTheme() {
        isDarkMode = !isDarkMode;
    }

    public FileManager getFileManager() { return fileManager; }
    public Map<String, UserAccount> getUsers() { return users; }
    public UserAccount getCurrentUser() { return currentUser; }
    public BackgroundScheduler getScheduler() { return scheduler; }

    public static String getPrimaryColor() {
        return isDarkMode ? "#1e1e1e" : "#ffffff";
    }

    public static String getSecondaryColor() {
        return isDarkMode ? "#2d2d2d" : "#f5f5f5";
    }

    public static String getTextColor() {
        return isDarkMode ? "#ffffff" : "#000000";
    }

    public static String getAccentColor() {
        return "#2196F3";
    }

    public static String getDangerColor() {
        return "#f44336";
    }

    public static String getSuccessColor() {
        return "#4CAF50";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
