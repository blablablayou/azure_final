package azurewallet.main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import azurewallet.models.UserAccount;
import azurewallet.system.AdminControl;
import java.util.List;
import java.util.Optional;

/**
 * Modern JavaFX UI for Azure Digital Wallet
 * Connects to existing backend: AzureDigitalApp, UserAccount, FileManager, VoucherSystem
 */
public class MainApp extends Application {
    
    private BorderPane root;
    private Stage primaryStage;
    private AzureDigitalApp azureApp;
    private AdminControl adminControl;
    private String currentUser;
    private boolean isAdminMode;
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize the backend (loads data, starts scheduler)
        this.azureApp = new AzureDigitalApp();
        this.adminControl = new AdminControl(azureApp.getFileManager(), azureApp.getUsers(), azureApp.getScheduler());
        
        root = new BorderPane();
        scene = new Scene(root, 1080, 1920);
        scene.setFill(Color.web("#FFFFFF"));
        java.net.URL css = getClass().getResource("/styles/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        
        // Show login screen
        showLoginScreen();
        
        primaryStage.setTitle("Azure Digital Wallet");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(e -> {
            // Ensure data is saved on close
            azureApp.shutdown();
        });
        primaryStage.show();
    }
    
    // ==================== LOGIN SCREEN ====================
    
    private void showLoginScreen() {
        VBox loginContainer = new VBox(30);
        loginContainer.setAlignment(Pos.CENTER);
        loginContainer.getStyleClass().add("login-container");
        
        // Logo and branding
        VBox logoBox = new VBox(10);
        logoBox.setAlignment(Pos.CENTER);
        
        Label logo = new Label("Azure");
        logo.setFont(Font.font("System", FontWeight.BOLD, 36));
        logo.setStyle("-fx-text-fill: #2563EB;");
        
        Label tagline = new Label("Digital Wallet");
        tagline.setFont(Font.font("System", 14));
        tagline.setStyle("-fx-text-fill: #8E9AAF;");
        
        logoBox.getChildren().addAll(logo, tagline);
        
        Region spacer1 = new Region();
        VBox.setVgrow(spacer1, Priority.ALWAYS);
        
        // Welcome text
        VBox welcomeBox = new VBox(8);
        welcomeBox.setAlignment(Pos.CENTER_LEFT);

        Label welcome = new Label("Welcome Back");
        welcome.setFont(Font.font("System", FontWeight.BOLD, 28));
        welcome.setStyle("-fx-text-fill: #000000;");

        Label subtitle = new Label("Sign in to continue");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setStyle("-fx-text-fill: #333333;");

        welcomeBox.getChildren().addAll(welcome, subtitle);
        
        // Username field
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("Username");
        usernameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        usernameLabel.setStyle("-fx-text-fill: #000000;");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        styleInputField(usernameField);
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        // PIN field
        VBox pinBox = new VBox(8);
        Label pinLabel = new Label("PIN (4 digits)");
        pinLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        pinLabel.setStyle("-fx-text-fill: #000000;");
        
        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Enter your 4-digit PIN");
        styleInputField(pinField);
        pinBox.getChildren().addAll(pinLabel, pinField);
        
        // Sign In button
        Button signInBtn = createPrimaryButton("Sign In");
        signInBtn.setOnAction(e -> handleLogin(usernameField, pinField));

        // Group the form fields so username->pin gap is exactly 8px
        VBox formBox = new VBox(8);
        formBox.getStyleClass().add("form-box");
        formBox.getChildren().addAll(usernameBox, pinBox, signInBtn);
        
        // Allow Enter key to submit
        pinField.setOnAction(e -> handleLogin(usernameField, pinField));
        
        Region spacer2 = new Region();
        VBox.setVgrow(spacer2, Priority.ALWAYS);
        
        // Sign up link and admin login
        HBox bottomRow = new HBox(20);
        bottomRow.setAlignment(Pos.CENTER);
        
        HBox signUpRow = new HBox(5);
        signUpRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Don't have an account?");
        noAccount.setStyle("-fx-text-fill: #333333; -fx-font-size: 13;");
        Hyperlink signUpLink = new Hyperlink("Sign Up");
        signUpLink.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 13; -fx-font-weight: bold;");
        signUpLink.setOnAction(e -> showRegistrationScreen());
        signUpRow.getChildren().addAll(noAccount, signUpLink);
        
        Button adminBtn = new Button("Admin Login");
        adminBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #333333; " +
            "-fx-font-size: 12; -fx-border-color: #E2E8F0; -fx-border-width: 1; " +
            "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;"
        );
        adminBtn.setOnAction(e -> showAdminLoginScreen());
        
        bottomRow.getChildren().addAll(signUpRow, adminBtn);
        
        loginContainer.getChildren().addAll(
            logoBox, spacer1, welcomeBox, formBox, spacer2, bottomRow
        );

