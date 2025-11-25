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
import javafx.scene.Group;
import javafx.scene.shape.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import azurewallet.utils.CardUtil;
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
        // Fixed size window to match the compact/mobile layout in the provided screenshot
        double fixedWidth = 480.0;
        double fixedHeight = 900.0;
        scene = new Scene(root, fixedWidth, fixedHeight);
        scene.setFill(Color.web("#FFFFFF"));
        java.net.URL css = getClass().getResource("/styles/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        // Show login screen
        showLoginScreen();

        primaryStage.setTitle("Azure Digital Wallet");
        primaryStage.setScene(scene);
        // enforce a fixed window size so layout matches the screenshot
        primaryStage.setWidth(fixedWidth);
        primaryStage.setHeight(fixedHeight);
        primaryStage.setMinWidth(fixedWidth);
        primaryStage.setMinHeight(fixedHeight);
        primaryStage.setMaxWidth(fixedWidth);
        primaryStage.setMaxHeight(fixedHeight);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(e -> {
            // Ensure data is saved on close
            azureApp.shutdown();
        });
        primaryStage.show();
    }

    // ==================== LOGIN SCREEN ====================
    private void showLoginScreen() {
        // container for login screen
        VBox loginContainer = new VBox(20);
        // place content towards top so the app title sits higher in window
        loginContainer.setAlignment(Pos.TOP_CENTER);
        // larger top padding to push the title further upward from center
        loginContainer.setPadding(new Insets(140, 0, 12, 0));
        loginContainer.getStyleClass().add("login-container");

        // Account dropdown (Logout only)
            MenuButton accountMenu = new MenuButton();
            // Create a full-width logout button inside the popup so it matches Account width
            Button logoutBtn = new Button("Logout");
            logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #111827; -fx-alignment: CENTER_LEFT; -fx-padding: 8 12; -fx-border-color: transparent; -fx-border-width: 0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
            logoutBtn.setOnAction(e -> {
                currentUser = null;
                showLoginScreen();
            });
            // Bind the logout button width slightly smaller than the accountMenu width
            logoutBtn.prefWidthProperty().bind(accountMenu.widthProperty().subtract(20));
            CustomMenuItem logoutItem = new CustomMenuItem(logoutBtn, true);
            accountMenu.getItems().addAll(logoutItem);
        // reduce spacing so the tagline sits closer under the brand
        VBox logoBox = new VBox(4);
        logoBox.setAlignment(Pos.CENTER);
        
        Label logo = new Label("Azure");
        logo.setFont(Font.font("System", FontWeight.BOLD, 36));
        logo.setStyle("-fx-text-fill: #FFD700;");
        
            // App title above the welcome box — use Windows 'Cambria' as a close display serif
            Label appTitle = new Label("Azure Digital Wallet");
            appTitle.setFont(Font.font("Cambria", FontWeight.BOLD, 36));
            appTitle.setStyle("-fx-font-family: 'Cambria'; -fx-text-fill: #FFD700;");
            // Welcome title and login form
            VBox welcomeBox = new VBox(6);
            welcomeBox.setAlignment(Pos.CENTER);
            Label titleLabel = new Label("Welcome Back");
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
            titleLabel.setStyle("-fx-text-fill: #111827;");
            Label subtitle = new Label("Sign in to continue");
            subtitle.setFont(Font.font("System", 12));
            subtitle.setStyle("-fx-text-fill: #475569;");
            welcomeBox.getChildren().addAll(titleLabel, subtitle);

            // Form wrapper with username and PIN inputs
            VBox formWrapper = new VBox(12);
            formWrapper.setAlignment(Pos.CENTER);
            TextField usernameField = new TextField();
            usernameField.setPromptText("Username");
            styleInputField(usernameField);
            PasswordField pinField = new PasswordField();
            pinField.setPromptText("PIN");
            styleInputField(pinField);
            Button signInBtn = createPrimaryButton("Sign In");
            // remove the blue outline/border and ensure a flat rounded blue button
            signInBtn.setStyle(
                "-fx-background-color: #FFD700; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: transparent; -fx-cursor: hand;"
            );
            // Override hover handlers (createPrimaryButton registers its own) to keep border transparent
            signInBtn.setOnMouseEntered(e ->
                signInBtn.setStyle(
                    "-fx-background-color: #FFA500; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: transparent; -fx-cursor: hand;"
                )
            );
            signInBtn.setOnMouseExited(e ->
                signInBtn.setStyle(
                    "-fx-background-color: #FFD700; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: transparent; -fx-cursor: hand;"
                )
            );
            signInBtn.setOnAction(e -> handleLogin(usernameField, pinField));
            formWrapper.getChildren().addAll(usernameField, pinField, signInBtn);
        
        HBox signUpRow = new HBox(5);
        signUpRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Don't have an account?");
        noAccount.setStyle("-fx-text-fill: #333333; -fx-font-size: 13;");
        Hyperlink signUpLink = new Hyperlink("Sign Up");
        signUpLink.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13; -fx-font-weight: bold;");
        signUpLink.setOnAction(e -> showRegistrationScreen());
        signUpRow.getChildren().addAll(noAccount, signUpLink);
        
        HBox forgotRow = new HBox(5);
        forgotRow.setAlignment(Pos.CENTER);
        Label forgotLabel = new Label("Forgot your PIN?");
        forgotLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13;");
        Hyperlink forgotLink = new Hyperlink("Reset");
        forgotLink.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13; -fx-font-weight: bold;");
        forgotLink.setOnAction(e -> showForgotPinDialog());
        forgotRow.getChildren().addAll(forgotLabel, forgotLink);
        
        Button adminBtn = new Button("Admin Login");
        adminBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #333333; " +
            "-fx-font-size: 12; -fx-border-color: #E2E8F0; -fx-border-width: 1; " +
            "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;"
        );
        adminBtn.setOnAction(e -> showAdminLoginScreen());
        // hide the admin button from the UI but keep it functional via accelerator
        adminBtn.setVisible(false);
        adminBtn.setManaged(false);
        
        HBox bottomRow = new HBox(12);
        bottomRow.setAlignment(Pos.CENTER);
        VBox linkBox = new VBox(8);
        linkBox.setAlignment(Pos.CENTER);
        linkBox.getChildren().addAll(signUpRow, forgotRow);
        bottomRow.getChildren().addAll(linkBox, adminBtn);

        // Add a global Ctrl+F accelerator to open admin login while keeping the button hidden
        if (scene != null) {
            scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                () -> {
                    // show admin login screen when Ctrl+F is pressed
                    showAdminLoginScreen();
                }
            );
        }
        
        // Place logo and app title above the welcome title
        loginContainer.getChildren().addAll(
            logoBox, appTitle, welcomeBox, formWrapper, bottomRow
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
        // If system maintenance mode is enabled, block sign-in but allow sign-up
        if (azureApp.getFileManager().isMaintenanceMode()) {
            showAlert("Maintenance", "Sign-in is temporarily disabled due to system maintenance. You may still create a new account.", Alert.AlertType.INFORMATION);
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
    
    private void showForgotPinDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Your PIN");
        dialog.setHeaderText("Verify your account to reset PIN");
        
        // Create a card-style container
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(16);
        grid.setPadding(new Insets(24));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 14;");
        
        // Set up columns
        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setMinWidth(120);
        leftCol.setPrefWidth(140);
        leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);
        
        // Info label
        Label infoLabel = new Label("Enter your details to verify your account and reset your PIN");
        infoLabel.setFont(Font.font("System", 13));
        infoLabel.setStyle("-fx-text-fill: #64748B; -fx-wrap-text: true;");
        infoLabel.setWrapText(true);
        grid.add(infoLabel, 0, 0, 2, 1);
        GridPane.setMargin(infoLabel, new Insets(0, 0, 8, 0));
        
        // Username field
        Label usernameLabel = new Label("Username");
        usernameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        usernameLabel.setStyle("-fx-text-fill: #374151;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle("-fx-padding: 10 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB; -fx-font-size: 13;");
        usernameField.setMinHeight(40);
        grid.add(usernameLabel, 0, 1);
        grid.add(usernameField, 1, 1);
        
        // Mobile Number field
        Label mobileLabel = new Label("Mobile Number");
        mobileLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        mobileLabel.setStyle("-fx-text-fill: #374151;");
        TextField mobileField = new TextField();
        mobileField.setPromptText("09XXXXXXXXX (registered)");
        mobileField.setStyle("-fx-padding: 10 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB; -fx-font-size: 13;");
        mobileField.setMinHeight(40);
        grid.add(mobileLabel, 0, 2);
        grid.add(mobileField, 1, 2);
        
        // New PIN field
        Label newPinLabel = new Label("New PIN");
        newPinLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        newPinLabel.setStyle("-fx-text-fill: #374151;");
        PasswordField newPinField = new PasswordField();
        newPinField.setPromptText("Enter new 4-digit PIN");
        newPinField.setStyle("-fx-padding: 10 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB; -fx-font-size: 13;");
        newPinField.setMinHeight(40);
        // Restrict to 4 digits
        newPinField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d{0,4}")) {
                newPinField.setText(oldV);
            }
        });
        grid.add(newPinLabel, 0, 3);
        grid.add(newPinField, 1, 3);
        
        // Confirm PIN field
        Label confirmPinLabel = new Label("Confirm PIN");
        confirmPinLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        confirmPinLabel.setStyle("-fx-text-fill: #374151;");
        PasswordField confirmPinField = new PasswordField();
        confirmPinField.setPromptText("Confirm your new PIN");
        confirmPinField.setStyle("-fx-padding: 10 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB; -fx-font-size: 13;");
        confirmPinField.setMinHeight(40);
        // Restrict to 4 digits
        confirmPinField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d{0,4}")) {
                confirmPinField.setText(oldV);
            }
        });
        grid.add(confirmPinLabel, 0, 4);
        grid.add(confirmPinField, 1, 4);
        
        // Hint text
        Label hintLabel = new Label("PIN must be exactly 4 digits");
        hintLabel.setFont(Font.font("System", 11));
        hintLabel.setStyle("-fx-text-fill: #94A3B8; -fx-italic: true;");
        grid.add(hintLabel, 0, 5, 2, 1);
        GridPane.setMargin(hintLabel, new Insets(8, 0, 0, 0));
        
        // Wrap in a container with shadow effect
        VBox wrapper = new VBox(grid);
        wrapper.setPadding(new Insets(12));
        wrapper.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 14;");
        
        dialog.getDialogPane().setContent(wrapper);
        
        ButtonType resetBtnType = new ButtonType("Reset PIN", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetBtnType, ButtonType.CANCEL);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        
        // Style the buttons
        javafx.scene.Node resetButton = dialog.getDialogPane().lookupButton(resetBtnType);
        resetButton.setStyle("-fx-background-color: #FFD700; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 32; -fx-font-size: 13;");
        javafx.scene.Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-weight: bold; -fx-padding: 10 32; -fx-font-size: 13;");
        
        // Enable/disable reset button based on validation
        resetButton.setDisable(true);
        Runnable validate = () -> {
            boolean valid = !usernameField.getText().trim().isEmpty() && 
                           !mobileField.getText().trim().isEmpty() && 
                           newPinField.getText().length() == 4 && 
                           confirmPinField.getText().length() == 4 &&
                           newPinField.getText().equals(confirmPinField.getText());
            resetButton.setDisable(!valid);
        };
        
        usernameField.textProperty().addListener((o, oldV, newV) -> validate.run());
        mobileField.textProperty().addListener((o, oldV, newV) -> validate.run());
        newPinField.textProperty().addListener((o, oldV, newV) -> validate.run());
        confirmPinField.textProperty().addListener((o, oldV, newV) -> validate.run());
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == resetBtnType) {
                String username = usernameField.getText().trim();
                String mobile = mobileField.getText().trim();
                String newPin = newPinField.getText().trim();
                
                // Verify user exists and mobile matches
                UserAccount user = azureApp.getUser(username);
                if (user == null) {
                    showAlert("Error", "Username not found.", Alert.AlertType.ERROR);
                    return;
                }
                
                if (!user.getMobile().equals(mobile)) {
                    showAlert("Error", "Mobile number does not match our records.", Alert.AlertType.ERROR);
                    return;
                }
                
                // Reset the PIN (without requiring current PIN verification)
                user.resetPin(newPin);
                azureApp.getFileManager().saveUsers(azureApp.getUsers());
                showAlert("Success", 
                    "Your PIN has been reset successfully!\nYou can now login with your new PIN.", 
                    Alert.AlertType.INFORMATION);
                showLoginScreen();
            }
        });
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
        subtitle.setFont(Font.font("System", 9));
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
        // Remove blue border for Register button, keep blue background
        registerBtn.setStyle(
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"
        );
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(
            "-fx-background-color: #FFA500; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"
        ));
        registerBtn.setOnMouseExited(e -> registerBtn.setStyle(
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"
        ));
        
        // Back to login
        HBox backRow = new HBox(5);
        backRow.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Already have an account?");
        hasAccount.setStyle("-fx-text-fill: #333333; -fx-font-size: 13;");
        Hyperlink loginLink = new Hyperlink("Sign In");
        loginLink.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13; -fx-font-weight: bold;");
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
        Pane actions = createActionButtons();
        
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
        
        // User avatar: larger, crisper circle with centered white user icon
        Circle avatar = new Circle(26);
        avatar.setFill(Color.web("#FFD700"));
        // subtle border to make the avatar edge crisp
        avatar.setStroke(Color.web("#e5e7eb"));
        avatar.setStrokeWidth(1.5);

        // avatar graphic (white user glyph) instead of emoji
        javafx.scene.Node avatarIcon = buildAvatarGraphic();

        StackPane avatarStack = new StackPane(avatar, avatarIcon);
        avatarStack.setPrefSize(52, 52);
        avatarStack.setMinSize(52, 52);
        avatarStack.setMaxSize(52, 52);
        avatarStack.setAlignment(Pos.CENTER);

        // Get user from backend
        UserAccount user = azureApp.getUser(currentUser);
        Label title = new Label(user.getUsername());
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #000000;");

        HBox nameBox = new HBox(8, title);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        // small left padding to separate username from avatar
        nameBox.setPadding(new Insets(0, 0, 0, 12));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Account dropdown (logout only)
        MenuButton accountMenu = new MenuButton();
        // Create a full-width logout button inside the popup so it matches Account width
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #111827; -fx-alignment: CENTER_LEFT; -fx-padding: 8 12; -fx-border-color: transparent; -fx-border-width: 0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            currentUser = null;
            showLoginScreen();
        });
        // Bind the logout button width slightly smaller than the accountMenu width
        logoutBtn.prefWidthProperty().bind(accountMenu.widthProperty().subtract(20));
        CustomMenuItem logoutItem = new CustomMenuItem(logoutBtn, true);
        logoutItem.setHideOnClick(true);

        accountMenu.getItems().addAll(logoutItem);
        accountMenu.setText("Account ▾");
        accountMenu.setStyle(
            "-fx-background-color: white; -fx-text-fill: #FFD700; " +
            "-fx-font-size: 12; -fx-font-weight: bold; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; " +
            "-fx-border-color: #E2E8F0; -fx-padding: 8 12; -fx-cursor: hand;"
        );

        header.getChildren().addAll(avatarStack, nameBox, spacer, accountMenu);
        return header;
    }
    
    private VBox createBalanceCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setPrefHeight(220);
        // Add blue gradient background with border and shadow
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFC107); " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-border-color: rgba(37,99,235,0.18); " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);"
        );
        
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
    
    private Pane createActionButtons() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(8, 20, 8, 20));

        // Four equal columns for a 4x2 layout (responsive)
        ColumnConstraints c0 = new ColumnConstraints(); c0.setPercentWidth(25);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(25);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(25);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(25);
        grid.getColumnConstraints().addAll(c0, c1, c2, c3);

        VBox depositBtn = createActionButton(buildIcon("deposit"), "Deposit", e -> showDepositDialog());
        VBox withdrawBtn = createActionButton(buildIcon("withdraw"), "Withdraw", e -> showWithdrawDialog());
        VBox transferBtn = createActionButton(buildIcon("transfer"), "Transfer", e -> showTransferDialog());
        VBox voucherBtn = createActionButton(buildIcon("voucher"), "Voucher", e -> showVoucherDialog());
        VBox payBtn = createActionButton(buildIcon("pay"), "Pay", e -> showPayOnlineDialog());
        VBox vCardBtn = createActionButton(buildIcon("vcard"), "V-Card", e -> showVCardDialog());
        VBox pointsBtn = createActionButton(buildIcon("points"), "Points", e -> showRedeemPointsDialog());
        VBox billsBtn = createActionButton(buildIcon("bills"), "Bills", e -> showBillsDialog());
        VBox loadBtn = createActionButton(buildIcon("load"), "Load", e -> showBuyLoadDialog());

        final java.util.List<VBox> btns = java.util.Arrays.asList(
            depositBtn, withdrawBtn, transferBtn, voucherBtn,
            payBtn, vCardBtn, pointsBtn, billsBtn, loadBtn
        );

        // Collapsible behavior: show first 3 buttons + Show more tile initially
        final boolean[] expanded = new boolean[] { false };

        final Runnable[] rebuild = new Runnable[1];

        VBox showMoreBox = createActionButton(buildIcon("more"), "Show more", e -> {});
        // Wire the inner button to toggle expanded state and update its label/icon
        Button showMoreInnerBtn = (Button) showMoreBox.getChildren().get(0);
        Label showMoreInnerLabel = (Label) showMoreBox.getChildren().get(1);
        showMoreInnerBtn.setOnAction(ev -> {
            expanded[0] = !expanded[0];
            if (expanded[0]) {
                showMoreInnerBtn.setText("▲");
                showMoreInnerLabel.setText("Show less");
            } else {
                showMoreInnerBtn.setText("⋯");
                showMoreInnerLabel.setText("Show more");
            }
            rebuild[0].run();
        });

        // Additional "Show less" tile placed on a new row when expanded
        VBox showLessBox = createActionButton("▲", "Show less", e -> {});
        Button showLessInnerBtn = (Button) showLessBox.getChildren().get(0);
        Label showLessInnerLabel = (Label) showLessBox.getChildren().get(1);
        showLessInnerBtn.setOnAction(ev -> {
            // Collapse back to compact view
            expanded[0] = false;
            // Reset the showMore tile visuals
                showMoreInnerBtn.setText("⋯");
            showMoreInnerLabel.setText("Show more");
            rebuild[0].run();
        });

        rebuild[0] = () -> {
            grid.getChildren().clear();
            if (!expanded[0]) {
                // first row: 3 buttons + show more
                for (int i = 0; i < 3 && i < btns.size(); i++) {
                    VBox b = btns.get(i);
                    b.setMaxWidth(Double.MAX_VALUE);
                    GridPane.setHgrow(b, Priority.ALWAYS);
                    grid.add(b, i, 0);
                    GridPane.setMargin(b, new Insets(6));
                }
                showMoreBox.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(showMoreBox, Priority.ALWAYS);
                grid.add(showMoreBox, 3, 0);
                GridPane.setMargin(showMoreBox, new Insets(6));
            } else {
                // expanded: place all buttons in 4 columns x rows as needed
                for (int i = 0; i < btns.size(); i++) {
                    VBox b = btns.get(i);
                    b.setMaxWidth(Double.MAX_VALUE);
                    GridPane.setHgrow(b, Priority.ALWAYS);
                    int row = i / 4;
                    int col = i % 4;
                    grid.add(b, col, row);
                    GridPane.setMargin(b, new Insets(6));
                }
                // Add the Show less tile: column 3 (aligned with Load) on row 2 (under Bills)
                int showLessRow = 2;
                GridPane.setHgrow(showLessBox, Priority.ALWAYS);
                showLessBox.setMaxWidth(Double.MAX_VALUE);
                // Place at column 3 to align with Load, row 2 to be under Bills
                grid.add(showLessBox, 3, showLessRow);
                GridPane.setMargin(showLessBox, new Insets(6));
            }
        };

        // initial layout
        rebuild[0].run();

        // Allow the grid to stretch horizontally
        grid.setMaxWidth(Double.MAX_VALUE);
        // Place the grid inside a card-style container with border and shadow
        VBox card = new VBox(grid);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, rgba(59,130,246,0.03), white); " +
            "-fx-background-radius: 14; " +
            "-fx-border-radius: 14; " +
            "-fx-border-color: rgba(59,130,246,0.18); " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(2,6,23,0.06), 14, 0, 0, 6);"
        );
        card.setMaxWidth(Double.MAX_VALUE);

        HBox wrapper = new HBox(card);
        wrapper.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);
        return wrapper;
    }
    
    private VBox createActionButton(String icon, String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        VBox btn = new VBox(8);
        btn.setAlignment(Pos.CENTER);
        btn.setPrefWidth(80);
        
        Button iconBtn = new Button(icon);
        iconBtn.setFont(Font.font(24));
        iconBtn.setStyle(
            "-fx-background-color: #FFD700; -fx-text-fill: white; " +
            "-fx-background-radius: 12; -fx-border-color: #FFD700; " +
            "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );
        iconBtn.setPrefSize(60, 60);
        iconBtn.setOnAction(handler);
        
        // Hover effect: slightly darker blue on hover
        iconBtn.setOnMouseEntered(e -> iconBtn.setStyle(
            "-fx-background-color: #FFA500; -fx-text-fill: white; " +
            "-fx-background-radius: 12; -fx-border-color: #FFA500; " +
            "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 8, 0, 0, 3);"
        ));
        iconBtn.setOnMouseExited(e -> iconBtn.setStyle(
            "-fx-background-color: #FFD700; -fx-text-fill: white; " +
            "-fx-background-radius: 12; -fx-border-color: #FFD700; " +
            "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        ));
        
        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 13));
        textLabel.setStyle("-fx-text-fill: #000000;");
        
        btn.getChildren().addAll(iconBtn, textLabel);
        return btn;
    }

    // Overload: accept a Node graphic instead of text icon
    private VBox createActionButton(javafx.scene.Node iconGraphic, String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        VBox btn = new VBox(8);
        btn.setAlignment(Pos.CENTER);
        btn.setPrefWidth(80);

        Button iconBtn = new Button();
        iconBtn.setGraphic(iconGraphic);
        iconBtn.setStyle(
            "-fx-background-color: #FFD700; -fx-text-fill: white; " +
            "-fx-background-radius: 12; -fx-border-color: #FFD700; " +
            "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );
        iconBtn.setPrefSize(60, 60);
        iconBtn.setOnAction(handler);

        // Hover effect
        iconBtn.setOnMouseEntered(e -> iconBtn.setStyle(
            "-fx-background-color: #FFA500; -fx-text-fill: white; " +
            "-fx-background-radius: 12; -fx-border-color: #FFA500; " +
            "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 8, 0, 0, 3);"
        ));
        iconBtn.setOnMouseExited(e -> iconBtn.setStyle(
            "-fx-background-color: #FFD700; -fx-text-fill: white; " +
            "-fx-background-radius: 12; -fx-border-color: #FFD700; " +
            "-fx-border-width: 1.5; -fx-border-radius: 12; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        ));

        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 13));
        textLabel.setStyle("-fx-text-fill: #000000;");

        btn.getChildren().addAll(iconBtn, textLabel);
        return btn;
    }

    // Build simple vector icons (white shapes) for use as button graphics
    private javafx.scene.Node buildIcon(String key) {
        // produce clearer, slightly larger white glyphs for action tiles
        switch (key) {
            case "deposit": {
                // Stack of coins with plus sign
                Circle coin1 = new Circle(9, 12, 5);
                coin1.setFill(Color.WHITE);
                Circle coin2 = new Circle(9, 10, 5);
                coin2.setFill(Color.web("#E5E7EB"));
                Circle coin3 = new Circle(9, 8, 5);
                coin3.setFill(Color.WHITE);
                // Plus sign on top coin
                Rectangle hLine = new Rectangle(7, 7.5, 4, 1);
                hLine.setFill(Color.web("#FFD700"));
                Rectangle vLine = new Rectangle(8.5, 6, 1, 4);
                vLine.setFill(Color.web("#FFD700"));
                Group g = new Group(coin1, coin2, coin3, hLine, vLine);
                return createIconWrapper(g);
            }
            case "withdraw": {
                // Modern wallet with money coming out
                Rectangle wallet = new Rectangle(3, 6, 12, 8);
                wallet.setArcWidth(3); wallet.setArcHeight(3);
                wallet.setFill(Color.WHITE);
                Rectangle fold = new Rectangle(3, 9, 12, 1);
                fold.setFill(Color.web("#FFD700"));
                // Money bills sticking out
                Rectangle bill1 = new Rectangle(13, 4, 3, 6);
                bill1.setFill(Color.web("#10B981"));
                Rectangle bill2 = new Rectangle(14, 3, 3, 6);
                bill2.setFill(Color.web("#059669"));
                Group g = new Group(wallet, fold, bill1, bill2);
                return createIconWrapper(g);
            }
            case "transfer": {
                // Curved transfer arrows
                // Top curved arrow (send)
                Arc arc1 = new Arc(9, 6, 5, 3, 0, 180);
                arc1.setType(ArcType.OPEN);
                arc1.setFill(Color.TRANSPARENT);
                arc1.setStroke(Color.WHITE);
                arc1.setStrokeWidth(2);
                Polygon arrow1 = new Polygon(12, 4, 15, 6, 12, 8);
                arrow1.setFill(Color.WHITE);
                // Bottom curved arrow (receive)
                Arc arc2 = new Arc(9, 12, 5, 3, 180, 180);
                arc2.setType(ArcType.OPEN);
                arc2.setFill(Color.TRANSPARENT);
                arc2.setStroke(Color.web("#E5E7EB"));
                arc2.setStrokeWidth(2);
                Polygon arrow2 = new Polygon(6, 14, 3, 12, 6, 10);
                arrow2.setFill(Color.web("#E5E7EB"));
                Group g = new Group(arc1, arrow1, arc2, arrow2);
                return createIconWrapper(g);
            }
            case "voucher": {
                // Ticket-style voucher with perforations
                Rectangle ticket = new Rectangle(2, 4, 14, 10);
                ticket.setFill(Color.WHITE);
                // Perforated edges
                Circle perf1 = new Circle(2, 9, 1);
                perf1.setFill(Color.web("#FFD700"));
                Circle perf2 = new Circle(16, 9, 1);
                perf2.setFill(Color.web("#FFD700"));
                // Decorative lines
                Rectangle line1 = new Rectangle(5, 7, 8, 1);
                line1.setFill(Color.web("#FFD700"));
                Rectangle line2 = new Rectangle(5, 10, 6, 1);
                line2.setFill(Color.web("#FFD700"));
                Group g = new Group(ticket, perf1, perf2, line1, line2);
                return createIconWrapper(g);
            }
            case "pay": {
                // Use the Peso glyph for clear recognition
                Label peso = new Label("₱");
                peso.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800;");
                StackPane pWrap = createIconWrapper(peso);
                return pWrap;
            }
            case "points": {
                // Sparkle/star design
                Polygon mainStar = new Polygon(9,2, 10.5,6.5, 15,6.5, 11.5,9.5, 12.8,14, 9,11.5, 5.2,14, 6.5,9.5, 3,6.5, 7.5,6.5);
                mainStar.setFill(Color.WHITE);
                // Small sparkles around main star
                Circle sparkle1 = new Circle(4, 4, 0.8);
                sparkle1.setFill(Color.web("#E5E7EB"));
                Circle sparkle2 = new Circle(14, 3, 0.8);
                sparkle2.setFill(Color.web("#E5E7EB"));
                Circle sparkle3 = new Circle(15, 13, 0.8);
                sparkle3.setFill(Color.web("#E5E7EB"));
                Group g = new Group(mainStar, sparkle1, sparkle2, sparkle3);
                return createIconWrapper(g);
            }
            case "bills": {
                // Stack of documents/bills
                Rectangle doc1 = new Rectangle(5, 5, 10, 12);
                doc1.setArcWidth(2); doc1.setArcHeight(2);
                doc1.setFill(Color.web("#E5E7EB"));
                Rectangle doc2 = new Rectangle(4, 4, 10, 12);
                doc2.setArcWidth(2); doc2.setArcHeight(2);
                doc2.setFill(Color.web("#F3F4F6"));
                Rectangle doc3 = new Rectangle(3, 3, 10, 12);
                doc3.setArcWidth(2); doc3.setArcHeight(2);
                doc3.setFill(Color.WHITE);
                // Text lines on top document
                Rectangle line1 = new Rectangle(6, 6, 6, 1);
                line1.setFill(Color.web("#FFD700"));
                Rectangle line2 = new Rectangle(6, 8, 5, 1);
                line2.setFill(Color.web("#FFD700"));
                Rectangle line3 = new Rectangle(6, 10, 4, 1);
                line3.setFill(Color.web("#6B7280"));
                Group g = new Group(doc1, doc2, doc3, line1, line2, line3);
                return createIconWrapper(g);
            }
            case "load": {
                // Modern smartphone with signal bars
                Rectangle phone = new Rectangle(5, 2, 8, 14);
                phone.setArcWidth(4); phone.setArcHeight(4);
                phone.setFill(Color.WHITE);
                Rectangle screen = new Rectangle(6.5, 4, 5, 10);
                screen.setArcWidth(2); screen.setArcHeight(2);
                screen.setFill(Color.web("#1F2937"));
                // Signal bars on screen
                Rectangle bar1 = new Rectangle(7.5, 11, 1, 2);
                bar1.setFill(Color.web("#10B981"));
                Rectangle bar2 = new Rectangle(8.8, 10, 1, 3);
                bar2.setFill(Color.web("#10B981"));
                Rectangle bar3 = new Rectangle(10.1, 9, 1, 4);
                bar3.setFill(Color.web("#10B981"));
                // Home button
                Circle homeBtn = new Circle(9, 15.5, 0.8);
                homeBtn.setFill(Color.web("#6B7280"));
                Group g = new Group(phone, screen, bar1, bar2, bar3, homeBtn);
                return createIconWrapper(g);
            }
            case "vcard": {
                // Credit card icon
                Rectangle card = new Rectangle(3, 5, 12, 10);
                card.setArcWidth(2); card.setArcHeight(2);
                card.setFill(Color.WHITE);
                // Chip
                Rectangle chip = new Rectangle(5, 7, 3, 3);
                chip.setFill(Color.web("#FFD700"));
                // Magnetic stripe
                Rectangle stripe = new Rectangle(3, 13, 12, 1.5);
                stripe.setFill(Color.web("#1F2937"));
                Group g = new Group(card, chip, stripe);
                return createIconWrapper(g);
            }
            case "more": {
                Circle d1 = new Circle(5,9,1.6); d1.setFill(Color.WHITE);
                Circle d2 = new Circle(9,9,1.6); d2.setFill(Color.WHITE);
                Circle d3 = new Circle(13,9,1.6); d3.setFill(Color.WHITE);
                Group g = new Group(d1,d2,d3);
                return createIconWrapper(g);
            }
            default: {
                Rectangle sq = new Rectangle(5,5,8,8);
                sq.setFill(Color.WHITE);
                return createIconWrapper(sq);
            }
        }
    }

    // Wrap an icon node in a fixed-size holder and scale it slightly so it appears larger on the tile
    private StackPane createIconWrapper(javafx.scene.Node inner) {
        StackPane holder = new StackPane(inner);
        holder.setPrefSize(28, 28);
        holder.setMinSize(28, 28);
        holder.setMaxSize(28, 28);
        // scale shapes to better fit the blue button
        inner.setScaleX(1.4);
        inner.setScaleY(1.4);
        return holder;
    }

    // Create a circular badge background and place the icon centered within it
    private StackPane createBadgeIcon(javafx.scene.Node icon, Color bgColor, int size) {
        Circle bg = new Circle(size / 2.0);
        bg.setFill(bgColor);
        // ensure icon is sized appropriately
        icon.setScaleX(1.2);
        icon.setScaleY(1.2);
        StackPane holder = new StackPane(bg, icon);
        holder.setPrefSize(size, size);
        holder.setMinSize(size, size);
        holder.setMaxSize(size, size);
        return holder;
    }

    // Create a classic coin-style badge (bronze/gold) with a center glyph
    private StackPane createCoinBadge(String glyph, Color coinColor, int size) {
        Circle outer = new Circle(size / 2.0);
        outer.setFill(coinColor);
        // inner ring to give depth
        Circle inner = new Circle(size / 2.0 - 5);
        inner.setFill(coinColor.darker().interpolate(Color.web("#FFFFFF"), 0.08));
        Label center = new Label(glyph);
        center.setFont(Font.font("System", FontWeight.BOLD, Math.max(12, size / 3)));
        center.setStyle("-fx-text-fill: white;");
        StackPane holder = new StackPane(outer, inner, center);
        holder.setPrefSize(size, size);
        holder.setMinSize(size, size);
        holder.setMaxSize(size, size);
        return holder;
    }

    // Build simple nav icons (tintable shapes)
    private javafx.scene.Node buildNavIcon(String key) {
        switch (key) {
            case "home": {
                // Simplified filled house silhouette for clarity
                Polygon house = new Polygon(9,3, 3,9, 5,9, 5,14, 13,14, 13,9, 15,9);
                house.setFill(Color.web("#94A3B8"));
                return house;
            }
            case "wallet": {
                // Simplified card icon (rounded rectangle)
                Rectangle card = new Rectangle(3,5,12,8);
                card.setArcWidth(3); card.setArcHeight(3);
                card.setFill(Color.web("#94A3B8"));
                return card;
            }
            case "history": {
                // Simple bar chart icon
                Rectangle b1 = new Rectangle(4,9,2,6); b1.setArcWidth(1); b1.setArcHeight(1); b1.setFill(Color.web("#94A3B8"));
                Rectangle b2 = new Rectangle(8,6,2,9); b2.setArcWidth(1); b2.setArcHeight(1); b2.setFill(Color.web("#94A3B8"));
                Rectangle b3 = new Rectangle(12,3,2,12); b3.setArcWidth(1); b3.setArcHeight(1); b3.setFill(Color.web("#94A3B8"));
                Group g = new Group(b1, b2, b3);
                return g;
            }
            case "settings": {
                // Gear-like icon: central circle with teeth (small circles) around it
                Circle gear = new Circle(9,9,6);
                gear.setFill(Color.web("#94A3B8"));
                Circle hole = new Circle(9,9,2.2);
                hole.setFill(Color.web("#F8FAFC"));
                Circle t1 = new Circle(15,9,1.2); t1.setFill(Color.web("#94A3B8"));
                Circle t2 = new Circle(13.2,4.8,1.2); t2.setFill(Color.web("#94A3B8"));
                Circle t3 = new Circle(9,3,1.2); t3.setFill(Color.web("#94A3B8"));
                Circle t4 = new Circle(4.8,4.8,1.2); t4.setFill(Color.web("#94A3B8"));
                Circle t5 = new Circle(3,9,1.2); t5.setFill(Color.web("#94A3B8"));
                Circle t6 = new Circle(4.8,13.2,1.2); t6.setFill(Color.web("#94A3B8"));
                Circle t7 = new Circle(9,15,1.2); t7.setFill(Color.web("#94A3B8"));
                Circle t8 = new Circle(13.2,13.2,1.2); t8.setFill(Color.web("#94A3B8"));
                Group g = new Group(gear, t1, t2, t3, t4, t5, t6, t7, t8, hole);
                return g;
            }
            default: {
                Rectangle sq = new Rectangle(4,4,10,10); sq.setFill(Color.web("#94A3B8"));
                return sq;
            }
        }
    }

    // Build a small white avatar glyph (head + shoulders)
    private javafx.scene.Node buildAvatarGraphic() {
        Circle head = new Circle(0, -4, 5);
        head.setFill(Color.WHITE);
        Rectangle shoulders = new Rectangle(-8,4,16,8);
        shoulders.setArcWidth(8); shoulders.setArcHeight(8);
        shoulders.setFill(Color.WHITE);
        Group g = new Group(head, shoulders);
        return g;
    }

    // Recursively tint shapes inside an icon node
    private void setIconColor(javafx.scene.Node node, Color color) {
        if (node instanceof Shape) {
            ((Shape) node).setFill(color);
            return;
        }
        if (node instanceof Group) {
            for (javafx.scene.Node child : ((Group) node).getChildren()) setIconColor(child, color);
            return;
        }
        if (node instanceof Label) {
            ((Label) node).setStyle("-fx-text-fill: " + (color.equals(Color.web("#FFD700")) ? "#FFD700" : "#94A3B8") + ";");
        }
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
            "-fx-border-color: rgba(37,99,235,0.12); -fx-border-width: 1; -fx-border-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        infoCard.getChildren().addAll(
            createInfoRow("Username", user.getUsername()),
            createDivider(),
            createInfoRow("Mobile", user.getMobile()),
            createDivider(),
            createInfoRow("Loyalty Tier", user.getLoyaltyTier()),
            createDivider(),
            // Rank title displayed in account information
            createInfoRow("Rank Title", user.getRank()),
            createDivider(),
            // Rank preview (visual line showing Bronze -> Platinum with current highlighted)
            createRankPreview(user.getRank()),
            createDivider(),
            createInfoRow("Current Balance", String.format("₱ %.2f", user.getBalance()))
        );

        // Make the account info card responsive but constrained to a reasonable width
        infoCard.setMinWidth(300);
        infoCard.setMaxWidth(440);
        if (scene != null) {
            infoCard.prefWidthProperty().bind(scene.widthProperty().subtract(40));
        }

        HBox cardWrapper = new HBox(infoCard);
        cardWrapper.setAlignment(Pos.CENTER);
        HBox.setHgrow(infoCard, Priority.ALWAYS);

        // Recent Transactions Section
        Label transTitle = new Label("Recent Transactions");
        transTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        transTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Header with Clear button
        HBox transHeaderBox = new HBox(10);
        transHeaderBox.setAlignment(Pos.CENTER_LEFT);
        transHeaderBox.setPadding(new Insets(0));
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        Button clearTransButton = new Button("🗑️ Clear History");
        clearTransButton.setStyle(
            "-fx-padding: 8 14; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #EF4444; " +
            "-fx-text-fill: white; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        );
        clearTransButton.setOnMouseEntered(e -> clearTransButton.setStyle(
            "-fx-padding: 8 14; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        ));
        clearTransButton.setOnMouseExited(e -> clearTransButton.setStyle(
            "-fx-padding: 8 14; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        ));
        
        clearTransButton.setOnAction(e -> {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Clear Transaction History");
            confirmDialog.setHeaderText("Delete All Transactions?");
            confirmDialog.setContentText("This action will permanently delete all your transaction history.\nThis cannot be undone.");
            confirmDialog.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                azureApp.getFileManager().clearTransactionHistory(currentUser);
                showAlert("Success", "Transaction history cleared successfully", Alert.AlertType.INFORMATION);
                refreshMainScreen();
            }
        });
        
        transHeaderBox.getChildren().addAll(transTitle, headerSpacer, clearTransButton);
        
        VBox transContent = new VBox(8);
        transContent.setPadding(new Insets(15));
        transContent.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12; " +
            "-fx-border-color: rgba(37,99,235,0.12); -fx-border-width: 1; -fx-border-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        // Get last 20 transactions
        java.util.List<String> transactions = azureApp.getFileManager().getTransactionHistory(currentUser);
        int totalLimit = Math.min(20, transactions.size());
        
        if (totalLimit == 0) {
            Label noTrans = new Label("No transactions yet");
            noTrans.setFont(Font.font("System", 13));
            noTrans.setStyle("-fx-text-fill: #94A3B8;");
            transContent.getChildren().add(noTrans);
        } else {
            String lastDate = null;
            for (int i = 0; i < totalLimit; i++) {
                String trans = transactions.get(i);
                
                // Extract date from transaction (format: "YYYY-MM-DD")
                String[] parts = trans.split("T");
                String currentDate = parts.length > 0 ? parts[0] : "";
                
                // Add date separator if date changed
                if (!currentDate.equals(lastDate)) {
                    if (lastDate != null) {
                        transContent.getChildren().add(createDivider());
                    }
                    Label dateLabel = new Label(formatDateLabel(currentDate));
                    dateLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
                    dateLabel.setStyle("-fx-text-fill: #64748B; -fx-padding: 8 0 4 0;");
                    transContent.getChildren().add(dateLabel);
                    lastDate = currentDate;
                }
                
                HBox transRow = createTransactionRow(trans);
                transContent.getChildren().add(transRow);
                
                // Add divider between rows except for the last one
                if (i < totalLimit - 1) {
                    transContent.getChildren().add(createDivider());
                }
            }
        }
        
        // Create scrollable container with fixed height for 5 transactions visible
        ScrollPane transScroll = new ScrollPane(transContent);
        transScroll.setFitToWidth(true);
        transScroll.setStyle("-fx-control-inner-background: white;");
        transScroll.setPrefHeight(320); // Height for ~5 transactions
        transScroll.setMinHeight(320);
        transScroll.setMaxHeight(320);
        transScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        transScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Style the scroll bar
        transScroll.setStyle(
            "-fx-control-inner-background: white; " +
            "-fx-padding: 0; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;"
        );
        
        // Add sliding up animation (auto-scroll loop)
        if (totalLimit > 0) {
            Timeline scrollTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0), e -> transScroll.setVvalue(0)),
                new KeyFrame(Duration.seconds(4), e -> transScroll.setVvalue(0)),
                new KeyFrame(Duration.seconds(8), e -> transScroll.setVvalue(1)),
                new KeyFrame(Duration.seconds(12), e -> transScroll.setVvalue(1)),
                new KeyFrame(Duration.seconds(13), e -> transScroll.setVvalue(0))
            );
            scrollTimeline.setCycleCount(Timeline.INDEFINITE);
            scrollTimeline.play();
        }
        
        HBox transWrapper = new HBox(transScroll);
        transWrapper.setAlignment(Pos.CENTER);
        transWrapper.setMinWidth(300);
        transWrapper.setMaxWidth(440);
        if (scene != null) {
            transWrapper.prefWidthProperty().bind(scene.widthProperty().subtract(40));
        }
        HBox.setHgrow(transScroll, Priority.ALWAYS);

        section.getChildren().addAll(title, cardWrapper, transHeaderBox, transWrapper);
        return section;
    }
    
    private HBox createTransactionRow(String transaction) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 0, 5, 0));
        
        // Parse transaction: format is "TIMESTAMP - USERNAME: TYPE - AMOUNT"
        // Example: "2025-11-23T11:08:48.183924500 - testuser18155: Deposit - PHP 5,000.00"
        try {
            // Split by " - " to get timestamp and rest
            String[] mainParts = transaction.split(" - ", 2);
            if (mainParts.length < 2) return row; // Invalid format
            
            String timestamp = mainParts[0];
            String rest = mainParts[1];
            
            // Split rest by ": " to get username and transaction details
            String[] detailParts = rest.split(": ", 2);
            if (detailParts.length < 2) return row;
            
            String transactionDetails = detailParts[1];
            
            // Extract type and amount from transactionDetails
            // Format: "Deposit - PHP 5,000.00" or "Withdraw - PHP 1,000.00"
            String type = "Transaction";
            String amount = "";
            
            if (transactionDetails.contains("Deposit")) {
                type = "Deposit";
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Withdraw")) {
                type = "Withdraw";
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Send")) {
                type = "Send";
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Receive")) {
                type = "Receive";
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Withdraw to")) {
                type = "Withdraw";
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            }
            
            // Transaction type icon
            Label icon = new Label();
            switch (type) {
                case "Deposit": icon.setText("➕"); break;
                case "Withdraw": icon.setText("➖"); break;
                case "Send": icon.setText("➡️"); break;
                case "Receive": icon.setText("⬅️"); break;
                default: icon.setText("📝");
            }
            icon.setFont(Font.font(14));
            
            // Transaction details
            VBox detailBox = new VBox(2);
            Label typeLabel = new Label(type);
            typeLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            typeLabel.setStyle("-fx-text-fill: #1E293B;");
            
            Label timeLabel = new Label(timestamp);
            timeLabel.setFont(Font.font("System", 10));
            timeLabel.setStyle("-fx-text-fill: #94A3B8;");
            
            detailBox.getChildren().addAll(typeLabel, timeLabel);
            
            // Amount
            Label amountLabel = new Label(amount);
            amountLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            amountLabel.setStyle("-fx-text-fill: #10B981;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            row.getChildren().addAll(icon, detailBox, spacer, amountLabel);
        } catch (Exception e) {
            // Fallback if parsing fails
            Label fallback = new Label(transaction);
            fallback.setFont(Font.font("System", 10));
            fallback.setStyle("-fx-text-fill: #94A3B8;");
            row.getChildren().add(fallback);
        }
        
        return row;
    }
    
    private String formatDateLabel(String dateStr) {
        // Input format: "2025-11-23"
        // Output: "Today", "Yesterday", or "Nov 23, 2025"
        try {
            java.time.LocalDate transDate = java.time.LocalDate.parse(dateStr);
            java.time.LocalDate today = java.time.LocalDate.now();
            
            if (transDate.equals(today)) {
                return "Today";
            } else if (transDate.equals(today.minusDays(1))) {
                return "Yesterday";
            } else {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy");
                return transDate.format(formatter);
            }
        } catch (Exception e) {
            return dateStr;
        }
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

    private HBox createRankPreview(String tier) {
        HBox row = new HBox(12);
        // center the rank badges within the account info card
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setPadding(new Insets(6, 0, 6, 0));

        String[] ranks = new String[] {"Bronze", "Silver", "Gold", "Platinum"};
        for (String r : ranks) {
            boolean active = r.equalsIgnoreCase(tier);

            // colors for each tier
            Color fill;
            switch (r.toLowerCase()) {
                case "bronze": fill = Color.web("#CD7F32"); break;
                case "silver": fill = Color.web("#C0C0C0"); break;
                case "gold": fill = Color.web("#D4AF37"); break;
                case "platinum": fill = Color.web("#86EFAC"); break;
                default: fill = Color.web("#CBD5E1");
            }

            double radius = active ? 10 : 8;
            Circle dot = new Circle(radius);
            dot.setFill(fill);

            StackPane badge;
            if (active) {
                Circle ring = new Circle(radius + 6);
                ring.setFill(Color.TRANSPARENT);
                ring.setStroke(Color.web("#FFD700"));
                ring.setStrokeWidth(2);
                badge = new StackPane(ring, dot);
                badge.setPadding(new Insets(6));
            } else {
                badge = new StackPane(dot);
                badge.setPadding(new Insets(6));
            }

            Label lbl = new Label(r);
            lbl.setFont(Font.font("System", 11));
            lbl.setStyle("-fx-text-fill: #475569;");

            VBox cell = new VBox(6, badge, lbl);
            cell.setAlignment(Pos.CENTER);
            row.getChildren().add(cell);
        }

        return row;
    }

    // Card helpers moved to azurewallet.utils.CardUtil
    
    private HBox createBottomNav() {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER);
        // Reduced padding for mobile-friendly footprint
        nav.setPadding(new Insets(10, 12, 12, 12));
        nav.setStyle("-fx-background-color: white; -fx-border-color: rgba(226,232,240,0.9); -fx-border-width: 1 0 0 0; -fx-effect: dropshadow(three-pass-box, rgba(2,6,23,0.03), 6, 0, 0, -2);");
        
        // Create nav buttons (handlers attached below so we can animate active state)
        navHomeBtn = createNavButton(buildNavIcon("home"), "Home", true);
        navWalletBtn = createNavButton(buildNavIcon("wallet"), "Wallet", false);
        navHistoryBtn = createNavButton(buildNavIcon("history"), "History", false);
        navSettingsBtn = createNavButton(buildNavIcon("settings"), "Settings", false);

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
        iconLabel.setStyle("-fx-text-fill: " + (active ? "#FFD700" : "#94A3B8") + ";");

        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 11));
        textLabel.setStyle("-fx-text-fill: " + (active ? "#FFD700" : "#94A3B8") + ";");

        // Indicator bar shown when active (animated)
        Region indicator = new Region();
        indicator.setPrefHeight(3);
        indicator.setMaxWidth(36);
        indicator.setStyle("-fx-background-color: #FFD700; -fx-background-radius: 2;");
        indicator.setOpacity(active ? 1.0 : 0.0);

        btn.getChildren().addAll(iconLabel, textLabel, indicator);
        btn.setUserData(indicator);
        // hover behavior: temporarily show active tint while hovered
        btn.setOnMouseEntered(e -> {
            iconLabel.setStyle("-fx-text-fill: #FFD700;");
            textLabel.setStyle("-fx-text-fill: #FFD700;");
        });
        btn.setOnMouseExited(e -> {
            Region ind = (Region) btn.getUserData();
            String color = ind.getOpacity() > 0.5 ? "#FFD700" : "#94A3B8";
            iconLabel.setStyle("-fx-text-fill: " + color + ";");
            textLabel.setStyle("-fx-text-fill: " + color + ";");
        });
        return btn;
    }

    // Overload to accept a graphic Node for the icon (tintable)
    private VBox createNavButton(javafx.scene.Node iconGraphic, String label, boolean active) {
        VBox btn = new VBox(4);
        btn.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-cursor: hand;");

        // Larger background circle behind the icon to act as tappable target
        Circle bg = new Circle(24);
        bg.setFill(active ? Color.web("#FFFACD") : Color.TRANSPARENT);
        bg.setStroke(active ? Color.web("#FFD700") : Color.TRANSPARENT);
        bg.setStrokeWidth(active ? 1.5 : 0);

        // tint and scale the graphic depending on active state (make icons more prominent)
        setIconColor(iconGraphic, active ? Color.web("#FFD700") : Color.web("#94A3B8"));
        iconGraphic.setScaleX(1.5);
        iconGraphic.setScaleY(1.5);
        StackPane iconHolder = new StackPane(bg, iconGraphic);
        iconHolder.setPrefSize(56, 56);
        iconHolder.setMinSize(56, 56);
        iconHolder.setMaxSize(56, 56);
        iconHolder.setUserData(bg); // store bg for active toggling

        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 12));
        textLabel.setStyle("-fx-text-fill: " + (active ? "#FFD700" : "#94A3B8") + ";");

        // small rounded indicator (becomes visible when active)
        Region indicator = new Region();
        indicator.setPrefHeight(5);
        indicator.setMaxWidth(36);
        indicator.setStyle("-fx-background-color: #FFD700; -fx-background-radius: 3;");
        indicator.setOpacity(active ? 1.0 : 0.0);

        btn.getChildren().addAll(iconHolder, textLabel, indicator);
        btn.setUserData(indicator);
        // hover behavior: tint icon and background like active while hovered
        btn.setOnMouseEntered(e -> {
            // tint graphic and bg
            setIconColor(iconGraphic, Color.web("#FFD700"));
            Object ud = iconHolder.getUserData();
            if (ud instanceof Circle) {
                Circle bgc = (Circle) ud;
                bgc.setFill(Color.web("#FFFACD"));
                bgc.setStroke(Color.web("#FFD700"));
            }
            textLabel.setStyle("-fx-text-fill: #FFD700;");
        });
        btn.setOnMouseExited(e -> {
            Region ind = (Region) btn.getUserData();
            boolean isActive = ind.getOpacity() > 0.5;
            Color tint = isActive ? Color.web("#FFD700") : Color.web("#94A3B8");
            setIconColor(iconGraphic, tint);
            Object ud = iconHolder.getUserData();
            if (ud instanceof Circle) {
                Circle bgc = (Circle) ud;
                bgc.setFill(isActive ? Color.web("#FFFACD") : Color.TRANSPARENT);
                bgc.setStroke(isActive ? Color.web("#FFD700") : Color.TRANSPARENT);
            }
            textLabel.setStyle("-fx-text-fill: " + (isActive ? "#FFD700" : "#94A3B8") + ";");
        });
        return btn;
    }

    // Animate and mark the active nav button
    private void setActiveNav(VBox activeBtn) {
        List<VBox> all = List.of(navHomeBtn, navWalletBtn, navHistoryBtn, navSettingsBtn);
        for (VBox b : all) {
            if (b == null) continue;
            javafx.scene.Node iconNode = b.getChildren().get(0);
            Label textLabel = (Label) b.getChildren().get(1);
            Region indicator = (Region) b.getUserData();
            boolean isActive = (b == activeBtn);
            String color = isActive ? "#FFD700" : "#FFD700";
            // Update icon color for graphic nodes or label text
            if (iconNode instanceof Label) {
                ((Label) iconNode).setStyle("-fx-text-fill: " + color + ";");
            } else {
                // If wrapped in a holder (StackPane) use its child and update background circle if present
                if (iconNode instanceof StackPane && ((StackPane) iconNode).getChildren().size() > 0) {
                    javafx.scene.Node child = ((StackPane) iconNode).getChildren().get(0);
                    setIconColor(child, Color.web(color));
                    // If the StackPane has a bg Circle stored as userData, toggle its fill
                    Object ud = ((StackPane) iconNode).getUserData();
                    if (ud instanceof Circle) {
                        Circle bg = (Circle) ud;
                        bg.setFill(isActive ? Color.web("#FFFACD") : Color.TRANSPARENT);
                        bg.setStroke(isActive ? Color.web("#FFD700") : Color.TRANSPARENT);
                    }
                } else {
                    setIconColor(iconNode, Color.web(color));
                }
            }
            textLabel.setStyle("-fx-text-fill: " + color + ";");

            FadeTransition ft = new FadeTransition(Duration.seconds(0.22), indicator);
            ft.setToValue(isActive ? 1.0 : 0.0);
            ft.playFromStart();
        }
    }
    
    // ==================== TRANSACTION DIALOGS ====================
    
    private void showDepositDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Deposit Money");
        dialog.setHeaderText("Enter deposit details");
        // Build a more user-friendly deposit dialog
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));
        // Card-like background for mobile aesthetic
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");
        // Make two logical columns: left labels (fixed-ish) and right inputs (flexible)
        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setMinWidth(120);
        leftCol.setPrefWidth(140);
        leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        UserAccount user = azureApp.getUser(currentUser);

        // Responsive base font size (relative to scene width)
        double baseFont = 14;
        if (scene != null) {
            baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        }

        Label balanceLabel = new Label(String.format("Balance: ₱%,.2f", user.getBalance()));
        balanceLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, baseFont + 1));
        balanceLabel.setStyle("-fx-text-fill: #111827;");
        Label limitLabel = new Label(String.format("Deposit limit: ₱%,.2f", user.getDepositLimit()));
        // keep the left-side helper text readable and consistent
        limitLabel.setFont(Font.font("System", baseFont - 1));
        limitLabel.setStyle("-fx-text-fill: #6B7280;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount (e.g. 500)");
        amountField.setPrefColumnCount(10);
        amountField.setStyle(String.format("-fx-font-size: %.0f; -fx-padding: 10 12 10 12; -fx-background-radius: 10; -fx-border-radius:10; -fx-border-color: #E6E9EE;", baseFont + 4));
        amountField.setAlignment(Pos.CENTER_RIGHT);
        // Restrict to numbers and dot
        amountField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldV);
            }
        });

        // Preset deposit amounts arranged as 4 columns x 2 rows for quick taps
        int[] presets = {50, 100, 200, 500, 1000, 2000, 5000, 10000};
        GridPane presetsGrid = new GridPane();
        presetsGrid.setHgap(8);
        presetsGrid.setVgap(8);
        // Ensure columns take equal space so button labels are not truncated
        for (int c = 0; c < 4; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            presetsGrid.getColumnConstraints().add(cc);
        }
        java.util.List<ToggleButton> presetButtons = new java.util.ArrayList<>();
        ToggleGroup tg = new ToggleGroup();
        for (int i = 0; i < presets.length; i++) {
            int value = presets[i];
            ToggleButton b = new ToggleButton("₱" + value);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(Math.max(44, (int)(baseFont * 3)));
            b.setMinWidth(84);
            String presetNormal = String.format("-fx-background-radius:12; -fx-border-color:#E5E7EB; -fx-background-color:white; -fx-font-weight:700; -fx-font-size:%.0f; -fx-text-fill: #0F172A;", baseFont);
            String presetSelected = String.format("-fx-background-radius:12; -fx-border-color:#FFD700; -fx-background-color:#EEF2FF; -fx-font-weight:700; -fx-font-size:%.0f;", baseFont);
            b.setStyle(presetNormal);
            b.setToggleGroup(tg);
            b.setOnAction(e -> {
                if (b.isSelected()) amountField.setText(String.valueOf(value));
                else amountField.clear();
            });
            // change style on selection for clearer feedback
            b.selectedProperty().addListener((obs,oldV,newV) -> {
                if (newV) b.setStyle(presetSelected); else b.setStyle(presetNormal);
            });
            presetButtons.add(b);
            int col = i % 4;
            int row = i / 4;
            presetsGrid.add(b, col, row);
            GridPane.setHgrow(b, Priority.ALWAYS);
        }

        // Source selector as ToggleButtons for clarity
        HBox sourceBox = new HBox(8);
        sourceBox.setAlignment(Pos.CENTER_LEFT);
        String[] sources = new String[]{"GCash","PayMaya","BPI","BDO","Other"};
        ToggleGroup sourceTG = new ToggleGroup();
        for (String s : sources) {
            ToggleButton tb = new ToggleButton(s);
            tb.setToggleGroup(sourceTG);
            String srcNormal = String.format("-fx-background-radius:20; -fx-border-color:#E5E7EB; -fx-background-color:white; -fx-padding:8 12; -fx-font-size:%.0f;", baseFont - 1);
            String srcSelected = String.format("-fx-background-radius:20; -fx-border-color:#FFD700; -fx-background-color:#EFF6FF; -fx-padding:6 10; -fx-font-size:%.0f;", baseFont - 1);
            tb.setStyle(srcNormal);
            tb.setMinWidth(72);
            // visual feedback when selected
            tb.selectedProperty().addListener((obs,oldV,newV) -> {
                if (newV) tb.setStyle(srcSelected); else tb.setStyle(srcNormal);
            });
            sourceBox.getChildren().add(tb);
        }
        // default select first
        if (!sourceBox.getChildren().isEmpty() && sourceBox.getChildren().get(0) instanceof ToggleButton) {
            ToggleButton first = (ToggleButton)sourceBox.getChildren().get(0);
            first.setSelected(true);
            sourceTG.selectToggle(first);
        }

        String reference = "AZR-" + (int)(Math.random() * 900000 + 100000);
        Label refLabel = new Label("Reference: " + reference);
        refLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11;");

        CheckBox receiptBox = new CheckBox("Save receipt to history");
        receiptBox.setSelected(true);

        CheckBox confirmBox = new CheckBox("I confirm I have completed the transfer");

        // Layout
        grid.add(balanceLabel, 0, 0, 2, 1);
        grid.add(limitLabel, 0, 1, 2, 1);
        Label amtLabel = new Label("Amount (₱)");
        amtLabel.setFont(Font.font("System", baseFont));
        amtLabel.setMaxWidth(Double.MAX_VALUE);
        amtLabel.setStyle("-fx-text-fill: #374151;");
        grid.add(amtLabel, 0, 2);
        grid.add(amountField, 1, 2);
        Label quickLabel = new Label("Quick Amounts");
        quickLabel.setFont(Font.font("System", baseFont - 1));
        quickLabel.setMaxWidth(Double.MAX_VALUE);
        quickLabel.setStyle("-fx-text-fill: #374151;");
        grid.add(quickLabel, 0, 3);
        grid.add(presetsGrid, 1, 3);
        Label srcLabel = new Label("Deposit Source");
        srcLabel.setFont(Font.font("System", baseFont - 1));
        srcLabel.setMaxWidth(Double.MAX_VALUE);
        srcLabel.setStyle("-fx-text-fill: #374151;");
        grid.add(srcLabel, 0, 4);
        grid.add(sourceBox, 1, 4);
        grid.add(refLabel, 0, 5, 2, 1);
        grid.add(receiptBox, 0, 6, 2, 1);
        grid.add(confirmBox, 0, 7, 2, 1);

        // Place content inside a VBox wrapper to center and constrain width for mobile feel
        VBox wrapper = new VBox(12, grid);
        wrapper.setPadding(new Insets(14));
        wrapper.setAlignment(Pos.CENTER);

        dialog.getDialogPane().setContent(wrapper);

        ButtonType depositBtnType = new ButtonType("Deposit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(depositBtnType, ButtonType.CANCEL);
        dialog.setResizable(false);
        // Widen slightly so text in buttons is visible on desktop
        dialog.getDialogPane().setPrefWidth(420);

        // Enable/disable deposit button depending on validation
        javafx.scene.Node depositButton = dialog.getDialogPane().lookupButton(depositBtnType);
        depositButton.setDisable(true);
        depositButton.setStyle("-fx-background-color: #FFD700; -fx-text-fill: white; -fx-background-radius: 10;");

        // validation helper
        Runnable validate = () -> {
            boolean ok = false;
            try {
                double v = Double.parseDouble(amountField.getText().trim());
                ok = v > 0 && confirmBox.isSelected();
            } catch (Exception ex) { ok = false; }
            depositButton.setDisable(!ok);
        };

        amountField.textProperty().addListener((o,oldV,newV) -> validate.run());
        confirmBox.selectedProperty().addListener((o,oldV,newV) -> validate.run());

        dialog.showAndWait().ifPresent(response -> {
            if (response == depositBtnType) {
                try {
                    double amt = Double.parseDouble(amountField.getText().trim());
                    String source = "Unknown";
                    ToggleButton sel = (ToggleButton) sourceTG.getSelectedToggle();
                    if (sel != null) source = sel.getText();
                    source = source + " (ref " + reference + ")";
                    if (azureApp.deposit(currentUser, amt, source)) {
                        showAlert("Success", String.format("Successfully deposited ₱%.2f", amt), Alert.AlertType.INFORMATION);
                        refreshMainScreen();
                    } else {
                        showAlert("Error", "Deposit failed. Check amount and deposit limit.", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void showWithdrawDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Withdraw Money");
        dialog.setHeaderText("Enter withdrawal details\n(₱15 fee will be charged)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // left/right column layout and responsive base font
        double baseFont = 14;
        if (scene != null) baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        ColumnConstraints leftCol = new ColumnConstraints(); leftCol.setMinWidth(120); leftCol.setPrefWidth(140); leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints(); rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        ChoiceBox<String> methodChoice = new ChoiceBox<>();
        methodChoice.getItems().addAll("Azure Wallet", "GCash", "PayMaya", "BPI", "BDO", "Metrobank");
        methodChoice.setValue("Azure Wallet");

        TextField acctNameField = new TextField();
        acctNameField.setPromptText("Account holder name (if applicable)");
        TextField acctNumberField = new TextField();
        acctNumberField.setPromptText("Account number (if applicable)");
        acctNameField.setDisable(true);
        acctNumberField.setDisable(true);

        methodChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean needs = !"Azure Wallet".equals(newV);
            acctNameField.setDisable(!needs);
            acctNumberField.setDisable(!needs);
        });

        CheckBox confirmBox = new CheckBox("I confirm the withdrawal and destination details are correct");

        Label amtLabel = new Label("Amount:"); amtLabel.setFont(Font.font("System", baseFont)); amtLabel.setMaxWidth(Double.MAX_VALUE); amtLabel.setStyle("-fx-text-fill: #374151;");
        Label destLabel = new Label("Destination:"); destLabel.setFont(Font.font("System", baseFont)); destLabel.setMaxWidth(Double.MAX_VALUE); destLabel.setStyle("-fx-text-fill: #374151;");
        Label nameLabel = new Label("Name:"); nameLabel.setFont(Font.font("System", baseFont)); nameLabel.setMaxWidth(Double.MAX_VALUE); nameLabel.setStyle("-fx-text-fill: #374151;");
        Label acctLabel = new Label("Account #:"); acctLabel.setFont(Font.font("System", baseFont)); acctLabel.setMaxWidth(Double.MAX_VALUE); acctLabel.setStyle("-fx-text-fill: #374151;");

        grid.add(amtLabel, 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(destLabel, 0, 1);
        grid.add(methodChoice, 1, 1);
        grid.add(nameLabel, 0, 2);
        grid.add(acctNameField, 1, 2);
        grid.add(acctLabel, 0, 3);
        grid.add(acctNumberField, 1, 3);
        grid.add(confirmBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    double amt = Double.parseDouble(amountField.getText().trim());
                    if (!confirmBox.isSelected()) {
                        showAlert("Error", "Please confirm the withdrawal before proceeding.", Alert.AlertType.ERROR);
                        return;
                    }
                    String method = methodChoice.getValue();
                    String destination;
                    if ("Azure Wallet".equals(method)) {
                        destination = "Azure Wallet";
                    } else {
                        String name = acctNameField.getText().trim();
                        String number = acctNumberField.getText().trim();
                        destination = method + " - " + name + " (" + number + ")";
                    }
                    if (azureApp.withdraw(currentUser, amt, destination)) {
                        showAlert("Success", String.format("Withdrawn ₱%.2f\n₱15 fee charged", amt), Alert.AlertType.INFORMATION);
                        refreshMainScreen();
                    } else {
                        showAlert("Error", "Withdrawal failed. Insufficient balance or invalid amount.", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
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
        // left/right columns and responsive font
        double baseFont = 14;
        if (scene != null) baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        ColumnConstraints leftCol = new ColumnConstraints(); leftCol.setMinWidth(120); leftCol.setPrefWidth(140); leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints(); rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        TextField recipientField = new TextField();
        recipientField.setPromptText("Recipient username");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        Label toLabel = new Label("To:"); toLabel.setFont(Font.font("System", baseFont)); toLabel.setMaxWidth(Double.MAX_VALUE); toLabel.setStyle("-fx-text-fill: #374151;");
        Label amtLabel = new Label("Amount:"); amtLabel.setFont(Font.font("System", baseFont)); amtLabel.setMaxWidth(Double.MAX_VALUE); amtLabel.setStyle("-fx-text-fill: #374151;");

        grid.add(toLabel, 0, 0);
        grid.add(recipientField, 1, 0);
        grid.add(amtLabel, 0, 1);
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Redeem Voucher");
        dialog.setHeaderText("Enter your voucher code");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(12));
        double baseFont = 14;
        if (scene != null) baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        ColumnConstraints leftCol = new ColumnConstraints(); leftCol.setMinWidth(120); leftCol.setPrefWidth(140); leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints(); rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        Label codeLabel = new Label("Voucher Code:"); codeLabel.setFont(Font.font("System", baseFont)); codeLabel.setStyle("-fx-text-fill: #374151;"); codeLabel.setMaxWidth(Double.MAX_VALUE);
        TextField codeField = new TextField(); codeField.setPromptText("Enter voucher code");

        grid.add(codeLabel, 0, 0);
        grid.add(codeField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String code = codeField.getText().trim();
                double value = azureApp.redeemVoucher(currentUser, code);
                if (value > 0) {
                    showAlert("Success", String.format("Voucher redeemed successfully! ₱%.2f added to your balance.", value), Alert.AlertType.INFORMATION);
                    refreshMainScreen();
                } else {
                    showAlert("Error", "Voucher redemption failed.\nCode may be invalid or already used.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showPayOnlineDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pay Online");
        dialog.setHeaderText("Choose payment method");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(16);
        grid.setPadding(new Insets(24));
        grid.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 14;");
        double baseFont = 14;
        if (scene != null) baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        ColumnConstraints leftCol = new ColumnConstraints(); leftCol.setMinWidth(120); leftCol.setPrefWidth(140); leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints(); rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        // Payment Method Selection
        ToggleGroup paymentMethodGroup = new ToggleGroup();
        
        RadioButton virtualCardOption = new RadioButton("Azure Virtual Card");
        virtualCardOption.setToggleGroup(paymentMethodGroup);
        virtualCardOption.setSelected(true);
        virtualCardOption.setFont(Font.font("System", FontWeight.SEMI_BOLD, baseFont));
        virtualCardOption.setStyle("-fx-text-fill: #1E293B;");
        
        RadioButton walletOption = new RadioButton("Azure Wallet");
        walletOption.setToggleGroup(paymentMethodGroup);
        walletOption.setFont(Font.font("System", FontWeight.SEMI_BOLD, baseFont));
        walletOption.setStyle("-fx-text-fill: #1E293B;");
        
        VBox paymentMethodBox = new VBox(10);
        paymentMethodBox.getChildren().addAll(virtualCardOption, walletOption);
        grid.add(new Label("Payment Method:"), 0, 0);
        grid.add(paymentMethodBox, 1, 0);
        GridPane.setValignment(new Label("Payment Method:"), javafx.geometry.VPos.TOP);

        ComboBox<String> merchantBox = new ComboBox<>();
        merchantBox.getItems().addAll(azureApp.getFixedMerchants());
        merchantBox.setEditable(true);
        merchantBox.setPromptText("Select or type merchant name");
        merchantBox.setStyle("-fx-padding: 8 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB;");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setStyle("-fx-padding: 8 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB;");

        Label merchantLabel = new Label("Merchant:"); merchantLabel.setFont(Font.font("System", baseFont)); merchantLabel.setStyle("-fx-text-fill: #374151;"); merchantLabel.setMaxWidth(Double.MAX_VALUE);
        Label amountLabel = new Label("Amount:"); amountLabel.setFont(Font.font("System", baseFont)); amountLabel.setStyle("-fx-text-fill: #374151;"); amountLabel.setMaxWidth(Double.MAX_VALUE);
        
        grid.add(merchantLabel, 0, 1);
        grid.add(merchantBox, 1, 1);
        grid.add(amountLabel, 0, 2);
        grid.add(amountField, 1, 2);

        // Virtual Card Info Section (shown when Virtual Card is selected)
        VBox virtualCardInfoBox = new VBox(8);
        virtualCardInfoBox.setPadding(new Insets(14));
        virtualCardInfoBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #FFD700; -fx-border-width: 2;");
        
        UserAccount user = azureApp.getUser(currentUser);
        String vbn = user != null ? user.getVirtualBankNumber() : null;
        if (vbn == null || vbn.isEmpty()) {
            vbn = "Not Available";
        }
        String lastFour = vbn.length() >= 4 ? vbn.substring(vbn.length() - 4) : vbn;
        String maskedCard = "**** **** **** " + lastFour;
        
        Label virtualCardTitle = new Label("💳 Azure Virtual Card");
        virtualCardTitle.setFont(Font.font("System", FontWeight.BOLD, baseFont + 1));
        virtualCardTitle.setStyle("-fx-text-fill: #FFD700;");
        
        Label cardNumberLabel = new Label("Card Number: " + maskedCard);
        cardNumberLabel.setFont(Font.font("System", baseFont - 1));
        cardNumberLabel.setStyle("-fx-text-fill: #1E293B;");
        
        Label cardStatusLabel = new Label("Status: Active & Ready");
        cardStatusLabel.setFont(Font.font("System", baseFont - 1));
        cardStatusLabel.setStyle("-fx-text-fill: #10B981;");
        
        virtualCardInfoBox.getChildren().addAll(virtualCardTitle, cardNumberLabel, cardStatusLabel);
        
        grid.add(virtualCardInfoBox, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style buttons
        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #FFD700; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 32;");
        javafx.scene.Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-weight: bold;");

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String merchant = merchantBox.getEditor().getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());
                    double fee = 15.0; // fixed online payment fee
                    double total = amt + fee;
                    
                    String paymentMethod = virtualCardOption.isSelected() ? "Virtual Card" : "Wallet";
                    String cardInfo = virtualCardOption.isSelected() ? maskedCard : "N/A";
                    
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Payment");
                    confirm.setHeaderText("Pay merchant using " + paymentMethod);
                    confirm.setContentText(String.format("Payment Method: %s\n%s\nMerchant: %s\nAmount: ₱%.2f\nService Fee: ₱%.2f\nTotal: ₱%.2f\n\nProceed?", 
                        paymentMethod, cardInfo, merchant, amt, fee, total));
                    confirm.showAndWait().ifPresent(cresp -> {
                        if (cresp == ButtonType.OK) {
                            if (azureApp.payOnline(currentUser, merchant, amt)) {
                                showAlert("Success", 
                                    String.format("Payment successful!\nPaid ₱%.2f to %s using %s\nService Fee: ₱%.2f\nTotal Charged: ₱%.2f", 
                                    amt, merchant, paymentMethod, fee, total), 
                                    Alert.AlertType.INFORMATION);
                                refreshMainScreen();
                            } else {
                                showAlert("Error", "Payment failed. Check merchant, amount, and balance.", Alert.AlertType.ERROR);
                            }
                        }
                    });
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showVCardDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("V-Card Shop");
        dialog.setHeaderText("Choose product category");

        VBox mainContent = new VBox(16);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: #F8FAFC;");

        // Category buttons
        Button gamingBtn = createCategoryButton("🎮 Gaming", "Steam, Roblox, Mobile Legends");
        Button streamingBtn = createCategoryButton("🎬 Streaming", "Netflix, Disney+, Amazon Prime");
        Button mobileBtn = createCategoryButton("📱 Mobile & Apps", "Google Play, App Store");
        Button musicBtn = createCategoryButton("🎵 Music", "Spotify, YouTube Music");

        HBox categoryBox = new HBox(12);
        categoryBox.setAlignment(Pos.CENTER);
        categoryBox.getChildren().addAll(gamingBtn, streamingBtn, mobileBtn, musicBtn);

        // Title for products section
        Label productsTitle = new Label("Choose Your Product");
        productsTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        productsTitle.setStyle("-fx-text-fill: #0F172A;");

        // Products container (scrollable)
        VBox productsContainer = new VBox(12);
        productsContainer.setPadding(new Insets(12));
        productsContainer.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-radius: 10; -fx-border-width: 1;");

        ScrollPane productsScroll = new ScrollPane(productsContainer);
        productsScroll.setFitToWidth(true);
        productsScroll.setPrefHeight(250);
        productsScroll.setStyle("-fx-control-inner-background: white;");

        // Initial products (Gaming)
        updateProductList(productsContainer, "gaming");

        // Wire category buttons
        gamingBtn.setOnAction(e -> {
            gamingBtn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #1F2937; -fx-padding: 10 16; -fx-border-radius: 8; -fx-font-weight: bold;");
            streamingBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            mobileBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            musicBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            updateProductList(productsContainer, "gaming");
        });
        streamingBtn.setOnAction(e -> {
            gamingBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            streamingBtn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #1F2937; -fx-padding: 10 16; -fx-border-radius: 8; -fx-font-weight: bold;");
            mobileBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            musicBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            updateProductList(productsContainer, "streaming");
        });
        mobileBtn.setOnAction(e -> {
            gamingBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            streamingBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            mobileBtn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #1F2937; -fx-padding: 10 16; -fx-border-radius: 8; -fx-font-weight: bold;");
            musicBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            updateProductList(productsContainer, "mobile");
        });
        musicBtn.setOnAction(e -> {
            gamingBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            streamingBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            mobileBtn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8;");
            musicBtn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #1F2937; -fx-padding: 10 16; -fx-border-radius: 8; -fx-font-weight: bold;");
            updateProductList(productsContainer, "music");
        });

        // Set initial style for gaming button
        gamingBtn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #1F2937; -fx-padding: 10 16; -fx-border-radius: 8; -fx-font-weight: bold;");

        mainContent.getChildren().addAll(categoryBox, productsTitle, productsScroll);

        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private Button createCategoryButton(String title, String description) {
        VBox buttonContent = new VBox(2);
        buttonContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        titleLabel.setStyle("-fx-text-fill: inherit;");

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("System", 10));
        descLabel.setStyle("-fx-text-fill: #6B7280;");
        descLabel.setWrapText(true);

        buttonContent.getChildren().addAll(titleLabel, descLabel);

        Button btn = new Button();
        btn.setGraphic(buttonContent);
        btn.setStyle("-fx-background-color: #FFF9E6; -fx-text-fill: #6B7280; -fx-padding: 10 16; -fx-border-radius: 8; -fx-cursor: hand;");
        btn.setPrefWidth(120);
        btn.setMinHeight(60);

        return btn;
    }

    private void updateProductList(VBox container, String category) {
        container.getChildren().clear();

        java.util.Map<String, java.util.List<String[]>> products = new java.util.HashMap<>();

        // Gaming products (name, price)
        java.util.List<String[]> gaming = java.util.Arrays.asList(
            new String[]{"Steam Wallet", "₱500"}, new String[]{"Steam Wallet", "₱1000"},
            new String[]{"Roblox Robux", "₱500"}, new String[]{"Roblox Robux", "₱1500"},
            new String[]{"Mobile Legends", "₱100"}, new String[]{"Mobile Legends", "₱500"}
        );

        java.util.List<String[]> streaming = java.util.Arrays.asList(
            new String[]{"Netflix", "₱249"}, new String[]{"Netflix", "₱549"},
            new String[]{"Disney+", "₱299"}, new String[]{"Disney+", "₱799"},
            new String[]{"Amazon Prime", "₱149"}, new String[]{"Amazon Prime", "₱1499"}
        );

        java.util.List<String[]> mobile = java.util.Arrays.asList(
            new String[]{"Google Play", "₱500"}, new String[]{"Google Play", "₱1000"},
            new String[]{"App Store", "₱500"}, new String[]{"App Store", "₱1000"},
            new String[]{"PlayStation Store", "₱500"}, new String[]{"PlayStation Store", "₱1500"}
        );

        java.util.List<String[]> music = java.util.Arrays.asList(
            new String[]{"Spotify Premium", "₱129"}, new String[]{"Spotify Premium", "₱1299"},
            new String[]{"YouTube Music", "₱129"}, new String[]{"YouTube Music", "₱1299"},
            new String[]{"Apple Music", "₱109"}, new String[]{"Apple Music", "₱1090"}
        );

        products.put("gaming", gaming);
        products.put("streaming", streaming);
        products.put("mobile", mobile);
        products.put("music", music);

        // Display provider header
        Label providerLabel = new Label();
        switch (category) {
            case "gaming":
                providerLabel.setText("🎮 Gaming Platforms");
                break;
            case "streaming":
                providerLabel.setText("🎬 Streaming Services");
                break;
            case "mobile":
                providerLabel.setText("📱 Mobile & App Stores");
                break;
            case "music":
                providerLabel.setText("🎵 Music Streaming");
                break;
        }
        providerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        providerLabel.setStyle("-fx-text-fill: #1F2937;");
        container.getChildren().add(providerLabel);

        java.util.List<String[]> categoryProducts = products.get(category);

        // Group products by provider
        java.util.Map<String, java.util.List<String>> groupedByProvider = new java.util.LinkedHashMap<>();
        for (String[] product : categoryProducts) {
            String provider = product[0].replaceAll(" \\d+", ""); // Get provider name
            String price = product[1];
            groupedByProvider.computeIfAbsent(provider, k -> new java.util.ArrayList<>()).add(price);
        }

        for (java.util.Map.Entry<String, java.util.List<String>> entry : groupedByProvider.entrySet()) {
            String provider = entry.getKey();
            java.util.List<String> prices = entry.getValue();

            VBox providerBox = new VBox(8);
            providerBox.setPadding(new Insets(12));
            providerBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-border-width: 1;");

            Label providerTitle = new Label(provider);
            providerTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            providerTitle.setStyle("-fx-text-fill: #1F2937;");

            HBox priceBox = new HBox(8);
            priceBox.setAlignment(Pos.CENTER_LEFT);
            priceBox.setStyle("-fx-wrap-text: true;");

            for (String price : prices) {
                Button priceBtn = new Button(price);
                priceBtn.setStyle("-fx-background-color: white; -fx-border-color: #FFD700; -fx-border-width: 2; -fx-border-radius: 6; -fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand;");
                priceBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Purchase Confirmation");
                    confirm.setHeaderText("Confirm purchase");
                    confirm.setContentText(String.format("Buy %s voucher for %s?", provider, price));
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            showAlert("Success", String.format("Successfully purchased %s voucher worth %s!\n\nCheck your email for the code.", provider, price), Alert.AlertType.INFORMATION);
                        }
                    });
                });
                priceBox.getChildren().add(priceBtn);
            }

            providerBox.getChildren().addAll(providerTitle, priceBox);
            container.getChildren().add(providerBox);
        }
    }

    private void showRedeemPointsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Redeem Points");
        dialog.setHeaderText("Enter points to redeem");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(12));
        double baseFont = 14;
        if (scene != null) baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        ColumnConstraints leftCol = new ColumnConstraints(); leftCol.setMinWidth(120); leftCol.setPrefWidth(140); leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints(); rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        // show available points as a clear, consistently-sized label
        azurewallet.models.UserAccount ua = azureApp.getUser(currentUser);
        int availablePoints = ua != null ? ua.getPoints() : 0;
        Label ptsLabel = new Label("Points:");
        ptsLabel.setFont(Font.font("System", baseFont));
        ptsLabel.setStyle("-fx-text-fill: #374151;");
        ptsLabel.setMaxWidth(Double.MAX_VALUE);
        Label availValue = new Label(String.format("%d pts", availablePoints));
        availValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, baseFont + 1));
        availValue.setStyle("-fx-text-fill: #0F172A;");
        availValue.setMaxWidth(Double.MAX_VALUE);
        TextField ptsField = new TextField();
        ptsField.setPromptText("Enter points to redeem");
        ptsField.setFont(Font.font("System", baseFont));
        ptsField.setPrefHeight(40);

        // Redeem max button and conversion label
        Button redeemMaxBtn = new Button("Redeem Max");
        redeemMaxBtn.setStyle("-fx-background-radius:8; -fx-background-color:#F1F5F9; -fx-border-color:#E2E8F0; -fx-cursor:hand;");
        redeemMaxBtn.setPrefHeight(34);

        Label convLabel = new Label("Equivalent: ₱0.00");
        convLabel.setFont(Font.font("System", Math.max(11, baseFont - 2)));
        convLabel.setStyle("-fx-text-fill: #6B7280;");

        HBox rightBox = new HBox(8, ptsField, redeemMaxBtn);
        rightBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(ptsField, Priority.ALWAYS);

        grid.add(ptsLabel, 0, 0);
        grid.add(rightBox, 1, 0);
        grid.add(availValue, 0, 1);
        grid.add(convLabel, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Enable/disable OK depending on input validity
        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable validate = () -> {
            boolean ok = false;
            try {
                int v = Integer.parseInt(ptsField.getText().trim());
                ok = v > 0 && v <= availablePoints;
            } catch (Exception ex) { ok = false; }
            okButton.setDisable(!ok);
        };

        // Update conversion label as user types
        ptsField.textProperty().addListener((o,oldV,newV) -> {
            try {
                int v = Integer.parseInt(newV.trim());
                // 1 point = ₱1 conversion for preview
                double value = v * 1.0;
                convLabel.setText(String.format("Equivalent: ₱%.2f", value));
            } catch (Exception ex) {
                convLabel.setText("Equivalent: ₱0.00");
            }
            validate.run();
        });

        // Redeem max behavior
        redeemMaxBtn.setOnAction(e -> {
            ptsField.setText(String.valueOf(availablePoints));
        });

        // Also validate when dialog shown in case redeemMax pre-fills
        dialog.setOnShown(ev -> validate.run());

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int pts = Integer.parseInt(ptsField.getText().trim());
                    if (azureApp.redeemPoints(currentUser, pts)) {
                        showAlert("Success", String.format("Redeemed %d points", pts), Alert.AlertType.INFORMATION);
                        refreshMainScreen();
                    } else {
                        showAlert("Error", "Redeem failed. Check available points.", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid points entered", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showBillsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Bills Payment");
        dialog.setHeaderText("Pay a biller");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> billerBox = new ComboBox<>();
        // common utility billers; editable so user can type a custom biller
        billerBox.getItems().addAll("Meralco", "Maynilad", "Manila Water", "PLDT", "Smart Billing", "Globe Billing");
        billerBox.setEditable(true);
        billerBox.setPromptText("Select or type biller name");
        TextField acctField = new TextField();
        acctField.setPromptText("Account number");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        double baseFont = 14;
        if (scene != null) baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        ColumnConstraints leftCol = new ColumnConstraints(); leftCol.setMinWidth(120); leftCol.setPrefWidth(140); leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints(); rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        Label billerLabel = new Label("Biller:"); billerLabel.setFont(Font.font("System", baseFont)); billerLabel.setStyle("-fx-text-fill: #374151;"); billerLabel.setMaxWidth(Double.MAX_VALUE);
        Label accountLabel = new Label("Account:"); accountLabel.setFont(Font.font("System", baseFont)); accountLabel.setStyle("-fx-text-fill: #374151;"); accountLabel.setMaxWidth(Double.MAX_VALUE);
        Label amountLabel = new Label("Amount:"); amountLabel.setFont(Font.font("System", baseFont)); amountLabel.setStyle("-fx-text-fill: #374151;"); amountLabel.setMaxWidth(Double.MAX_VALUE);

        grid.add(billerLabel, 0, 0);
        grid.add(billerBox, 1, 0);
        grid.add(accountLabel, 0, 1);
        grid.add(acctField, 1, 1);
        grid.add(amountLabel, 0, 2);
        grid.add(amountField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String biller = billerBox.getEditor().getText().trim();
                    String acct = acctField.getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());
                    // Compute fee preview (same logic as backend): 2% with minimum ₱10
                    double fee = Math.max(10.0, amt * 0.02);
                    double total = amt + fee;
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Bill Payment");
                    confirm.setHeaderText("Pay bill and applicable fee");
                    confirm.setContentText(String.format("You are about to pay ₱%.2f to %s\nService/Tax Fee: ₱%.2f\nTotal: ₱%.2f\nProceed?", amt, biller, fee, total));
                    confirm.showAndWait().ifPresent(cresp -> {
                        if (cresp == ButtonType.OK) {
                            if (azureApp.billsPayment(currentUser, biller, acct, amt)) {
                                showAlert("Success", String.format("Paid ₱%.2f to %s (Fee: ₱%.2f)", amt, biller, fee), Alert.AlertType.INFORMATION);
                                refreshMainScreen();
                            } else {
                                showAlert("Error", "Payment failed. Check amount and balance.", Alert.AlertType.ERROR);
                            }
                        }
                    });
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showBuyLoadDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buy Prepaid Load");
        dialog.setHeaderText("Purchase load with Virtual Card or Wallet");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(16);
        grid.setPadding(new Insets(24));
        grid.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 14;");

        // Payment Method Selection
        ToggleGroup paymentMethodGroup = new ToggleGroup();
        
        RadioButton virtualCardOption = new RadioButton("Azure Virtual Card");
        virtualCardOption.setToggleGroup(paymentMethodGroup);
        virtualCardOption.setSelected(true);
        virtualCardOption.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        virtualCardOption.setStyle("-fx-text-fill: #1E293B;");
        
        RadioButton walletOption = new RadioButton("Azure Wallet");
        walletOption.setToggleGroup(paymentMethodGroup);
        walletOption.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        walletOption.setStyle("-fx-text-fill: #1E293B;");
        
        VBox paymentMethodBox = new VBox(8);
        paymentMethodBox.getChildren().addAll(virtualCardOption, walletOption);
        
        Label paymentLabel = new Label("Payment:");
        paymentLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        paymentLabel.setStyle("-fx-text-fill: #374151;");
        grid.add(paymentLabel, 0, 0);
        grid.add(paymentMethodBox, 1, 0);
        GridPane.setValignment(paymentLabel, javafx.geometry.VPos.TOP);

        ChoiceBox<String> networkChoice = new ChoiceBox<>();
        networkChoice.getItems().addAll("Globe/TM", "Smart/TNT", "DITO");
        networkChoice.setValue("Globe/TM");

        TextField numberField = new TextField();
        numberField.setPromptText("09XXXXXXXXX");
        numberField.setStyle("-fx-padding: 8 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB;");
        
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setStyle("-fx-padding: 8 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E5E7EB;");

        // Preset load amounts arranged as 2 columns x 4 rows (8 presets)
        int[] presets = {50, 100, 150, 200, 250, 300, 500, 1000};
        GridPane presetsGrid = new GridPane();
        presetsGrid.setHgap(8);
        presetsGrid.setVgap(8);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(50);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(50);
        presetsGrid.getColumnConstraints().addAll(cc1, cc2);

        java.util.List<Button> presetButtons = new java.util.ArrayList<>();
        for (int i = 0; i < presets.length; i++) {
            int value = presets[i];
            Button b = new Button("₱" + value);
            b.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(b, Priority.ALWAYS);
            b.setStyle("-fx-background-radius:8; -fx-border-color:#E2E8F0; -fx-background-color:white; -fx-font-weight: bold;");
            b.setOnAction(e -> {
                amountField.setText(String.valueOf(value));
                for (Button pb : presetButtons) pb.setStyle("-fx-background-radius:8; -fx-border-color:#E2E8F0; -fx-background-color:white; -fx-font-weight: bold;");
                b.setStyle("-fx-background-radius:8; -fx-border-color:#FFD700; -fx-background-color:#FFFACD; -fx-font-weight: bold;");
            });
            presetButtons.add(b);
            int row = i / 2;
            int col = i % 2;
            presetsGrid.add(b, col, row);
        }

        double baseFont = 14;
        if (scene != null) baseFont = Math.max(12, Math.min(18, scene.getWidth() * 0.035));
        ColumnConstraints leftCol = new ColumnConstraints(); leftCol.setMinWidth(120); leftCol.setPrefWidth(140); leftCol.setHgrow(Priority.NEVER);
        ColumnConstraints rightCol = new ColumnConstraints(); rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        Label networkLabel = new Label("Network:"); networkLabel.setFont(Font.font("System", baseFont)); networkLabel.setStyle("-fx-text-fill: #374151;"); networkLabel.setMaxWidth(Double.MAX_VALUE);
        Label numberLabel = new Label("Mobile #:"); numberLabel.setFont(Font.font("System", baseFont)); numberLabel.setStyle("-fx-text-fill: #374151;"); numberLabel.setMaxWidth(Double.MAX_VALUE);
        Label presetLabel = new Label("Quick Amounts:"); presetLabel.setFont(Font.font("System", baseFont)); presetLabel.setStyle("-fx-text-fill: #374151;"); presetLabel.setMaxWidth(Double.MAX_VALUE);
        Label enterLabel = new Label("Or enter:"); enterLabel.setFont(Font.font("System", baseFont)); enterLabel.setStyle("-fx-text-fill: #374151;"); enterLabel.setMaxWidth(Double.MAX_VALUE);

        grid.add(networkLabel, 0, 1);
        grid.add(networkChoice, 1, 1);
        grid.add(numberLabel, 0, 2);
        grid.add(numberField, 1, 2);
        grid.add(presetLabel, 0, 3);
        grid.add(presetsGrid, 1, 3);
        grid.add(enterLabel, 0, 4);
        grid.add(amountField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style buttons
        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #FFD700; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 32;");
        javafx.scene.Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-weight: bold;");

        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(480);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String network = networkChoice.getValue();
                    String number = numberField.getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());
                    String paymentMethod = virtualCardOption.isSelected() ? "Virtual Card" : "Wallet";

                    if (!number.matches("^09\\d{9}$")) {
                        showAlert("Error", "Invalid mobile number format. Use 09XXXXXXXXX.", Alert.AlertType.ERROR);
                        return;
                    }

                    String prefix4 = number.substring(0,4);
                    java.util.Set<String> globe = java.util.Set.of("0905","0906","0915","0916","0917","0926","0927","0935","0936","0945","0955","0965","0975","0994");
                    java.util.Set<String> smart = java.util.Set.of("0907","0908","0909","0910","0911","0912","0918","0919","0920","0921","0928","0929","0938","0948","0951","0952","0967","0973","0998");
                    java.util.Set<String> dito = java.util.Set.of("0991","0998","0999");

                    if (network != null) {
                        if (network.toLowerCase().contains("globe") && !globe.contains(prefix4)) {
                            showAlert("Error", "Wrong input! Number not recognized for Globe network.", Alert.AlertType.ERROR);
                            return;
                        }
                        if (network.toLowerCase().contains("smart") && !smart.contains(prefix4)) {
                            showAlert("Error", "Wrong input! Number not recognized for Smart/TNT number.", Alert.AlertType.ERROR);
                            return;
                        }
                        if (network.toLowerCase().contains("dito") && !dito.contains(prefix4)) {
                            showAlert("Error", "Wrong input! Number not recognized for DITO number.", Alert.AlertType.ERROR);
                            return;
                        }
                    }
                    double fee = Math.round((amt * 0.01) * 100.0) / 100.0;
                    double total = amt + fee;
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Load Purchase");
                    confirm.setHeaderText("Buy load using " + paymentMethod);
                    confirm.setContentText(String.format("Network: %s\nNumber: %s\nLoad Amount: ₱%.2f\nService Fee (1%%): ₱%.2f\nTotal: ₱%.2f\n\nPayment Method: %s\nProceed?", 
                        network, number, amt, fee, total, paymentMethod));
                    confirm.showAndWait().ifPresent(cresp -> {
                        if (cresp == ButtonType.OK) {
                            if (azureApp.buyLoad(currentUser, network, number, amt)) {
                                showAlert("Success", 
                                    String.format("Load purchased successfully!\n%s load sent to %s\nService Fee: ₱%.2f\nTotal Charged: ₱%.2f\nPayment: %s", 
                                    network, number, fee, total, paymentMethod), 
                                    Alert.AlertType.INFORMATION);
                                refreshMainScreen();
                            } else {
                                showAlert("Error", "Load purchase failed. Check number format and balance.", Alert.AlertType.ERROR);
                            }
                        }
                    });
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
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
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-border-color: #FFD700; " +
            "-fx-border-width: 1.5; " +
            "-fx-cursor: hand;"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: #FFA500; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #FFA500; " +
                "-fx-border-width: 1.5; " +
                "-fx-cursor: hand;"
            )
        );
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(
                "-fx-background-color: #FFD700; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #FFD700; " +
                "-fx-border-width: 1.5; " +
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
        
        // Header (includes avatar and username)
        HBox header = createHeader();
        
        // Scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(20, 20, 100, 20));
        
        // Get user info
        UserAccount user = azureApp.getUser(currentUser);

        // Large Virtual Card Display
        VBox virtualCard = new VBox(20);
        virtualCard.getStyleClass().add("virtual-card-large");
        virtualCard.setPadding(new Insets(30, 25, 30, 25));
        virtualCard.setPrefHeight(240);
        virtualCard.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFC107); " +
            "-fx-background-radius: 16; " +
            "-fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.25), 15, 0, 0, 8);"
        );

        // Top row: bank logo
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label bankLogo = new Label("AVC");
        bankLogo.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 14));
        bankLogo.setStyle("-fx-text-fill: white;");
        
        Label bankSubtitle = new Label("Mastercard");
        bankSubtitle.setFont(Font.font("System", 10));
        bankSubtitle.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9);");
        
        VBox bankInfo = new VBox(2, bankLogo, bankSubtitle);
        topRow.getChildren().add(bankInfo);
        
        Region cardSpacer = new Region();
        VBox.setVgrow(cardSpacer, Priority.ALWAYS);

        // Card number - masked with last 4 digits visible
        String vbn = user != null ? user.getVirtualBankNumber() : null;
        String cardDisplay;
        if (vbn == null || vbn.isBlank()) {
            cardDisplay = "**** **** **** 3456";
        } else {
            String formatted = CardUtil.formatCardNumber(vbn);
            String[] parts = formatted.split(" ");
            if (parts.length == 4) {
                cardDisplay = "**** **** **** " + parts[3];
            } else {
                cardDisplay = "**** **** **** " + vbn.substring(Math.max(0, vbn.length() - 4));
            }
        }
        
        Label cardNumber = new Label(cardDisplay);
        cardNumber.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 24));
        cardNumber.setStyle("-fx-text-fill: white; -fx-letter-spacing: 2px;");

        // Holder name and expiry/CVV row
        HBox detailsRow = new HBox(30);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox holderBox = new VBox(5);
        Label holderLabel = new Label(user != null ? user.getUsername().toUpperCase() : "CARDHOLDER");
        holderLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        holderLabel.setStyle("-fx-text-fill: white;");
        holderBox.getChildren().add(holderLabel);
        
        VBox expiryBox = new VBox(5);
        String expiryDate = CardUtil.generateExpiry();
        Label expiryLabel = new Label("Valid Thru  " + expiryDate);
        expiryLabel.setFont(Font.font("System", 11));
        expiryLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9);");
        expiryBox.getChildren().add(expiryLabel);
        
        VBox cvvBox = new VBox(5);
        Label cvvLabel = new Label("CVV  ***");
        cvvLabel.setFont(Font.font("System", 11));
        cvvLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9);");
        cvvBox.getChildren().add(cvvLabel);
        
        detailsRow.getChildren().addAll(holderBox, expiryBox, cvvBox);

        // Slide hint button
        HBox slideButtonRow = new HBox();
        slideButtonRow.setAlignment(Pos.CENTER);
        
        Button slideHint = new Button("→  Click to view card details");
        slideHint.setFont(Font.font("System", 12));
        slideHint.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.2); " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 20; " +
            "-fx-padding: 8 20; " +
            "-fx-cursor: hand; " +
            "-fx-border-color: rgba(255, 255, 255, 0.3); " +
            "-fx-border-radius: 20; " +
            "-fx-border-width: 1;"
        );
        slideHint.setOnAction(e -> showCardDetailsDialog(user, vbn, expiryDate));
        
        slideButtonRow.getChildren().add(slideHint);

        // Mastercard logo in bottom right
        HBox logoRow = new HBox();
        logoRow.setAlignment(Pos.CENTER_RIGHT);
        Label mastercardLogo = new Label("⬤⬤");
        mastercardLogo.setFont(Font.font(20));
        mastercardLogo.setStyle("-fx-text-fill: white; -fx-letter-spacing: -8px;");
        logoRow.getChildren().add(mastercardLogo);

        virtualCard.getChildren().addAll(topRow, cardSpacer, cardNumber, detailsRow, slideButtonRow, logoRow);

        // Balance display section with border
        VBox balanceSection = new VBox(15);
        balanceSection.setPadding(new Insets(20));
        balanceSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 16; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 16;"
        );

        // Balance row: show numeric balance (peso sign) on the left and Top Up on the right
        Button topUpBtn = new Button("Top Up");
        topUpBtn.setFont(Font.font("System", FontWeight.BOLD, 11));
        topUpBtn.setStyle(
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 24; " +
            "-fx-cursor: hand;"
        );
        topUpBtn.setOnAction(e -> showDepositDialog());

        // left: numeric balance with peso sign
        Label walletBalance = new Label(String.format("₱ %.2f", azureApp.getBalance(currentUser)));
        walletBalance.setFont(Font.font("System", FontWeight.BOLD, 20));
        walletBalance.setStyle("-fx-text-fill: #111827;");

        HBox topUpRow = new HBox();
        topUpRow.setAlignment(Pos.CENTER);
        topUpRow.setPadding(new Insets(6, 0, 0, 0));
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topUpRow.getChildren().addAll(walletBalance, topSpacer, topUpBtn);

        balanceSection.getChildren().add(topUpRow);

        // Points and Rank section
        VBox pointsRankSection = new VBox(15);
        pointsRankSection.setPadding(new Insets(20));
        pointsRankSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 16; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 16;"
        );

        // Points row with icon
        HBox pointsRow = new HBox(12);
        pointsRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox pointsInfo = new VBox(5);
        
        HBox pointsLabelRow = new HBox(10);
        pointsLabelRow.setAlignment(Pos.CENTER_LEFT);
        // Small star icon similar to Account Rank and Loyalty Tier icons
        Label starIcon = new Label("★");
        starIcon.setFont(Font.font(16));
        starIcon.setStyle("-fx-text-fill: #6B7280;");
        Label pointsLabel = new Label("Reward Points");
        pointsLabel.setFont(Font.font("System", 14));
        pointsLabel.setStyle("-fx-text-fill: #6B7280;");
        pointsLabelRow.getChildren().addAll(starIcon, pointsLabel);
        
        Label pointsAmount = new Label(String.format("%,d points", user.getPoints()));
        pointsAmount.setFont(Font.font("System", FontWeight.BOLD, 20));
        pointsAmount.setStyle("-fx-text-fill: #0F172A;");
        
        pointsInfo.getChildren().addAll(pointsLabelRow, pointsAmount);
        
        Region pointsSpacer = new Region();
        HBox.setHgrow(pointsSpacer, Priority.ALWAYS);
        
        Button redeemBtn = new Button("Redeem");
        redeemBtn.setFont(Font.font("System", FontWeight.BOLD, 12));
        redeemBtn.setStyle(
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8 20; " +
            "-fx-cursor: hand;"
        );
        redeemBtn.setOnAction(e -> showRedeemPointsDialog());
        
        pointsRow.getChildren().addAll(pointsInfo, pointsSpacer, redeemBtn);

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #E5E7EB;");

        // Rank row with icon
        HBox rankRow = new HBox(12);
        rankRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox rankInfo = new VBox(5);
        
        HBox rankLabelRow = new HBox(8);
        rankLabelRow.setAlignment(Pos.CENTER_LEFT);
        Label trophyIcon = new Label("🏆");
        trophyIcon.setFont(Font.font(16));
        Label rankLabel = new Label("Account Rank");
        rankLabel.setFont(Font.font("System", 14));
        rankLabel.setStyle("-fx-text-fill: #6B7280;");
        rankLabelRow.getChildren().addAll(trophyIcon, rankLabel);
        
        // Rank badge with color
        String rank = user.getRank();
        String rankColor;
        switch (rank.toLowerCase()) {
            case "bronze": rankColor = "#CD7F32"; break;
            case "silver": rankColor = "#C0C0C0"; break;
            case "gold": rankColor = "#D4AF37"; break;
            case "platinum": rankColor = "#86EFAC"; break;
            default: rankColor = "#CD7F32";
        }
        
        Label rankBadge = new Label(rank);
        rankBadge.setFont(Font.font("System", FontWeight.BOLD, 20));
        rankBadge.setStyle("-fx-text-fill: " + rankColor + ";");
        
        rankInfo.getChildren().addAll(rankLabelRow, rankBadge);
        
        Region rankSpacer = new Region();
        HBox.setHgrow(rankSpacer, Priority.ALWAYS);
        
        // Rank progress indicator
        Label progressLabel = new Label("Total Remaining to RankUp: ₱ " + String.format("%,.2f", user.getTotalTransacted()));
        progressLabel.setFont(Font.font("System", 11));
        progressLabel.setStyle("-fx-text-fill: #6B7280;");
        
        rankRow.getChildren().addAll(rankInfo, rankSpacer, progressLabel);

        // Divider 2
        Region divider2 = new Region();
        divider2.setPrefHeight(1);
        divider2.setStyle("-fx-background-color: #E5E7EB;");

        // Loyalty Tier row with icon
        HBox tierRow = new HBox(12);
        tierRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox tierInfo = new VBox(5);
        
        HBox tierLabelRow = new HBox(8);
        tierLabelRow.setAlignment(Pos.CENTER_LEFT);
        Label crownIcon = new Label("👑");
        crownIcon.setFont(Font.font(16));
        Label tierLabel = new Label("Loyalty Tier");
        tierLabel.setFont(Font.font("System", 14));
        tierLabel.setStyle("-fx-text-fill: #6B7280;");
        tierLabelRow.getChildren().addAll(crownIcon, tierLabel);
        
        // Tier badge with color
        String loyaltyTier = user.getLoyaltyTier();
        String tierColor;
        switch (loyaltyTier.toLowerCase()) {
            case "classic": tierColor = "#6B7280"; break;
            case "premium": tierColor = "#8B5CF6"; break;
            case "elite": tierColor = "#EC4899"; break;
            case "vip": tierColor = "#EAB308"; break;
            default: tierColor = "#FFD700";
        }
        
        Label tierBadge = new Label(loyaltyTier);
        tierBadge.setFont(Font.font("System", FontWeight.BOLD, 20));
        tierBadge.setStyle("-fx-text-fill: " + tierColor + ";");
        
        tierInfo.getChildren().addAll(tierLabelRow, tierBadge);
        
        Region tierSpacer = new Region();
        HBox.setHgrow(tierSpacer, Priority.ALWAYS);
        
        // Tier benefits indicator (dynamic based on loyalty tier)
        VBox benefitsBox = new VBox(4);
        benefitsBox.setAlignment(Pos.CENTER_RIGHT);

        Label benefitsTitle = new Label("Member Benefits");
        benefitsTitle.setFont(Font.font("System", 11));
        benefitsTitle.setStyle("-fx-text-fill: #6B7280;");

        Label benefitsDetail = new Label();
        benefitsDetail.setFont(Font.font("System", 12));
        benefitsDetail.setStyle("-fx-text-fill: #475569;");
        benefitsDetail.setWrapText(true);

        String benefitsText;
        switch (loyaltyTier.toLowerCase()) {
            case "classic":
                benefitsText = "Access to basic features and standard support";
                break;
            case "premium":
                benefitsText = "Higher daily limits and occasional fee discounts";
                break;
            case "elite":
                benefitsText = "Priority support, reduced transaction fees, special promos";
                break;
            case "vip":
                benefitsText = "Dedicated concierge support, waived fees, exclusive offers";
                break;
            default:
                benefitsText = "Member benefits vary by tier";
        }

        benefitsDetail.setText(benefitsText);
        benefitsBox.getChildren().addAll(benefitsTitle, benefitsDetail);

        tierRow.getChildren().addAll(tierInfo, tierSpacer, benefitsBox);

        pointsRankSection.getChildren().addAll(pointsRow, divider, rankRow, divider2, tierRow);

        content.getChildren().addAll(virtualCard, balanceSection, pointsRankSection);
        scrollPane.setContent(content);
        
        // Bottom navigation bar
        HBox bottomNav = createBottomNav();
        
        // Add a subsection title for the wallet screen below the header
        Label sectionTitle = new Label("Azure Virtual Card");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        sectionTitle.setPadding(new Insets(10, 20, 0, 20));
        sectionTitle.setStyle("-fx-text-fill: #0F172A;");

        mainContainer.getChildren().addAll(header, sectionTitle, scrollPane, bottomNav);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        root.setCenter(mainContainer);
    }
    
    // ==================== HISTORY SCREEN ====================
    
    private void showHistoryScreen() {
        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: #F5F7FA;");
        
        // Header (no back button on this screen)
        HBox header = createHeaderNoBack("Transaction History");
        
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

        // Clear recent button (removes the most recent 10 transactions for the current user)
        Button clearRecentBtn = new Button("Clear Recent");
        clearRecentBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-background-radius: 8; -fx-cursor: hand;");
        clearRecentBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Clear");
            confirm.setHeaderText("Remove recent transactions");
            confirm.setContentText("This will remove the 10 most recent transactions from your history. Continue?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    azureApp.getFileManager().removeRecentTransactions(currentUser, 10);
                    // refresh the history screen
                    showHistoryScreen();
                }
            });
        });

        HBox transHeaderRow = new HBox();
        transHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Region transSpacer = new Region();
        HBox.setHgrow(transSpacer, Priority.ALWAYS);
        transHeaderRow.getChildren().addAll(transTitle, transSpacer, clearRecentBtn);
        
        VBox transList = new VBox(10);
        transList.setPadding(new Insets(10, 0, 0, 0));
        
        // Get transaction history from backend
        var transactions = azureApp.getFileManager().getTransactionHistory(currentUser);
        if (transactions.isEmpty()) {
            Label noTrans = new Label("No transactions yet");
            noTrans.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
            transList.getChildren().add(noTrans);
        } else {
            // Show most recent 10 transactions (newest first)
            int size = transactions.size();
            int start = Math.max(0, size - 10);
            java.util.List<String> latest = new java.util.ArrayList<>(transactions.subList(start, size));
            java.util.Collections.reverse(latest);
            for (String trans : latest) {
                Label transLabel = new Label(trans);
                transLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13;");
                transLabel.setWrapText(true);
                transList.getChildren().add(transLabel);
            }
        }
        
        transactionCard.getChildren().addAll(transHeaderRow, transList);
        
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
        
        // Header without back button for Settings
        HBox header = createHeaderNoBack("Settings");
        
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
        
        // Change PIN button (use lock graphic instead of emoji)
        Button changePinBtn = createSettingsButton(buildNavIcon("settings"), "Change PIN");
        changePinBtn.setOnAction(e -> showChangePinDialog());
        
        // Account Info button (avatar graphic)
        Button accountInfoBtn = createSettingsButton(buildAvatarGraphic(), "Account Information");
        accountInfoBtn.setOnAction(e -> showAccountInfoDialog());
        
        settingsList.getChildren().addAll(changePinBtn, accountInfoBtn);
        
        accountCard.getChildren().addAll(accountTitle, settingsList);
        
        // Points & Rewards Information
        VBox pointsCard = new VBox(15);
        pointsCard.setPadding(new Insets(20));
        pointsCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label pointsTitle = new Label("Points & Rewards System");
        pointsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        pointsTitle.setStyle("-fx-text-fill: #1E293B;");
        
        VBox pointsInfo = new VBox(15);
        pointsInfo.setPadding(new Insets(10, 0, 0, 0));
        
        // How to Earn Points
        VBox earnSection = new VBox(8);
        Label earnTitle = new Label("💰 How to Earn Points");
        earnTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        earnTitle.setStyle("-fx-text-fill: #0F172A;");
        
        Label earnText = new Label(
            "• Deposit Funds: Earn 1 point per ₱1000 deposited\n" +
            "• Transfer Money: Earn 1 point per ₱1000 sent\n" +
            "• Pay Bills: Earn 2 points per ₱1000 paid\n" +
            "• Buy Load: Earn 1 point per ₱500 purchased\n" +
            "• Pay Online: Earn 1 point per ₱1000 spent"
        );
        earnText.setFont(Font.font("System", 13));
        earnText.setStyle("-fx-text-fill: #475569;");
        earnText.setWrapText(true);
        
        earnSection.getChildren().addAll(earnTitle, earnText);
        
        // Divider
        Region divider1 = new Region();
        divider1.setPrefHeight(1);
        divider1.setStyle("-fx-background-color: #E2E8F0;");
        
        // How to Redeem Points
        VBox redeemSection = new VBox(8);
        Label redeemTitle = new Label("🎁 How to Redeem Points");
        redeemTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        redeemTitle.setStyle("-fx-text-fill: #0F172A;");
        
        Label redeemText = new Label(
            "• 50 Points = ₱50 Cash\n" +
            "• 100 Points = ₱100 Cash\n" +
            "• 500 Points = ₱500 Cash\n" +
            "• 800 Points = ₱800 Cash\n\n" +
            "Go to Home > Points > Redeem to convert your points to cash!"
        );
        redeemText.setFont(Font.font("System", 13));
        redeemText.setStyle("-fx-text-fill: #475569;");
        redeemText.setWrapText(true);
        
        redeemSection.getChildren().addAll(redeemTitle, redeemText);
        
        // Divider
        Region divider2 = new Region();
        divider2.setPrefHeight(1);
        divider2.setStyle("-fx-background-color: #E2E8F0;");
        
        // Account Ranks
        VBox rankSection = new VBox(8);
        Label rankTitle = new Label("🏆 Account Ranks");
        rankTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        rankTitle.setStyle("-fx-text-fill: #0F172A;");
        
        Label rankText = new Label(
            "Your rank is based on total transaction amount:\n\n" +
            "• Bronze: ₱0 - ₱150,000 (Starting rank)\n" +
            "• Silver: ₱150,001 - ₱300,000 (Higher deposit limits)\n" +
            "• Gold: ₱300,001 - ₱600,000 (Premium support)\n" +
            "• Platinum: ₱600,001+ (VIP benefits)\n\n" +
            "Higher ranks unlock better transaction limits and exclusive features!"
        );
        rankText.setFont(Font.font("System", 13));
        rankText.setStyle("-fx-text-fill: #475569;");
        rankText.setWrapText(true);
        
        rankSection.getChildren().addAll(rankTitle, rankText);

        // Divider for loyalty perks
        Region divider3 = new Region();
        divider3.setPrefHeight(1);
        divider3.setStyle("-fx-background-color: #E2E8F0;");

        // Loyalty Perks section (below Account Ranks)
        VBox perksSection = new VBox(8);
        Label perksTitle = new Label("✨ Loyalty Tier Perks");
        perksTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        perksTitle.setStyle("-fx-text-fill: #0F172A;");

        Label perksText = new Label(
            "Perks available per tier:\n\n" +
            "• Classic: Access to basic features and standard support\n" +
            "• Premium: Higher daily limits and occasional fee discounts\n" +
            "• Elite: Priority support, reduced transaction fees, special promos\n" +
            "• VIP: Dedicated concierge support, waived fees, exclusive offers\n\n" +
            "Perks are applied automatically based on your account rank."
        );
        perksText.setFont(Font.font("System", 13));
        perksText.setStyle("-fx-text-fill: #475569;");
        perksText.setWrapText(true);

        perksSection.getChildren().addAll(perksTitle, perksText);

        pointsInfo.getChildren().addAll(earnSection, divider1, redeemSection, divider2, rankSection, divider3, perksSection);
        
        pointsCard.getChildren().addAll(pointsTitle, pointsInfo);
        
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
        
        content.getChildren().addAll(accountCard, pointsCard, appCard);
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
        // User avatar and name (mirror createHeader)
        Circle avatar = new Circle(26);
        avatar.setFill(Color.web("#FFD700"));
        avatar.setStroke(Color.web("#e5e7eb"));
        avatar.setStrokeWidth(1.5);

        javafx.scene.Node avatarIcon = buildAvatarGraphic();

        StackPane avatarStack = new StackPane(avatar, avatarIcon);
        avatarStack.setPrefSize(52, 52);
        avatarStack.setMinSize(52, 52);
        avatarStack.setMaxSize(52, 52);
        avatarStack.setAlignment(Pos.CENTER);

        UserAccount user = azureApp.getUser(currentUser);
        Label titleName = new Label(user != null ? user.getUsername() : "");
        titleName.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleName.setStyle("-fx-text-fill: #000000;");

        HBox nameBox = new HBox(8, titleName);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setPadding(new Insets(0, 0, 0, 12));

        // Back button and screen title grouped to the right
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

        HBox rightGroup = new HBox(8, backBtn, titleLabel);
        rightGroup.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Account dropdown (logout only)
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            currentUser = null;
            showLoginScreen();
        });

        MenuButton accountMenu = new MenuButton();
        accountMenu.getItems().addAll(logoutItem);
        accountMenu.setText("Account ▾");
        accountMenu.setStyle(
            "-fx-background-color: white; -fx-text-fill: #FFD700; " +
            "-fx-font-size: 12; -fx-font-weight: bold; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; " +
            "-fx-border-color: #E2E8F0; -fx-padding: 8 12; -fx-cursor: hand;"
        );

        header.getChildren().addAll(avatarStack, nameBox, spacer, rightGroup, accountMenu);
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

    /**
     * Create a header similar to createHeaderWithBack but without the back button.
     * Used for screens where we want the avatar + username + title + account menu,
     * but not a back navigation button (e.g., History, Settings).
     */
    private HBox createHeaderNoBack(String title) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(36, 20, 8, 20));
        header.setStyle("-fx-background-color: #F5F7FA;");

        // Top row: avatar + username on the left, account menu on the right
        Circle avatar = new Circle(26);
        avatar.setFill(Color.web("#FFD700"));
        avatar.setStroke(Color.web("#e5e7eb"));
        avatar.setStrokeWidth(1.5);

        javafx.scene.Node avatarIcon = buildAvatarGraphic();

        StackPane avatarStack = new StackPane(avatar, avatarIcon);
        avatarStack.setPrefSize(52, 52);
        avatarStack.setAlignment(Pos.CENTER);

        UserAccount user = azureApp.getUser(currentUser);
        Label titleName = new Label(user != null ? user.getUsername() : "");
        titleName.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleName.setStyle("-fx-text-fill: #000000;");

        HBox leftTop = new HBox(8, avatarStack, titleName);
        leftTop.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            currentUser = null;
            showLoginScreen();
        });

        MenuButton accountMenu = new MenuButton();
        accountMenu.getItems().addAll(logoutItem);
        accountMenu.setText("Account ▾");
        accountMenu.setStyle(
            "-fx-background-color: white; -fx-text-fill: #FFD700; " +
            "-fx-font-size: 12; -fx-font-weight: bold; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; " +
            "-fx-border-color: #E2E8F0; -fx-padding: 8 12; -fx-cursor: hand;"
        );

        HBox topRow = new HBox(8, leftTop, spacer, accountMenu);
        topRow.setAlignment(Pos.CENTER);
        topRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(topRow, Priority.ALWAYS);

        // Make leftTop and accountMenu match heights for consistent header appearance
        leftTop.setAlignment(Pos.CENTER_LEFT);
        leftTop.setMinHeight(52);
        leftTop.setPrefHeight(52);
        accountMenu.setPrefHeight(52);
        accountMenu.setMinHeight(52);
        accountMenu.setPadding(new Insets(8, 12, 8, 12));

        // Title row: centered label below the top row
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #1E293B;");
        HBox titleRow = new HBox(titleLabel);
        titleRow.setAlignment(Pos.CENTER);
        titleRow.setPadding(new Insets(6, 0, 0, 0));

        VBox stack = new VBox(4, topRow, titleRow);
        stack.setAlignment(Pos.CENTER_LEFT);
        stack.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(stack, Priority.ALWAYS);
        header.getChildren().add(stack);
        return header;
    }
    
    private Button createSettingsButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(50);
        btn.setAlignment(Pos.CENTER_LEFT);
        // Use a consistent blue border and uniform width across states
        btn.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-text-fill: #475569; " +
            "-fx-font-size: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #FFD700; " +
            "-fx-border-width: 1.5; " +
            "-fx-border-radius: 8; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0 16;"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: #EFF6FF; " +
                "-fx-text-fill: #475569; " +
                "-fx-font-size: 14; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #FFD700; " +
                "-fx-border-width: 1.5; " +
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
                "-fx-border-color: #FFD700; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 16;"
            )
        );
        
        return btn;
    }

    // Overload: allow a graphic plus text for settings buttons (replaces emoji usage)
    private Button createSettingsButton(javafx.scene.Node graphic, String text) {
        Button btn = createSettingsButton(text);
        btn.setGraphic(graphic);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(12);
        return btn;
    }
    
    private void showChangePinDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change PIN");
        dialog.setHeaderText("Enter your current PIN and choose a new 4-digit PIN");

        ButtonType changeButtonType = new ButtonType("Change PIN", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        Label currentLbl = new Label("Current PIN:");
        PasswordField currentPinField = new PasswordField();
        currentPinField.setPromptText("Current PIN");
        styleInputField(currentPinField);
        currentPinField.setPrefWidth(240);

        Label newLbl = new Label("New PIN:");
        PasswordField newPinField = new PasswordField();
        newPinField.setPromptText("New PIN (4 digits)");
        styleInputField(newPinField);
        newPinField.setPrefWidth(240);

        Label confirmLbl = new Label("Confirm PIN:");
        PasswordField confirmPinField = new PasswordField();
        confirmPinField.setPromptText("Confirm new PIN");
        styleInputField(confirmPinField);
        confirmPinField.setPrefWidth(240);

        grid.add(currentLbl, 0, 0);
        grid.add(currentPinField, 1, 0);
        grid.add(newLbl, 0, 1);
        grid.add(newPinField, 1, 1);
        grid.add(confirmLbl, 0, 2);
        grid.add(confirmPinField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(currentPinField::requestFocus);

        javafx.scene.Node changeBtn = dialog.getDialogPane().lookupButton(changeButtonType);
        changeBtn.setDisable(true);

        // Validation: enable when all fields valid and match
        Runnable validate = () -> {
            String cur = currentPinField.getText().trim();
            String nw = newPinField.getText().trim();
            String cf = confirmPinField.getText().trim();
            boolean ok = cur.matches("\\d{4}") && nw.matches("\\d{4}") && nw.equals(cf) && !nw.equals(cur);
            changeBtn.setDisable(!ok);
        };

        currentPinField.textProperty().addListener((o,oldV,newV) -> validate.run());
        newPinField.textProperty().addListener((o,oldV,newV) -> validate.run());
        confirmPinField.textProperty().addListener((o,oldV,newV) -> validate.run());

        dialog.setResultConverter(btn -> btn == changeButtonType ? changeButtonType : null);

        dialog.showAndWait().ifPresent(response -> {
            if (response == changeButtonType) {
                String currentPin = currentPinField.getText().trim();
                String newPin = newPinField.getText().trim();
                // Call backend to change PIN
                boolean changed = azureApp.changePin(currentUser, currentPin, newPin);
                if (changed) {
                    showAlert("Success", "PIN changed successfully.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "PIN change failed. Check current PIN and try again.", Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void showAccountInfoDialog() {
        UserAccount user = azureApp.getUser(currentUser);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account Information");
        alert.setHeaderText("Your Account Details");
        
        String info = String.format(
            "Username: %s\n" +
            "Mobile: %s\n" +
            "Balance: %.2f\n" +
            "Tier: %s",
            user.getUsername(),
            user.getMobile(),
            user.getBalance(),
            user.getLoyaltyTier()
        );
        
        alert.setContentText(info);
        alert.showAndWait();
    }
    
    private void showCardDetailsDialog(UserAccount user, String vbn, String expiryDate) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Virtual Card Details");
        dialog.setHeaderText("Full Card Information");
        
        ButtonType okButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okButton);
        
        // Create card details display
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #F8FAFC;");
        
        // Card visual representation
        VBox cardVisual = new VBox(15);
        cardVisual.setPadding(new Insets(25));
        cardVisual.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFC107); " +
            "-fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.25), 10, 0, 0, 5);"
        );
        cardVisual.setPrefWidth(380);
        
        // Bank name
        Label bankName = new Label("Azure Digital Wallet");
        bankName.setFont(Font.font("System", FontWeight.BOLD, 16));
        bankName.setStyle("-fx-text-fill: white;");
        
        // Full card number
        String fullCardNumber;
        if (vbn == null || vbn.isBlank()) {
            fullCardNumber = "4532 1234 5678 3456";
        } else {
            fullCardNumber = CardUtil.formatCardNumber(vbn);
        }
        
        Label cardNumLabel = new Label(fullCardNumber);
        cardNumLabel.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 20));
        cardNumLabel.setStyle("-fx-text-fill: white; -fx-letter-spacing: 2px;");
        
        // Cardholder name
        Label holderName = new Label("CARDHOLDER: " + (user != null ? user.getUsername().toUpperCase() : "CARDHOLDER"));
        holderName.setFont(Font.font("System", FontWeight.BOLD, 12));
        holderName.setStyle("-fx-text-fill: white;");
        
        // Expiry and CVV
        HBox detailsRow = new HBox(30);
        Label expiryInfo = new Label("EXPIRY: " + expiryDate);
        expiryInfo.setFont(Font.font("System", 11));
        expiryInfo.setStyle("-fx-text-fill: white;");
        
        // Generate a random CVV for display
        int cvv = 100 + (int)(Math.random() * 900);
        Label cvvInfo = new Label("CVV: " + cvv);
        cvvInfo.setFont(Font.font("System", 11));
        cvvInfo.setStyle("-fx-text-fill: white;");
        
        detailsRow.getChildren().addAll(expiryInfo, cvvInfo);
        
        cardVisual.getChildren().addAll(bankName, cardNumLabel, holderName, detailsRow);
        
        // Card information text
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1;"
        );
        
        Label infoTitle = new Label("Card Information");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        infoTitle.setStyle("-fx-text-fill: #1E293B;");
        
        String cardInfo = String.format(
            "Card Number: %s\n" +
            "Cardholder: %s\n" +
            "Expiry Date: %s\n" +
            "CVV: %d\n" +
            "Card Type: Mastercard\n" +
            "Status: Active",
            fullCardNumber,
            user != null ? user.getUsername().toUpperCase() : "CARDHOLDER",
            expiryDate,
            cvv
        );
        
        Label infoText = new Label(cardInfo);
        infoText.setFont(Font.font("System", 12));
        infoText.setStyle("-fx-text-fill: #475569;");
        infoText.setWrapText(true);
        
        infoBox.getChildren().addAll(infoTitle, infoText);
        
        // Security warning
        Label warningLabel = new Label("⚠ Keep your card details secure. Never share your CVV with anyone.");
        warningLabel.setFont(Font.font("System", 11));
        warningLabel.setStyle("-fx-text-fill: #DC2626;");
        warningLabel.setWrapText(true);
        
        content.getChildren().addAll(cardVisual, infoBox, warningLabel);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(450);
        dialog.showAndWait();
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
        // Remove blue border for admin login button (keep background similar to primary)
        loginBtn.setStyle(
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"
        );
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
            "-fx-background-color: #FFA500; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"
        ));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"
        ));
        
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

        // Open the admin panel in a separate resizable window (Stage) so it's windowed
        if (adminStage == null) {
            adminStage = new Stage();
            Scene adminScene = new Scene(mainContainer, 1000, 720);
            adminStage.setScene(adminScene);
            adminStage.setTitle("Admin Panel - Azure Digital Wallet");
            adminStage.setResizable(true);
            adminStage.centerOnScreen();
            adminStage.setOnCloseRequest(e -> {
                // When admin window closes, return to main login/home
                isAdminMode = false;
                showLoginScreen();
                adminStage = null;
            });
            adminStage.show();
        } else {
            // If already open, bring to front and update content
            adminStage.getScene().setRoot(mainContainer);
            adminStage.toFront();
        }
    }
    
    private StackPane adminContentArea;
    private VBox adminSidebar;
    private BorderPane adminMainContainer;
    private boolean sidebarVisible;
    private Stage adminStage;
    
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
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand; " +
            "-fx-border-color: #FFD700; " +
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
        
        Button dashboardBtn = createSidebarButton("Dashboard", true);
        Button usersBtn = createSidebarButton("👥 User Management", false);
        Button vouchersBtn = createSidebarButton("🎫 Voucher System", false);
        Button systemBtn = createSidebarButton("🔧 System Tools", false);
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
        
        Button logoutBtn = new Button("Logout");
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
            // close admin window if open
            if (adminStage != null) {
                adminStage.close();
                adminStage = null;
            }
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
                "-fx-background-color: #FFD700; " +
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
                "-fx-background-color: #FFD700; " +
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
        
        // Search & Filter Card
        VBox searchCard = new VBox(15);
        searchCard.setPadding(new Insets(25));
        searchCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        HBox searchRow = new HBox(15);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search by username or mobile number...");
        searchField.setPrefWidth(400);
        searchField.setStyle(
            "-fx-padding: 10; " +
            "-fx-font-size: 12; " +
            "-fx-border-radius: 5; " +
            "-fx-border-color: #E2E8F0;"
        );
        
        Button clearFilterBtn = createModernButton("Clear Filter", "#64748B");
        clearFilterBtn.setPrefWidth(120);
        
        searchRow.getChildren().addAll(
            new Label("Filter Users:"),
            searchField,
            clearFilterBtn
        );
        
        searchCard.getChildren().add(searchRow);
        
        // User Table Card
        VBox tableCard = new VBox(15);
        tableCard.setPadding(new Insets(25));
        tableCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);"
        );
        
        Label tableTitle = new Label("User Accounts");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        tableTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Create TableView with modern styling
        TableView<UserAccountRow> userTable = new TableView<>();
        userTable.setPrefHeight(450);
        userTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        userTable.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-family: 'Segoe UI', 'System'; " +
            "-fx-background-color: #FAFBFC; " +
            "-fx-control-inner-background: #FAFBFC; " +
            "-fx-table-cell-border-color: #E2E8F0; " +
            "-fx-table-header-border-color: #CBD5E1; " +
            "-fx-padding: 0;"
        );
        
        // Checkbox column for bulk delete
        TableColumn<UserAccountRow, Boolean> selectCol = new TableColumn<>("");
        selectCol.setPrefWidth(50);
        selectCol.setStyle("-fx-alignment: CENTER;");
        selectCol.setCellValueFactory(param -> param.getValue().selectedProperty());
        selectCol.setCellFactory(col -> {
            javafx.scene.control.cell.CheckBoxTableCell<UserAccountRow, Boolean> cell = 
                new javafx.scene.control.cell.CheckBoxTableCell<>();
            cell.setStyle("-fx-alignment: CENTER;");
            return cell;
        });
        
        // Username column
        TableColumn<UserAccountRow, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setPrefWidth(140);
        usernameCol.setCellValueFactory(param -> param.getValue().usernameProperty());
        usernameCol.setStyle("-fx-alignment: CENTER_LEFT;");
        usernameCol.setCellFactory(col -> createStyledTableCell());
        
        // Mobile column
        TableColumn<UserAccountRow, String> mobileCol = new TableColumn<>("Mobile");
        mobileCol.setPrefWidth(130);
        mobileCol.setCellValueFactory(param -> param.getValue().mobileProperty());
        mobileCol.setStyle("-fx-alignment: CENTER_LEFT;");
        mobileCol.setCellFactory(col -> createStyledTableCell());
        
        // Balance column
        TableColumn<UserAccountRow, String> balanceCol = new TableColumn<>("Balance (PHP)");
        balanceCol.setPrefWidth(130);
        balanceCol.setCellValueFactory(param -> param.getValue().balanceProperty());
        balanceCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        balanceCol.setCellFactory(col -> {
            TableCell<UserAccountRow, String> cell = new TableCell<UserAccountRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText("PHP " + item);
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    }
                }
            };
            return cell;
        });
        
        // Rank column
        TableColumn<UserAccountRow, String> rankCol = new TableColumn<>("Rank");
        rankCol.setPrefWidth(100);
        rankCol.setCellValueFactory(param -> param.getValue().rankProperty());
        rankCol.setStyle("-fx-alignment: CENTER;");
        rankCol.setCellFactory(col -> {
            TableCell<UserAccountRow, String> cell = new TableCell<UserAccountRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        String color = switch(item) {
                            case "Gold" -> "#F59E0B";
                            case "Silver" -> "#A1A5AD";
                            case "Platinum" -> "#06B6D4";
                            case "Bronze" -> "#92400E";
                            default -> "#64748B";
                        };
                        setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            };
            return cell;
        });
        
        // Points column
        TableColumn<UserAccountRow, String> pointsCol = new TableColumn<>("Points");
        pointsCol.setPrefWidth(90);
        pointsCol.setCellValueFactory(param -> param.getValue().pointsProperty());
        pointsCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        pointsCol.setCellFactory(col -> {
            TableCell<UserAccountRow, String> cell = new TableCell<UserAccountRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #FFC107; -fx-font-weight: bold; -fx-alignment: CENTER_RIGHT;");
                    }
                }
            };
            return cell;
        });
        
        userTable.getColumns().addAll(selectCol, usernameCol, mobileCol, balanceCol, rankCol, pointsCol);
        
        // Set alternating row colors and hover effects with selection support
        userTable.setRowFactory(tv -> new TableRow<UserAccountRow>() {
            @Override
            protected void updateItem(UserAccountRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setStyle("");
                } else {
                    // Check if this row is selected
                    if (isSelected()) {
                        setStyle("-fx-background-color: #DBEAFE; -fx-border-color: #FFC107; -fx-border-width: 1;");
                    } else if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #F8FAFC;");
                    } else {
                        setStyle("-fx-background-color: #FFFFFF;");
                    }
                }
            }
        });
        
        // Populate table with users
        java.util.List<UserAccountRow> userRows = new java.util.ArrayList<>();
        for (UserAccount user : azureApp.getUsers().values()) {
            userRows.add(new UserAccountRow(user));
        }
        ObservableList<UserAccountRow> tableData = FXCollections.observableArrayList(userRows);
        userTable.setItems(tableData);
        
        // Search filter
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                userTable.setItems(tableData);
            } else {
                String filter = newVal.toLowerCase();
                java.util.List<UserAccountRow> filtered = tableData.stream()
                    .filter(row -> row.getUsername().toLowerCase().contains(filter) || 
                                   row.getMobile().contains(filter))
                    .collect(java.util.stream.Collectors.toList());
                userTable.setItems(FXCollections.observableArrayList(filtered));
            }
        });
        
        clearFilterBtn.setOnAction(e -> {
            searchField.clear();
            userTable.setItems(tableData);
        });
        
        tableCard.getChildren().addAll(tableTitle, userTable);
        
        // Action Buttons Card
        VBox actionCard = new VBox(15);
        actionCard.setPadding(new Insets(25));
        actionCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        HBox actionRow = new HBox(15);
        actionRow.setAlignment(Pos.CENTER);
        
        // Delete Selected Button
        Button deleteSelectedBtn = createModernButton("🗑️ Delete Selected", "#EF4444");
        deleteSelectedBtn.setOnAction(e -> {
            java.util.List<UserAccountRow> selected = userTable.getItems().stream()
                .filter(UserAccountRow::isSelected)
                .collect(java.util.stream.Collectors.toList());
            
            if (selected.isEmpty()) {
                showAlert("No Selection", "Please select at least one user to delete.", Alert.AlertType.WARNING);
                return;
            }
            
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Users");
            confirm.setHeaderText("Delete " + selected.size() + " user(s)?");
            confirm.setContentText("This action cannot be undone. Continue?");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                for (UserAccountRow row : selected) {
                    adminControl.deleteUser(row.getUsername());
                }
                // Refresh the table
                switchAdminContent("users");
                showAlert("Success", selected.size() + " user(s) deleted successfully.", Alert.AlertType.INFORMATION);
            }
        });
        
        // Delete Multiple Button (select manually from list)
        Button deleteMultipleBtn = createModernButton("⚠️ Delete Multiple", "#F59E0B");
        deleteMultipleBtn.setOnAction(e -> {
            java.util.List<UserAccountRow> allUsers = userTable.getItems();
            if (allUsers.isEmpty()) {
                showAlert("No Users", "There are no users to delete.", Alert.AlertType.WARNING);
                return;
            }
            
            Alert deleteMultDialog = new Alert(Alert.AlertType.CONFIRMATION);
            deleteMultDialog.setTitle("Delete Multiple Users");
            deleteMultDialog.setHeaderText("Select how many recent users to delete:");
            deleteMultDialog.setContentText("Delete the last N users (by most recent):");
            
            // Create spinner for count selection
            Spinner<Integer> countSpinner = new Spinner<>(1, allUsers.size(), 5);
            VBox multContent = new VBox(10);
            multContent.setPadding(new Insets(10));
            multContent.getChildren().addAll(new Label("Number of users to delete:"), countSpinner);
            deleteMultDialog.getDialogPane().setContent(multContent);
            deleteMultDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            
            Optional<ButtonType> res = deleteMultDialog.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                int countToDelete = countSpinner.getValue();
                int startIndex = Math.max(0, allUsers.size() - countToDelete);
                java.util.List<UserAccountRow> toDelete = allUsers.subList(startIndex, allUsers.size());
                
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Delete");
                confirm.setHeaderText("Delete " + toDelete.size() + " user(s)?");
                confirm.setContentText("This action cannot be undone. Continue?");
                Optional<ButtonType> confirmRes = confirm.showAndWait();
                if (confirmRes.isPresent() && confirmRes.get() == ButtonType.OK) {
                    for (UserAccountRow row : toDelete) {
                        adminControl.deleteUser(row.getUsername());
                    }
                    switchAdminContent("users");
                    showAlert("Success", toDelete.size() + " user(s) deleted successfully.", Alert.AlertType.INFORMATION);
                }
            }
        });
        
        // Delete All Button
        Button deleteAllBtn = createModernButton("❌ Delete All", "#DC2626");
        deleteAllBtn.setOnAction(e -> {
            java.util.List<UserAccountRow> allUsers = userTable.getItems();
            if (allUsers.isEmpty()) {
                showAlert("No Users", "There are no users to delete.", Alert.AlertType.WARNING);
                return;
            }
            
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Delete All Users - WARNING");
            warning.setHeaderText("⚠️ This will DELETE ALL USERS!");
            warning.setContentText("This is a permanent action that cannot be undone.\nType 'DELETE ALL' in the field below to confirm:\n(Leave empty to cancel)");
            
            TextField confirmField = new TextField();
            confirmField.setPromptText("Type 'DELETE ALL' to confirm");
            VBox warnContent = new VBox(10);
            warnContent.setPadding(new Insets(10));
            warnContent.getChildren().addAll(new Label("Confirmation required:"), confirmField);
            warning.getDialogPane().setContent(warnContent);
            warning.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            
            Optional<ButtonType> res = warning.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK && "DELETE ALL".equalsIgnoreCase(confirmField.getText().trim())) {
                for (UserAccountRow row : allUsers) {
                    adminControl.deleteUser(row.getUsername());
                }
                switchAdminContent("users");
                showAlert("Success", allUsers.size() + " user(s) deleted. Database cleared.", Alert.AlertType.INFORMATION);
            } else if (res.isPresent() && res.get() == ButtonType.OK) {
                showAlert("Cancelled", "Confirmation text did not match. No users were deleted.", Alert.AlertType.WARNING);
            }
        });
        
        actionRow.getChildren().addAll(deleteSelectedBtn, deleteMultipleBtn, deleteAllBtn);
        actionCard.getChildren().addAll(actionRow);
        
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
        
        HBox statsRow = new HBox();
        statsRow.setSpacing(50);
        statsRow.setAlignment(Pos.CENTER);
        
        int totalUsers = azureApp.getUsers().size();
        int activeUsers = (int) azureApp.getUsers().values().stream()
            .filter(u -> u.getBalance() > 0).count();
        int inactiveUsers = totalUsers - activeUsers;
        
        VBox totalStat = createStatusItem("👥 Total Users", String.valueOf(totalUsers));
        VBox activeStat = createStatusItem("🔥 Active Users", String.valueOf(activeUsers));
        VBox inactiveStat = createStatusItem("💤 Inactive Users", String.valueOf(inactiveUsers));
        
        HBox.setHgrow(totalStat, Priority.ALWAYS);
        HBox.setHgrow(activeStat, Priority.ALWAYS);
        HBox.setHgrow(inactiveStat, Priority.ALWAYS);
        
        statsRow.getChildren().addAll(totalStat, activeStat, inactiveStat);
        
        statsCard.getChildren().addAll(statsTitle, statsRow);
        
        content.getChildren().addAll(pageTitle, searchCard, tableCard, actionCard, statsCard);
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
        Button maintenanceBtn = createModernButton("🧹 Maintenance Clean", "#F97316");
        Button disableMaintenanceBtn = createModernButton("✅ End Maintenance", "#10B981");
        
        schedulerBtn.setOnAction(e -> {
            adminControl.triggerScheduler();
            showAlert("Success", "Scheduler executed successfully!", Alert.AlertType.INFORMATION);
        });
        
        revenueBtn.setOnAction(e -> showRevenueDialog());
        
        maintenanceBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Maintenance");
            confirm.setHeaderText("Perform maintenance clean?");
            confirm.setContentText("This will remove all vouchers and enable maintenance notification on the login screen. Continue?");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                adminControl.performMaintenanceClean();
                showAlert("Success", "Maintenance clean completed. Login screen will show maintenance notice.", Alert.AlertType.INFORMATION);
                    switchAdminContent("system");
            }
        });
        
        
        disableMaintenanceBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("End Maintenance");
            confirm.setHeaderText("Disable maintenance mode?");
            confirm.setContentText("This will remove the maintenance notification from the login screen. Continue?");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                adminControl.endMaintenance();
                showAlert("Success", "Maintenance mode disabled.", Alert.AlertType.INFORMATION);
                switchAdminContent("system");
            }
        });
        actionsRow.getChildren().addAll(schedulerBtn, revenueBtn, maintenanceBtn, disableMaintenanceBtn);
        
        actionsCard.getChildren().addAll(actionsTitle, actionsRow);
        
        // Data Reset/Cleanup Card
        VBox resetCard = new VBox(20);
        resetCard.setPadding(new Insets(25));
        resetCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        
        Label resetTitle = new Label("Data Reset & Cleanup");
        resetTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        resetTitle.setStyle("-fx-text-fill: #1E293B;");
        
        HBox resetRow = new HBox(15);
        resetRow.setAlignment(Pos.CENTER);
        
        Button resetLogsBtn = createModernButton("🗑️ Clear Admin Logs", "#EF4444");
        Button resetRevenueBtn = createModernButton("💸 Reset Revenue", "#EF4444");
        Button resetVouchersBtn = createModernButton("🎟️ Clear Vouchers", "#EF4444");
        
        resetLogsBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Clear Admin Logs");
            confirm.setHeaderText("Remove all admin activity logs?");
            confirm.setContentText("This action cannot be undone. All admin activity records will be permanently deleted. Continue?");
            confirm.getDialogPane().setStyle("-fx-font-size: 12;");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                adminControl.clearAdminLogs();
                showAlert("Success", "Admin logs cleared successfully.", Alert.AlertType.INFORMATION);
                switchAdminContent("system");
            }
        });
        
        resetRevenueBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Reset Revenue");
            confirm.setHeaderText("Clear all system revenue records?");
            confirm.setContentText("This action cannot be undone. All revenue data will be permanently deleted. Continue?");
            confirm.getDialogPane().setStyle("-fx-font-size: 12;");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                adminControl.clearSystemRevenue();
                showAlert("Success", "Revenue records cleared successfully.", Alert.AlertType.INFORMATION);
                switchAdminContent("system");
            }
        });
        
        resetVouchersBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Clear Vouchers");
            confirm.setHeaderText("Remove all vouchers?");
            confirm.setContentText("This action cannot be undone. All active vouchers will be permanently deleted. Continue?");
            confirm.getDialogPane().setStyle("-fx-font-size: 12;");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                adminControl.clearVouchersData();
                showAlert("Success", "Vouchers cleared successfully.", Alert.AlertType.INFORMATION);
                switchAdminContent("system");
            }
        });
        
        resetRow.getChildren().addAll(resetLogsBtn, resetRevenueBtn, resetVouchersBtn);
        resetCard.getChildren().addAll(resetTitle, resetRow);
        
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
        
        HBox statusRow = new HBox();
        statusRow.setSpacing(50);
        statusRow.setAlignment(Pos.CENTER);
        
        VBox schedulerStatus = createStatusItem("⏰ Scheduler", "Last Run: " + azureApp.getFileManager().readLastSchedulerRun());
        VBox dataStatus = createStatusItem("💾 Data Files", "All systems operational");
        VBox securityStatus = createStatusItem("🔒 Security", "Admin access active");
        
        // Make status items equal width
        HBox.setHgrow(schedulerStatus, Priority.ALWAYS);
        HBox.setHgrow(dataStatus, Priority.ALWAYS);
        HBox.setHgrow(securityStatus, Priority.ALWAYS);
        
        statusRow.getChildren().addAll(schedulerStatus, dataStatus, securityStatus);
        
        statusCard.getChildren().addAll(statusTitle, statusRow);
        
        content.getChildren().addAll(pageTitle, actionsCard, resetCard, statusCard);
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
    
    // Helper method for styled table cells
    private TableCell<UserAccountRow, String> createStyledTableCell() {
        return new TableCell<UserAccountRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #1E293B; -fx-padding: 8px;");
                }
            }
        };
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
                "-fx-background-color: #FFD700; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #FFD700; " +
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
    
    // Helper class for user table
    public static class UserAccountRow {
        private javafx.beans.property.BooleanProperty selected;
        private javafx.beans.property.StringProperty username;
        private javafx.beans.property.StringProperty mobile;
        private javafx.beans.property.StringProperty balance;
        private javafx.beans.property.StringProperty rank;
        private javafx.beans.property.StringProperty points;
        
        public UserAccountRow(UserAccount user) {
            this.selected = new javafx.beans.property.SimpleBooleanProperty(false);
            this.username = new javafx.beans.property.SimpleStringProperty(user.getUsername());
            this.mobile = new javafx.beans.property.SimpleStringProperty(user.getMobile());
            this.balance = new javafx.beans.property.SimpleStringProperty(String.format("%.2f", user.getBalance()));
            this.rank = new javafx.beans.property.SimpleStringProperty(user.getRank());
            this.points = new javafx.beans.property.SimpleStringProperty(String.valueOf(user.getPoints()));
        }
        
        public boolean isSelected() {
            return selected.get();
        }
        
        public javafx.beans.property.BooleanProperty selectedProperty() {
            return selected;
        }
        
        public String getUsername() {
            return username.get();
        }
        
        public javafx.beans.property.StringProperty usernameProperty() {
            return username;
        }
        
        public String getMobile() {
            return mobile.get();
        }
        
        public javafx.beans.property.StringProperty mobileProperty() {
            return mobile;
        }
        
        public String getBalance() {
            return balance.get();
        }
        
        public javafx.beans.property.StringProperty balanceProperty() {
            return balance;
        }
        
        public String getRank() {
            return rank.get();
        }
        
        public javafx.beans.property.StringProperty rankProperty() {
            return rank;
        }
        
        public String getPoints() {
            return points.get();
        }
        
        public javafx.beans.property.StringProperty pointsProperty() {
            return points;
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}