        StackPane wrapper = new StackPane(loginContainer);
        wrapper.getStyleClass().add("login-wrapper");
        wrapper.setAlignment(Pos.CENTER);
        root.setCenter(wrapper);
    }
    
    private void handleLogin(TextField usernameField, PasswordField pinField) {
        String username = usernameField.getText().trim();
        String pin = pinField.getText().trim();
        
        if (username.isEmpty() || pin.isEmpty()) {
            showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
            return;
        }
        
        // Call backend login method
        if (azureApp.loginUser(username, pin)) {
            currentUser = username;
            showMainScreen();
        } else {
            showAlert("Login Failed", 
                "Invalid username or PIN.\nAccount locks after 3 failed attempts for 1 minute.", 
                Alert.AlertType.ERROR);
            pinField.clear();
        }
    }
    
    // ==================== REGISTRATION SCREEN ====================
    
    private void showRegistrationScreen() {
        VBox regContainer = new VBox(25);
        regContainer.setAlignment(Pos.CENTER);
        regContainer.setPadding(new Insets(60, 40, 60, 40));
        regContainer.setStyle("-fx-background-color: #FFFFFF;");
        
        // Header
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Create Account");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: #000000;");
        
        Label subtitle = new Label("Join Azure Digital Wallet");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setStyle("-fx-text-fill: #333333;");
        
        headerBox.getChildren().addAll(title, subtitle);
        
        // Username field
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("Username");
        usernameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        usernameLabel.setStyle("-fx-text-fill: #000000;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");
        styleInputField(usernameField);
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        // Mobile field
        VBox mobileBox = new VBox(8);
        Label mobileLabel = new Label("Mobile Number");
        mobileLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        mobileLabel.setStyle("-fx-text-fill: #000000;");
        TextField mobileField = new TextField();
        mobileField.setPromptText("09XXXXXXXXX (11 digits)");
        styleInputField(mobileField);
        mobileBox.getChildren().addAll(mobileLabel, mobileField);
        
        // PIN field
        VBox pinBox = new VBox(8);
        Label pinLabel = new Label("4-Digit PIN");
        pinLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        pinLabel.setStyle("-fx-text-fill: #000000;");
        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Create 4-digit PIN");
        styleInputField(pinField);
        pinBox.getChildren().addAll(pinLabel, pinField);
        
        // Register button
        Button registerBtn = createPrimaryButton("Create Account");
        registerBtn.setOnAction(e -> handleRegistration(usernameField, mobileField, pinField));
        
        // Back to login
        HBox backRow = new HBox(5);
        backRow.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Already have an account?");
        hasAccount.setStyle("-fx-text-fill: #333333; -fx-font-size: 13;");
        Hyperlink loginLink = new Hyperlink("Sign In");
        loginLink.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 13; -fx-font-weight: bold;");
        loginLink.setOnAction(e -> showLoginScreen());
        backRow.getChildren().addAll(hasAccount, loginLink);
        
        regContainer.getChildren().addAll(
            headerBox, usernameBox, mobileBox, pinBox, registerBtn, backRow
        );
        
        root.setCenter(regContainer);
    }
    
    private void handleRegistration(TextField usernameField, TextField mobileField, PasswordField pinField) {
        String username = usernameField.getText().trim();
        String mobile = mobileField.getText().trim();
        String pin = pinField.getText().trim();
        
        if (username.isEmpty() || mobile.isEmpty() || pin.isEmpty()) {
            showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
            return;
        }
        
        // Call backend registration method
        if (azureApp.registerUser(username, pin, mobile)) {
            showAlert("Success", 
                "Account created successfully!\nYou can now login with your credentials.", 
                Alert.AlertType.INFORMATION);
            showLoginScreen();
        } else {
            showAlert("Registration Failed", 
                "Username or mobile already exists, or invalid format.\n" +
                "PIN must be 4 digits. Mobile must start with 09 and be 11 digits.", 
                Alert.AlertType.ERROR);
        }
    }
    
    // ==================== MAIN DASHBOARD ====================
    
    private void showMainScreen() {
        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: #FFFFFF;");
        
        // Header with user info and logout
        HBox header = createHeader();
        
        // Scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 20, 100, 20));
        
        // Balance card with tier and amount
        VBox balanceCard = createBalanceCard();
        
        // Action buttons (Deposit, Withdraw, Transfer, Voucher)
        HBox actions = createActionButtons();
        
        // Account information section
        VBox accountInfo = createAccountInfoSection();
        
        content.getChildren().addAll(balanceCard, actions, accountInfo);
        scrollPane.setContent(content);
        
        // Bottom navigation bar
        HBox bottomNav = createBottomNav();
        
        mainContainer.getChildren().addAll(header, scrollPane, bottomNav);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        root.setCenter(mainContainer);
    }
    
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(50, 20, 15, 20));
        header.setStyle("-fx-background-color: #FFFFFF;");
        
        // User avatar
        Circle avatar = new Circle(20);
        avatar.setFill(Color.web("#2563EB"));
        
        // Get user from backend
        UserAccount user = azureApp.getUser(currentUser);
        Label title = new Label("  " + user.getUsername());
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #000000;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Account dropdown (profile, settings, logout)
        MenuItem profileItem = new MenuItem("Profile (placeholder)");
        profileItem.setOnAction(e -> showAlert("Info", "Profile action (placeholder)", Alert.AlertType.INFORMATION));
        MenuItem settingsItem = new MenuItem("Settings (placeholder)");
        settingsItem.setOnAction(e -> showAlert("Info", "Settings action (placeholder)", Alert.AlertType.INFORMATION));
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            currentUser = null;
            showLoginScreen();
        });

        MenuButton accountMenu = new MenuButton();
        accountMenu.getItems().addAll(profileItem, settingsItem, logoutItem);
        accountMenu.setText("Account ▾");
        accountMenu.setStyle(
            "-fx-background-color: white; -fx-text-fill: #2563EB; " +
            "-fx-font-size: 12; -fx-font-weight: bold; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; " +
            "-fx-border-color: #E2E8F0; -fx-padding: 8 12; -fx-cursor: hand;"
        );

        header.getChildren().addAll(avatar, title, spacer, accountMenu);
        return header;
    }
    
    private VBox createBalanceCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setPrefHeight(200);
        card.getStyleClass().add("balance-card");
        
        UserAccount user = azureApp.getUser(currentUser);
        
        // Tier badge
        Label tierLabel = new Label(user.getLoyaltyTier() + " Account");
        tierLabel.setFont(Font.font("System", 12));
        tierLabel.getStyleClass().add("tier-pill");
        
        // Brand
        Label brandLabel = new Label("Azure Wallet ✦");
        brandLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        brandLabel.getStyleClass().add("brand-label");
        
        Region cardSpacer = new Region();
        VBox.setVgrow(cardSpacer, Priority.ALWAYS);
        
        // Balance display
        Label balanceTitle = new Label("Available Balance");
        balanceTitle.setFont(Font.font("System", 12));
        balanceTitle.getStyleClass().add("balance-title");
        
        Label balanceAmount = new Label(String.format("₱ %.2f", azureApp.getBalance(currentUser)));
        balanceAmount.setFont(Font.font("System", FontWeight.BOLD, 32));
        balanceAmount.getStyleClass().add("balance-amount");
        
        card.getChildren().addAll(tierLabel, brandLabel, cardSpacer, balanceTitle, balanceAmount);
        return card;
    }
    
    private HBox createActionButtons() {
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        
        VBox depositBtn = createActionButton("↓", "Deposit", e -> showDepositDialog());
        VBox withdrawBtn = createActionButton("↑", "Withdraw", e -> showWithdrawDialog());
        VBox transferBtn = createActionButton("→", "Transfer", e -> showTransferDialog());
        VBox voucherBtn = createActionButton("🎁", "Voucher", e -> showVoucherDialog());
        
        actions.getChildren().addAll(depositBtn, withdrawBtn, transferBtn, voucherBtn);
        return actions;
    }
    
    private VBox createActionButton(String icon, String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        VBox btn = new VBox(8);
        btn.setAlignment(Pos.CENTER);
        btn.setPrefWidth(80);
        
        Button iconBtn = new Button(icon);
        iconBtn.setFont(Font.font(24));
        iconBtn.setStyle(
            "-fx-background-color: white; -fx-text-fill: #1E293B; " +
            "-fx-background-radius: 12; -fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );
        iconBtn.setPrefSize(60, 60);
        iconBtn.setOnAction(handler);
        
        // Hover effect
        iconBtn.setOnMouseEntered(e -> iconBtn.setStyle(
            "-fx-background-color: #2563EB; -fx-text-fill: white; " +
            "-fx-background-radius: 12; -fx-border-color: #2563EB; " +
            "-fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 8, 0, 0, 3);"
        ));
        iconBtn.setOnMouseExited(e -> iconBtn.setStyle(
            "-fx-background-color: white; -fx-text-fill: #1E293B; " +
            "-fx-background-radius: 12; -fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        ));
        
        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 13));
        textLabel.setStyle("-fx-text-fill: #000000;");
        
        btn.getChildren().addAll(iconBtn, textLabel);
        return btn;
    }
    
    private VBox createAccountInfoSection() {
        VBox section = new VBox(15);
        
        Label title = new Label("Account Information");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: #1E293B;");
        
        UserAccount user = azureApp.getUser(currentUser);
        
        VBox infoCard = new VBox(15);
        infoCard.setPadding(new Insets(20));
        infoCard.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        infoCard.getChildren().addAll(
            createInfoRow("Username", user.getUsername()),
            createDivider(),
            createInfoRow("Mobile", user.getMobile()),
            createDivider(),
            createInfoRow("Loyalty Tier", user.getLoyaltyTier()),
            createDivider(),
            createInfoRow("Current Balance", String.format("₱ %.2f", user.getBalance()))
        );
        
        section.getChildren().addAll(title, infoCard);
        return section;
    }
    
    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label labelText = new Label(label);
        labelText.setFont(Font.font("System", 14));
        labelText.setStyle("-fx-text-fill: #64748B;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label valueText = new Label(value);
        valueText.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        valueText.setStyle("-fx-text-fill: #1E293B;");
        
        row.getChildren().addAll(labelText, spacer, valueText);
        return row;
    }
    
    private Region createDivider() {
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #E2E8F0;");
        return divider;
    }
    
    private HBox createBottomNav() {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(15, 40, 25, 40));
        nav.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0;");
        
        // Create nav buttons (handlers attached below so we can animate active state)
        navHomeBtn = createNavButton("🏠", "Home", true);
        navWalletBtn = createNavButton("💳", "Wallet", false);
        navHistoryBtn = createNavButton("📊", "History", false);
        navSettingsBtn = createNavButton("⚙", "Settings", false);

        // Attach handlers that also set active highlight
        navHomeBtn.setOnMouseClicked(e -> { showMainScreen(); setActiveNav(navHomeBtn); });
        navWalletBtn.setOnMouseClicked(e -> { showWalletScreen(); setActiveNav(navWalletBtn); });
        navHistoryBtn.setOnMouseClicked(e -> { showHistoryScreen(); setActiveNav(navHistoryBtn); });
        navSettingsBtn.setOnMouseClicked(e -> { showSettingsScreen(); setActiveNav(navSettingsBtn); });

        nav.getChildren().addAll(navHomeBtn, navWalletBtn, navHistoryBtn, navSettingsBtn);
        HBox.setHgrow(navHomeBtn, Priority.ALWAYS);
        HBox.setHgrow(navWalletBtn, Priority.ALWAYS);
        HBox.setHgrow(navHistoryBtn, Priority.ALWAYS);
        HBox.setHgrow(navSettingsBtn, Priority.ALWAYS);
        
        return nav;
    }
    
    private VBox createNavButton(String icon, String label, boolean active) {
        VBox btn = new VBox(5);
        btn.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-cursor: hand;");

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(20));
        iconLabel.setStyle("-fx-text-fill: " + (active ? "#2563EB" : "#94A3B8") + ";");

        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 11));
        textLabel.setStyle("-fx-text-fill: " + (active ? "#2563EB" : "#94A3B8") + ";");

        // Indicator bar shown when active (animated)
        Region indicator = new Region();
        indicator.setPrefHeight(3);
        indicator.setMaxWidth(36);
        indicator.setStyle("-fx-background-color: #2563EB; -fx-background-radius: 2;");
        indicator.setOpacity(active ? 1.0 : 0.0);

        btn.getChildren().addAll(iconLabel, textLabel, indicator);
        btn.setUserData(indicator);
        return btn;
    }

    // Animate and mark the active nav button
    private void setActiveNav(VBox activeBtn) {
        List<VBox> all = List.of(navHomeBtn, navWalletBtn, navHistoryBtn, navSettingsBtn);
        for (VBox b : all) {
            if (b == null) continue;
            Label iconLabel = (Label) b.getChildren().get(0);
            Label textLabel = (Label) b.getChildren().get(1);
            Region indicator = (Region) b.getUserData();
            boolean isActive = (b == activeBtn);
            String color = isActive ? "#2563EB" : "#94A3B8";
            iconLabel.setStyle("-fx-text-fill: " + color + ";");
            textLabel.setStyle("-fx-text-fill: " + color + ";");

            FadeTransition ft = new FadeTransition(Duration.seconds(0.22), indicator);
            ft.setToValue(isActive ? 1.0 : 0.0);
            ft.playFromStart();
        }
    }
    
    // ==================== TRANSACTION DIALOGS ====================
    
    private void showDepositDialog() {
        Dialog<String> dialog = createAmountDialog("Deposit Money", "Enter amount to deposit:");
        dialog.showAndWait().ifPresent(amount -> {
            try {
                double amt = Double.parseDouble(amount);
                if (azureApp.deposit(currentUser, amt)) {
                    showAlert("Success", 
                        String.format("Successfully deposited ₱%.2f", amt), 
                        Alert.AlertType.INFORMATION);
                    refreshMainScreen();
                } else {
                    showAlert("Error", "Deposit failed. Amount must be greater than 0.", Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
            }
        });
    }
    
    private void showWithdrawDialog() {
        Dialog<String> dialog = createAmountDialog("Withdraw Money", "Enter amount to withdraw:\n(₱15 fee will be charged)");
        dialog.showAndWait().ifPresent(amount -> {
            try {
                double amt = Double.parseDouble(amount);
                if (azureApp.withdraw(currentUser, amt)) {
                    showAlert("Success", 
                        String.format("Withdrawn ₱%.2f\n₱15 fee charged", amt), 
                        Alert.AlertType.INFORMATION);
                    refreshMainScreen();
                } else {
                    showAlert("Error", 
                        "Withdrawal failed. Insufficient balance or invalid amount.\n" +
                        "Remember: Amount + ₱15 fee must be available.", 
                        Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
            }
        });
    }
    
    private void showTransferDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Send Money");
        dialog.setHeaderText("Transfer to another user");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField recipientField = new TextField();
        recipientField.setPromptText("Recipient username");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        
        grid.add(new Label("To:"), 0, 0);
        grid.add(recipientField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String recipient = recipientField.getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());
                    
                    if (azureApp.sendMoney(currentUser, recipient, amt)) {
                        showAlert("Success", 
                            String.format("Successfully sent ₱%.2f to %s", amt, recipient), 
                            Alert.AlertType.INFORMATION);
                        refreshMainScreen();
                    } else {
                        showAlert("Error", 
                            "Transfer failed. Check:\n" +
                            "• Recipient username exists\n" +
                            "• Sufficient balance\n" +
                            "• Amount is greater than 0", 
                            Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void showVoucherDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Redeem Voucher");
        dialog.setHeaderText("Enter your voucher code");
        dialog.setContentText("Voucher Code:");
        
        dialog.showAndWait().ifPresent(code -> {
            double value = azureApp.redeemVoucher(currentUser, code.trim());
            if (value > 0) {
                showAlert("Success", 
                    String.format("Voucher redeemed successfully! ₱%.2f added to your balance.", value), 
                    Alert.AlertType.INFORMATION);
                refreshMainScreen();
            } else {
                showAlert("Error", 
                    "Voucher redemption failed.\nCode may be invalid or already used.", 
                    Alert.AlertType.ERROR);
            }
        });
    }
    
    private Dialog<String> createAmountDialog(String title, String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Amount (₱):");
        return dialog;
    }
    
    // ==================== UTILITY METHODS ====================
    
    private void styleInputField(TextField field) {
        field.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 16; " +
            "-fx-font-size: 14;"
        );
        field.setPrefHeight(50);
    }
    
    private Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(54);
        btn.setStyle(
            "-fx-background-color: #4C6EF5; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: #3B5BDB; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand;"
            )
        );
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(
                "-fx-background-color: #4C6EF5; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand;"
            )
        );
        
        return btn;
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void refreshMainScreen() {
        showMainScreen();
    }
    
    // ==================== WALLET SCREEN ====================
    
    private void showWalletScreen() {
        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: #FFFFFF;");
        
        // Header with back button
        HBox header = createHeaderWithBack("Wallet");
        
        // Scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 20, 100, 20));
        
        // Balance card (similar to main screen but larger)
        VBox balanceCard = createBalanceCard();
        balanceCard.setPrefHeight(250);
        
        // Quick actions
        VBox actionsCard = new VBox(15);
        actionsCard.setPadding(new Insets(20));
        actionsCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        actionsTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);
        
        Button depositBtn = createActionButton("💰", "Deposit");
        Button withdrawBtn = createActionButton("💸", "Withdraw");
        Button transferBtn = createActionButton("📤", "Transfer");
        Button voucherBtn = createActionButton("🎫", "Voucher");
        
        depositBtn.setOnAction(e -> showDepositDialog());
        withdrawBtn.setOnAction(e -> showWithdrawDialog());
        transferBtn.setOnAction(e -> showTransferDialog());
        voucherBtn.setOnAction(e -> showVoucherDialog());
        
        actionButtons.getChildren().addAll(depositBtn, withdrawBtn, transferBtn, voucherBtn);
        
        actionsCard.getChildren().addAll(actionsTitle, actionButtons);
        
        content.getChildren().addAll(balanceCard, actionsCard);
        scrollPane.setContent(content);
        
        // Bottom navigation bar
        HBox bottomNav = createBottomNav();
        
        mainContainer.getChildren().addAll(header, scrollPane, bottomNav);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        root.setCenter(mainContainer);
    }
    
    // ==================== HISTORY SCREEN ====================
    
    private void showHistoryScreen() {
        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: #F5F7FA;");
        
        // Header with back button
        HBox header = createHeaderWithBack("Transaction History");
        
        // Scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F7FA; -fx-background-color: #F5F7FA;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 20, 100, 20));
        
        // Transaction History
        VBox transactionCard = new VBox(15);
        transactionCard.setPadding(new Insets(20));
        transactionCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label transTitle = new Label("Recent Transactions");
        transTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        transTitle.setStyle("-fx-text-fill: #1E293B;");
        
        VBox transList = new VBox(10);
        transList.setPadding(new Insets(10, 0, 0, 0));
        
        // Get transaction history from backend
        var transactions = azureApp.getFileManager().getTransactionHistory(currentUser);
        if (transactions.isEmpty()) {
            Label noTrans = new Label("No transactions yet");
            noTrans.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
            transList.getChildren().add(noTrans);
        } else {
            // Show last 10 transactions
            transactions.stream()
                .limit(10)
                .forEach(trans -> {
                    Label transLabel = new Label(trans);
                    transLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");
                    transLabel.setWrapText(true);
                    transList.getChildren().add(transLabel);
                });
        }
        
        transactionCard.getChildren().addAll(transTitle, transList);
        
        // Voucher History
        VBox voucherCard = new VBox(15);
        voucherCard.setPadding(new Insets(20));
        voucherCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label voucherTitle = new Label("Voucher Activity");
        voucherTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        voucherTitle.setStyle("-fx-text-fill: #1E293B;");
        
        VBox voucherList = new VBox(10);
        voucherList.setPadding(new Insets(10, 0, 0, 0));
        
        // Get voucher history from backend
        var vouchers = azureApp.getFileManager().getVoucherHistory(currentUser);
        if (vouchers.isEmpty()) {
            Label noVouch = new Label("No voucher activity yet");
            noVouch.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
            voucherList.getChildren().add(noVouch);
        } else {
            // Show last 10 voucher activities
            vouchers.stream()
                .limit(10)
                .forEach(vouch -> {
                    Label vouchLabel = new Label(vouch);
                    vouchLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");
                    vouchLabel.setWrapText(true);
                    voucherList.getChildren().add(vouchLabel);
                });
        }
        
        voucherCard.getChildren().addAll(voucherTitle, voucherList);
        
        content.getChildren().addAll(transactionCard, voucherCard);
        scrollPane.setContent(content);
        
        // Bottom navigation bar
        HBox bottomNav = createBottomNav();
        
        mainContainer.getChildren().addAll(header, scrollPane, bottomNav);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        root.setCenter(mainContainer);
    }
    
    // ==================== SETTINGS SCREEN ====================
    
    private void showSettingsScreen() {
        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: #F5F7FA;");
        
        // Header with back button
        HBox header = createHeaderWithBack("Settings");
        
        // Scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F7FA; -fx-background-color: #F5F7FA;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 20, 100, 20));
        
        // Account Settings
        VBox accountCard = new VBox(15);
        accountCard.setPadding(new Insets(20));
        accountCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label accountTitle = new Label("Account Settings");
        accountTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        accountTitle.setStyle("-fx-text-fill: #1E293B;");
        
        VBox settingsList = new VBox(10);
        
        // Change PIN button
        Button changePinBtn = createSettingsButton("🔐 Change PIN");
        changePinBtn.setOnAction(e -> showChangePinDialog());
        
        // Account Info button
        Button accountInfoBtn = createSettingsButton("👤 Account Information");
        accountInfoBtn.setOnAction(e -> showAccountInfoDialog());
        
        settingsList.getChildren().addAll(changePinBtn, accountInfoBtn);
        
        accountCard.getChildren().addAll(accountTitle, settingsList);
        
        // App Settings
        VBox appCard = new VBox(15);
        appCard.setPadding(new Insets(20));
        appCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label appTitle = new Label("App Information");
        appTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        appTitle.setStyle("-fx-text-fill: #1E293B;");
        
        VBox appInfo = new VBox(10);
        
        UserAccount user = azureApp.getUser(currentUser);
        Label versionLabel = new Label("Azure Digital Wallet v1.0");
        versionLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14;");
        
        Label lastSyncLabel = new Label("Last Background Sync: " + azureApp.getFileManager().readLastSchedulerRun());
        lastSyncLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14;");
        
        appInfo.getChildren().addAll(versionLabel, lastSyncLabel);
        
        appCard.getChildren().addAll(appTitle, appInfo);
        
        content.getChildren().addAll(accountCard, appCard);
        scrollPane.setContent(content);
        
        // Bottom navigation bar
        HBox bottomNav = createBottomNav();
        
        mainContainer.getChildren().addAll(header, scrollPane, bottomNav);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        root.setCenter(mainContainer);
    }
    
    // ==================== HELPER METHODS ====================
    
    private HBox createHeaderWithBack(String title) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(50, 20, 15, 20));
        header.setStyle("-fx-background-color: #F5F7FA;");
        
        // Back button
        Button backBtn = new Button("←");
        backBtn.setStyle(
            "-fx-background-color: white; -fx-text-fill: #4C6EF5; " +
            "-fx-font-size: 18; -fx-font-weight: bold; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; " +
            "-fx-border-color: #E2E8F0; -fx-padding: 8 12; -fx-cursor: hand;"
        );
        backBtn.setOnAction(e -> showMainScreen());
        
        Label titleLabel = new Label("  " + title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #1E293B;");
        
        header.getChildren().addAll(backBtn, titleLabel);
        return header;
    }
    
    private Button createActionButton(String icon, String text) {
        Button btn = new Button(icon + "\n" + text);
        btn.setPrefSize(80, 80);
        btn.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-text-fill: #475569; " +
            "-fx-font-size: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-cursor: hand; " +
            "-fx-text-alignment: center;"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: #4C6EF5; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #4C6EF5; " +
                "-fx-border-radius: 12; " +
                "-fx-cursor: hand; " +
                "-fx-text-alignment: center;"
            )
        );
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(
                "-fx-background-color: #F8FAFC; " +
                "-fx-text-fill: #475569; " +
                "-fx-font-size: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 12; " +
                "-fx-cursor: hand; " +
                "-fx-text-alignment: center;"
            )
        );
        
        return btn;
    }
    
    private Button createSettingsButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(50);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-text-fill: #475569; " +
            "-fx-font-size: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 8; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0 16;"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: #E2E8F0; " +
                "-fx-text-fill: #475569; " +
                "-fx-font-size: 14; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #CBD5E1; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 16;"
            )
        );
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(
                "-fx-background-color: #F8FAFC; " +
                "-fx-text-fill: #475569; " +
                "-fx-font-size: 14; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 16;"
            )
        );
        
        return btn;
    }
    
    private void showChangePinDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Change PIN");
        dialog.setHeaderText("Enter new 4-digit PIN");
        
        ButtonType changeButtonType = new ButtonType("Change PIN", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        PasswordField currentPinField = new PasswordField();
        currentPinField.setPromptText("Current PIN");
        styleInputField(currentPinField);
        
        PasswordField newPinField = new PasswordField();
        newPinField.setPromptText("New PIN (4 digits)");
        styleInputField(newPinField);
        
        PasswordField confirmPinField = new PasswordField();
        confirmPinField.setPromptText("Confirm new PIN");
        styleInputField(confirmPinField);
        
        content.getChildren().addAll(
            new Label("Current PIN:"), currentPinField,
            new Label("New PIN:"), newPinField,
            new Label("Confirm PIN:"), confirmPinField
        );
        
        dialog.getDialogPane().setContent(content);
        Platform.runLater(currentPinField::requestFocus);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                String currentPin = currentPinField.getText().trim();
                String newPin = newPinField.getText().trim();
                String confirmPin = confirmPinField.getText().trim();
                
                if (currentPin.length() == 4 && newPin.length() == 4 && newPin.equals(confirmPin)) {
                    // Note: This would require adding a changePin method to AzureDigitalApp
                    // For now, show a placeholder message
                    showAlert("Info", "PIN change functionality not yet implemented in backend.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Invalid PIN format or confirmation mismatch.", Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void showAccountInfoDialog() {
        UserAccount user = azureApp.getUser(currentUser);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account Information");
        alert.setHeaderText("Your Account Details");
        
        String info = String.format(
            "Username: %s\n" +
            "Mobile: %s\n" +
            "Balance: $%.2f\n" +
            "Tier: %s",
            user.getUsername(),
            user.getMobile(),
            user.getBalance(),
            user.getLoyaltyTier()
        );
        
        alert.setContentText(info);
        alert.showAndWait();
    }
    
    // ==================== ADMIN LOGIN SCREEN ====================
    
    private void showAdminLoginScreen() {
        VBox adminContainer = new VBox(30);
        adminContainer.setAlignment(Pos.CENTER);
        adminContainer.setPadding(new Insets(60, 40, 60, 40));
        adminContainer.setStyle("-fx-background-color: #F5F7FA;");
        
        // Header
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Admin Access");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: #1E293B;");
        
        Label subtitle = new Label("Enter administrator password");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setStyle("-fx-text-fill: #64748B;");
        
        headerBox.getChildren().addAll(title, subtitle);
        
        // Password field
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("Password");
        passwordLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        passwordLabel.setStyle("-fx-text-fill: #475569;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");
        styleInputField(passwordField);
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        // Login button
        Button loginBtn = createPrimaryButton("Login as Admin");
        loginBtn.setOnAction(e -> handleAdminLogin(passwordField));
        
        // Allow Enter key to submit
        passwordField.setOnAction(e -> handleAdminLogin(passwordField));
        
        // Back button
        Button backBtn = new Button("← Back to User Login");
        backBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #64748B; " +
            "-fx-font-size: 12; -fx-cursor: hand;"
        );
        backBtn.setOnAction(e -> showLoginScreen());
        
        adminContainer.getChildren().addAll(headerBox, passwordBox, loginBtn, backBtn);
        
        root.setCenter(adminContainer);
    }
    
    private void handleAdminLogin(PasswordField passwordField) {
        String password = passwordField.getText().trim();
        
        if (password.isEmpty()) {
            showAlert("Error", "Please enter the admin password", Alert.AlertType.ERROR);
            return;
        }
        
        if (adminControl.authenticateAdmin(password)) {
            isAdminMode = true;
            showAdminPanel();
        } else {
            showAlert("Access Denied", "Invalid admin password", Alert.AlertType.ERROR);
            passwordField.clear();
        }
    }
    
    // ==================== ADMIN PANEL ====================
    
    private void showAdminPanel() {
        BorderPane mainContainer = new BorderPane();
        mainContainer.setStyle("-fx-background-color: #F5F7FA;");
        
        // Sidebar
        VBox sidebar = createAdminSidebar();
        
        // Main content area
        StackPane contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #F5F7FA;");
        
        // Initially show dashboard
        contentArea.getChildren().add(createDashboardContent());
        
        // Store references for sidebar navigation and toggle functionality
        this.adminContentArea = contentArea;
        this.adminSidebar = sidebar;
        this.adminMainContainer = mainContainer;
        this.sidebarVisible = true;
        
        mainContainer.setLeft(sidebar);
        mainContainer.setCenter(contentArea);
        
        root.setCenter(mainContainer);
    }
    
    private StackPane adminContentArea;
    private VBox adminSidebar;
    private BorderPane adminMainContainer;
    private boolean sidebarVisible;
    
    // Bottom nav button references for active highlight
    private VBox navHomeBtn;
    private VBox navWalletBtn;
    private VBox navHistoryBtn;
    private VBox navSettingsBtn;
    
    private VBox createAdminSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setMinWidth(250);
        sidebar.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 0 0;");
        
        // Header with toggle button
        HBox headerBox = new HBox();
        headerBox.setPadding(new Insets(20));
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Button toggleBtn = new Button("☰");
        toggleBtn.setPrefSize(30, 30);
        toggleBtn.setStyle(
            "-fx-background-color: #2563EB; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand; " +
            "-fx-border-color: #2563EB; " +
            "-fx-border-radius: 6;"
        );
        toggleBtn.setOnAction(e -> toggleSidebar());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        headerBox.getChildren().addAll(toggleBtn, spacer);
        
        // Title section
        VBox titleBox = new VBox(8);
        titleBox.setPadding(new Insets(0, 20, 20, 20));
        titleBox.setAlignment(Pos.CENTER);
        
        Label title = new Label("Admin Panel");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: #1E293B;");
        
        Label subtitle = new Label("Azure Digital Wallet");
        subtitle.setFont(Font.font("System", 12));
        subtitle.setStyle("-fx-text-fill: #64748B;");
        
        titleBox.getChildren().addAll(title, subtitle);
        
        // Navigation items
        VBox navItems = new VBox(5);
        navItems.setPadding(new Insets(20, 0, 20, 0));
        
        Button dashboardBtn = createSidebarButton("📊 Dashboard", true);
        Button usersBtn = createSidebarButton("👥 User Management", false);
        Button vouchersBtn = createSidebarButton("🎫 Voucher System", false);
        Button systemBtn = createSidebarButton("⚙️ System Tools", false);
        Button logsBtn = createSidebarButton("📋 Activity Logs", false);
        
        dashboardBtn.setOnAction(e -> switchAdminContent("dashboard"));
        usersBtn.setOnAction(e -> switchAdminContent("users"));
        vouchersBtn.setOnAction(e -> switchAdminContent("vouchers"));
        systemBtn.setOnAction(e -> switchAdminContent("system"));
        logsBtn.setOnAction(e -> switchAdminContent("logs"));
        
        navItems.getChildren().addAll(dashboardBtn, usersBtn, vouchersBtn, systemBtn, logsBtn);
        
        // Logout button at bottom
        VBox bottomSection = new VBox();
        bottomSection.setPadding(new Insets(20));
        VBox.setVgrow(bottomSection, Priority.ALWAYS);
        
        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setPrefHeight(40);
        logoutBtn.setStyle(
            "-fx-background-color: #EF4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );
        logoutBtn.setOnAction(e -> {
            isAdminMode = false;
            showLoginScreen();
        });
        
        bottomSection.getChildren().add(logoutBtn);
        
        sidebar.getChildren().addAll(headerBox, titleBox, navItems, bottomSection);
        return sidebar;
    }
    
    private Button createSidebarButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(45);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(0, 20, 0, 20));
        
        if (active) {
            btn.setStyle(
                "-fx-background-color: #2563EB; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #333333; " +
                "-fx-font-size: 14; " +
                "-fx-cursor: hand;"
            );
            
            btn.setOnMouseEntered(e -> 
                btn.setStyle(
                    "-fx-background-color: #F8F9FA; " +
                    "-fx-text-fill: #000000; " +
                    "-fx-font-size: 14; " +
                    "-fx-cursor: hand;"
                )
            );
            
            btn.setOnMouseExited(e -> 
                btn.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-text-fill: #333333; " +
                    "-fx-font-size: 14; " +
                    "-fx-cursor: hand;"
                )
            );
        }
        
        return btn;
    }
    
    private void switchAdminContent(String section) {
        adminContentArea.getChildren().clear();
        
        switch (section) {
            case "dashboard" -> adminContentArea.getChildren().add(createDashboardContent());
            case "users" -> adminContentArea.getChildren().add(createUsersContent());
            case "vouchers" -> adminContentArea.getChildren().add(createVouchersContent());
            case "system" -> adminContentArea.getChildren().add(createSystemContent());
            case "logs" -> adminContentArea.getChildren().add(createLogsContent());
        }
    }
    
    private void toggleSidebar() {
        if (sidebarVisible) {
            // Hide sidebar
            adminMainContainer.setLeft(null);
            sidebarVisible = false;
            
            // Add a small toggle button in the top-left corner when sidebar is hidden
            Button showSidebarBtn = new Button("☰");
            showSidebarBtn.setPrefSize(40, 40);
            showSidebarBtn.setStyle(
                "-fx-background-color: #2563EB; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-border-color: #1D4ED8; " +
                "-fx-border-radius: 8;"
            );
            showSidebarBtn.setOnAction(e -> {
                adminMainContainer.setLeft(adminSidebar);
                sidebarVisible = true;
                adminMainContainer.setTop(null); // Remove the toggle button
            });
            
            // Position the button in top-left
            adminMainContainer.setTop(showSidebarBtn);
            BorderPane.setAlignment(showSidebarBtn, Pos.TOP_LEFT);
            BorderPane.setMargin(showSidebarBtn, new Insets(20));
            
        } else {
            // Show sidebar
            adminMainContainer.setLeft(adminSidebar);
            sidebarVisible = true;
            adminMainContainer.setTop(null); // Remove the toggle button
        }
    }
    
    private VBox createDashboardContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 30, 100, 30));
        
        // Page header
        Label pageTitle = new Label("Dashboard Overview");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
        pageTitle.setStyle("-fx-text-fill: #000000;");
        
        // Statistics Cards Row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        
        // Get real data
        int totalUsers = azureApp.getUsers().size();
        double totalBalance = azureApp.getUsers().values().stream().mapToDouble(UserAccount::getBalance).sum();
        double totalRevenue = adminControl.getSystemRevenue();
        int activeUsers = (int) azureApp.getUsers().values().stream()
            .filter(u -> u.getBalance() > 0).count();
        
        VBox usersCard = createStatCard("👥 Total Users", String.valueOf(totalUsers), "#4C6EF5");
        VBox balanceCard = createStatCard("💰 Total Balance", String.format("₱%,.2f", totalBalance), "#10B981");
        VBox revenueCard = createStatCard("💵 Total Revenue", String.format("₱%,.2f", totalRevenue), "#F59E0B");
        VBox activeCard = createStatCard("🔥 Active Users", String.valueOf(activeUsers), "#EF4444");
        
        statsRow.getChildren().addAll(usersCard, balanceCard, revenueCard, activeCard);
        
        // System Status Card
        VBox systemCard = new VBox(15);
        systemCard.setPadding(new Insets(25));
        systemCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label systemTitle = new Label("System Status");
        systemTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        systemTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox statusRow = new HBox(30);
        statusRow.setAlignment(Pos.CENTER);
        
        VBox schedulerStatus = createStatusItem("⏰ Scheduler", "Last Run: " + azureApp.getFileManager().readLastSchedulerRun());
        VBox dataStatus = createStatusItem("💾 Data Files", "All systems operational");
        VBox securityStatus = createStatusItem("🔒 Security", "Admin access active");
        
        statusRow.getChildren().addAll(schedulerStatus, dataStatus, securityStatus);
        
        systemCard.getChildren().addAll(systemTitle, statusRow);
        
        // Quick Actions Card
        VBox actionsCard = new VBox(15);
        actionsCard.setPadding(new Insets(25));
        actionsCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        actionsTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox actionsRow1 = new HBox(15);
        actionsRow1.setAlignment(Pos.CENTER);
        
        Button schedulerBtn = createModernButton("⚡ Run Scheduler", "#4C6EF5");
        Button revenueBtn = createModernButton("💰 View Revenue", "#10B981");
        Button logBtn = createModernButton("📋 Admin Logs", "#F59E0B");
        
        schedulerBtn.setOnAction(e -> {
            adminControl.triggerScheduler();
            showAlert("Success", "Scheduler executed successfully!", Alert.AlertType.INFORMATION);
            switchAdminContent("dashboard"); // Refresh dashboard
        });
        
        revenueBtn.setOnAction(e -> showRevenueDialog());
        logBtn.setOnAction(e -> showAdminLogDialog());
        
        actionsRow1.getChildren().addAll(schedulerBtn, revenueBtn, logBtn);
        
        actionsCard.getChildren().addAll(actionsTitle, actionsRow1);
        
        // Recent Activity Card
        VBox activityCard = new VBox(15);
        activityCard.setPadding(new Insets(25));
        activityCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label activityTitle = new Label("Recent Admin Activity");
        activityTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        activityTitle.setStyle("-fx-text-fill: #1E293B;");
        
        VBox activityList = new VBox(8);
        activityList.setPadding(new Insets(10, 0, 0, 0));
        
        List<String> logs = adminControl.getAdminLog();
        int logCount = Math.min(5, logs.size()); // Show last 5 activities
        
        for (int i = logs.size() - logCount; i < logs.size(); i++) {
            if (i >= 0) {
                Label logLabel = new Label("• " + logs.get(i));
                logLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
                logLabel.setWrapText(true);
                activityList.getChildren().add(logLabel);
            }
        }
        
        if (logs.isEmpty()) {
            Label noActivity = new Label("No recent activity");
            noActivity.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            activityList.getChildren().add(noActivity);
        }
        
        activityCard.getChildren().addAll(activityTitle, activityList);
        
        content.getChildren().addAll(pageTitle, statsRow, systemCard, actionsCard, activityCard);
        scrollPane.setContent(content);
        
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox createUsersContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F7FA; -fx-background-color: #F5F7FA;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 30, 100, 30));
        
        // Page header
        Label pageTitle = new Label("User Management");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
        pageTitle.setStyle("-fx-text-fill: #000000;");
        
        // User Actions Card
        VBox actionsCard = new VBox(20);
        actionsCard.setPadding(new Insets(25));
        actionsCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label actionsTitle = new Label("User Actions");
        actionsTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        actionsTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox actionsRow = new HBox(15);
        actionsRow.setAlignment(Pos.CENTER);
        
        Button viewUsersBtn = createModernButton("👥 View All Users", "#4C6EF5");
        Button deleteUserBtn = createModernButton("🗑️ Delete User", "#EF4444");
        
        viewUsersBtn.setOnAction(e -> showUserListDialog());
        deleteUserBtn.setOnAction(e -> showDeleteUserDialog());
        
        actionsRow.getChildren().addAll(viewUsersBtn, deleteUserBtn);
        
        actionsCard.getChildren().addAll(actionsTitle, actionsRow);
        
        // User Statistics
        VBox statsCard = new VBox(15);
        statsCard.setPadding(new Insets(25));
        statsCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label statsTitle = new Label("User Statistics");
        statsTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        statsTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox statsRow = new HBox(30);
        statsRow.setAlignment(Pos.CENTER);
        
        int totalUsers = azureApp.getUsers().size();
        int activeUsers = (int) azureApp.getUsers().values().stream()
            .filter(u -> u.getBalance() > 0).count();
        int inactiveUsers = totalUsers - activeUsers;
        
        VBox totalStat = createStatusItem("👥 Total Users", String.valueOf(totalUsers));
        VBox activeStat = createStatusItem("🔥 Active Users", String.valueOf(activeUsers));
        VBox inactiveStat = createStatusItem("💤 Inactive Users", String.valueOf(inactiveUsers));
        
        statsRow.getChildren().addAll(totalStat, activeStat, inactiveStat);
        
        statsCard.getChildren().addAll(statsTitle, statsRow);
        
        content.getChildren().addAll(pageTitle, actionsCard, statsCard);
        scrollPane.setContent(content);
        
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox createVouchersContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F7FA; -fx-background-color: #F5F7FA;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 30, 100, 30));
        
        // Page header
        Label pageTitle = new Label("Voucher Management");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
        pageTitle.setStyle("-fx-text-fill: #000000;");
        
        // Voucher Generation Card
        VBox voucherCard = new VBox(20);
        voucherCard.setPadding(new Insets(25));
        voucherCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label voucherTitle = new Label("Generate Vouchers");
        voucherTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        voucherTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox voucherRow1 = new HBox(15);
        voucherRow1.setAlignment(Pos.CENTER);
        
        Button monthlyBtn = createModernButton("📅 Monthly", "#06B6D4");
        Button holidayBtn = createModernButton("🎄 Holiday", "#EC4899");
        
        monthlyBtn.setOnAction(e -> {
            adminControl.generateMonthlyVouchers();
            showAlert("Success", "Monthly vouchers generated!", Alert.AlertType.INFORMATION);
        });
        
        holidayBtn.setOnAction(e -> {
            adminControl.generateHolidayVouchers();
            showAlert("Success", "Holiday vouchers generated!", Alert.AlertType.INFORMATION);
        });
        
        voucherRow1.getChildren().addAll(monthlyBtn, holidayBtn);
        
        HBox voucherRow2 = new HBox(15);
        voucherRow2.setAlignment(Pos.CENTER);
        
        Button singleBtn = createModernButton("🎫 Single User", "#84CC16");
        
        singleBtn.setOnAction(e -> {
            adminControl.generateSingleVouchers();
            showAlert("Success", "Single vouchers generated!", Alert.AlertType.INFORMATION);
        });
        
        voucherRow2.getChildren().addAll(singleBtn);
        
        voucherCard.getChildren().addAll(voucherTitle, voucherRow1, voucherRow2);
        
        content.getChildren().addAll(pageTitle, voucherCard);
        scrollPane.setContent(content);
        
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox createSystemContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F7FA; -fx-background-color: #F5F7FA;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 30, 100, 30));
        
        // Page header
        Label pageTitle = new Label("System Tools");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
        pageTitle.setStyle("-fx-text-fill: #000000;");
        
        // System Actions Card
        VBox actionsCard = new VBox(20);
        actionsCard.setPadding(new Insets(25));
        actionsCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label actionsTitle = new Label("System Actions");
        actionsTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        actionsTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox actionsRow = new HBox(15);
        actionsRow.setAlignment(Pos.CENTER);
        
        Button schedulerBtn = createModernButton("⚡ Run Scheduler", "#4C6EF5");
        Button revenueBtn = createModernButton("💰 View Revenue", "#10B981");
        
        schedulerBtn.setOnAction(e -> {
            adminControl.triggerScheduler();
            showAlert("Success", "Scheduler executed successfully!", Alert.AlertType.INFORMATION);
        });
        
        revenueBtn.setOnAction(e -> showRevenueDialog());
        
        actionsRow.getChildren().addAll(schedulerBtn, revenueBtn);
        
        actionsCard.getChildren().addAll(actionsTitle, actionsRow);
        
        // System Status Card
        VBox statusCard = new VBox(15);
        statusCard.setPadding(new Insets(25));
        statusCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label statusTitle = new Label("System Status");
        statusTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        statusTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox statusRow = new HBox(30);
        statusRow.setAlignment(Pos.CENTER);
        
        VBox schedulerStatus = createStatusItem("⏰ Scheduler", "Last Run: " + azureApp.getFileManager().readLastSchedulerRun());
        VBox dataStatus = createStatusItem("💾 Data Files", "All systems operational");
        VBox securityStatus = createStatusItem("🔒 Security", "Admin access active");
        
        statusRow.getChildren().addAll(schedulerStatus, dataStatus, securityStatus);
        
        statusCard.getChildren().addAll(statusTitle, statusRow);
        
        content.getChildren().addAll(pageTitle, actionsCard, statusCard);
        scrollPane.setContent(content);
        
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox createLogsContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F7FA; -fx-background-color: #F5F7FA;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 30, 100, 30));
        
        // Page header
        Label pageTitle = new Label("Activity Logs");
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 28));
        pageTitle.setStyle("-fx-text-fill: #000000;");
        
        // Logs Card
        VBox logsCard = new VBox(15);
        logsCard.setPadding(new Insets(25));
        logsCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label logsTitle = new Label("Admin Activity Log");
        logsTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        logsTitle.setStyle("-fx-text-fill: #1E293B;");
        
        VBox logsList = new VBox(8);
        logsList.setPadding(new Insets(10, 0, 0, 0));
        
        List<String> logs = adminControl.getAdminLog();
        
        if (logs.isEmpty()) {
            Label noLogs = new Label("No admin activity recorded");
            noLogs.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            logsList.getChildren().add(noLogs);
        } else {
            for (String log : logs) {
                Label logLabel = new Label("• " + log);
                logLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
                logLabel.setWrapText(true);
                logsList.getChildren().add(logLabel);
            }
        }
        
        logsCard.getChildren().addAll(logsTitle, logsList);
        
        content.getChildren().addAll(pageTitle, logsCard);
        scrollPane.setContent(content);
        
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(200);
        card.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);"
        );
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setOpacity(0.9);
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        valueLabel.setTextFill(Color.WHITE);
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }
    
    private VBox createStatusItem(String title, String status) {
        VBox item = new VBox(5);
        item.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #1E293B;");
        
        Label statusLabel = new Label(status);
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setStyle("-fx-text-fill: #64748B;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(150);
        statusLabel.setAlignment(Pos.CENTER);
        
        item.getChildren().addAll(titleLabel, statusLabel);
        return item;
    }
    
    private Button createModernButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(120);
        btn.setPrefHeight(45);
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 10; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 4); " +
                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
            )
        );
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(
                "-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
            )
        );
        
        return btn;
    }
    
    private Button createAdminButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(45);
        btn.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-size: 12; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 10; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0 16;"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: #2563EB; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #2563EB; " +
                "-fx-border-radius: 10; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 16;"
            )
        );
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-text-fill: #000000; " +
                "-fx-font-size: 12; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 10; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 16;"
            )
        );
        
        return btn;
    }
    
    private void showUserListDialog() {
        List<String> users = adminControl.getAllUsers();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("All Users");
        alert.setHeaderText("Registered Users (" + users.size() + " total)");
        
        TextArea textArea = new TextArea(String.join("\n", users));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
    
    private void showDeleteUserDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Delete User");
        dialog.setHeaderText("Enter username to delete");
        dialog.setContentText("Username:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            if (adminControl.deleteUser(username.trim())) {
                showAlert("Success", "User '" + username + "' deleted successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "User '" + username + "' not found!", Alert.AlertType.ERROR);
            }
        });
    }
    
    private void showRevenueDialog() {
        double revenue = adminControl.getSystemRevenue();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Revenue");
        alert.setHeaderText("Total Revenue Collected");
        alert.setContentText(String.format("₱%,.2f", revenue));
        alert.showAndWait();
    }
    
    private void showAdminLogDialog() {
        List<String> logs = adminControl.getAdminLog();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin Activity Log");
        alert.setHeaderText("Recent Admin Actions");
        
        TextArea textArea = new TextArea(String.join("\n", logs));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}