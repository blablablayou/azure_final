package azurewallet.main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.Popup;
import javafx.scene.Group;
import javafx.scene.shape.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.TranslateTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Animation;
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
        scene.setFill(Color.web("#57595B"));
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
        loginContainer.setPadding(new Insets(200, 0, 12, 0));
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
        
            // Welcome title and login form
            VBox welcomeBox = new VBox(6);
            welcomeBox.setAlignment(Pos.CENTER);
            Label titleLabel = new Label("");
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
            usernameField.setPromptText("Username/PhoneNumber");
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
            // Add Enter key functionality
            usernameField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handleLogin(usernameField, pinField);
                }
            });
            pinField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    handleLogin(usernameField, pinField);
                }
            });
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
        Hyperlink forgotLink = new Hyperlink("Click here");
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
        bottomRow.setPadding(new Insets(-10, 0, 0, 0));
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
        
        // Place content above the welcome title
        loginContainer.getChildren().addAll(
            welcomeBox, formWrapper, bottomRow
        );

        // Create logo - load from resources
        javafx.scene.image.ImageView logoImageView = new javafx.scene.image.ImageView();
        
        // Try to load the logo image from resources
        try {
            java.net.URL logoUrl = getClass().getResource("/images/azure-logo.png");
            if (logoUrl != null) {
                javafx.scene.image.Image logoImage = new javafx.scene.image.Image(logoUrl.toExternalForm());
                logoImageView.setImage(logoImage);
                logoImageView.setFitWidth(350);
                logoImageView.setFitHeight(350);
                logoImageView.setPreserveRatio(true);
                logoImageView.setSmooth(true);
            } else {
                System.out.println("Logo image not found at /images/azure-logo.png");
                createFallbackLogo(logoImageView);
            }
        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
            createFallbackLogo(logoImageView);
        }
        
        // Position logo as overlay on top without affecting layout
        VBox logoOverlay = new VBox(logoImageView);
        logoOverlay.setAlignment(Pos.TOP_CENTER);
        logoOverlay.setPadding(new Insets(20, 0, 30, 0));
        logoOverlay.setMouseTransparent(true);
        logoOverlay.setPickOnBounds(false);

        StackPane wrapper = new StackPane(loginContainer, logoOverlay);
        wrapper.getStyleClass().add("login-wrapper");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setCenter(wrapper);
    }
    
    // Create a fallback logo if image loading fails
    private void createFallbackLogo(javafx.scene.image.ImageView logoImageView) {
        // Create a canvas-based logo with gradient circle and icons matching your design
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(300, 300);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw radial gradient circle (gold to orange)
        javafx.scene.paint.RadialGradient gradient = new javafx.scene.paint.RadialGradient(
            0, 0, 0.5, 0.5, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.web("#FFD700")),
            new javafx.scene.paint.Stop(0.7, Color.web("#FFC107")),
            new javafx.scene.paint.Stop(1, Color.web("#FF8C00"))
        );
        gc.setFill(gradient);
        gc.fillOval(0, 0, 300, 300);
        
        // Draw decorative dots around circle
        gc.setFill(Color.color(1, 1, 1, 0.6));
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            double x = 150 + 136.67 * Math.cos(rad);
            double y = 150 + 136.67 * Math.sin(rad);
            double size = (angle % 90 == 0) ? 8.75 : 6.25;
            gc.fillOval(x - size/2, y - size/2, size, size);
        }
        
        // Draw main arrow pointing up-right (white)
        gc.setFill(Color.color(1, 1, 1, 0.95));
        double[] arrow1X = {112.5, 162.5, 137.5};
        double[] arrow1Y = {125, 75, 162.5};
        gc.fillPolygon(arrow1X, arrow1Y, 3);
        
        // Draw secondary arrow pointing down-right
        gc.setFill(Color.color(1, 1, 1, 0.9));
        double[] arrow2X = {130, 180, 155};
        double[] arrow2Y = {187.5, 137.5, 225};
        gc.fillPolygon(arrow2X, arrow2Y, 3);
        
        // Draw dollar sign circle (top right)
        gc.setFill(Color.color(1, 1, 1, 0.95));
        gc.fillOval(195, 62.5, 45, 45);
        
        // Dollar sign text inside circle
        gc.setFill(Color.web("#FF8C00"));
        gc.setFont(javafx.scene.text.Font.font("System", 40));
        gc.fillText("$", 205, 97.5);
        
        // Draw smile/curve (white arc)
        gc.setStroke(Color.color(1, 1, 1, 0.85));
        gc.setLineWidth(12.5);
        
        // Draw curved smile using path
        javafx.scene.shape.Path smilePath = new javafx.scene.shape.Path();
        javafx.scene.shape.MoveTo moveTo = new javafx.scene.shape.MoveTo(87.5, 212.5);
        javafx.scene.shape.QuadCurveTo quadTo = new javafx.scene.shape.QuadCurveTo(150, 262.5, 212.5, 212.5);
        smilePath.getElements().addAll(moveTo, quadTo);
        smilePath.setStroke(Color.color(1, 1, 1, 0.85));
        smilePath.setStrokeWidth(12.5);
        
        // Draw decorative curved lines
        gc.setLineWidth(7.5);
        gc.setStroke(Color.color(1, 1, 1, 0.65));
        
        // Small accent dots inside
        gc.setFill(Color.color(1, 1, 1, 0.8));
        double[][] dots = {{87.5, 150}, {175, 125}, {137.5, 212.5}, {200, 225}};
        for (double[] dot : dots) {
            gc.fillOval(dot[0] - 3.75, dot[1] - 3.75, 7.5, 7.5);
        }
        
        // Create image from canvas
        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(300, 300);
        canvas.snapshot(null, writableImage);
        logoImageView.setImage(writableImage);
        logoImageView.setFitWidth(300);
        logoImageView.setFitHeight(300);
        logoImageView.setPreserveRatio(true);
    }
    
    private void handleLogin(TextField usernameField, PasswordField pinField) {
        String input = usernameField.getText().trim();
        String pin = pinField.getText().trim();
        
        if (input.isEmpty() || pin.isEmpty()) {
            showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
            return;
        }
        // If system maintenance mode is enabled, block sign-in but allow sign-up
        if (azureApp.getFileManager().isMaintenanceMode()) {
            showAlert("Maintenance", "Sign-in is temporarily disabled due to system maintenance. You may still create a new account.", Alert.AlertType.INFORMATION);
            return;
        }
        
        String username = input;
        
        // Check if input is a phone number (11 digits starting with 09)
        if (input.matches("^09\\d{9}$")) {
            // Find username by phone number
            String foundUsername = null;
            for (UserAccount user : azureApp.getUsers().values()) {
                if (user.getMobile().equals(input)) {
                    foundUsername = user.getUsername();
                    break;
                }
            }
            
            if (foundUsername != null) {
                username = foundUsername;
            } else {
                showAlert("Login Failed", 
                    "No account found with this phone number.", 
                    Alert.AlertType.ERROR);
                pinField.clear();
                return;
            }
        }
        
        // Call backend login method
        if (azureApp.loginUser(username, pin)) {
            currentUser = username;
            showMainScreen();
        } else {
            showAlert("Login Failed", 
                "Invalid username/phone or PIN.\nAccount locks after 3 failed attempts for 1 minute.", 
                Alert.AlertType.ERROR);
            pinField.clear();
        }
    }
    
    // Create a fallback logo if image loading fails
    
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
        
        // First Name and Last Name fields in 2-column layout
        HBox nameRow = new HBox(12);
        nameRow.setPrefWidth(Double.MAX_VALUE);
        
        VBox firstNameBox = new VBox(8);
        HBox.setHgrow(firstNameBox, Priority.ALWAYS);
        Label firstNameLabel = new Label("First Name");
        firstNameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        firstNameLabel.setStyle("-fx-text-fill: #000000;");
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("Enter first name");
        styleInputField(firstNameField);
        firstNameBox.getChildren().addAll(firstNameLabel, firstNameField);
        
        VBox lastNameBox = new VBox(8);
        HBox.setHgrow(lastNameBox, Priority.ALWAYS);
        Label lastNameLabel = new Label("Last Name");
        lastNameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        lastNameLabel.setStyle("-fx-text-fill: #000000;");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Enter last name");
        styleInputField(lastNameField);
        lastNameBox.getChildren().addAll(lastNameLabel, lastNameField);
        
        nameRow.getChildren().addAll(firstNameBox, lastNameBox);
        
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
        registerBtn.setOnAction(e -> handleRegistration(firstNameField, lastNameField, usernameField, mobileField, pinField));
        // Add Enter key functionality
        firstNameField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleRegistration(firstNameField, lastNameField, usernameField, mobileField, pinField);
            }
        });
        lastNameField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleRegistration(firstNameField, lastNameField, usernameField, mobileField, pinField);
            }
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleRegistration(firstNameField, lastNameField, usernameField, mobileField, pinField);
            }
        });
        mobileField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleRegistration(firstNameField, lastNameField, usernameField, mobileField, pinField);
            }
        });
        pinField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleRegistration(firstNameField, lastNameField, usernameField, mobileField, pinField);
            }
        });
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
            headerBox, nameRow, usernameBox, mobileBox, pinBox, registerBtn, backRow
        );
        
        root.setCenter(regContainer);
    }
    
    private void handleRegistration(TextField firstNameField, TextField lastNameField, TextField usernameField, TextField mobileField, PasswordField pinField) {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String mobile = mobileField.getText().trim();
        String pin = pinField.getText().trim();
        
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || mobile.isEmpty() || pin.isEmpty()) {
            showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
            return;
        }
        
        // Call backend registration method
        if (azureApp.registerUser(firstName, lastName, username, pin, mobile)) {
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
        avatarStack.setCursor(Cursor.HAND);
        
        // Create context menu for avatar
        ContextMenu avatarMenu = new ContextMenu();
        
        MenuItem helpItem = new MenuItem("Help");
        helpItem.setOnAction(e -> showHelpDialog());
        
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            currentUser = null;
            showLoginScreen();
        });
        
        avatarMenu.getItems().addAll(helpItem, logoutItem);
        
        // Make avatar clickable to show menu
        avatarStack.setOnMouseClicked(e -> {
            showUserMenu(avatarStack);
        });

        // Get user from backend
        UserAccount user = azureApp.getUser(currentUser);
        Label title = new Label("Welcome back, " + user.getFirstName() + " " + user.getLastName() + "!");
        title.setFont(Font.font("System", FontWeight.BOLD, 15));
        title.setStyle("-fx-text-fill: #000000;");
        
        Label accountNumber = new Label(user.getMobile());
        accountNumber.setFont(Font.font("System", 11));
        accountNumber.setStyle("-fx-text-fill: #64748B;");

        VBox nameBox = new VBox(4, title, accountNumber);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        // small left padding to separate username from avatar
        nameBox.setPadding(new Insets(0, 0, 0, 12));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(avatarStack, nameBox, spacer);
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
        
        // Get transactions and combine with voucher logs and points logs
        java.util.List<String> transactions = azureApp.getFileManager().getTransactionHistory(currentUser);
        java.util.List<String> allHistory = new java.util.ArrayList<>(transactions);
        
        // Add voucher logs in transaction format
        try (java.io.BufferedReader voucherReader = new java.io.BufferedReader(
                new java.io.FileReader("src/azurewallet/data/voucher_log.txt"))) {
            String voucherLine;
            while ((voucherLine = voucherReader.readLine()) != null) {
                // Use " - username " pattern to match only logs for this user (avoid matching partial usernames)
                if (voucherLine.contains(" - " + currentUser + " ")) {
                    // Convert voucher log format to transaction format
                    // Original: "2025-11-26T10:30:45.123456789 - username redeemed CODE (PHP 1,000.00)"
                    // Convert to: "2025-11-26T10:30:45.123456789 - username: Redeem Voucher - PHP 1,000.00"
                    voucherLine = voucherLine.replace(" redeemed ", ": Redeem Voucher - ");
                    allHistory.add(voucherLine);
                }
            }
        } catch (Exception e) {
            // Ignore if voucher log doesn't exist or can't be read
        }
        
        // Add points logs in transaction format
        try (java.io.BufferedReader pointsReader = new java.io.BufferedReader(
                new java.io.FileReader("src/azurewallet/data/points_log.txt"))) {
            String pointsLine;
            while ((pointsLine = pointsReader.readLine()) != null) {
                // Use " - username " pattern to match only logs for this user (avoid matching partial usernames)
                if (pointsLine.contains(" - " + currentUser + " ")) {
                    // Handle earned points
                    // Original: "2025-11-26T10:30:45.123456789 - username earned 100 points (Description)"
                    // Convert to: "2025-11-26T10:30:45.123456789 - username: Earned Points - 100 pts"
                    if (pointsLine.contains(" earned ")) {
                        String[] parts = pointsLine.split(" earned ");
                        if (parts.length >= 2) {
                            String timeAndUser = parts[0];
                            String restOfLine = parts[1];
                            String[] pointsParts = restOfLine.split(" points ");
                            if (pointsParts.length >= 1) {
                                String points = pointsParts[0].trim();
                                String formattedLine = timeAndUser + ": Earned Points - " + points + " pts";
                                allHistory.add(formattedLine);
                            }
                        }
                    }
                    // Handle redeemed points
                    // Original: "2025-11-26T10:30:45.123456789 - username redeemed 100 points (converted to PHP 100.00)"
                    // Convert to: "2025-11-26T10:30:45.123456789 - username: Redeem Points - 100 pts"
                    else if (pointsLine.contains(" redeemed ")) {
                        String[] parts = pointsLine.split(" redeemed ");
                        if (parts.length >= 2) {
                            String timeAndUser = parts[0];
                            String restOfLine = parts[1];
                            String[] pointsParts = restOfLine.split(" points ");
                            if (pointsParts.length >= 1) {
                                String points = pointsParts[0].trim();
                                String formattedLine = timeAndUser + ": Redeem Points - " + points + " pts";
                                allHistory.add(formattedLine);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore if points log doesn't exist or can't be read
        }
        
        // Sort by date and time (newest first)
        allHistory.sort((a, b) -> {
            try {
                // Extract full timestamp from format: "2025-11-23T11:08:48.183924500"
                String timestampA = a.split(" - ")[0];
                String timestampB = b.split(" - ")[0];
                // Compare timestamps in reverse order (newest first)
                return timestampB.compareTo(timestampA);
            } catch (Exception e) {
                return 0;
            }
        });
        
        int totalLimit = Math.min(20, allHistory.size());
        
        if (totalLimit == 0) {
            Label noTrans = new Label("No transactions yet");
            noTrans.setFont(Font.font("System", 13));
            noTrans.setStyle("-fx-text-fill: #94A3B8;");
            transContent.getChildren().add(noTrans);
        } else {
            String lastDate = null;
            for (int i = 0; i < totalLimit; i++) {
                String trans = allHistory.get(i);
                
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
            // Format: "Deposit - PHP 5,000.00" or "Withdraw - PHP 1,000.00" or "Redeem Voucher - PHP 1,000.00" or "Earned Points - 100 pts"
            String type = "Transaction";
            String amount = "";
            
            if (transactionDetails.contains("Earned Points")) {
                type = "Earned Points";
                // Extract points value from format: "Earned Points - 100 pts"
                int dashIndex = transactionDetails.indexOf("Earned Points - ");
                if (dashIndex >= 0) {
                    String afterDash = transactionDetails.substring(dashIndex + 16).trim();
                    // Get everything up to " pts" or just the number part
                    if (afterDash.contains(" pts")) {
                        String ptsNum = afterDash.substring(0, afterDash.indexOf(" pts")).trim();
                        // Calculate PHP equivalent (1000 PHP = 1 pts, so pts * 1000 = PHP)
                        try {
                            int pts = Integer.parseInt(ptsNum);
                            double phpEquivalent = pts * 1000.0;
                            amount = ptsNum + " pts (₱" + String.format("%.0f", phpEquivalent) + ")";
                        } catch (Exception e) {
                            amount = afterDash.substring(0, afterDash.indexOf(" pts")).trim() + " pts";
                        }
                    } else {
                        amount = afterDash;
                    }
                }
            } else if (transactionDetails.contains("Redeem Points")) {
                type = "Redeem Points";
                // Extract points value from format: "Redeem Points - 100 pts"
                int dashIndex = transactionDetails.indexOf("Redeem Points - ");
                if (dashIndex >= 0) {
                    String afterDash = transactionDetails.substring(dashIndex + 16).trim();
                    // Get everything up to " pts" or just the number part
                    if (afterDash.contains(" pts")) {
                        String ptsNum = afterDash.substring(0, afterDash.indexOf(" pts")).trim();
                        // Calculate PHP equivalent (1000 PHP = 1 pts, so pts * 1000 = PHP)
                        try {
                            int pts = Integer.parseInt(ptsNum);
                            double phpEquivalent = pts * 1000.0;
                            amount = ptsNum + " pts (₱" + String.format("%.0f", phpEquivalent) + ")";
                        } catch (Exception e) {
                            amount = afterDash.substring(0, afterDash.indexOf(" pts")).trim() + " pts";
                        }
                    } else {
                        amount = afterDash;
                    }
                }
            } else if (transactionDetails.contains("Deposit")) {
                type = "Deposit";
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Redeem Voucher")) {
                type = "Redeem Voucher";
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
            } else if (transactionDetails.contains("Send to")) {
                type = "Send";
                // Extract recipient name from format: "Send to USERNAME - PHP 1,000.00"
                int toIndex = transactionDetails.indexOf("Send to ") + 8;
                int dashIndex = transactionDetails.indexOf(" -");
                if (dashIndex > toIndex) {
                    String recipient = transactionDetails.substring(toIndex, dashIndex).trim();
                    type = "Send to " + recipient;
                }
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Receive from")) {
                type = "Receive";
                // Extract sender name from format: "Receive from USERNAME - PHP 1,000.00"
                int fromIndex = transactionDetails.indexOf("Receive from ") + 13;
                int dashIndex = transactionDetails.indexOf(" -");
                if (dashIndex > fromIndex) {
                    String sender = transactionDetails.substring(fromIndex, dashIndex).trim();
                    type = "Receive from " + sender;
                }
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
                // Extract destination from format: "Withdraw to PayMaya - John Doe (09123456789) - PHP 5,000.00"
                int toIndex = transactionDetails.indexOf("Withdraw to ") + 12;
                int dashIndex = transactionDetails.lastIndexOf(" -");
                if (dashIndex > toIndex && dashIndex >= 0) {
                    String destination = transactionDetails.substring(toIndex, dashIndex).trim();
                    type = "Withdraw to " + destination;
                }
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Bills Payment to")) {
                type = "Bills Payment";
                // Extract biller name from format: "Bills Payment to BILLER (Acct: XXXX) - PHP 5,000.00"
                int toIndex = transactionDetails.indexOf("Bills Payment to ") + 17;
                int acctIndex = transactionDetails.indexOf(" (Acct:");
                if (acctIndex > toIndex) {
                    String biller = transactionDetails.substring(toIndex, acctIndex).trim();
                    type = "Pay " + biller;
                }
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Prepaid Load Purchase")) {
                type = "Prepaid Load";
                // Extract network and number from format: "Prepaid Load Purchase (NETWORK - NUMBER) - PHP 500.00"
                int startParen = transactionDetails.indexOf("(");
                int endParen = transactionDetails.indexOf(")");
                if (startParen >= 0 && endParen > startParen) {
                    String networkInfo = transactionDetails.substring(startParen + 1, endParen).trim();
                    type = "Load " + networkInfo;
                }
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Paid to")) {
                type = "Payment";
                // Extract merchant name from format: "Paid to MERCHANT - PHP 1,000.00"
                int toIndex = transactionDetails.indexOf("Paid to ") + 8;
                int dashIndex = transactionDetails.indexOf(" -");
                if (dashIndex > toIndex) {
                    String merchant = transactionDetails.substring(toIndex, dashIndex).trim();
                    type = "Pay " + merchant;
                }
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Buy ")) {
                type = "Shopping";
                // Extract product name from format: "Buy PRODUCT - PHP 500.00"
                int buyIndex = transactionDetails.indexOf("Buy ") + 4;
                int dashIndex = transactionDetails.indexOf(" -");
                if (dashIndex > buyIndex) {
                    String product = transactionDetails.substring(buyIndex, dashIndex).trim();
                    type = "Buy " + product;
                }
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Top Up Azure Virtual Card")) {
                type = "Top Up Virtual Card";
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            } else if (transactionDetails.contains("Service Fee")) {
                type = "Service Fee";
                // Extract what the fee is for: "Service Fee (Bills)", "Service Fee (Load)", etc.
                int startParen = transactionDetails.indexOf("(");
                int endParen = transactionDetails.indexOf(")");
                if (startParen >= 0 && endParen > startParen) {
                    String feeType = transactionDetails.substring(startParen + 1, endParen).trim();
                    type = "Fee - " + feeType;
                }
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            }
            
            // If no specific type matched, use a descriptive generic label
            if (type.equals("Transaction")) {
                // Extract the entire transaction detail as the type
                if (transactionDetails.length() > 50) {
                    type = transactionDetails.substring(0, 47) + "...";
                } else {
                    type = transactionDetails;
                }
                // Try to extract PHP amount if present
                int phpIndex = transactionDetails.indexOf("PHP");
                if (phpIndex > 0 && amount.isEmpty()) {
                    amount = transactionDetails.substring(phpIndex + 3).trim();
                }
            }
            
            // Transaction type icon
            Label icon = new Label();
            if (type.startsWith("Pay ")) {
                icon.setText("📄");
            } else if (type.startsWith("Buy ")) {
                icon.setText("🛍️");
            } else if (type.startsWith("Load ")) {
                icon.setText("📱");
            } else if (type.startsWith("Fee")) {
                icon.setText("⚙️");
            } else if (type.startsWith("Send to")) {
                icon.setText("➡️");
            } else if (type.startsWith("Receive from")) {
                icon.setText("⬅️");
            } else if (type.startsWith("Withdraw to")) {
                icon.setText("💸");
            } else {
                switch (type) {
                    case "Deposit": icon.setText("➕"); break;
                    case "Earned Points": icon.setText("💎"); break;
                    case "Redeem Points": icon.setText("💸"); break;
                    case "Redeem Voucher": icon.setText("🎟️"); break;
                    case "Withdraw": icon.setText("➖"); break;
                    case "Send": icon.setText("➡️"); break;
                    case "Receive": icon.setText("⬅️"); break;
                    case "Service Fee": icon.setText("⚙️"); break;
                    case "Top Up Virtual Card": icon.setText("💳"); break;
                    default: icon.setText("📝");
                }
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
            
            // Amount - with different styling for points
            Label amountLabel = new Label(amount);
            amountLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            
            // Color coding: points in purple, PHP in green
            if (type.equals("Earned Points") || type.equals("Redeem Points")) {
                amountLabel.setStyle("-fx-text-fill: #7C3AED;");
            } else {
                amountLabel.setStyle("-fx-text-fill: #10B981;");
            }
            
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
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        UserAccount user = azureApp.getUser(currentUser);

        // Header Section
        Label headerTitle = new Label("Deposit Money");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerDesc = new Label("Add funds to your Azure Wallet");
        headerDesc.setFont(Font.font("System", 13));
        headerDesc.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, headerTitle, headerDesc);

        // Deposit Source Section
        VBox sourceSection = new VBox(12);
        sourceSection.setPadding(new Insets(16));
        sourceSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label sourceLabel = new Label("Deposit Source");
        sourceLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        sourceLabel.setStyle("-fx-text-fill: #1E293B;");

        GridPane sourceButtonGrid = new GridPane();
        sourceButtonGrid.setHgap(12);
        sourceButtonGrid.setVgap(12);
        sourceButtonGrid.setPrefWidth(Double.MAX_VALUE);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        sourceButtonGrid.getColumnConstraints().addAll(col1, col2);

        ToggleGroup sourceGroup = new ToggleGroup();
        String[] sources = {"GCash", "PayMaya", "BPI", "BDO", "Metrobank"};
        
        int sourceRow = 0;
        int sourceCol = 0;
        
        for (String source : sources) {
            ToggleButton sourceBtn = new ToggleButton("🏦 " + source);
            sourceBtn.setToggleGroup(sourceGroup);
            sourceBtn.setMaxWidth(Double.MAX_VALUE);
            sourceBtn.setPrefHeight(50);
            sourceBtn.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
            sourceBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 2; " +
                "-fx-text-fill: #1E293B; " +
                "-fx-cursor: hand;"
            );
            
            // Set first button as selected
            if ("GCash".equals(source)) {
                sourceBtn.setSelected(true);
                sourceBtn.setStyle(
                    "-fx-background-color: #FEFCE8; " +
                    "-fx-border-color: #F59E0B; " +
                    "-fx-border-radius: 8; " +
                    "-fx-border-width: 2; " +
                    "-fx-text-fill: #78350F; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand;"
                );
            }
            
            final ToggleButton btn = sourceBtn;
            sourceBtn.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    btn.setStyle(
                        "-fx-background-color: #FEFCE8; " +
                        "-fx-border-color: #F59E0B; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-text-fill: #78350F; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;"
                    );
                } else {
                    btn.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-text-fill: #1E293B; " +
                        "-fx-cursor: hand;"
                    );
                }
            });
            
            sourceButtonGrid.add(btn, sourceCol, sourceRow);
            GridPane.setHgrow(btn, Priority.ALWAYS);
            
            sourceCol++;
            if (sourceCol >= 2) {
                sourceCol = 0;
                sourceRow++;
            }
        }

        sourceSection.getChildren().addAll(sourceLabel, sourceButtonGrid);

        // Amount Section
        VBox amountSection = new VBox(12);
        amountSection.setPadding(new Insets(16));
        amountSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label amountLabel = new Label("Deposit Amount");
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        amountLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount to deposit");
        amountField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        amountField.setPrefHeight(45);
        amountField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldV);
            }
        });

        Label presetLabel = new Label("Quick Amounts");
        presetLabel.setFont(Font.font("System", 11));
        presetLabel.setStyle("-fx-text-fill: #64748B;");

        GridPane presetsGrid = new GridPane();
        presetsGrid.setHgap(10);
        presetsGrid.setVgap(10);
        
        for (int c = 0; c < 4; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            presetsGrid.getColumnConstraints().add(cc);
        }

        int[] presets = {50, 100, 200, 500, 1000, 2000, 5000, 10000};
        ToggleGroup presetGroup = new ToggleGroup();
        
        for (int i = 0; i < presets.length; i++) {
            int value = presets[i];
            ToggleButton presetBtn = new ToggleButton("₱" + value);
            presetBtn.setToggleGroup(presetGroup);
            presetBtn.setMaxWidth(Double.MAX_VALUE);
            presetBtn.setPrefHeight(45);
            presetBtn.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
            presetBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1.5; " +
                "-fx-text-fill: #1E293B; " +
                "-fx-cursor: hand;"
            );
            
            presetBtn.setOnAction(e -> {
                if (presetBtn.isSelected()) {
                    amountField.setText(String.valueOf(value));
                } else {
                    amountField.clear();
                }
            });
            
            final ToggleButton pBtn = presetBtn;
            presetBtn.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    pBtn.setStyle(
                        "-fx-background-color: #FEFCE8; " +
                        "-fx-border-color: #F59E0B; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-text-fill: #78350F; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;"
                    );
                } else {
                    pBtn.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-text-fill: #1E293B; " +
                        "-fx-cursor: hand;"
                    );
                }
            });
            
            int col = i % 4;
            int row = i / 4;
            presetsGrid.add(presetBtn, col, row);
            GridPane.setHgrow(presetBtn, Priority.ALWAYS);
        }

        amountSection.getChildren().addAll(amountLabel, amountField, presetLabel, presetsGrid);

        // Info Box
        Label infoBox = new Label("Deposits are processed instantly and credited to your wallet");
        infoBox.setFont(Font.font("System", 11));
        infoBox.setStyle(
            "-fx-background-color: #FEF3C7; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #78350F;"
        );
        infoBox.setWrapText(true);

        // Confirmation Section
        VBox confirmSection = new VBox(12);
        confirmSection.setPadding(new Insets(16));
        confirmSection.setStyle(
            "-fx-background-color: #FEFCE8; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1;"
        );

        CheckBox confirmBox = new CheckBox("I confirm that the deposit details are correct");
        confirmBox.setFont(Font.font("System", 12));
        confirmBox.setStyle("-fx-text-fill: #78350F;");

        confirmSection.getChildren().add(confirmBox);

        mainContent.getChildren().addAll(headerBox, sourceSection, amountSection, infoBox, confirmSection);

        dialog.getDialogPane().setContent(mainContent);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(750);

        ButtonType depositType = new ButtonType("Deposit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(depositType, cancelType);

        // Style the buttons
        for (ButtonType btn : dialog.getDialogPane().getButtonTypes()) {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btn);
            if (button != null) {
                button.setStyle(
                    "-fx-padding: 12 32; " +
                    "-fx-font-size: 13; " +
                    "-fx-font-weight: bold; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;"
                );
                if (btn == depositType) {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #F59E0B; " +
                        "-fx-text-fill: white;");
                } else {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #E2E8F0; " +
                        "-fx-text-fill: #475569;");
                }
            }
        }

        // Validation and result handling
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == depositType) {
                try {
                    double amt = Double.parseDouble(amountField.getText().trim());
                    
                    if (amt <= 0) {
                        showAlert("Error", "Please enter a valid amount.", Alert.AlertType.ERROR);
                        return null;
                    }

                    if (!confirmBox.isSelected()) {
                        showAlert("Error", "Please confirm the deposit details before proceeding.", Alert.AlertType.ERROR);
                        return null;
                    }

                    String source = ((ToggleButton) sourceGroup.getSelectedToggle()).getText().replaceAll("🏦 ", "");
                    String reference = "AZR-" + (int)(Math.random() * 900000 + 100000);
                    
                    showDepositConfirmationDialog(source, amt, reference, user);
                    return depositType;
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }
    
    private void showDepositConfirmationDialog(final String source, final double amount, final String reference, final UserAccount user) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Deposit Successful");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#E0E7FF"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#3B82F6"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Deposit...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #F59E0B;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and message (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(200);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Deposit Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Your deposit has been completed");
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Create overlay stack pane with loading on top
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(200);
        overlayPane.getChildren().addAll(successBox, loadingBox);
        StackPane.setAlignment(successBox, Pos.CENTER);
        StackPane.setAlignment(loadingBox, Pos.CENTER);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Receipt Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox sourceReceiptRow = new HBox(15);
        Label sourceReceiptLabel = new Label("Source:");
        sourceReceiptLabel.setPrefWidth(80);
        sourceReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label sourceReceiptValue = new Label(source);
        sourceReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        sourceReceiptRow.getChildren().addAll(sourceReceiptLabel, sourceReceiptValue);
        
        HBox amountReceiptRow = new HBox(15);
        Label amountReceiptLabel = new Label("Amount:");
        amountReceiptLabel.setPrefWidth(80);
        amountReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountReceiptValue = new Label(String.format("₱%.2f", amount));
        amountReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountReceiptValue.setStyle("-fx-text-fill: #10B981;");
        amountReceiptRow.getChildren().addAll(amountReceiptLabel, amountReceiptValue);
        
        HBox refReceiptRow = new HBox(15);
        Label refReceiptLabel = new Label("Reference:");
        refReceiptLabel.setPrefWidth(80);
        refReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label refReceiptValue = new Label(reference);
        refReceiptValue.setFont(Font.font("System", 11));
        refReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        refReceiptRow.getChildren().addAll(refReceiptLabel, refReceiptValue);
        
        HBox timeReceiptRow = new HBox(15);
        Label timeReceiptLabel = new Label("Time:");
        timeReceiptLabel.setPrefWidth(80);
        timeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeReceiptValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        timeReceiptRow.getChildren().addAll(timeReceiptLabel, timeReceiptValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            sourceReceiptRow,
            amountReceiptRow,
            refReceiptRow,
            timeReceiptRow
        );
        
        // Confirmation message (initially hidden)
        Label confirmMsg = new Label(
            "Your deposit has been processed successfully.\n" +
            "Your new balance is now available in your wallet."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #FEFCE8; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #78350F;"
        );
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle(confirmMsg.getStyle() + " -fx-opacity: 0;");
        
        // Initially show overlay with loading on top of success box
        content.getChildren().addAll(overlayPane, receiptBox, confirmMsg);
        
        // Process the actual deposit
        if (user != null) {
            String source_ref = source + " (ref " + reference + ")";
            azureApp.deposit(currentUser, amount, source_ref);
            azureApp.getFileManager().saveUsers(azureApp.getUsers());
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(600);
        
        // Schedule transition from loading to success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second loading animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Platform.runLater(() -> {
                // Fade out loading box to reveal success box beneath it
                FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                fadeOutLoading.setFromValue(1);
                fadeOutLoading.setToValue(0);
                
                // Fade in success box (checkmark, title, message)
                FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                fadeInSuccess.setFromValue(0);
                fadeInSuccess.setToValue(1);
                
                // Fade in receipt
                FadeTransition fadeInReceipt = new FadeTransition(Duration.millis(500), receiptBox);
                fadeInReceipt.setFromValue(0);
                fadeInReceipt.setToValue(1);
                
                // Fade in confirmation
                FadeTransition fadeInConfirm = new FadeTransition(Duration.millis(500), confirmMsg);
                fadeInConfirm.setFromValue(0);
                fadeInConfirm.setToValue(1);
                
                fadeOutLoading.play();
                fadeInSuccess.play();
                fadeInReceipt.play();
                fadeInConfirm.play();
            });
        }).start();
        
        dialog.showAndWait();
        
        // Refresh the main screen after deposit
        refreshMainScreen();
    }
    
    private void showPaymentReceiptDialog(final String merchant, final double amount, final double fee, final double total, final String paymentMethod, final String reference) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Payment Receipt");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#E0E7FF"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#3B82F6"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Payment...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #3B82F6;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and title (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(150);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 60));
        checkmark.setStyle("-fx-text-fill: #10B981;");
        
        Label successTitle = new Label("Payment Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #1E293B;");
        
        successBox.getChildren().addAll(checkmark, successTitle);
        
        // Overlay pane - loading and success will be on top of each other
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(150);
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.getChildren().addAll(loadingBox, successBox);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Receipt Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox merchantRow = new HBox(15);
        Label merchantLabel = new Label("Merchant:");
        merchantLabel.setPrefWidth(100);
        merchantLabel.setStyle("-fx-text-fill: #64748B;");
        Label merchantValue = new Label(merchant);
        merchantValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        merchantValue.setStyle("-fx-text-fill: #1E293B;");
        merchantRow.getChildren().addAll(merchantLabel, merchantValue);
        
        HBox amountRow = new HBox(15);
        Label amountLabel = new Label("Amount:");
        amountLabel.setPrefWidth(100);
        amountLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountValue = new Label(String.format("₱%.2f", amount));
        amountValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountValue.setStyle("-fx-text-fill: #10B981;");
        amountRow.getChildren().addAll(amountLabel, amountValue);
        
        HBox feeRow = new HBox(15);
        Label feeLabel = new Label("Service Fee:");
        feeLabel.setPrefWidth(100);
        feeLabel.setStyle("-fx-text-fill: #64748B;");
        Label feeValue = new Label(String.format("₱%.2f", fee));
        feeValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        feeValue.setStyle("-fx-text-fill: #F59E0B;");
        feeRow.getChildren().addAll(feeLabel, feeValue);
        
        Separator divider = new Separator();
        divider.setStyle("-fx-border-color: #E2E8F0;");
        
        HBox totalRow = new HBox(15);
        Label totalLabel = new Label("Total:");
        totalLabel.setPrefWidth(100);
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalLabel.setStyle("-fx-text-fill: #1E293B;");
        Label totalValue = new Label(String.format("₱%.2f", total));
        totalValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalValue.setStyle("-fx-text-fill: #FFD700;");
        totalRow.getChildren().addAll(totalLabel, totalValue);
        
        HBox methodRow = new HBox(15);
        Label methodLabel = new Label("Payment Method:");
        methodLabel.setPrefWidth(100);
        methodLabel.setStyle("-fx-text-fill: #64748B;");
        Label methodValue = new Label(paymentMethod);
        methodValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        methodValue.setStyle("-fx-text-fill: #1E293B;");
        methodRow.getChildren().addAll(methodLabel, methodValue);
        
        HBox referenceRow = new HBox(15);
        Label refLabel = new Label("Reference:");
        refLabel.setPrefWidth(100);
        refLabel.setStyle("-fx-text-fill: #64748B;");
        Label refValue = new Label(reference);
        refValue.setFont(Font.font("System", 11));
        refValue.setStyle("-fx-text-fill: #1E293B;");
        referenceRow.getChildren().addAll(refLabel, refValue);
        
        HBox timeRow = new HBox(15);
        Label timeLabel = new Label("Time:");
        timeLabel.setPrefWidth(100);
        timeLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeValue.setStyle("-fx-text-fill: #1E293B;");
        timeRow.getChildren().addAll(timeLabel, timeValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            merchantRow,
            amountRow,
            feeRow,
            divider,
            totalRow,
            methodRow,
            referenceRow,
            timeRow
        );
        
        Label confirmMsg = new Label("Your payment has been processed successfully.\nThe merchant has been notified about this transaction.");
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #E0F2FE; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #7DD3FC; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #0369A1;"
        );
        confirmMsg.setWrapText(true);
        
        content.getChildren().addAll(loadingBox, successBox, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(600);
        
        // Simulate processing and show success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    // Fade out loading
                    FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                    fadeOutLoading.setFromValue(1.0);
                    fadeOutLoading.setToValue(0.0);
                    
                    // Fade in success
                    FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                    fadeInSuccess.setFromValue(0.0);
                    fadeInSuccess.setToValue(1.0);
                    
                    fadeOutLoading.play();
                    fadeInSuccess.play();
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        dialog.showAndWait();
    }
    
    private void showVirtualCardTopUpDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Top Up Virtual Card");
        dialog.setHeaderText("Add funds to your Azure Virtual Card only");

        VBox mainContent = new VBox(16);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: #F8FAFC;");

        // Header info
        Label headerTitle = new Label("Top Up Virtual Card");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerInfo = new Label("💳 This will add funds to your Virtual Card only\n(Does NOT affect your main wallet balance)");
        headerInfo.setFont(Font.font("System", 11));
        headerInfo.setStyle("-fx-text-fill: #64748B;");
        headerInfo.setWrapText(true);

        VBox headerBox = new VBox(6, headerTitle, headerInfo);

        // Amount section
        VBox amountSection = new VBox(12);
        amountSection.setPadding(new Insets(14));
        amountSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 1;"
        );

        Label amountLabel = new Label("Top Up Amount (₱)");
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        amountLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.setStyle(
            "-fx-padding: 12; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 12;"
        );
        amountField.setPrefHeight(40);
        amountField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldV);
            }
        });

        // Quick amounts
        HBox quickAmountsBox = new HBox(8);
        quickAmountsBox.setAlignment(Pos.CENTER_LEFT);
        int[] quickAmounts = {500, 1000, 2000, 5000};
        for (int amount : quickAmounts) {
            Button quickBtn = new Button("₱" + amount);
            quickBtn.setStyle(
                "-fx-padding: 8 12; " +
                "-fx-background-color: white; " +
                "-fx-border-color: #FFD700; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 6; " +
                "-fx-text-fill: #FFD700; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand;"
            );
            quickBtn.setOnAction(e -> amountField.setText(String.valueOf(amount)));
            quickAmountsBox.getChildren().add(quickBtn);
        }

        amountSection.getChildren().addAll(amountLabel, amountField, quickAmountsBox);

        // Deposit Source section
        VBox sourceSection = new VBox(12);
        sourceSection.setPadding(new Insets(14));
        sourceSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 1;"
        );

        Label sourceLabel = new Label("Deposit Source");
        sourceLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        sourceLabel.setStyle("-fx-text-fill: #1E293B;");

        HBox sourceBox = new HBox(8);
        sourceBox.setAlignment(Pos.CENTER_LEFT);
        String[] sources = new String[]{"GCash","PayMaya","BPI","BDO","Metrobank"};
        ToggleGroup sourceTG = new ToggleGroup();
        for (String s : sources) {
            ToggleButton tb = new ToggleButton(s);
            tb.setToggleGroup(sourceTG);
            String srcNormal = "-fx-background-radius:20; -fx-border-color:#E5E7EB; -fx-background-color:white; -fx-padding:8 12; -fx-font-size:11;";
            String srcSelected = "-fx-background-radius:20; -fx-border-color:#FFD700; -fx-background-color:#EFF6FF; -fx-padding:8 12; -fx-font-size:11;";
            tb.setStyle(srcNormal);
            tb.setMinWidth(72);
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

        sourceSection.getChildren().addAll(sourceLabel, sourceBox);

        // Virtual Card Info
        VBox cardInfoBox = new VBox(10);
        cardInfoBox.setPadding(new Insets(14));
        cardInfoBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFC107); " +
            "-fx-background-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);"
        );

        UserAccount user = azureApp.getUser(currentUser);
        String vbn = user != null ? user.getVirtualBankNumber() : null;

        if ((vbn == null || vbn.isEmpty()) && user != null) {
            vbn = azurewallet.utils.CardUtil.generateLuhn16();
            user.setVirtualBankNumber(vbn);
            azureApp.getFileManager().saveUsers(azureApp.getUsers());
        }

        String maskedCard;
        if (vbn == null || vbn.isEmpty() || vbn.length() < 4) {
            maskedCard = "Not Available";
        } else {
            String lastFour = vbn.substring(vbn.length() - 4);
            maskedCard = "**** **** **** " + lastFour;
        }

        Label cardTitle = new Label("Your Virtual Card");
        cardTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        cardTitle.setStyle("-fx-text-fill: white;");

        Label cardNumber = new Label("Card: " + maskedCard);
        cardNumber.setFont(Font.font("System", 11));
        cardNumber.setStyle("-fx-text-fill: white;");

        Label cardStatus = new Label("Status: ✓ Active");
        cardStatus.setFont(Font.font("System", 11));
        cardStatus.setStyle("-fx-text-fill: rgba(255,255,255,0.95);");

        cardInfoBox.getChildren().addAll(cardTitle, cardNumber, cardStatus);

        // Warning info
        Label warningLabel = new Label("⚠ Virtual Card funds are separate from your main wallet\nUse Top Up to add money only to the Virtual Card");
        warningLabel.setFont(Font.font("System", 10));
        warningLabel.setStyle(
            "-fx-text-fill: #92400E; " +
            "-fx-padding: 10; " +
            "-fx-background-color: #FEF3C7; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #FCD34D; " +
            "-fx-border-radius: 8;"
        );
        warningLabel.setWrapText(true);

        mainContent.getChildren().addAll(headerBox, amountSection, sourceSection, cardInfoBox, warningLabel);

        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(420);

        // Style buttons
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Add to Virtual Card");
        okBtn.setStyle(
            "-fx-padding: 12 28; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: #1E293B; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );
        okBtn.setDisable(true);

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle(
            "-fx-padding: 12 28; " +
            "-fx-font-size: 13; " +
            "-fx-background-color: #E2E8F0; " +
            "-fx-text-fill: #475569; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        // Enable OK button only when amount is valid
        amountField.textProperty().addListener((obs, oldV, newV) -> {
            try {
                double amt = Double.parseDouble(newV.trim());
                okBtn.setDisable(amt <= 0);
            } catch (Exception e) {
                okBtn.setDisable(true);
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    double topUpAmount = Double.parseDouble(amountField.getText().trim());
                    
                    if (topUpAmount <= 0) {
                        showAlert("Error", "Please enter a valid amount", Alert.AlertType.ERROR);
                        return;
                    }

                    // Get selected source
                    ToggleButton selectedSourceBtn = (ToggleButton) sourceTG.getSelectedToggle();
                    String selectedSource = selectedSourceBtn != null ? selectedSourceBtn.getText() : "Unknown";

                    // Add funds only to virtual card, not to wallet
                    if (user != null) {
                        // Get current virtual card balance or initialize it
                        double currentCardBalance = user.getVirtualCardBalance();
                        double newCardBalance = currentCardBalance + topUpAmount;
                        user.setVirtualCardBalance(newCardBalance);
                        azureApp.getFileManager().saveUsers(azureApp.getUsers());

                        // Log top-up to transactions with source
                        azureApp.getFileManager().logTransaction(currentUser, "Top Up Azure Virtual Card via " + selectedSource, topUpAmount);

                        // Show success dialog with loading and check animation
                        showTopUpSuccessDialog(selectedSource, topUpAmount, newCardBalance, user.getBalance());
                    } else {
                        showAlert("Error", "User not found", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showTopUpSuccessDialog(final String source, final double amount, final double newCardBalance, final double walletBalance) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Top Up Successful");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#E0E7FF"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#3B82F6"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Top Up...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #3B82F6;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and message (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(200);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Top Up Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Your virtual card has been topped up");
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Create overlay stack pane with loading on top
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(200);
        overlayPane.getChildren().addAll(successBox, loadingBox);
        StackPane.setAlignment(successBox, Pos.CENTER);
        StackPane.setAlignment(loadingBox, Pos.CENTER);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Transaction Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox sourceReceiptRow = new HBox(15);
        Label sourceReceiptLabel = new Label("Source:");
        sourceReceiptLabel.setPrefWidth(120);
        sourceReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label sourceReceiptValue = new Label(source);
        sourceReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        sourceReceiptRow.getChildren().addAll(sourceReceiptLabel, sourceReceiptValue);
        
        HBox amountReceiptRow = new HBox(15);
        Label amountReceiptLabel = new Label("Top Up Amount:");
        amountReceiptLabel.setPrefWidth(120);
        amountReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountReceiptValue = new Label(String.format("₱%.2f", amount));
        amountReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountReceiptValue.setStyle("-fx-text-fill: #10B981;");
        amountReceiptRow.getChildren().addAll(amountReceiptLabel, amountReceiptValue);
        
        HBox cardBalanceReceiptRow = new HBox(15);
        Label cardBalanceReceiptLabel = new Label("Virtual Card Balance:");
        cardBalanceReceiptLabel.setPrefWidth(120);
        cardBalanceReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label cardBalanceReceiptValue = new Label(String.format("₱%.2f", newCardBalance));
        cardBalanceReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        cardBalanceReceiptValue.setStyle("-fx-text-fill: #3B82F6;");
        cardBalanceReceiptRow.getChildren().addAll(cardBalanceReceiptLabel, cardBalanceReceiptValue);
        
        HBox walletBalanceReceiptRow = new HBox(15);
        Label walletBalanceReceiptLabel = new Label("Wallet Balance:");
        walletBalanceReceiptLabel.setPrefWidth(120);
        walletBalanceReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label walletBalanceReceiptValue = new Label(String.format("₱%.2f", walletBalance));
        walletBalanceReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        walletBalanceReceiptRow.getChildren().addAll(walletBalanceReceiptLabel, walletBalanceReceiptValue);
        
        HBox timeReceiptRow = new HBox(15);
        Label timeReceiptLabel = new Label("Time:");
        timeReceiptLabel.setPrefWidth(120);
        timeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeReceiptValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        timeReceiptRow.getChildren().addAll(timeReceiptLabel, timeReceiptValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            sourceReceiptRow,
            amountReceiptRow,
            cardBalanceReceiptRow,
            walletBalanceReceiptRow,
            timeReceiptRow
        );
        
        // Confirmation message (initially hidden)
        Label confirmMsg = new Label(
            "Your top-up has been processed successfully.\n" +
            "Your virtual card balance has been updated and is ready to use."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #E0F2FE; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #7DD3FC; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #0369A1;"
        );
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle(confirmMsg.getStyle() + " -fx-opacity: 0;");
        
        // Initially show overlay with loading on top of success box
        content.getChildren().addAll(overlayPane, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(650);
        
        // Schedule transition from loading to success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second loading animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Platform.runLater(() -> {
                // Fade out loading box to reveal success box beneath it
                FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                fadeOutLoading.setFromValue(1);
                fadeOutLoading.setToValue(0);
                
                // Fade in success box (checkmark, title, message)
                FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                fadeInSuccess.setFromValue(0);
                fadeInSuccess.setToValue(1);
                
                // Fade in receipt
                FadeTransition fadeInReceipt = new FadeTransition(Duration.millis(500), receiptBox);
                fadeInReceipt.setFromValue(0);
                fadeInReceipt.setToValue(1);
                
                // Fade in confirmation
                FadeTransition fadeInConfirm = new FadeTransition(Duration.millis(500), confirmMsg);
                fadeInConfirm.setFromValue(0);
                fadeInConfirm.setToValue(1);
                
                fadeOutLoading.play();
                fadeInSuccess.play();
                fadeInReceipt.play();
                fadeInConfirm.play();
            });
        }).start();
        
        dialog.showAndWait();
        
        // Refresh the main screen after top-up
        refreshMainScreen();
    }

    private void showWithdrawDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Withdraw Money");
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        // Header Section
        Label headerTitle = new Label("Withdraw Money");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerDesc = new Label("Transfer funds to your bank account or mobile wallet");
        headerDesc.setFont(Font.font("System", 13));
        headerDesc.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, headerTitle, headerDesc);

        // Withdrawal Method Section
        VBox methodSection = new VBox(12);
        methodSection.setPadding(new Insets(16));
        methodSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label methodLabel = new Label("Withdrawal Method");
        methodLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        methodLabel.setStyle("-fx-text-fill: #1E293B;");

        // Create toggle group for method selection
        ToggleGroup methodGroup = new ToggleGroup();
        
        GridPane methodButtonGrid = new GridPane();
        methodButtonGrid.setHgap(12);
        methodButtonGrid.setVgap(12);
        methodButtonGrid.setPrefWidth(Double.MAX_VALUE);
        
        String[] methods = {"GCash", "PayMaya", "BPI", "BDO", "Metrobank", ""};
        ToggleButton selectedMethodBtn = null;
        
        int gridRow = 0;
        int gridCol = 0;
        
        for (String method : methods) {
            if (method.isEmpty()) break;
            
            ToggleButton methodBtn = new ToggleButton("💳 " + method);
            methodBtn.setToggleGroup(methodGroup);
            methodBtn.setMaxWidth(Double.MAX_VALUE);
            methodBtn.setPrefHeight(60);
            methodBtn.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
            methodBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 2; " +
                "-fx-text-fill: #1E293B; " +
                "-fx-cursor: hand;"
            );
            
            // Set first button as selected
            if ("GCash".equals(method)) {
                methodBtn.setSelected(true);
                selectedMethodBtn = methodBtn;
                methodBtn.setStyle(
                    "-fx-background-color: #DBEAFE; " +
                    "-fx-border-color: #3B82F6; " +
                    "-fx-border-radius: 8; " +
                    "-fx-border-width: 2; " +
                    "-fx-text-fill: #1E40AF; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand;"
                );
            }
            
            // Add style change on selection
            final ToggleButton btn = methodBtn;
            methodBtn.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    btn.setStyle(
                        "-fx-background-color: #DBEAFE; " +
                        "-fx-border-color: #3B82F6; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-text-fill: #1E40AF; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;"
                    );
                } else {
                    btn.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-text-fill: #1E293B; " +
                        "-fx-cursor: hand;"
                    );
                }
            });
            
            methodButtonGrid.add(btn, gridCol, gridRow);
            GridPane.setHgrow(btn, Priority.ALWAYS);
            
            gridCol++;
            if (gridCol >= 2) {
                gridCol = 0;
                gridRow++;
            }
        }
        
        // Set column constraints for equal width
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        methodButtonGrid.getColumnConstraints().addAll(col1, col2);

        methodSection.getChildren().addAll(methodLabel, methodButtonGrid);

        // Account Details Section
        VBox detailsSection = new VBox(12);
        detailsSection.setPadding(new Insets(16));
        detailsSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label nameLabel = new Label("Account Holder Name");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField acctNameField = new TextField();
        acctNameField.setPromptText("Full name on the account");
        acctNameField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        acctNameField.setPrefHeight(45);

        Label numberLabel = new Label("Account Number");
        numberLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        numberLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField acctNumberField = new TextField();
        acctNumberField.setPromptText("Enter account number");
        acctNumberField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        acctNumberField.setPrefHeight(45);

        detailsSection.getChildren().addAll(nameLabel, acctNameField, numberLabel, acctNumberField);

        // Amount Section
        VBox amountSection = new VBox(12);
        amountSection.setPadding(new Insets(16));
        amountSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label amountLabel = new Label("Withdrawal Amount");
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        amountLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount to withdraw");
        amountField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        amountField.setPrefHeight(45);

        Label feeInfo = new Label("⚠️ Withdrawal Fee: ₱15.00 (will be deducted from your balance)");
        feeInfo.setFont(Font.font("System", 11));
        feeInfo.setStyle("-fx-text-fill: #F59E0B;");

        amountSection.getChildren().addAll(amountLabel, amountField, feeInfo);

        // Confirmation Section
        VBox confirmSection = new VBox(12);
        confirmSection.setPadding(new Insets(16));
        confirmSection.setStyle(
            "-fx-background-color: #FEF3C7; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1;"
        );

        CheckBox confirmBox = new CheckBox("I confirm that all the withdrawal details are correct");
        confirmBox.setFont(Font.font("System", 12));
        confirmBox.setStyle("-fx-text-fill: #78350F;");

        confirmSection.getChildren().add(confirmBox);

        mainContent.getChildren().addAll(headerBox, methodSection, detailsSection, amountSection, confirmSection);

        dialog.getDialogPane().setContent(mainContent);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(700);

        ButtonType withdrawType = new ButtonType("Withdraw", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(withdrawType, cancelType);

        // Style the buttons
        for (ButtonType btn : dialog.getDialogPane().getButtonTypes()) {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btn);
            if (button != null) {
                button.setStyle(
                    "-fx-padding: 12 32; " +
                    "-fx-font-size: 13; " +
                    "-fx-font-weight: bold; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;"
                );
                if (btn == withdrawType) {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #F59E0B; " +
                        "-fx-text-fill: white;");
                } else {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #E2E8F0; " +
                        "-fx-text-fill: #475569;");
                }
            }
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == withdrawType) {
                try {
                    double amt = Double.parseDouble(amountField.getText().trim());
                    
                    if (amt <= 0) {
                        showAlert("Error", "Please enter a valid amount.", Alert.AlertType.ERROR);
                        return null;
                    }

                    if (!confirmBox.isSelected()) {
                        showAlert("Error", "Please confirm the withdrawal before proceeding.", Alert.AlertType.ERROR);
                        return null;
                    }

                    String method = ((ToggleButton) methodGroup.getSelectedToggle()).getText().replaceAll("💳 ", "");
                    String name = acctNameField.getText().trim();
                    String number = acctNumberField.getText().trim();
                    
                    if (name.isEmpty() || number.isEmpty()) {
                        showAlert("Error", "Please enter account holder name and account number.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    // Validate account number based on method
                    boolean isValidNumber = false;
                    String errorMsg = "";
                    
                    if ("GCash".equals(method) || "PayMaya".equals(method)) {
                        if (number.matches("^09\\d{9}$")) {
                            isValidNumber = true;
                        } else if (number.matches("^\\d{10,11}$")) {
                            isValidNumber = true;
                        } else {
                            errorMsg = method + " number should be a phone number (09XXXXXXXXX) or account number (10-11 digits).";
                        }
                    } else if ("BPI".equals(method)) {
                        if (number.matches("^\\d{10}$")) {
                            isValidNumber = true;
                        } else {
                            errorMsg = "BPI account number should be 10 digits.";
                        }
                    } else if ("BDO".equals(method) || "Metrobank".equals(method)) {
                        if (number.matches("^\\d{16}$")) {
                            isValidNumber = true;
                        } else {
                            errorMsg = method + " account number should be 16 digits.";
                        }
                    }
                    
                    if (!isValidNumber) {
                        showAlert("Invalid Account Number", errorMsg, Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    String destination = method + " - " + name + " (" + number + ")";
                    if (azureApp.withdraw(currentUser, amt, destination)) {
                        showWithdrawSuccessDialog(method, name, number, amt);
                        return withdrawType;
                    } else {
                        showAlert("Error", "Withdrawal failed. Insufficient balance or invalid amount.", Alert.AlertType.ERROR);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showWithdrawSuccessDialog(final String method, final String accountName, final String accountNumber, final double amount) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Withdrawal Successful");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#E0E7FF"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#F59E0B"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Withdrawal...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #F59E0B;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and message (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(200);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Withdrawal Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Your funds will arrive in 1-3 business days");
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Create overlay stack pane with loading on top
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(200);
        overlayPane.getChildren().addAll(successBox, loadingBox);
        StackPane.setAlignment(successBox, Pos.CENTER);
        StackPane.setAlignment(loadingBox, Pos.CENTER);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Transaction Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox methodReceiptRow = new HBox(15);
        Label methodReceiptLabel = new Label("Method:");
        methodReceiptLabel.setPrefWidth(130);
        methodReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label methodReceiptValue = new Label(method);
        methodReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        methodReceiptRow.getChildren().addAll(methodReceiptLabel, methodReceiptValue);
        
        HBox nameReceiptRow = new HBox(15);
        Label nameReceiptLabel = new Label("Account Name:");
        nameReceiptLabel.setPrefWidth(130);
        nameReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label nameReceiptValue = new Label(accountName);
        nameReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        nameReceiptRow.getChildren().addAll(nameReceiptLabel, nameReceiptValue);
        
        HBox numberReceiptRow = new HBox(15);
        Label numberReceiptLabel = new Label("Account #:");
        numberReceiptLabel.setPrefWidth(130);
        numberReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label numberReceiptValue = new Label(accountNumber);
        numberReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        numberReceiptRow.getChildren().addAll(numberReceiptLabel, numberReceiptValue);
        
        HBox amountReceiptRow = new HBox(15);
        Label amountReceiptLabel = new Label("Withdrawal Amount:");
        amountReceiptLabel.setPrefWidth(130);
        amountReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountReceiptValue = new Label(String.format("₱%.2f", amount));
        amountReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountReceiptValue.setStyle("-fx-text-fill: #10B981;");
        amountReceiptRow.getChildren().addAll(amountReceiptLabel, amountReceiptValue);
        
        HBox feeReceiptRow = new HBox(15);
        Label feeReceiptLabel = new Label("Withdrawal Fee:");
        feeReceiptLabel.setPrefWidth(130);
        feeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label feeReceiptValue = new Label("₱15.00");
        feeReceiptValue.setStyle("-fx-text-fill: #F59E0B;");
        feeReceiptRow.getChildren().addAll(feeReceiptLabel, feeReceiptValue);
        
        HBox totalReceiptRow = new HBox(15);
        Label totalReceiptLabel = new Label("Total Deducted:");
        totalReceiptLabel.setPrefWidth(130);
        totalReceiptLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
        Label totalReceiptValue = new Label(String.format("₱%.2f", amount + 15.0));
        totalReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalReceiptValue.setStyle("-fx-text-fill: #F59E0B;");
        totalReceiptRow.getChildren().addAll(totalReceiptLabel, totalReceiptValue);
        
        HBox timeReceiptRow = new HBox(15);
        Label timeReceiptLabel = new Label("Time:");
        timeReceiptLabel.setPrefWidth(130);
        timeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeReceiptValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        timeReceiptRow.getChildren().addAll(timeReceiptLabel, timeReceiptValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            methodReceiptRow,
            nameReceiptRow,
            numberReceiptRow,
            amountReceiptRow,
            feeReceiptRow,
            totalReceiptRow,
            timeReceiptRow
        );
        
        // Confirmation message (initially hidden)
        Label confirmMsg = new Label(
            "Your withdrawal has been processed successfully.\n" +
            "Please allow 1-3 business days for the funds to arrive."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #FEFCE8; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #FDE047; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #78350F;"
        );
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle(confirmMsg.getStyle() + " -fx-opacity: 0;");
        
        // Initially show overlay with loading on top of success box
        content.getChildren().addAll(overlayPane, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(750);
        
        // Schedule transition from loading to success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second loading animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Platform.runLater(() -> {
                // Fade out loading box to reveal success box beneath it
                FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                fadeOutLoading.setFromValue(1);
                fadeOutLoading.setToValue(0);
                
                // Fade in success box (checkmark, title, message)
                FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                fadeInSuccess.setFromValue(0);
                fadeInSuccess.setToValue(1);
                
                // Fade in receipt
                FadeTransition fadeInReceipt = new FadeTransition(Duration.millis(500), receiptBox);
                fadeInReceipt.setFromValue(0);
                fadeInReceipt.setToValue(1);
                
                // Fade in confirmation
                FadeTransition fadeInConfirm = new FadeTransition(Duration.millis(500), confirmMsg);
                fadeInConfirm.setFromValue(0);
                fadeInConfirm.setToValue(1);
                
                fadeOutLoading.play();
                fadeInSuccess.play();
                fadeInReceipt.play();
                fadeInConfirm.play();
            });
        }).start();
        
        dialog.showAndWait();
        
        // Refresh the main screen after withdrawal
        refreshMainScreen();
    }
    
    private void showWithdrawReceiptDialog(final String method, final String accountName, final String accountNumber, final double amount, final String destination) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Withdrawal Receipt");
        dialog.setHeaderText(null);

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");

        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#E0E7FF"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#3B82F6"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Withdrawal...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #3B82F6;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and title (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(150);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 60));
        checkmark.setStyle("-fx-text-fill: #10B981;");
        
        Label successTitle = new Label("Withdrawal Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #1E293B;");
        
        successBox.getChildren().addAll(checkmark, successTitle);
        
        // Overlay pane - loading and success will be on top of each other
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(150);
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.getChildren().addAll(loadingBox, successBox);

        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label receiptTitle = new Label("Receipt Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");

        // Receipt items
        HBox methodRow = new HBox(15);
        Label methodLabel = new Label("Method:");
        methodLabel.setPrefWidth(100);
        methodLabel.setStyle("-fx-text-fill: #64748B;");
        Label methodValue = new Label(method);
        methodValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        methodValue.setStyle("-fx-text-fill: #1E293B;");
        methodRow.getChildren().addAll(methodLabel, methodValue);

        HBox nameRow = new HBox(15);
        Label nameLabel = new Label("Account Name:");
        nameLabel.setPrefWidth(100);
        nameLabel.setStyle("-fx-text-fill: #64748B;");
        Label nameValue = new Label(accountName);
        nameValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        nameValue.setStyle("-fx-text-fill: #1E293B;");
        nameRow.getChildren().addAll(nameLabel, nameValue);

        HBox numberRow = new HBox(15);
        Label numberLabel = new Label("Account #:");
        numberLabel.setPrefWidth(100);
        numberLabel.setStyle("-fx-text-fill: #64748B;");
        Label numberValue = new Label(accountNumber);
        numberValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        numberValue.setStyle("-fx-text-fill: #1E293B;");
        numberRow.getChildren().addAll(numberLabel, numberValue);

        HBox amountRow = new HBox(15);
        Label amountLabel = new Label("Amount:");
        amountLabel.setPrefWidth(100);
        amountLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountValue = new Label(String.format("₱%.2f", amount));
        amountValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountValue.setStyle("-fx-text-fill: #10B981;");
        amountRow.getChildren().addAll(amountLabel, amountValue);

        HBox feeRow = new HBox(15);
        Label feeLabel = new Label("Withdrawal Fee:");
        feeLabel.setPrefWidth(100);
        feeLabel.setStyle("-fx-text-fill: #64748B;");
        Label feeValue = new Label("₱15.00");
        feeValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        feeValue.setStyle("-fx-text-fill: #F59E0B;");
        feeRow.getChildren().addAll(feeLabel, feeValue);

        Separator divider = new Separator();
        divider.setStyle("-fx-border-color: #E2E8F0;");

        HBox totalRow = new HBox(15);
        Label totalLabel = new Label("Total Deducted:");
        totalLabel.setPrefWidth(100);
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalLabel.setStyle("-fx-text-fill: #1E293B;");
        Label totalValue = new Label(String.format("₱%.2f", amount + 15.0));
        totalValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalValue.setStyle("-fx-text-fill: #FFD700;");
        totalRow.getChildren().addAll(totalLabel, totalValue);

        HBox referenceRow = new HBox(15);
        Label refLabel = new Label("Reference:");
        refLabel.setPrefWidth(100);
        refLabel.setStyle("-fx-text-fill: #64748B;");
        String reference = "WTH-" + (int)(Math.random() * 900000 + 100000);
        Label refValue = new Label(reference);
        refValue.setFont(Font.font("System", 11));
        refValue.setStyle("-fx-text-fill: #1E293B;");
        referenceRow.getChildren().addAll(refLabel, refValue);

        HBox timeRow = new HBox(15);
        Label timeLabel = new Label("Time:");
        timeLabel.setPrefWidth(100);
        timeLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeValue.setStyle("-fx-text-fill: #1E293B;");
        timeRow.getChildren().addAll(timeLabel, timeValue);

        receiptBox.getChildren().addAll(
            receiptTitle,
            methodRow,
            nameRow,
            numberRow,
            amountRow,
            feeRow,
            divider,
            totalRow,
            referenceRow,
            timeRow
        );

        Label confirmMsg = new Label("Your withdrawal has been processed successfully.\nThe funds will be transferred to your " + method + " account shortly.");
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #E0F2FE; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #7DD3FC; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #0369A1;"
        );
        confirmMsg.setWrapText(true);

        content.getChildren().addAll(overlayPane, receiptBox, confirmMsg);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(620);

        // Simulate processing and show success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    // Fade out loading
                    FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                    fadeOutLoading.setFromValue(1.0);
                    fadeOutLoading.setToValue(0.0);
                    
                    // Fade in success
                    FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                    fadeInSuccess.setFromValue(0.0);
                    fadeInSuccess.setToValue(1.0);
                    
                    fadeOutLoading.play();
                    fadeInSuccess.play();
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }).start();

        dialog.showAndWait();
    }
    
    private void showTransferDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Azure to Azure Transfer");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Header section
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label headerTitle = new Label("Send Money");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #0F172A;");
        
        Label headerSubtitle = new Label("Transfer funds between Azure Wallet accounts");
        headerSubtitle.setFont(Font.font("System", 13));
        headerSubtitle.setStyle("-fx-text-fill: #64748B;");
        
        headerBox.getChildren().addAll(headerTitle, headerSubtitle);
        
        // Transfer details section
        VBox transferBox = new VBox(16);
        transferBox.setPadding(new Insets(20));
        transferBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        // From section
        VBox fromSection = new VBox(8);
        Label fromLabel = new Label("From (Your Account)");
        fromLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        fromLabel.setStyle("-fx-text-fill: #64748B;");
        
        Label fromValue = new Label(currentUser);
        fromValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        fromValue.setStyle("-fx-text-fill: #0F172A;");
        
        fromSection.getChildren().addAll(fromLabel, fromValue);
        
        // Separator
        Separator separator1 = new Separator();
        separator1.setStyle("-fx-border-color: #E2E8F0;");
        
        // Recipient section
        VBox recipientSection = new VBox(8);
        Label recipientLabel = new Label("To (Recipient Username)");
        recipientLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        recipientLabel.setStyle("-fx-text-fill: #64748B;");
        
        TextField recipientField = new TextField();
        recipientField.setPromptText("Enter recipient's username");
        recipientField.setStyle(
            "-fx-padding: 10; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1.5; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-font-size: 13;"
        );
        recipientField.setPrefHeight(40);
        
        recipientSection.getChildren().addAll(recipientLabel, recipientField);
        
        // Separator
        Separator separator2 = new Separator();
        separator2.setStyle("-fx-border-color: #E2E8F0;");
        
        // Amount section
        VBox amountSection = new VBox(8);
        Label amountLabel = new Label("Amount");
        amountLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        amountLabel.setStyle("-fx-text-fill: #64748B;");
        
        HBox amountInputBox = new HBox(8);
        Label pesoSign = new Label("₱");
        pesoSign.setFont(Font.font("System", FontWeight.BOLD, 16));
        pesoSign.setStyle("-fx-text-fill: #10B981;");
        
        TextField amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.setStyle(
            "-fx-padding: 10; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1.5; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-font-size: 13;"
        );
        amountField.setPrefHeight(40);
        
        amountInputBox.getChildren().addAll(pesoSign, amountField);
        HBox.setHgrow(amountField, Priority.ALWAYS);
        
        amountSection.getChildren().addAll(amountLabel, amountInputBox);
        
        transferBox.getChildren().addAll(fromSection, separator1, recipientSection, separator2, amountSection);
        
        // Balance info box
        VBox balanceBox = new VBox(10);
        balanceBox.setPadding(new Insets(12));
        balanceBox.setStyle(
            "-fx-background-color: #E0F2FE; " +
            "-fx-border-color: #7DD3FC; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1.5;"
        );
        
        Label balanceInfoLabel = new Label("Your Available Balance");
        balanceInfoLabel.setFont(Font.font("System", 11));
        balanceInfoLabel.setStyle("-fx-text-fill: #0369A1;");
        
        Label balanceValue = new Label(String.format("₱%.2f", azureApp.getBalance(currentUser)));
        balanceValue.setFont(Font.font("System", FontWeight.BOLD, 18));
        balanceValue.setStyle("-fx-text-fill: #0369A1;");
        
        balanceBox.getChildren().addAll(balanceInfoLabel, balanceValue);
        
        // Security notice
        VBox securityBox = new VBox(8);
        securityBox.setPadding(new Insets(12));
        securityBox.setStyle(
            "-fx-background-color: #FEF3C7; " +
            "-fx-border-color: #FCD34D; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1.5;"
        );
        
        Label securityIcon = new Label("🔒");
        securityIcon.setFont(Font.font("System", 14));
        
        Label securityText = new Label("Transfers are secure and encrypted. Only Azure Wallet users can receive transfers.");
        securityText.setFont(Font.font("System", 11));
        securityText.setStyle("-fx-text-fill: #92400E;");
        securityText.setWrapText(true);
        
        HBox securityRow = new HBox(8);
        securityRow.setAlignment(Pos.TOP_LEFT);
        securityRow.getChildren().addAll(securityIcon, securityText);
        
        securityBox.getChildren().add(securityRow);
        
        content.getChildren().addAll(headerBox, transferBox, balanceBox, securityBox);
        
        dialog.getDialogPane().setContent(content);
        
        // Buttons
        ButtonType transferBtn = new ButtonType("Send Transfer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(transferBtn, cancelBtn);
        
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(600);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == transferBtn) {
                try {
                    String recipient = recipientField.getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());
                    
                    if (recipient.isEmpty()) {
                        showAlert("Error", "Please enter recipient's username", Alert.AlertType.ERROR);
                        return;
                    }
                    
                    if (recipient.equalsIgnoreCase(currentUser)) {
                        showAlert("Error", "You cannot transfer money to yourself", Alert.AlertType.ERROR);
                        return;
                    }
                    
                    if (amt <= 0) {
                        showAlert("Error", "Amount must be greater than 0", Alert.AlertType.ERROR);
                        return;
                    }
                    
                    if (azureApp.sendMoney(currentUser, recipient, amt)) {
                        showTransferSuccessDialog(recipient, amt);
                        refreshMainScreen();
                    } else {
                        showAlert("Error", 
                            "Transfer failed. Please check:\n" +
                            "• Recipient username exists\n" +
                            "• You have sufficient balance\n" +
                            "• Amount is valid", 
                            Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter a valid amount", Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    private void showTransferSuccessDialog(String recipient, double amount) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transfer Successful");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Success icon and message
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Transfer Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Money has been transferred successfully");
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Transfer Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox recipientRow = new HBox(15);
        Label recipientLabel = new Label("To:");
        recipientLabel.setPrefWidth(80);
        recipientLabel.setStyle("-fx-text-fill: #64748B;");
        Label recipientValue = new Label(recipient);
        recipientValue.setStyle("-fx-text-fill: #1E293B;");
        recipientRow.getChildren().addAll(recipientLabel, recipientValue);
        
        HBox amountRow = new HBox(15);
        Label amountLabel = new Label("Amount:");
        amountLabel.setPrefWidth(80);
        amountLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountValue = new Label(String.format("₱%.2f", amount));
        amountValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountValue.setStyle("-fx-text-fill: #10B981;");
        amountRow.getChildren().addAll(amountLabel, amountValue);
        
        HBox timeRow = new HBox(15);
        Label timeLabel = new Label("Time:");
        timeLabel.setPrefWidth(80);
        timeLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeValue.setStyle("-fx-text-fill: #1E293B;");
        timeRow.getChildren().addAll(timeLabel, timeValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            recipientRow,
            amountRow,
            timeRow
        );
        
        // Confirmation message
        Label confirmMsg = new Label(
            "The recipient has been notified about this transfer.\n" +
            "Both accounts have been updated successfully."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #E0F2FE; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #7DD3FC; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #0369A1;"
        );
        confirmMsg.setWrapText(true);
        
        content.getChildren().addAll(successBox, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(600);
        
        dialog.showAndWait();
    }
    
    private void showVoucherDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Redeem Voucher");
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        // Header Section
        Label headerTitle = new Label("Redeem Voucher");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerDesc = new Label("Enter your voucher code to redeem rewards");
        headerDesc.setFont(Font.font("System", 13));
        headerDesc.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, headerTitle, headerDesc);

        // Voucher Code Input Section
        VBox voucherSection = new VBox(12);
        voucherSection.setPadding(new Insets(16));
        voucherSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label codeLabel = new Label("Voucher Code");
        codeLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        codeLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField codeField = new TextField();
        codeField.setPromptText("Enter your 12-character voucher code (e.g., VOUCHER123)");
        codeField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        codeField.setPrefHeight(45);

        voucherSection.getChildren().addAll(codeLabel, codeField);

        // Information Box
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(12));
        infoBox.setStyle(
            "-fx-background-color: #FEF3C7; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1;"
        );

        Label infoIcon = new Label("ℹ");
        infoIcon.setFont(Font.font("System", 14));

        Label infoText = new Label("Each voucher can only be redeemed once.\nVouchers expire 1 year from generation date.");
        infoText.setFont(Font.font("System", 12));
        infoText.setStyle("-fx-text-fill: #78350F;");
        infoText.setWrapText(true);

        HBox infoContent = new HBox(10);
        infoContent.getChildren().addAll(infoIcon, infoText);

        infoBox.getChildren().add(infoContent);

        mainContent.getChildren().addAll(headerBox, voucherSection, infoBox);

        dialog.getDialogPane().setContent(mainContent);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(420);

        ButtonType redeemType = new ButtonType("Redeem Voucher", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(redeemType, cancelType);

        // Style the buttons
        for (ButtonType btn : dialog.getDialogPane().getButtonTypes()) {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btn);
            if (button != null) {
                button.setStyle(
                    "-fx-padding: 12 32; " +
                    "-fx-font-size: 13; " +
                    "-fx-font-weight: bold; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;"
                );
                if (btn == redeemType) {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #10B981; " +
                        "-fx-text-fill: white;");
                } else {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #E2E8F0; " +
                        "-fx-text-fill: #475569;");
                }
            }
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == redeemType) {
                String code = codeField.getText().trim();
                if (code.isEmpty()) {
                    showAlert("Error", "Please enter a voucher code", Alert.AlertType.ERROR);
                    return null;
                }

                try {
                    double value = azureApp.redeemVoucher(currentUser, code);
                    if (value > 0) {
                        // Show success dialog with loading and check animation
                        showVoucherRedeemSuccessDialog(code, value);
                        return redeemType;
                    } else {
                        showAlert("Error", "Voucher redemption failed.\nCode may be invalid or already used.", Alert.AlertType.ERROR);
                        return null;
                    }
                } catch (Exception ex) {
                    showAlert("Error", "An error occurred while redeeming the voucher.", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showVoucherRedeemSuccessDialog(final String voucherCode, final double amount) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Voucher Redeemed");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#E0E7FF"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#F59E0B"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Voucher...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #F59E0B;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and message (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(200);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Voucher Redeemed!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Your voucher has been successfully redeemed");
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Create overlay stack pane with loading on top
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(200);
        overlayPane.getChildren().addAll(successBox, loadingBox);
        StackPane.setAlignment(successBox, Pos.CENTER);
        StackPane.setAlignment(loadingBox, Pos.CENTER);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Redemption Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox codeReceiptRow = new HBox(15);
        Label codeReceiptLabel = new Label("Voucher Code:");
        codeReceiptLabel.setPrefWidth(130);
        codeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label codeReceiptValue = new Label(voucherCode);
        codeReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 12));
        codeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        codeReceiptRow.getChildren().addAll(codeReceiptLabel, codeReceiptValue);
        
        HBox amountReceiptRow = new HBox(15);
        Label amountReceiptLabel = new Label("Amount Redeemed:");
        amountReceiptLabel.setPrefWidth(130);
        amountReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountReceiptValue = new Label(String.format("₱%.2f", amount));
        amountReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountReceiptValue.setStyle("-fx-text-fill: #10B981;");
        amountReceiptRow.getChildren().addAll(amountReceiptLabel, amountReceiptValue);
        
        HBox timeReceiptRow = new HBox(15);
        Label timeReceiptLabel = new Label("Time:");
        timeReceiptLabel.setPrefWidth(130);
        timeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeReceiptValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        timeReceiptRow.getChildren().addAll(timeReceiptLabel, timeReceiptValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            codeReceiptRow,
            amountReceiptRow,
            timeReceiptRow
        );
        
        // Confirmation message (initially hidden)
        Label confirmMsg = new Label(
            "Your voucher redemption has been completed successfully.\n" +
            "The redeemed amount has been added to your wallet balance."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #DBEAFE; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #93C5FD; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #1E40AF;"
        );
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle(confirmMsg.getStyle() + " -fx-opacity: 0;");
        
        // Initially show overlay with loading on top of success box
        content.getChildren().addAll(overlayPane, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(600);
        
        // Schedule transition from loading to success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second loading animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Platform.runLater(() -> {
                // Fade out loading box to reveal success box beneath it
                FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                fadeOutLoading.setFromValue(1);
                fadeOutLoading.setToValue(0);
                
                // Fade in success box (checkmark, title, message)
                FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                fadeInSuccess.setFromValue(0);
                fadeInSuccess.setToValue(1);
                
                // Fade in receipt
                FadeTransition fadeInReceipt = new FadeTransition(Duration.millis(500), receiptBox);
                fadeInReceipt.setFromValue(0);
                fadeInReceipt.setToValue(1);
                
                // Fade in confirmation
                FadeTransition fadeInConfirm = new FadeTransition(Duration.millis(500), confirmMsg);
                fadeInConfirm.setFromValue(0);
                fadeInConfirm.setToValue(1);
                
                fadeOutLoading.play();
                fadeInSuccess.play();
                fadeInReceipt.play();
                fadeInConfirm.play();
            });
        }).start();
        
        dialog.showAndWait();
        
        // Refresh the main screen after redeeming
        refreshMainScreen();
    }

    private void showPayOnlineDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pay Online");
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #F8FAFC;");

        // Header
        Label headerTitle = new Label("Pay Online");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerDesc = new Label("Select payment method, merchant, and amount");
        headerDesc.setFont(Font.font("System", 12));
        headerDesc.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, headerTitle, headerDesc);

        // Payment Method Section
        VBox paymentMethodSection = new VBox(12);
        paymentMethodSection.setPadding(new Insets(16));
        paymentMethodSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        Label paymentMethodLabel = new Label("Payment Method");
        paymentMethodLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        paymentMethodLabel.setStyle("-fx-text-fill: #1E293B;");

        ToggleGroup paymentMethodGroup = new ToggleGroup();

        RadioButton virtualCardOption = new RadioButton("💳 Azure Virtual Card");
        virtualCardOption.setToggleGroup(paymentMethodGroup);
        virtualCardOption.setSelected(true);
        virtualCardOption.setFont(Font.font("System", 13));
        virtualCardOption.setStyle("-fx-text-fill: #1E293B;");

        RadioButton walletOption = new RadioButton("💰 Azure Wallet");
        walletOption.setToggleGroup(paymentMethodGroup);
        walletOption.setFont(Font.font("System", 13));
        walletOption.setStyle("-fx-text-fill: #1E293B;");

        VBox paymentOptions = new VBox(10);
        paymentOptions.getChildren().addAll(virtualCardOption, walletOption);

        paymentMethodSection.getChildren().addAll(paymentMethodLabel, paymentOptions);

        // Merchant Selection Section
        VBox merchantSection = new VBox(12);
        merchantSection.setPadding(new Insets(16));
        merchantSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        Label merchantLabel = new Label("Select E-Commerce Platform");
        merchantLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        merchantLabel.setStyle("-fx-text-fill: #1E293B;");

        ComboBox<String> merchantBox = new ComboBox<>();
        merchantBox.getItems().addAll(azureApp.getFixedMerchants());
        merchantBox.setEditable(true);
        merchantBox.setPromptText("Select or type merchant name");
        merchantBox.setStyle(
            "-fx-padding: 12; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 12;"
        );
        merchantBox.setPrefHeight(40);

        merchantSection.getChildren().addAll(merchantLabel, merchantBox);

        // Amount Section
        VBox amountSection = new VBox(12);
        amountSection.setPadding(new Insets(16));
        amountSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        Label amountLabel = new Label("Payment Amount");
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount (₱)");
        amountField.setStyle(
            "-fx-padding: 12; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 12;"
        );
        amountField.setPrefHeight(40);

        Label feeInfo = new Label("Service Fee: ₱15.00 (fixed)");
        feeInfo.setFont(Font.font("System", 11));
        feeInfo.setStyle(
            "-fx-text-fill: #64748B; " +
            "-fx-padding: 8; " +
            "-fx-background-color: #F1F5F9; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 6;"
        );

        amountSection.getChildren().addAll(amountLabel, amountField, feeInfo);

        // Virtual Card Info Section
        VBox virtualCardInfoBox = new VBox(12);
        virtualCardInfoBox.setPadding(new Insets(16));
        virtualCardInfoBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFC107); " +
            "-fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);"
        );

        Label cardInfoTitle = new Label("💳 Your Virtual Card");
        cardInfoTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        cardInfoTitle.setStyle("-fx-text-fill: white;");

        UserAccount user = azureApp.getUser(currentUser);
        String vbn = user != null ? user.getVirtualBankNumber() : null;

        // If no VBN, generate one
        if ((vbn == null || vbn.isEmpty()) && user != null) {
            vbn = azurewallet.utils.CardUtil.generateLuhn16();
            user.setVirtualBankNumber(vbn);
            azureApp.getFileManager().saveUsers(azureApp.getUsers());
        }

        String maskedCard;
        if (vbn == null || vbn.isEmpty() || vbn.length() < 4) {
            maskedCard = "Not Available";
        } else {
            String lastFour = vbn.substring(vbn.length() - 4);
            maskedCard = "**** **** **** " + lastFour;
        }

        Label cardNumberLabel = new Label("Card: " + maskedCard);
        cardNumberLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        cardNumberLabel.setStyle("-fx-text-fill: white;");

        Label cardStatusLabel = new Label("Status: ✓ Active & Ready");
        cardStatusLabel.setFont(Font.font("System", 11));
        cardStatusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.95);");

        virtualCardInfoBox.getChildren().addAll(cardInfoTitle, cardNumberLabel, cardStatusLabel);

        mainContent.getChildren().addAll(headerBox, paymentMethodSection, merchantSection, amountSection, virtualCardInfoBox);

        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        // Style buttons
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Pay Now");
        okBtn.setStyle(
            "-fx-padding: 12 28; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: #1E293B; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle(
            "-fx-padding: 12 28; " +
            "-fx-font-size: 13; " +
            "-fx-background-color: #E2E8F0; " +
            "-fx-text-fill: #475569; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String merchant = merchantBox.getEditor().getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());
                    double fee = 15.0;
                    double total = amt + fee;

                    if (merchant.isEmpty() || amt <= 0) {
                        showAlert("Error", "Please enter valid merchant and amount", Alert.AlertType.ERROR);
                        return;
                    }

                    String paymentMethod = virtualCardOption.isSelected() ? "Virtual Card" : "Wallet";

                    // Modern confirmation dialog
                    Dialog<ButtonType> confirmDialog = new Dialog<>();
                    confirmDialog.setTitle("Confirm Payment");
                    confirmDialog.setHeaderText(null);

                    VBox confirmContent = new VBox(20);
                    confirmContent.setPadding(new Insets(24));
                    confirmContent.setStyle("-fx-background-color: #F8FAFC;");

                    Label confirmTitle = new Label("Confirm Your Payment");
                    confirmTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
                    confirmTitle.setStyle("-fx-text-fill: #1E293B;");

                    VBox detailsBox = new VBox(14);
                    detailsBox.setPadding(new Insets(20));
                    detailsBox.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 1;"
                    );

                    HBox merchantRow = new HBox(15);
                    Label merchantKey = new Label("Merchant:");
                    merchantKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label merchantValue = new Label(merchant);
                    merchantValue.setFont(Font.font("System", FontWeight.BOLD, 13));
                    merchantValue.setStyle("-fx-text-fill: #1E293B;");
                    merchantRow.getChildren().addAll(merchantKey, merchantValue);

                    HBox amountRow = new HBox(15);
                    Label amountKey = new Label("Amount:");
                    amountKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label amountValue = new Label(String.format("₱%.2f", amt));
                    amountValue.setFont(Font.font("System", FontWeight.BOLD, 13));
                    amountValue.setStyle("-fx-text-fill: #10B981;");
                    amountRow.getChildren().addAll(amountKey, amountValue);

                    HBox feeRow = new HBox(15);
                    Label feeKey = new Label("Service Fee:");
                    feeKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label feeValue = new Label(String.format("₱%.2f", fee));
                    feeValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
                    feeValue.setStyle("-fx-text-fill: #F59E0B;");
                    feeRow.getChildren().addAll(feeKey, feeValue);

                    Separator divider = new Separator();
                    divider.setStyle("-fx-border-color: #E2E8F0;");

                    HBox totalRow = new HBox(15);
                    Label totalKey = new Label("Total:");
                    totalKey.setFont(Font.font("System", FontWeight.BOLD, 14));
                    totalKey.setStyle("-fx-text-fill: #1E293B; -fx-min-width: 100;");
                    Label totalValue = new Label(String.format("₱%.2f", total));
                    totalValue.setFont(Font.font("System", FontWeight.BOLD, 14));
                    totalValue.setStyle("-fx-text-fill: #FFD700;");
                    totalRow.getChildren().addAll(totalKey, totalValue);

                    HBox paymentMethodRow = new HBox(15);
                    Label methodKey = new Label("Method:");
                    methodKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label methodValue = new Label(paymentMethod);
                    methodValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
                    methodValue.setStyle("-fx-text-fill: #1E293B;");
                    paymentMethodRow.getChildren().addAll(methodKey, methodValue);

                    detailsBox.getChildren().addAll(
                        merchantRow, amountRow, feeRow, divider, totalRow, paymentMethodRow
                    );

                    Label warningLabel = new Label("⚠ Please verify all details before confirming");
                    warningLabel.setFont(Font.font("System", 11));
                    warningLabel.setStyle(
                        "-fx-text-fill: #D97706; " +
                        "-fx-padding: 10; " +
                        "-fx-background-color: #FEF3C7; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #FCD34D; " +
                        "-fx-border-radius: 8;"
                    );

                    confirmContent.getChildren().addAll(confirmTitle, detailsBox, warningLabel);

                    confirmDialog.getDialogPane().setContent(confirmContent);
                    confirmDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                    confirmDialog.getDialogPane().setPrefWidth(420);

                    Button confirmOk = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.OK);
                    confirmOk.setText("Confirm Payment");
                    confirmOk.setStyle(
                        "-fx-padding: 12 28; " +
                        "-fx-font-size: 13; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: #FFD700; " +
                        "-fx-text-fill: #1E293B; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
                    );

                    Button confirmCancel = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
                    confirmCancel.setStyle(
                        "-fx-padding: 12 28; " +
                        "-fx-font-size: 13; " +
                        "-fx-background-color: #E2E8F0; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
                    );

                    confirmDialog.showAndWait().ifPresent(cresp -> {
                        if (cresp == ButtonType.OK) {
                            boolean success;
                            if (virtualCardOption.isSelected()) {
                                // Check Virtual Card balance before attempting payment
                                UserAccount currentUserAccount = azureApp.getUser(currentUser);
                                double virtualCardBalance = (currentUserAccount != null) ? currentUserAccount.getVirtualCardBalance() : 0.0;
                                double totalAmount = amt + fee;
                                
                                if (totalAmount > virtualCardBalance) {
                                    showAlert("Insufficient Balance",
                                        String.format("❌ Insufficient balance on Azure Virtual Card\n\nRequired: ₱%.2f\nVirtual Card Balance: ₱%.2f\nShortage: ₱%.2f\n\nPlease top up your Virtual Card or use Azure Wallet instead.",
                                        totalAmount, virtualCardBalance, totalAmount - virtualCardBalance),
                                        Alert.AlertType.WARNING);
                                    return;
                                }
                                // Pay using Virtual Card only
                                success = azureApp.payOnlineWithVirtualCard(currentUser, merchant, amt);
                            } else {
                                // Pay using main wallet
                                success = azureApp.payOnline(currentUser, merchant, amt);
                            }
                            
                            if (success) {
                                // Show receipt dialog
                                String reference = "PAY-" + (int)(Math.random() * 900000 + 100000);
                                showPaymentReceiptDialog(merchant, amt, fee, total, paymentMethod, reference);
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

        VBox mainContent = new VBox(18);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        // Header title
        Label headerTitle = new Label("Choose product category");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerTitle.setStyle("-fx-text-fill: #1F2937;");

        // Category buttons with consistent styling
        Button gamingBtn = createModernCategoryButton("🎮 Gaming", "Steam, Roblox,\nMobile Legends");
        Button streamingBtn = createModernCategoryButton("🎬 Streaming", "Netflix, Disney+,\nAmazon Prime");
        Button mobileBtn = createModernCategoryButton("📱 Mobile & Apps", "Google Play,\nApp Store");
        Button musicBtn = createModernCategoryButton("🎵 Music", "Spotify,\nYouTube Music");

        // Create category grid (2x2 for better mobile layout)
        GridPane categoryGrid = new GridPane();
        categoryGrid.setHgap(12);
        categoryGrid.setVgap(12);
        categoryGrid.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        categoryGrid.getColumnConstraints().addAll(col1, col2);

        gamingBtn.setMaxWidth(Double.MAX_VALUE);
        streamingBtn.setMaxWidth(Double.MAX_VALUE);
        mobileBtn.setMaxWidth(Double.MAX_VALUE);
        musicBtn.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(gamingBtn, Priority.ALWAYS);
        GridPane.setHgrow(streamingBtn, Priority.ALWAYS);
        GridPane.setHgrow(mobileBtn, Priority.ALWAYS);
        GridPane.setHgrow(musicBtn, Priority.ALWAYS);

        categoryGrid.add(gamingBtn, 0, 0);
        categoryGrid.add(streamingBtn, 1, 0);
        categoryGrid.add(mobileBtn, 0, 1);
        categoryGrid.add(musicBtn, 1, 1);

        // Title for products section
        Label productsTitle = new Label("Choose Your Product");
        productsTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        productsTitle.setStyle("-fx-text-fill: #1F2937; -fx-padding: 8 0 0 0;");

        // Products container (scrollable) with modern card styling
        VBox productsContainer = new VBox(12);
        productsContainer.setPadding(new Insets(16));
        productsContainer.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);"
        );

        ScrollPane productsScroll = new ScrollPane(productsContainer);
        productsScroll.setFitToWidth(true);
        productsScroll.setPrefHeight(280);
        productsScroll.setMinHeight(280);
        productsScroll.setStyle("-fx-control-inner-background: white; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        productsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        productsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Initial products (Gaming)
        updateProductListModern(productsContainer, "gaming");

        // Category button action handlers with consistent visual feedback
        final String[] selectedCategory = {"gaming"};

        Runnable updateButtonStyles = () -> {
            String activeStyle = "-fx-background-color: #FFD700; -fx-text-fill: #1F2937; -fx-padding: 12 16; -fx-border-radius: 10; -fx-border-color: #FFD700; -fx-border-width: 2; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.2), 6, 0, 0, 2);";
            String inactiveStyle = "-fx-background-color: #F3F4F6; -fx-text-fill: #6B7280; -fx-padding: 12 16; -fx-border-radius: 10; -fx-border-color: #E5E7EB; -fx-border-width: 1.5; -fx-font-weight: normal;";

            gamingBtn.setStyle(selectedCategory[0].equals("gaming") ? activeStyle : inactiveStyle);
            streamingBtn.setStyle(selectedCategory[0].equals("streaming") ? activeStyle : inactiveStyle);
            mobileBtn.setStyle(selectedCategory[0].equals("mobile") ? activeStyle : inactiveStyle);
            musicBtn.setStyle(selectedCategory[0].equals("music") ? activeStyle : inactiveStyle);
        };

        gamingBtn.setOnAction(e -> {
            selectedCategory[0] = "gaming";
            updateButtonStyles.run();
            updateProductListModern(productsContainer, "gaming");
        });
        streamingBtn.setOnAction(e -> {
            selectedCategory[0] = "streaming";
            updateButtonStyles.run();
            updateProductListModern(productsContainer, "streaming");
        });
        mobileBtn.setOnAction(e -> {
            selectedCategory[0] = "mobile";
            updateButtonStyles.run();
            updateProductListModern(productsContainer, "mobile");
        });
        musicBtn.setOnAction(e -> {
            selectedCategory[0] = "music";
            updateButtonStyles.run();
            updateProductListModern(productsContainer, "music");
        });

        // Set initial button styles
        updateButtonStyles.run();

        mainContent.getChildren().addAll(headerTitle, categoryGrid, productsTitle, productsScroll);
        VBox.setVgrow(productsScroll, Priority.ALWAYS);

        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(700);

        dialog.showAndWait();
    }

    private Button createModernCategoryButton(String title, String description) {
        VBox buttonContent = new VBox(4);
        buttonContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: inherit;");

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("System", 10));
        descLabel.setStyle("-fx-text-fill: inherit; -fx-wrap-text: true;");
        descLabel.setWrapText(true);

        buttonContent.getChildren().addAll(titleLabel, descLabel);

        Button btn = new Button();
        btn.setGraphic(buttonContent);
        btn.setStyle(
            "-fx-background-color: #F3F4F6; " +
            "-fx-text-fill: #6B7280; " +
            "-fx-padding: 12 16; " +
            "-fx-border-radius: 10; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1.5; " +
            "-fx-cursor: hand; " +
            "-fx-font-size: 13;"
        );
        btn.setPrefHeight(80);
        btn.setMinHeight(80);
        btn.setWrapText(true);

        // Hover effect
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("#FFD700")) {
                btn.setStyle(
                    "-fx-background-color: #F0F0F0; " +
                    "-fx-text-fill: #374151; " +
                    "-fx-padding: 12 16; " +
                    "-fx-border-radius: 10; " +
                    "-fx-border-color: #D1D5DB; " +
                    "-fx-border-width: 1.5; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 13; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);"
                );
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("#FFD700")) {
                btn.setStyle(
                    "-fx-background-color: #F3F4F6; " +
                    "-fx-text-fill: #6B7280; " +
                    "-fx-padding: 12 16; " +
                    "-fx-border-radius: 10; " +
                    "-fx-border-color: #E5E7EB; " +
                    "-fx-border-width: 1.5; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 13;"
                );
            }
        });

        return btn;
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

    private void updateProductListModern(VBox container, String category) {
        container.getChildren().clear();

        java.util.Map<String, java.util.List<String[]>> products = new java.util.HashMap<>();

        // Gaming products (name, price) - Expanded with many varieties
        java.util.List<String[]> gaming = java.util.Arrays.asList(
            // Steam Wallet
            new String[]{"Steam Wallet", "₱100"}, new String[]{"Steam Wallet", "₱300"},
            new String[]{"Steam Wallet", "₱500"}, new String[]{"Steam Wallet", "₱1000"},
            new String[]{"Steam Wallet", "₱2000"}, new String[]{"Steam Wallet", "₱5000"},
            // Roblox Robux
            new String[]{"Roblox Robux", "₱250"}, new String[]{"Roblox Robux", "₱500"},
            new String[]{"Roblox Robux", "₱1500"}, new String[]{"Roblox Robux", "₱3000"},
            // Mobile Legends
            new String[]{"Mobile Legends", "₱50"}, new String[]{"Mobile Legends", "₱100"},
            new String[]{"Mobile Legends", "₱500"}, new String[]{"Mobile Legends", "₱2000"},
            // Call of Duty
            new String[]{"Call of Duty", "₱300"}, new String[]{"Call of Duty", "₱600"},
            new String[]{"Call of Duty", "₱1500"}, new String[]{"Call of Duty", "₱3000"},
            // PUBG Mobile
            new String[]{"PUBG Mobile", "₱75"}, new String[]{"PUBG Mobile", "₱150"},
            new String[]{"PUBG Mobile", "₱500"}, new String[]{"PUBG Mobile", "₱2000"},
            // Genshin Impact
            new String[]{"Genshin Impact", "₱30"}, new String[]{"Genshin Impact", "₱300"},
            new String[]{"Genshin Impact", "₱600"}, new String[]{"Genshin Impact", "₱3000"},
            // Final Fantasy
            new String[]{"Final Fantasy XIV", "₱500"}, new String[]{"Final Fantasy XIV", "₱1000"},
            // Valorant
            new String[]{"Valorant Points", "₱100"}, new String[]{"Valorant Points", "₱500"},
            new String[]{"Valorant Points", "₱1000"}, new String[]{"Valorant Points", "₱3000"}
        );

        java.util.List<String[]> streaming = java.util.Arrays.asList(
            // Netflix
            new String[]{"Netflix", "₱149"}, new String[]{"Netflix", "₱349"},
            new String[]{"Netflix", "₱549"}, new String[]{"Netflix", "₱899"},
            // Disney+
            new String[]{"Disney+", "₱99"}, new String[]{"Disney+", "₱299"},
            new String[]{"Disney+", "₱799"}, new String[]{"Disney+", "₱1999"},
            // Amazon Prime Video
            new String[]{"Amazon Prime", "₱149"}, new String[]{"Amazon Prime", "₱499"},
            new String[]{"Amazon Prime", "₱1499"},
            // HBO Max
            new String[]{"HBO Max", "₱199"}, new String[]{"HBO Max", "₱599"},
            new String[]{"HBO Max", "₱1499"}, new String[]{"HBO Max", "₱2999"},
            // iQIYI
            new String[]{"iQIYI", "₱99"}, new String[]{"iQIYI", "₱299"},
            new String[]{"iQIYI", "₱599"}, new String[]{"iQIYI", "₱1299"},
            // WeTV
            new String[]{"WeTV", "₱99"}, new String[]{"WeTV", "₱249"},
            new String[]{"WeTV", "₱499"}, new String[]{"WeTV", "₱999"},
            // Viu
            new String[]{"Viu", "₱49"}, new String[]{"Viu", "₱149"},
            new String[]{"Viu", "₱299"}, new String[]{"Viu", "₱749"},
            // Apple TV+
            new String[]{"Apple TV+", "₱99"}, new String[]{"Apple TV+", "₱299"},
            new String[]{"Apple TV+", "₱799"}
        );

        java.util.List<String[]> mobile = java.util.Arrays.asList(
            // Google Play
            new String[]{"Google Play", "₱50"}, new String[]{"Google Play", "₱100"},
            new String[]{"Google Play", "₱300"}, new String[]{"Google Play", "₱500"},
            new String[]{"Google Play", "₱1000"}, new String[]{"Google Play", "₱3000"},
            // Apple App Store
            new String[]{"App Store", "₱100"}, new String[]{"App Store", "₱300"},
            new String[]{"App Store", "₱500"}, new String[]{"App Store", "₱1000"},
            new String[]{"App Store", "₱3000"},
            // PlayStation Store
            new String[]{"PlayStation Store", "₱200"}, new String[]{"PlayStation Store", "₱500"},
            new String[]{"PlayStation Store", "₱1000"}, new String[]{"PlayStation Store", "₱2000"},
            new String[]{"PlayStation Store", "₱5000"},
            // Xbox Game Pass
            new String[]{"Xbox Game Pass", "₱99"}, new String[]{"Xbox Game Pass", "₱299"},
            new String[]{"Xbox Game Pass", "₱999"},
            // Nintendo eShop
            new String[]{"Nintendo eShop", "₱100"}, new String[]{"Nintendo eShop", "₱500"},
            new String[]{"Nintendo eShop", "₱1000"}, new String[]{"Nintendo eShop", "₱2000"}
        );

        java.util.List<String[]> music = java.util.Arrays.asList(
            // Spotify
            new String[]{"Spotify Premium", "₱109"}, new String[]{"Spotify Premium", "₱129"},
            new String[]{"Spotify Premium", "₱299"}, new String[]{"Spotify Premium", "₱1299"},
            // YouTube Music
            new String[]{"YouTube Music", "₱109"}, new String[]{"YouTube Music", "₱129"},
            new String[]{"YouTube Music", "₱299"}, new String[]{"YouTube Music", "₱1299"},
            // Apple Music
            new String[]{"Apple Music", "₱109"}, new String[]{"Apple Music", "₱129"},
            new String[]{"Apple Music", "₱299"}, new String[]{"Apple Music", "₱1299"},
            // Amazon Music
            new String[]{"Amazon Music", "₱99"}, new String[]{"Amazon Music", "₱149"},
            new String[]{"Amazon Music", "₱299"}, new String[]{"Amazon Music", "₱999"},
            // Deezer
            new String[]{"Deezer", "₱99"}, new String[]{"Deezer", "₱199"},
            new String[]{"Deezer", "₱499"}, new String[]{"Deezer", "₱999"},
            // Tidal
            new String[]{"Tidal", "₱149"}, new String[]{"Tidal", "₱299"},
            new String[]{"Tidal", "₱1499"},
            // SoundCloud
            new String[]{"SoundCloud", "₱99"}, new String[]{"SoundCloud", "₱299"},
            new String[]{"SoundCloud", "₱999"}
        );

        products.put("gaming", gaming);
        products.put("streaming", streaming);
        products.put("mobile", mobile);
        products.put("music", music);

        // Display provider header with icon
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
        providerLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
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

            VBox providerBox = new VBox(10);
            providerBox.setPadding(new Insets(14));
            providerBox.setStyle(
                "-fx-background-color: #F9FAFB; " +
                "-fx-border-color: #E5E7EB; " +
                "-fx-border-radius: 10; " +
                "-fx-border-width: 1.5; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);"
            );

            Label providerTitle = new Label(provider);
            providerTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
            providerTitle.setStyle("-fx-text-fill: #1F2937; -fx-padding: 0 0 4 0;");

            HBox priceBox = new HBox(8);
            priceBox.setAlignment(Pos.CENTER_LEFT);

            for (String price : prices) {
                Button priceBtn = new Button(price);
                priceBtn.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: #FFD700; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-text-fill: #FFD700; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 12; " +
                    "-fx-padding: 8 14; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.1), 4, 0, 0, 1);"
                );
                priceBtn.setPrefHeight(36);
                priceBtn.setMinWidth(70);
                
                // Hover effect
                priceBtn.setOnMouseEntered(e -> priceBtn.setStyle(
                    "-fx-background-color: #FFFACD; " +
                    "-fx-border-color: #FFD700; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-text-fill: #FFD700; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 12; " +
                    "-fx-padding: 8 14; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.2), 6, 0, 0, 2);"
                ));
                priceBtn.setOnMouseExited(e -> priceBtn.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: #FFD700; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-text-fill: #FFD700; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 12; " +
                    "-fx-padding: 8 14; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.1), 4, 0, 0, 1);"
                ));
                
                priceBtn.setOnAction(e -> {
                    showPaymentConfirmationDialog(provider, price);
                });
                priceBox.getChildren().add(priceBtn);
            }

            providerBox.getChildren().addAll(providerTitle, priceBox);
            container.getChildren().add(providerBox);
        }
    }

    private void updateProductList(VBox container, String category) {
        container.getChildren().clear();

        java.util.Map<String, java.util.List<String[]>> products = new java.util.HashMap<>();

        // Gaming products (name, price) - Expanded with many varieties
        java.util.List<String[]> gaming = java.util.Arrays.asList(
            // Steam Wallet
            new String[]{"Steam Wallet", "₱100"}, new String[]{"Steam Wallet", "₱300"},
            new String[]{"Steam Wallet", "₱500"}, new String[]{"Steam Wallet", "₱1000"},
            new String[]{"Steam Wallet", "₱2000"}, new String[]{"Steam Wallet", "₱5000"},
            // Roblox Robux
            new String[]{"Roblox Robux", "₱250"}, new String[]{"Roblox Robux", "₱500"},
            new String[]{"Roblox Robux", "₱1500"}, new String[]{"Roblox Robux", "₱3000"},
            // Mobile Legends
            new String[]{"Mobile Legends", "₱50"}, new String[]{"Mobile Legends", "₱100"},
            new String[]{"Mobile Legends", "₱500"}, new String[]{"Mobile Legends", "₱2000"},
            // Call of Duty
            new String[]{"Call of Duty", "₱300"}, new String[]{"Call of Duty", "₱600"},
            new String[]{"Call of Duty", "₱1500"}, new String[]{"Call of Duty", "₱3000"},
            // PUBG Mobile
            new String[]{"PUBG Mobile", "₱75"}, new String[]{"PUBG Mobile", "₱150"},
            new String[]{"PUBG Mobile", "₱500"}, new String[]{"PUBG Mobile", "₱2000"},
            // Genshin Impact
            new String[]{"Genshin Impact", "₱30"}, new String[]{"Genshin Impact", "₱300"},
            new String[]{"Genshin Impact", "₱600"}, new String[]{"Genshin Impact", "₱3000"},
            // Final Fantasy
            new String[]{"Final Fantasy XIV", "₱500"}, new String[]{"Final Fantasy XIV", "₱1000"},
            // Valorant
            new String[]{"Valorant Points", "₱100"}, new String[]{"Valorant Points", "₱500"},
            new String[]{"Valorant Points", "₱1000"}, new String[]{"Valorant Points", "₱3000"}
        );

        java.util.List<String[]> streaming = java.util.Arrays.asList(
            // Netflix
            new String[]{"Netflix", "₱149"}, new String[]{"Netflix", "₱349"},
            new String[]{"Netflix", "₱549"}, new String[]{"Netflix", "₱899"},
            // Disney+
            new String[]{"Disney+", "₱99"}, new String[]{"Disney+", "₱299"},
            new String[]{"Disney+", "₱799"}, new String[]{"Disney+", "₱1999"},
            // Amazon Prime Video
            new String[]{"Amazon Prime", "₱149"}, new String[]{"Amazon Prime", "₱499"},
            new String[]{"Amazon Prime", "₱1499"},
            // HBO Max
            new String[]{"HBO Max", "₱199"}, new String[]{"HBO Max", "₱599"},
            new String[]{"HBO Max", "₱1499"}, new String[]{"HBO Max", "₱2999"},
            // iQIYI
            new String[]{"iQIYI", "₱99"}, new String[]{"iQIYI", "₱299"},
            new String[]{"iQIYI", "₱599"}, new String[]{"iQIYI", "₱1299"},
            // WeTV
            new String[]{"WeTV", "₱99"}, new String[]{"WeTV", "₱249"},
            new String[]{"WeTV", "₱499"}, new String[]{"WeTV", "₱999"},
            // Viu
            new String[]{"Viu", "₱49"}, new String[]{"Viu", "₱149"},
            new String[]{"Viu", "₱299"}, new String[]{"Viu", "₱749"},
            // Apple TV+
            new String[]{"Apple TV+", "₱99"}, new String[]{"Apple TV+", "₱299"},
            new String[]{"Apple TV+", "₱799"}
        );

        java.util.List<String[]> mobile = java.util.Arrays.asList(
            // Google Play
            new String[]{"Google Play", "₱50"}, new String[]{"Google Play", "₱100"},
            new String[]{"Google Play", "₱300"}, new String[]{"Google Play", "₱500"},
            new String[]{"Google Play", "₱1000"}, new String[]{"Google Play", "₱3000"},
            // Apple App Store
            new String[]{"App Store", "₱100"}, new String[]{"App Store", "₱300"},
            new String[]{"App Store", "₱500"}, new String[]{"App Store", "₱1000"},
            new String[]{"App Store", "₱3000"},
            // PlayStation Store
            new String[]{"PlayStation Store", "₱200"}, new String[]{"PlayStation Store", "₱500"},
            new String[]{"PlayStation Store", "₱1000"}, new String[]{"PlayStation Store", "₱2000"},
            new String[]{"PlayStation Store", "₱5000"},
            // Xbox Game Pass
            new String[]{"Xbox Game Pass", "₱99"}, new String[]{"Xbox Game Pass", "₱299"},
            new String[]{"Xbox Game Pass", "₱999"},
            // Nintendo eShop
            new String[]{"Nintendo eShop", "₱100"}, new String[]{"Nintendo eShop", "₱500"},
            new String[]{"Nintendo eShop", "₱1000"}, new String[]{"Nintendo eShop", "₱2000"}
        );

        java.util.List<String[]> music = java.util.Arrays.asList(
            // Spotify
            new String[]{"Spotify Premium", "₱109"}, new String[]{"Spotify Premium", "₱129"},
            new String[]{"Spotify Premium", "₱299"}, new String[]{"Spotify Premium", "₱1299"},
            // YouTube Music
            new String[]{"YouTube Music", "₱109"}, new String[]{"YouTube Music", "₱129"},
            new String[]{"YouTube Music", "₱299"}, new String[]{"YouTube Music", "₱1299"},
            // Apple Music
            new String[]{"Apple Music", "₱109"}, new String[]{"Apple Music", "₱129"},
            new String[]{"Apple Music", "₱299"}, new String[]{"Apple Music", "₱1299"},
            // Amazon Music
            new String[]{"Amazon Music", "₱99"}, new String[]{"Amazon Music", "₱149"},
            new String[]{"Amazon Music", "₱299"}, new String[]{"Amazon Music", "₱999"},
            // Deezer
            new String[]{"Deezer", "₱99"}, new String[]{"Deezer", "₱199"},
            new String[]{"Deezer", "₱499"}, new String[]{"Deezer", "₱999"},
            // Tidal
            new String[]{"Tidal", "₱149"}, new String[]{"Tidal", "₱299"},
            new String[]{"Tidal", "₱1499"},
            // SoundCloud
            new String[]{"SoundCloud", "₱99"}, new String[]{"SoundCloud", "₱299"},
            new String[]{"SoundCloud", "₱999"}
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

            for (String price : prices) {
                Button priceBtn = new Button(price);
                priceBtn.setStyle("-fx-background-color: white; -fx-border-color: #FFD700; -fx-border-width: 2; -fx-border-radius: 6; -fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand;");
                priceBtn.setOnAction(e -> {
                    showPaymentConfirmationDialog(provider, price);
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
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        azurewallet.models.UserAccount user = azureApp.getUser(currentUser);
        int availablePoints = user != null ? user.getPoints() : 0;

        // Header Section
        Label headerTitle = new Label("Redeem Points");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerDesc = new Label("Convert your points to cash rewards");
        headerDesc.setFont(Font.font("System", 13));
        headerDesc.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, headerTitle, headerDesc);

        // Available Points Section
        VBox pointsInfoSection = new VBox(12);
        pointsInfoSection.setPadding(new Insets(16));
        pointsInfoSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label pointsInfoLabel = new Label("Your Points");
        pointsInfoLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        pointsInfoLabel.setStyle("-fx-text-fill: #1E293B;");

        HBox pointsDisplayBox = new HBox(15);
        pointsDisplayBox.setAlignment(Pos.CENTER_LEFT);

        Label pointsValue = new Label(String.format("%d", availablePoints));
        pointsValue.setFont(Font.font("System", FontWeight.BOLD, 32));
        pointsValue.setStyle("-fx-text-fill: #F59E0B;");

        VBox pointsTextBox = new VBox(2);
        Label pointsLabel = new Label("Points Available");
        pointsLabel.setFont(Font.font("System", 12));
        pointsLabel.setStyle("-fx-text-fill: #64748B;");

        Label conversionLabel = new Label("₱1 = 1 point");
        conversionLabel.setFont(Font.font("System", 11));
        conversionLabel.setStyle("-fx-text-fill: #94A3B8;");

        pointsTextBox.getChildren().addAll(pointsLabel, conversionLabel);
        pointsDisplayBox.getChildren().addAll(pointsValue, pointsTextBox);

        pointsInfoSection.getChildren().addAll(pointsInfoLabel, pointsDisplayBox);

        // Redeem Amount Section
        VBox redeemSection = new VBox(12);
        redeemSection.setPadding(new Insets(16));
        redeemSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label redeemLabel = new Label("Redeem Amount");
        redeemLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        redeemLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField pointsField = new TextField();
        pointsField.setPromptText("Enter points to redeem");
        pointsField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        pointsField.setPrefHeight(45);

        // Quick redemption amounts
        Label quickLabel = new Label("Quick Redemptions");
        quickLabel.setFont(Font.font("System", 11));
        quickLabel.setStyle("-fx-text-fill: #64748B;");

        GridPane quickGrid = new GridPane();
        quickGrid.setHgap(10);
        quickGrid.setVgap(10);
        
        for (int c = 0; c < 4; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            quickGrid.getColumnConstraints().add(cc);
        }

        int[] quickAmounts = {50, 100, 200, 500};
        ToggleGroup quickGroup = new ToggleGroup();
        
        for (int i = 0; i < quickAmounts.length; i++) {
            int amount = quickAmounts[i];
            ToggleButton quickBtn = new ToggleButton(amount + " pts");
            quickBtn.setToggleGroup(quickGroup);
            quickBtn.setMaxWidth(Double.MAX_VALUE);
            quickBtn.setPrefHeight(45);
            quickBtn.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
            quickBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1.5; " +
                "-fx-text-fill: #1E293B; " +
                "-fx-cursor: hand;"
            );
            
            quickBtn.setOnAction(e -> {
                if (quickBtn.isSelected()) {
                    pointsField.setText(String.valueOf(amount));
                } else {
                    pointsField.clear();
                }
            });
            
            final ToggleButton btn = quickBtn;
            quickBtn.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    btn.setStyle(
                        "-fx-background-color: #FEFCE8; " +
                        "-fx-border-color: #F59E0B; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-text-fill: #78350F; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;"
                    );
                } else {
                    btn.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-text-fill: #1E293B; " +
                        "-fx-cursor: hand;"
                    );
                }
            });
            
            quickGrid.add(btn, i, 0);
            GridPane.setHgrow(btn, Priority.ALWAYS);
        }

        // Equivalent cash display
        Label equivalentLabel = new Label("Equivalent Cash Value");
        equivalentLabel.setFont(Font.font("System", 11));
        equivalentLabel.setStyle("-fx-text-fill: #64748B;");

        HBox equivalentBox = new HBox(10);
        equivalentBox.setAlignment(Pos.CENTER_LEFT);

        Label currencyLabel = new Label("₱");
        currencyLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        currencyLabel.setStyle("-fx-text-fill: #10B981;");

        Label equivalentValue = new Label("0.00");
        equivalentValue.setFont(Font.font("System", FontWeight.BOLD, 24));
        equivalentValue.setStyle("-fx-text-fill: #10B981;");

        equivalentBox.getChildren().addAll(currencyLabel, equivalentValue);

        redeemSection.getChildren().addAll(redeemLabel, pointsField, quickLabel, quickGrid, equivalentLabel, equivalentBox);

        // Info box
        Label infoBox = new Label("⭐ Conversion: 1 point = ₱1 in cash rewards");
        infoBox.setFont(Font.font("System", 11));
        infoBox.setStyle(
            "-fx-background-color: #FEF3C7; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #78350F;"
        );
        infoBox.setWrapText(true);

        // Confirmation box
        VBox confirmSection = new VBox(12);
        confirmSection.setPadding(new Insets(16));
        confirmSection.setStyle(
            "-fx-background-color: #FEFCE8; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1;"
        );

        CheckBox confirmBox = new CheckBox("I confirm redeeming these points");
        confirmBox.setFont(Font.font("System", 12));
        confirmBox.setStyle("-fx-text-fill: #78350F;");

        confirmSection.getChildren().add(confirmBox);

        mainContent.getChildren().addAll(headerBox, pointsInfoSection, redeemSection, infoBox, confirmSection);

        dialog.getDialogPane().setContent(mainContent);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(750);

        ButtonType redeemType = new ButtonType("Redeem", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(redeemType, cancelType);

        // Style the buttons
        for (ButtonType btn : dialog.getDialogPane().getButtonTypes()) {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btn);
            if (button != null) {
                button.setStyle(
                    "-fx-padding: 12 32; " +
                    "-fx-font-size: 13; " +
                    "-fx-font-weight: bold; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;"
                );
                if (btn == redeemType) {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #F59E0B; " +
                        "-fx-text-fill: white;");
                } else {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #E2E8F0; " +
                        "-fx-text-fill: #475569;");
                }
            }
        }

        // Validation
        Runnable validate = () -> {
            boolean ok = false;
            try {
                int pts = Integer.parseInt(pointsField.getText().trim());
                ok = pts > 0 && pts <= availablePoints && confirmBox.isSelected();
            } catch (Exception ex) {
                ok = false;
            }
            
            javafx.scene.control.Button redeemBtn = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(redeemType);
            if (redeemBtn != null) {
                redeemBtn.setDisable(!ok);
            }
        };

        // Update equivalent value as user types
        pointsField.textProperty().addListener((o, oldV, newV) -> {
            try {
                int pts = Integer.parseInt(newV.trim());
                // 1 point = ₱1 conversion
                equivalentValue.setText(String.format("%.2f", pts * 1.0));
            } catch (Exception ex) {
                equivalentValue.setText("0.00");
            }
            validate.run();
        });

        confirmBox.selectedProperty().addListener((o, oldV, newV) -> validate.run());

        dialog.setOnShown(ev -> validate.run());

        dialog.showAndWait().ifPresent(response -> {
            if (response == redeemType) {
                try {
                    int pts = Integer.parseInt(pointsField.getText().trim());
                    if (azureApp.redeemPoints(currentUser, pts)) {
                        showRedeemPointsSuccessDialog(pts);
                        refreshMainScreen();
                    } else {
                        showAlert("Error", "Redemption failed. Please check your points balance.", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showRedeemPointsSuccessDialog(final int pointsRedeemed) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Points Redeemed Successfully");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#FEF3C7"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#F59E0B"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Redemption...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #F59E0B;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and message (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(200);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Points Redeemed!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Your cash reward has been credited to your wallet");
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Create overlay stack pane with loading on top
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(200);
        overlayPane.getChildren().addAll(successBox, loadingBox);
        StackPane.setAlignment(successBox, Pos.CENTER);
        StackPane.setAlignment(loadingBox, Pos.CENTER);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Redemption Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox pointsRedeemRow = new HBox(15);
        Label pointsRedeemLabel = new Label("Points Redeemed:");
        pointsRedeemLabel.setPrefWidth(130);
        pointsRedeemLabel.setStyle("-fx-text-fill: #64748B;");
        Label pointsRedeemValue = new Label(String.valueOf(pointsRedeemed));
        pointsRedeemValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        pointsRedeemValue.setStyle("-fx-text-fill: #F59E0B;");
        pointsRedeemRow.getChildren().addAll(pointsRedeemLabel, pointsRedeemValue);
        
        HBox cashRow = new HBox(15);
        Label cashLabel = new Label("Cash Reward:");
        cashLabel.setPrefWidth(130);
        cashLabel.setStyle("-fx-text-fill: #64748B;");
        Label cashValue = new Label(String.format("₱%.2f", pointsRedeemed * 1.0));
        cashValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        cashValue.setStyle("-fx-text-fill: #10B981;");
        cashRow.getChildren().addAll(cashLabel, cashValue);
        
        HBox timeRow = new HBox(15);
        Label timeLabel = new Label("Time:");
        timeLabel.setPrefWidth(130);
        timeLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeValue.setStyle("-fx-text-fill: #1E293B;");
        timeRow.getChildren().addAll(timeLabel, timeValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            pointsRedeemRow,
            cashRow,
            timeRow
        );
        
        // Confirmation message (initially hidden)
        Label confirmMsg = new Label(
            "Your points have been successfully redeemed.\\n" +
            "The cash reward is now available in your wallet."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #FEFCE8; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #78350F;"
        );
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle(confirmMsg.getStyle() + " -fx-opacity: 0;");
        
        // Initially show overlay with loading on top of success box
        content.getChildren().addAll(overlayPane, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(650);
        
        // Schedule transition from loading to success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second loading animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Platform.runLater(() -> {
                // Fade out loading box to reveal success box beneath it
                FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                fadeOutLoading.setFromValue(1);
                fadeOutLoading.setToValue(0);
                
                // Fade in success box (checkmark, title, message)
                FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                fadeInSuccess.setFromValue(0);
                fadeInSuccess.setToValue(1);
                
                // Fade in receipt
                FadeTransition fadeInReceipt = new FadeTransition(Duration.millis(500), receiptBox);
                fadeInReceipt.setFromValue(0);
                fadeInReceipt.setToValue(1);
                
                // Fade in confirmation
                FadeTransition fadeInConfirm = new FadeTransition(Duration.millis(500), confirmMsg);
                fadeInConfirm.setFromValue(0);
                fadeInConfirm.setToValue(1);
                
                fadeOutLoading.play();
                fadeInSuccess.play();
                fadeInReceipt.play();
                fadeInConfirm.play();
            });
        }).start();
        
        dialog.showAndWait();
    }

    private void showBillsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Bills Payment");
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #F8FAFC;");

        // Header
        Label headerTitle = new Label("Pay Your Bills");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerDesc = new Label("Select biller, enter account details and amount");
        headerDesc.setFont(Font.font("System", 12));
        headerDesc.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, headerTitle, headerDesc);

        // Biller Selection Section
        VBox billerSection = new VBox(12);
        billerSection.setPadding(new Insets(16));
        billerSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        Label billerLabel = new Label("Select Biller");
        billerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        billerLabel.setStyle("-fx-text-fill: #1E293B;");

        ComboBox<String> billerBox = new ComboBox<>();
        billerBox.getItems().addAll("Meralco", "Maynilad", "Manila Water", "PLDT", "Smart Billing", "Globe Billing");
        billerBox.setEditable(true);
        billerBox.setPromptText("Select or type biller name");
        billerBox.setStyle(
            "-fx-padding: 12; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 12;"
        );
        billerBox.setPrefHeight(40);

        billerSection.getChildren().addAll(billerLabel, billerBox);

        // Account Number Section
        VBox accountSection = new VBox(12);
        accountSection.setPadding(new Insets(16));
        accountSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        Label accountLabel = new Label("Account Number");
        accountLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        accountLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField acctField = new TextField();
        acctField.setPromptText("Enter your account number");
        acctField.setStyle(
            "-fx-padding: 12; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 12;"
        );
        acctField.setPrefHeight(40);

        accountSection.getChildren().addAll(accountLabel, acctField);

        // Amount Section
        VBox amountSection = new VBox(12);
        amountSection.setPadding(new Insets(16));
        amountSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        Label amountLabel = new Label("Amount to Pay");
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount (₱)");
        amountField.setStyle(
            "-fx-padding: 12; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 12;"
        );
        amountField.setPrefHeight(40);
        amountField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldV);
            }
        });

        Label feeInfo = new Label("📋 Service Fee: 2% (minimum ₱10)");
        feeInfo.setFont(Font.font("System", 11));
        feeInfo.setStyle(
            "-fx-text-fill: #64748B; " +
            "-fx-padding: 8; " +
            "-fx-background-color: #F1F5F9; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 6;"
        );

        amountSection.getChildren().addAll(amountLabel, amountField, feeInfo);

        mainContent.getChildren().addAll(headerBox, billerSection, accountSection, amountSection);

        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        // Style buttons
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Review Payment");
        okButton.setStyle(
            "-fx-padding: 12 28; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: #1E293B; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(
            "-fx-padding: 12 28; " +
            "-fx-font-size: 13; " +
            "-fx-background-color: #E2E8F0; " +
            "-fx-text-fill: #475569; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String biller = billerBox.getEditor().getText().trim();
                    String acct = acctField.getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());

                    if (biller.isEmpty() || acct.isEmpty() || amt <= 0) {
                        showAlert("Error", "Please fill in all fields with valid values", Alert.AlertType.ERROR);
                        return;
                    }

                    // Compute fee: 2% with minimum ₱10
                    double fee = Math.max(10.0, amt * 0.02);
                    double total = amt + fee;

                    // Modern confirmation dialog
                    Dialog<ButtonType> confirmDialog = new Dialog<>();
                    confirmDialog.setTitle("Confirm Payment");
                    confirmDialog.setHeaderText(null);

                    VBox confirmContent = new VBox(20);
                    confirmContent.setPadding(new Insets(24));
                    confirmContent.setStyle("-fx-background-color: #F8FAFC;");

                    Label confirmTitle = new Label("Confirm Your Payment");
                    confirmTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
                    confirmTitle.setStyle("-fx-text-fill: #1E293B;");

                    VBox detailsBox = new VBox(14);
                    detailsBox.setPadding(new Insets(20));
                    detailsBox.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 1;"
                    );

                    HBox billerRow = new HBox(15);
                    Label billerKey = new Label("Biller:");
                    billerKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label billerValue = new Label(biller);
                    billerValue.setFont(Font.font("System", FontWeight.BOLD, 13));
                    billerValue.setStyle("-fx-text-fill: #1E293B;");
                    billerRow.getChildren().addAll(billerKey, billerValue);

                    HBox acctRow = new HBox(15);
                    Label acctKey = new Label("Account:");
                    acctKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label acctValue = new Label(acct);
                    acctValue.setFont(Font.font("System", 12));
                    acctValue.setStyle("-fx-text-fill: #1E293B;");
                    acctRow.getChildren().addAll(acctKey, acctValue);

                    HBox amountRow = new HBox(15);
                    Label amountKey = new Label("Amount:");
                    amountKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label amountValue = new Label(String.format("₱%.2f", amt));
                    amountValue.setFont(Font.font("System", FontWeight.BOLD, 13));
                    amountValue.setStyle("-fx-text-fill: #10B981;");
                    amountRow.getChildren().addAll(amountKey, amountValue);

                    HBox feeRow = new HBox(15);
                    Label feeKey = new Label("Service Fee:");
                    feeKey.setStyle("-fx-text-fill: #64748B; -fx-min-width: 100;");
                    Label feeValue = new Label(String.format("₱%.2f", fee));
                    feeValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
                    feeValue.setStyle("-fx-text-fill: #F59E0B;");
                    feeRow.getChildren().addAll(feeKey, feeValue);

                    Separator divider = new Separator();
                    divider.setStyle("-fx-border-color: #E2E8F0;");

                    HBox totalRow = new HBox(15);
                    Label totalKey = new Label("Total:");
                    totalKey.setFont(Font.font("System", FontWeight.BOLD, 14));
                    totalKey.setStyle("-fx-text-fill: #1E293B; -fx-min-width: 100;");
                    Label totalValue = new Label(String.format("₱%.2f", total));
                    totalValue.setFont(Font.font("System", FontWeight.BOLD, 14));
                    totalValue.setStyle("-fx-text-fill: #FFD700;");
                    totalRow.getChildren().addAll(totalKey, totalValue);

                    detailsBox.getChildren().addAll(
                        billerRow, acctRow, amountRow, feeRow, divider, totalRow
                    );

                    Label warningLabel = new Label("⚠ Please verify all details before confirming");
                    warningLabel.setFont(Font.font("System", 11));
                    warningLabel.setStyle(
                        "-fx-text-fill: #D97706; " +
                        "-fx-padding: 10; " +
                        "-fx-background-color: #FEF3C7; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #FCD34D; " +
                        "-fx-border-radius: 8;"
                    );

                    // Loading box section
                    VBox loadingBox = new VBox(20);
                    loadingBox.setAlignment(Pos.CENTER);
                    loadingBox.setPrefHeight(100);
                    
                    StackPane spinnerPane = new StackPane();
                    spinnerPane.setPrefSize(60, 60);
                    
                    Circle spinner = new Circle(30);
                    spinner.setFill(Color.TRANSPARENT);
                    spinner.setStroke(Color.web("#E0E7FF"));
                    spinner.setStrokeWidth(3);
                    spinner.setStrokeLineCap(StrokeLineCap.ROUND);
                    
                    Circle spinnerActive = new Circle(30);
                    spinnerActive.setFill(Color.TRANSPARENT);
                    spinnerActive.setStroke(Color.web("#3B82F6"));
                    spinnerActive.setStrokeWidth(3);
                    spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
                    spinnerActive.getStrokeDashArray().addAll(40.0, 120.0);
                    
                    RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
                    rotateTransition.setFromAngle(0);
                    rotateTransition.setToAngle(360);
                    rotateTransition.setCycleCount(Animation.INDEFINITE);
                    
                    spinnerPane.getChildren().addAll(spinner, spinnerActive);
                    
                    Label processingLabel = new Label("Processing...");
                    processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
                    processingLabel.setStyle("-fx-text-fill: #3B82F6;");
                    
                    loadingBox.getChildren().addAll(spinnerPane, processingLabel);
                    
                    // Success box (hidden initially)
                    VBox successBox = new VBox(10);
                    successBox.setAlignment(Pos.CENTER);
                    successBox.setPrefHeight(100);
                    successBox.setOpacity(0);
                    
                    Label checkmark = new Label("✓");
                    checkmark.setFont(Font.font("System", FontWeight.BOLD, 50));
                    checkmark.setStyle("-fx-text-fill: #10B981;");
                    
                    Label successMsg = new Label("Confirmed!");
                    successMsg.setFont(Font.font("System", FontWeight.BOLD, 16));
                    successMsg.setStyle("-fx-text-fill: #1E293B;");
                    
                    successBox.getChildren().addAll(checkmark, successMsg);
                    
                    // Overlay pane
                    StackPane overlayPane = new StackPane();
                    overlayPane.setPrefHeight(100);
                    overlayPane.setAlignment(Pos.CENTER);
                    overlayPane.getChildren().addAll(loadingBox, successBox);

                    confirmContent.getChildren().addAll(confirmTitle, detailsBox, overlayPane, warningLabel);

                    confirmDialog.getDialogPane().setContent(confirmContent);
                    confirmDialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
                    confirmDialog.setResizable(false);
                    confirmDialog.getDialogPane().setPrefWidth(420);

                    rotateTransition.play();
                    
                    // Process payment and animate after 2 seconds
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(() -> {
                                // Fade out loading
                                FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                                fadeOutLoading.setFromValue(1.0);
                                fadeOutLoading.setToValue(0.0);
                                
                                // Fade in success
                                FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                                fadeInSuccess.setFromValue(0.0);
                                fadeInSuccess.setToValue(1.0);
                                
                                fadeOutLoading.play();
                                fadeInSuccess.play();
                            });
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();

                    confirmDialog.showAndWait();
                    
                    // Process the actual payment after dialog closes
                    if (azureApp.billsPayment(currentUser, biller, acct, amt)) {
                        refreshMainScreen();
                    } else {
                        showAlert("Error", "Payment failed. Check amount and balance.", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showBuyLoadDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buy Prepaid Load");
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        // Header Section
        Label headerTitle = new Label("Buy Prepaid Load");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerTitle.setStyle("-fx-text-fill: #1E293B;");

        Label headerDesc = new Label("Purchase mobile load using Virtual Card or Wallet");
        headerDesc.setFont(Font.font("System", 13));
        headerDesc.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(4, headerTitle, headerDesc);

        // Payment Method Section
        VBox paymentMethodSection = new VBox(12);
        paymentMethodSection.setPadding(new Insets(16));
        paymentMethodSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label paymentMethodLabel = new Label("Payment Method");
        paymentMethodLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        paymentMethodLabel.setStyle("-fx-text-fill: #1E293B;");

        ToggleGroup paymentMethodGroup = new ToggleGroup();

        RadioButton virtualCardOption = new RadioButton("💳 Azure Virtual Card");
        virtualCardOption.setToggleGroup(paymentMethodGroup);
        virtualCardOption.setSelected(true);
        virtualCardOption.setFont(Font.font("System", 13));
        virtualCardOption.setStyle("-fx-text-fill: #1E293B;");

        RadioButton walletOption = new RadioButton("💰 Azure Wallet");
        walletOption.setToggleGroup(paymentMethodGroup);
        walletOption.setFont(Font.font("System", 13));
        walletOption.setStyle("-fx-text-fill: #1E293B;");

        VBox paymentOptions = new VBox(10);
        paymentOptions.getChildren().addAll(virtualCardOption, walletOption);

        paymentMethodSection.getChildren().addAll(paymentMethodLabel, paymentOptions);

        // Network and Mobile Number Section
        VBox detailsSection = new VBox(12);
        detailsSection.setPadding(new Insets(16));
        detailsSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label networkLabel = new Label("Network");
        networkLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        networkLabel.setStyle("-fx-text-fill: #1E293B;");

        // Create toggle group for network selection (1 row, 3 columns)
        ToggleGroup networkGroup = new ToggleGroup();
        
        HBox networkButtonBox = new HBox(10);
        networkButtonBox.setAlignment(Pos.CENTER_LEFT);
        
        String[] networks = {"Globe/TM", "Smart/TNT", "DITO"};
        ToggleButton selectedNetworkBtn = null;
        
        for (String network : networks) {
            ToggleButton networkBtn = new ToggleButton("📱 " + network);
            networkBtn.setToggleGroup(networkGroup);
            networkBtn.setMaxWidth(Double.MAX_VALUE);
            networkBtn.setPrefHeight(50);
            networkBtn.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
            networkBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 2; " +
                "-fx-text-fill: #1E293B; " +
                "-fx-cursor: hand;"
            );
            
            // Set first button as selected
            if ("Globe/TM".equals(network)) {
                networkBtn.setSelected(true);
                selectedNetworkBtn = networkBtn;
                networkBtn.setStyle(
                    "-fx-background-color: #FEFCE8; " +
                    "-fx-border-color: #F59E0B; " +
                    "-fx-border-radius: 8; " +
                    "-fx-border-width: 2; " +
                    "-fx-text-fill: #78350F; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand;"
                );
            }
            
            // Add style change on selection
            final ToggleButton btn = networkBtn;
            networkBtn.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    btn.setStyle(
                        "-fx-background-color: #FEFCE8; " +
                        "-fx-border-color: #F59E0B; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-text-fill: #78350F; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;"
                    );
                } else {
                    btn.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 2; " +
                        "-fx-text-fill: #1E293B; " +
                        "-fx-cursor: hand;"
                    );
                }
            });
            
            networkButtonBox.getChildren().add(networkBtn);
            HBox.setHgrow(networkBtn, Priority.ALWAYS);
        }

        Label numberLabel = new Label("Mobile Number");
        numberLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        numberLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField numberField = new TextField();
        numberField.setPromptText("09XXXXXXXXX");
        numberField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        numberField.setPrefHeight(45);

        detailsSection.getChildren().addAll(networkLabel, networkButtonBox, numberLabel, numberField);

        // Amount Section
        VBox amountSection = new VBox(12);
        amountSection.setPadding(new Insets(16));
        amountSection.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );

        Label amountLabel = new Label("Load Amount");
        amountLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        amountLabel.setStyle("-fx-text-fill: #1E293B;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount or select preset");
        amountField.setStyle(
            "-fx-padding: 14; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-font-size: 13; " +
            "-fx-text-fill: #1E293B;"
        );
        amountField.setPrefHeight(45);

        // Preset amounts
        int[] presets = {50, 100, 150, 200, 300, 500};
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
            b.setStyle(
                "-fx-background-radius: 8; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-background-color: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12; " +
                "-fx-padding: 12; " +
                "-fx-border-width: 2;"
            );
            b.setOnAction(e -> {
                amountField.setText(String.valueOf(value));
                for (Button pb : presetButtons) {
                    pb.setStyle(
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-background-color: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 12; " +
                        "-fx-padding: 12; " +
                        "-fx-border-width: 2;"
                    );
                }
                b.setStyle(
                    "-fx-background-radius: 8; " +
                    "-fx-border-color: #F59E0B; " +
                    "-fx-background-color: #FEFCE8; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 12; " +
                    "-fx-padding: 12; " +
                    "-fx-border-width: 2; " +
                    "-fx-text-fill: #78350F;"
                );
            });
            presetButtons.add(b);
            int row = i / 2;
            int col = i % 2;
            presetsGrid.add(b, col, row);
        }

        amountSection.getChildren().addAll(amountLabel, amountField, new Label("Quick Amounts:"), presetsGrid);

        mainContent.getChildren().addAll(headerBox, paymentMethodSection, detailsSection, amountSection);

        dialog.getDialogPane().setContent(mainContent);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(650);

        ButtonType buyType = new ButtonType("Buy Load", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(buyType, cancelType);

        // Style the buttons
        for (ButtonType btn : dialog.getDialogPane().getButtonTypes()) {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btn);
            if (button != null) {
                button.setStyle(
                    "-fx-padding: 12 32; " +
                    "-fx-font-size: 13; " +
                    "-fx-font-weight: bold; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;"
                );
                if (btn == buyType) {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #F59E0B; " +
                        "-fx-text-fill: white;");
                } else {
                    button.setStyle(button.getStyle() + 
                        "-fx-background-color: #E2E8F0; " +
                        "-fx-text-fill: #475569;");
                }
            }
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == buyType) {
                try {
                    String network = ((ToggleButton) networkGroup.getSelectedToggle()).getText().replaceAll("📱 ", "");
                    String number = numberField.getText().trim();
                    double amt = Double.parseDouble(amountField.getText().trim());
                    String paymentMethod = virtualCardOption.isSelected() ? "Virtual Card" : "Wallet";

                    if (!number.matches("^09\\d{9}$")) {
                        showAlert("Error", "Invalid mobile number format. Use 09XXXXXXXXX.", Alert.AlertType.ERROR);
                        return null;
                    }

                    if (amt <= 0) {
                        showAlert("Error", "Please enter a valid amount.", Alert.AlertType.ERROR);
                        return null;
                    }

                    String prefix4 = number.substring(0, 4);
                    
                    // Define network prefixes
                    java.util.Set<String> globePrefixes = java.util.Set.of(
                        "0905", "0906", "0915", "0916", "0917", "0926", "0927", 
                        "0935", "0936", "0945", "0955", "0965", "0975"
                    );
                    java.util.Set<String> smartPrefixes = java.util.Set.of(
                        "0907", "0908", "0909", "0910", "0911", "0912", "0918", 
                        "0919", "0920", "0921", "0928", "0929", "0938", "0948", 
                        "0951", "0952", "0967", "0973", "0939"
                    );
                    java.util.Set<String> ditoPrefixes = java.util.Set.of(
                        "0991", "0992", "0993" , "0994"
                    );

                    // Validate that the number matches the selected network
                    String selectedNetwork = network.toLowerCase();
                    boolean isValidForNetwork = false;
                    String detectedNetwork = "Unknown";

                    if (globePrefixes.contains(prefix4)) {
                        detectedNetwork = "Globe/TM";
                        isValidForNetwork = selectedNetwork.contains("globe");
                    } else if (smartPrefixes.contains(prefix4)) {
                        detectedNetwork = "Smart/TNT";
                        isValidForNetwork = selectedNetwork.contains("smart");
                    } else if (ditoPrefixes.contains(prefix4)) {
                        detectedNetwork = "DITO";
                        isValidForNetwork = selectedNetwork.contains("dito");
                    } else {
                        showAlert("Error", "Mobile number prefix not recognized. Please check the number and network.\nDetected prefix: " + prefix4, Alert.AlertType.ERROR);
                        return null;
                    }

                    if (!isValidForNetwork) {
                        showAlert("Error", 
                            String.format("Mobile number mismatch!\nNumber prefix %s belongs to %s network,\nbut you selected %s.", 
                            prefix4, detectedNetwork, network), 
                            Alert.AlertType.ERROR);
                        return null;
                    }

                    double fee = Math.round((amt * 0.015) * 100.0) / 100.0;
                    double total = amt + fee;

                    if (azureApp.buyLoad(currentUser, network, number, amt)) {
                        // Show success dialog with loading and check animation
                        showBuyLoadSuccessDialog(network, number, amt, fee, total, paymentMethod);
                        return buyType;
                    } else {
                        showAlert("Error", "Load purchase failed. Check number format and balance.", Alert.AlertType.ERROR);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount entered", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showBuyLoadSuccessDialog(final String network, final String number, final double amount, final double fee, final double total, final String paymentMethod) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Load Purchase Successful");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Loading animation section (initially visible)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPrefHeight(150);
        
        // Rotating spinner animation
        StackPane spinnerPane = new StackPane();
        spinnerPane.setPrefSize(80, 80);
        
        Circle spinner = new Circle(40);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(Color.web("#E0E7FF"));
        spinner.setStrokeWidth(4);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        Circle spinnerActive = new Circle(40);
        spinnerActive.setFill(Color.TRANSPARENT);
        spinnerActive.setStroke(Color.web("#3B82F6"));
        spinnerActive.setStrokeWidth(4);
        spinnerActive.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Create dash array for partial circle
        spinnerActive.getStrokeDashArray().addAll(50.0, 150.0);
        
        // Rotation animation
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1), spinnerActive);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();
        
        spinnerPane.getChildren().addAll(spinner, spinnerActive);
        
        Label processingLabel = new Label("Processing Your Load Purchase...");
        processingLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        processingLabel.setStyle("-fx-text-fill: #F59E0B;");
        
        loadingBox.getChildren().addAll(spinnerPane, processingLabel);
        
        // Success icon and message (initially hidden with opacity 0)
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        successBox.setPrefHeight(200);
        successBox.setOpacity(0);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Load Purchase Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Your load has been sent to " + number);
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Create overlay stack pane with loading on top
        StackPane overlayPane = new StackPane();
        overlayPane.setPrefHeight(200);
        overlayPane.getChildren().addAll(successBox, loadingBox);
        StackPane.setAlignment(successBox, Pos.CENTER);
        StackPane.setAlignment(loadingBox, Pos.CENTER);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Transaction Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox networkReceiptRow = new HBox(15);
        Label networkReceiptLabel = new Label("Network:");
        networkReceiptLabel.setPrefWidth(130);
        networkReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label networkReceiptValue = new Label(network);
        networkReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        networkReceiptRow.getChildren().addAll(networkReceiptLabel, networkReceiptValue);
        
        HBox numberReceiptRow = new HBox(15);
        Label numberReceiptLabel = new Label("Mobile Number:");
        numberReceiptLabel.setPrefWidth(130);
        numberReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label numberReceiptValue = new Label(number);
        numberReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        numberReceiptRow.getChildren().addAll(numberReceiptLabel, numberReceiptValue);
        
        HBox amountReceiptRow = new HBox(15);
        Label amountReceiptLabel = new Label("Load Amount:");
        amountReceiptLabel.setPrefWidth(130);
        amountReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountReceiptValue = new Label(String.format("₱%.2f", amount));
        amountReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountReceiptValue.setStyle("-fx-text-fill: #10B981;");
        amountReceiptRow.getChildren().addAll(amountReceiptLabel, amountReceiptValue);
        
        HBox feeReceiptRow = new HBox(15);
        Label feeReceiptLabel = new Label("Service Fee (1.5%):");
        feeReceiptLabel.setPrefWidth(130);
        feeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label feeReceiptValue = new Label(String.format("₱%.2f", fee));
        feeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        feeReceiptRow.getChildren().addAll(feeReceiptLabel, feeReceiptValue);
        
        HBox totalReceiptRow = new HBox(15);
        Label totalReceiptLabel = new Label("Total Charged:");
        totalReceiptLabel.setPrefWidth(130);
        totalReceiptLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
        Label totalReceiptValue = new Label(String.format("₱%.2f", total));
        totalReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalReceiptValue.setStyle("-fx-text-fill: #F59E0B;");
        totalReceiptRow.getChildren().addAll(totalReceiptLabel, totalReceiptValue);
        
        HBox methodReceiptRow = new HBox(15);
        Label methodReceiptLabel = new Label("Payment Method:");
        methodReceiptLabel.setPrefWidth(130);
        methodReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label methodReceiptValue = new Label(paymentMethod);
        methodReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        methodReceiptRow.getChildren().addAll(methodReceiptLabel, methodReceiptValue);
        
        HBox timeReceiptRow = new HBox(15);
        Label timeReceiptLabel = new Label("Time:");
        timeReceiptLabel.setPrefWidth(130);
        timeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeReceiptValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        timeReceiptRow.getChildren().addAll(timeReceiptLabel, timeReceiptValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            networkReceiptRow,
            numberReceiptRow,
            amountReceiptRow,
            feeReceiptRow,
            totalReceiptRow,
            methodReceiptRow,
            timeReceiptRow
        );
        
        // Confirmation message (initially hidden)
        Label confirmMsg = new Label(
            "Your load purchase has been completed successfully.\n" +
            "The load should arrive within a few seconds."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle(
            "-fx-background-color: #FEFCE8; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #FBBF24; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #78350F;"
        );
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle(confirmMsg.getStyle() + " -fx-opacity: 0;");
        
        // Initially show overlay with loading on top of success box
        content.getChildren().addAll(overlayPane, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(750);
        
        // Schedule transition from loading to success after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second loading animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Platform.runLater(() -> {
                // Fade out loading box to reveal success box beneath it
                FadeTransition fadeOutLoading = new FadeTransition(Duration.millis(500), loadingBox);
                fadeOutLoading.setFromValue(1);
                fadeOutLoading.setToValue(0);
                
                // Fade in success box (checkmark, title, message)
                FadeTransition fadeInSuccess = new FadeTransition(Duration.millis(500), successBox);
                fadeInSuccess.setFromValue(0);
                fadeInSuccess.setToValue(1);
                
                // Fade in receipt
                FadeTransition fadeInReceipt = new FadeTransition(Duration.millis(500), receiptBox);
                fadeInReceipt.setFromValue(0);
                fadeInReceipt.setToValue(1);
                
                // Fade in confirmation
                FadeTransition fadeInConfirm = new FadeTransition(Duration.millis(500), confirmMsg);
                fadeInConfirm.setFromValue(0);
                fadeInConfirm.setToValue(1);
                
                fadeOutLoading.play();
                fadeInSuccess.play();
                fadeInReceipt.play();
                fadeInConfirm.play();
            });
        }).start();
        
        dialog.showAndWait();
        
        // Refresh the main screen after purchase
        refreshMainScreen();
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
    
    private void showHelpDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Help");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Title
        Label helpTitle = new Label("Help & Support");
        helpTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        helpTitle.setStyle("-fx-text-fill: #0F172A;");
        
        // FAQ Section
        VBox faqBox = new VBox(12);
        
        Label faqLabel = new Label("Frequently Asked Questions");
        faqLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        faqLabel.setStyle("-fx-text-fill: #1E293B;");
        
        String[] faqs = {
            "• How do I deposit money?\n  Use the Deposit button on the home screen to add funds from your bank account.",
            "• How do I transfer money?\n  Click Transfer and enter the recipient's username to send money to another Azure Wallet user.",
            "• What are loyalty points?\n  Earn points with every transaction and redeem them for rewards and benefits.",
            "• How do I lock my card?\n  Use the Card Lock feature in the Wallet section to temporarily disable your card.",
            "• What is my loyalty tier?\n  Your tier depends on your total transaction amount (Classic, Silver, Gold, Platinum)."
        };
        
        VBox faqContent = new VBox(10);
        for (String faq : faqs) {
            Label faqItem = new Label(faq);
            faqItem.setFont(Font.font("System", 11));
            faqItem.setStyle("-fx-text-fill: #475569; -fx-wrap-text: true;");
            faqItem.setWrapText(true);
            faqContent.getChildren().add(faqItem);
        }
        
        faqBox.getChildren().addAll(faqLabel, faqContent);
        faqBox.setStyle(
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #F8FAFC; " +
            "-fx-background-radius: 8;"
        );
        
        // Contact Section
        VBox contactBox = new VBox(8);
        Label contactLabel = new Label("Contact Support");
        contactLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        contactLabel.setStyle("-fx-text-fill: #1E293B;");
        
        Label emailLabel = new Label("Email: support@azurewallet.com");
        emailLabel.setFont(Font.font("System", 11));
        emailLabel.setStyle("-fx-text-fill: #3B82F6;");
        
        Label phoneLabel = new Label("Phone: 1-800-AZURE");
        phoneLabel.setFont(Font.font("System", 11));
        phoneLabel.setStyle("-fx-text-fill: #3B82F6;");
        
        contactBox.getChildren().addAll(contactLabel, emailLabel, phoneLabel);
        contactBox.setStyle(
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #F8FAFC; " +
            "-fx-background-radius: 8;"
        );
        
        content.getChildren().addAll(helpTitle, faqBox, contactBox);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(550);
        
        dialog.showAndWait();
    }
    
    private void showUserMenu(StackPane avatarStack) {
        // Create modern styled popup
        VBox menuContent = new VBox(0);
        menuContent.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);"
        );
        
        // Help button
        HBox helpBox = new HBox(12);
        helpBox.setPadding(new Insets(12, 16, 12, 16));
        helpBox.setAlignment(Pos.CENTER_LEFT);
        helpBox.setCursor(Cursor.HAND);
        helpBox.setStyle("-fx-background-color: transparent;");
        
        helpBox.setOnMouseEntered(e -> {
            helpBox.setStyle("-fx-background-color: #F1F5F9;");
        });
        helpBox.setOnMouseExited(e -> {
            helpBox.setStyle("-fx-background-color: transparent;");
        });
        helpBox.setOnMouseClicked(e -> {
            popup.hide();
            showHelpDialog();
        });
        
        Label helpIcon = new Label("❓");
        helpIcon.setFont(Font.font("System", 16));
        
        VBox helpTextBox = new VBox(2);
        Label helpLabel = new Label("Help");
        helpLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        helpLabel.setStyle("-fx-text-fill: #1E293B;");
        
        Label helpDesc = new Label("Get support and FAQs");
        helpDesc.setFont(Font.font("System", 11));
        helpDesc.setStyle("-fx-text-fill: #64748B;");
        
        helpTextBox.getChildren().addAll(helpLabel, helpDesc);
        helpBox.getChildren().addAll(helpIcon, helpTextBox);
        
        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 0; -fx-border-color: #E2E8F0;");
        
        // Logout button
        HBox logoutBox = new HBox(12);
        logoutBox.setPadding(new Insets(12, 16, 12, 16));
        logoutBox.setAlignment(Pos.CENTER_LEFT);
        logoutBox.setCursor(Cursor.HAND);
        logoutBox.setStyle("-fx-background-color: transparent;");
        
        logoutBox.setOnMouseEntered(e -> {
            logoutBox.setStyle("-fx-background-color: #FEE2E2;");
        });
        logoutBox.setOnMouseExited(e -> {
            logoutBox.setStyle("-fx-background-color: transparent;");
        });
        logoutBox.setOnMouseClicked(e -> {
            popup.hide();
            currentUser = null;
            showLoginScreen();
        });
        
        Label logoutIcon = new Label("🚪");
        logoutIcon.setFont(Font.font("System", 16));
        
        VBox logoutTextBox = new VBox(2);
        Label logoutLabel = new Label("Logout");
        logoutLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        logoutLabel.setStyle("-fx-text-fill: #DC2626;");
        
        Label logoutDesc = new Label("Sign out of your account");
        logoutDesc.setFont(Font.font("System", 11));
        logoutDesc.setStyle("-fx-text-fill: #64748B;");
        
        logoutTextBox.getChildren().addAll(logoutLabel, logoutDesc);
        logoutBox.getChildren().addAll(logoutIcon, logoutTextBox);
        
        menuContent.getChildren().addAll(helpBox, separator, logoutBox);
        
        // Create popup
        popup = new Popup();
        popup.getContent().add(menuContent);
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        
        // Calculate position relative to avatar
        Bounds bounds = avatarStack.localToScreen(avatarStack.getBoundsInLocal());
        popup.show(avatarStack, bounds.getCenterX() - 90, bounds.getCenterY() + 35);
    }
    
    private Popup popup;
    
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
        
        // Get or create expiry date (persisted)
        String expiryDate = user != null ? user.getCardExpiryDate() : null;
        if (expiryDate == null || expiryDate.isBlank()) {
            expiryDate = CardUtil.generateExpiry();
            if (user != null) {
                user.setCardExpiryDate(expiryDate);
                azureApp.getFileManager().saveUsers(azureApp.getUsers());
            }
        }
        
        VBox expiryBox = new VBox(5);
        Label expiryLabel = new Label("Valid Thru  " + expiryDate);
        expiryLabel.setFont(Font.font("System", 11));
        expiryLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9);");
        expiryBox.getChildren().add(expiryLabel);
        
        // Get or create CVV (persisted)
        String cardCVV = user != null ? user.getCardCVV() : null;
        if (cardCVV == null || cardCVV.isBlank()) {
            cardCVV = String.format("%03d", 100 + (int)(Math.random() * 900));
            if (user != null) {
                user.setCardCVV(cardCVV);
                azureApp.getFileManager().saveUsers(azureApp.getUsers());
            }
        }
        
        VBox cvvBox = new VBox(5);
        Label cvvLabel = new Label("CVV  ***");
        cvvLabel.setFont(Font.font("System", 11));
        cvvLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9);");
        cvvBox.getChildren().add(cvvLabel);
        
        detailsRow.getChildren().addAll(holderBox, expiryBox, cvvBox);

        // Slide hint button
        HBox slideButtonRow = new HBox();
        slideButtonRow.setAlignment(Pos.CENTER);
        
        final String finalExpiryDate = expiryDate; // Make final for lambda
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
        slideHint.setOnAction(e -> showCardDetailsDialog(user, vbn, finalExpiryDate));
        
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
        topUpBtn.setOnAction(e -> showVirtualCardTopUpDialog());

        // Display Virtual Card balance (top-up amount only)
        UserAccount walletUser = azureApp.getUser(currentUser);
        double virtualCardBalance = (walletUser != null) ? walletUser.getVirtualCardBalance() : 0.0;
        Label walletBalance = new Label(String.format("₱ %.2f", virtualCardBalance));
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
        Label progressLabel = new Label("150,000~ above to RankUp Total: ₱ " + String.format("%,.2f", user.getTotalTransacted()));
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
     * Used for screens where we want the avatar + username + title,
     * but not a back navigation button (e.g., History, Settings).
     * Avatar is clickable to open the user menu.
     */
    private HBox createHeaderNoBack(String title) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(36, 20, 8, 20));
        header.setStyle("-fx-background-color: #F5F7FA;");

        // Top row: avatar + username on the left, spacer and title on the right
        Circle avatar = new Circle(26);
        avatar.setFill(Color.web("#FFD700"));
        avatar.setStroke(Color.web("#e5e7eb"));
        avatar.setStrokeWidth(1.5);

        javafx.scene.Node avatarIcon = buildAvatarGraphic();

        StackPane avatarStack = new StackPane(avatar, avatarIcon);
        avatarStack.setPrefSize(52, 52);
        avatarStack.setAlignment(Pos.CENTER);
        avatarStack.setCursor(Cursor.HAND);
        avatarStack.setStyle("-fx-background-color: transparent;");

        // Make avatar clickable to open user menu
        avatarStack.setOnMouseClicked(e -> {
            showUserMenu(avatarStack);
        });

        UserAccount user = azureApp.getUser(currentUser);
        Label titleName = new Label(user != null ? user.getUsername() : "");
        titleName.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleName.setStyle("-fx-text-fill: #000000;");

        HBox leftTop = new HBox(8, avatarStack, titleName);
        leftTop.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, leftTop, spacer);
        topRow.setAlignment(Pos.CENTER);
        topRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(topRow, Priority.ALWAYS);

        // Make leftTop match height for consistent header appearance
        leftTop.setAlignment(Pos.CENTER_LEFT);
        leftTop.setMinHeight(52);
        leftTop.setPrefHeight(52);

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
    
    private void showPaymentConfirmationDialog(String productName, String price) {
        final UserAccount user = azureApp.getUser(currentUser);
        if (user == null) {
            showAlert("Error", "User not found. Please login again.", Alert.AlertType.ERROR);
            return;
        }
        
        // Extract price amount as integer
        final int priceAmount;
        try {
            priceAmount = Integer.parseInt(price.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid price format.", Alert.AlertType.ERROR);
            return;
        }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Complete Payment");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Order Summary Section
        VBox summaryBox = new VBox(12);
        summaryBox.setPadding(new Insets(20));
        summaryBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);"
        );
        
        Label summaryTitle = new Label("Order Summary");
        summaryTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        summaryTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Product details
        HBox productRow = new HBox(15);
        productRow.setAlignment(Pos.CENTER_LEFT);
        Label productLabel = new Label("Product:");
        productLabel.setStyle("-fx-text-fill: #64748B; -fx-min-width: 80;");
        Label productValue = new Label(productName);
        productValue.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        productValue.setStyle("-fx-text-fill: #1E293B;");
        productRow.getChildren().addAll(productLabel, productValue);
        
        // Amount details
        HBox amountRow = new HBox(15);
        amountRow.setAlignment(Pos.CENTER_LEFT);
        Label amountLabel = new Label("Amount:");
        amountLabel.setStyle("-fx-text-fill: #64748B; -fx-min-width: 80;");
        Label amountValue = new Label(price);
        amountValue.setFont(Font.font("System", FontWeight.BOLD, 16));
        amountValue.setStyle("-fx-text-fill: #FFD700;");
        amountRow.getChildren().addAll(amountLabel, amountValue);
        
        summaryBox.getChildren().addAll(summaryTitle, productRow, amountRow);
        
        // Payment Method Section
        VBox paymentBox = new VBox(12);
        paymentBox.setPadding(new Insets(20));
        paymentBox.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);"
        );
        
        Label paymentTitle = new Label("Payment Method");
        paymentTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        paymentTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Virtual Card Display
        VBox cardDisplay = new VBox(8);
        cardDisplay.setPadding(new Insets(14));
        cardDisplay.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFC107); " +
            "-fx-background-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.15), 8, 0, 0, 3);"
        );
        
        Label bankLabel = new Label("Azure Digital Wallet");
        bankLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        bankLabel.setStyle("-fx-text-fill: white;");
        
        final String cardNumber = user.getVirtualBankNumber() != null ? 
            CardUtil.formatCardNumber(user.getVirtualBankNumber()) : "4532 1234 5678 3456";
        Label cardLabel = new Label(cardNumber);
        cardLabel.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 16));
        cardLabel.setStyle("-fx-text-fill: white;");
        
        HBox cardDetailsRow = new HBox(20);
        Label expiryLabel = new Label("EXPIRY: " + (user.getCardExpiryDate() != null ? user.getCardExpiryDate() : "11/30"));
        expiryLabel.setFont(Font.font("System", 10));
        expiryLabel.setStyle("-fx-text-fill: white;");
        Label cvvLabel = new Label("CVV: •••");
        cvvLabel.setFont(Font.font("System", 10));
        cvvLabel.setStyle("-fx-text-fill: white;");
        cardDetailsRow.getChildren().addAll(expiryLabel, cvvLabel);
        
        cardDisplay.getChildren().addAll(bankLabel, cardLabel, cardDetailsRow);
        
        // CVV Verification Section
        VBox cvvBox = new VBox(10);
        cvvBox.setPadding(new Insets(14));
        cvvBox.setStyle(
            "-fx-background-color: #FEF3C7; " +
            "-fx-border-color: #FCD34D; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 1.5;"
        );
        
        Label cvvPrompt = new Label("Enter CVV for verification");
        cvvPrompt.setFont(Font.font("System", FontWeight.BOLD, 13));
        cvvPrompt.setStyle("-fx-text-fill: #92400E;");
        
        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("Enter 3-digit CVV");
        cvvField.setStyle(
            "-fx-padding: 12; " +
            "-fx-border-color: #FCD34D; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 2; " +
            "-fx-font-size: 16; " +
            "-fx-font-family: 'Courier New';"
        );
        cvvField.setPrefHeight(40);
        cvvField.setMaxWidth(150);
        
        Label cvvWarning = new Label("✓ Secure 3-digit code on back of card");
        cvvWarning.setFont(Font.font("System", 11));
        cvvWarning.setStyle("-fx-text-fill: #78350F;");
        
        cvvBox.getChildren().addAll(cvvPrompt, cvvField, cvvWarning);
        
        paymentBox.getChildren().addAll(paymentTitle, cardDisplay, cvvBox);
        
        // Security Notice
        VBox securityBox = new VBox(8);
        securityBox.setPadding(new Insets(12));
        securityBox.setStyle(
            "-fx-background-color: #EFF6FF; " +
            "-fx-border-color: #BFDBFE; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1;"
        );
        
        Label securityIcon = new Label("🔒");
        Label securityText = new Label("Your payment is secure and encrypted");
        securityText.setFont(Font.font("System", 12));
        securityText.setStyle("-fx-text-fill: #1E40AF;");
        securityText.setWrapText(true);
        
        HBox securityRow = new HBox(8);
        securityRow.getChildren().addAll(securityIcon, securityText);
        securityBox.getChildren().add(securityRow);
        
        content.getChildren().addAll(summaryBox, paymentBox, securityBox);
        
        dialog.getDialogPane().setContent(content);
        
        // Buttons
        ButtonType confirmButton = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, cancelButton);
        
        // Disable Pay button until CVV is entered
        javafx.scene.Node payButton = dialog.getDialogPane().lookupButton(confirmButton);
        payButton.setDisable(true);
        
        // Validate CVV entry
        cvvField.textProperty().addListener((obs, oldVal, newVal) -> {
            String correctCVV = user.getCardCVV() != null ? user.getCardCVV() : "123";
            boolean isValid = newVal.length() == 3 && newVal.matches("\\d{3}");
            payButton.setDisable(!isValid);
        });
        
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(650);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == confirmButton) {
                String enteredCVV = cvvField.getText().trim();
                String correctCVV = user.getCardCVV() != null ? user.getCardCVV() : "123";
                
                // Verify CVV
                if (!enteredCVV.equals(correctCVV)) {
                    showAlert("Payment Failed", "Invalid CVV. Payment declined.", Alert.AlertType.ERROR);
                    return;
                }
                
                // Check balance
                if (user.getBalance() < priceAmount) {
                    showAlert("Insufficient Balance", 
                        String.format("You need ₱%d but only have ₱%.2f in your account.", 
                        priceAmount, user.getBalance()), 
                        Alert.AlertType.ERROR);
                    return;
                }
                
                // Process payment using withdraw method
                user.withdraw(priceAmount);
                
                // Log shopping transaction
                azureApp.getFileManager().logTransaction(currentUser, "Buy " + productName, priceAmount);
                
                // Award points: 1 point per ₱1000 spent
                int points = (int)(priceAmount / 1000.0);
                if (points > 0) {
                    user.addPoints(points);
                    azureApp.getFileManager().logPoints(currentUser, "earned", points, "Buy " + productName + " PHP " + priceAmount);
                }
                
                azureApp.getFileManager().saveUsers(azureApp.getUsers());
                
                // Show success dialog with transaction details
                showPaymentSuccessDialog(productName, price, cardNumber);
            }
        });
    }
    
    private void showPaymentSuccessDialog(String productName, String price, String cardNumber) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Payment Successful");
        dialog.setHeaderText(null);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: #FFFFFF;");
        
        // Success icon and message
        VBox successBox = new VBox(12);
        successBox.setAlignment(Pos.CENTER);
        
        Label checkmark = new Label("✓");
        checkmark.setFont(Font.font("System", FontWeight.BOLD, 64));
        checkmark.setStyle(
            "-fx-text-fill: #10B981; " +
            "-fx-alignment: center;"
        );
        
        Label successTitle = new Label("Payment Successful!");
        successTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        successTitle.setStyle("-fx-text-fill: #10B981;");
        
        Label successMsg = new Label("Your purchase has been completed");
        successMsg.setFont(Font.font("System", 14));
        successMsg.setStyle("-fx-text-fill: #6B7280;");
        
        successBox.getChildren().addAll(checkmark, successTitle, successMsg);
        
        // Receipt Section
        VBox receiptBox = new VBox(14);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle(
            "-fx-background-color: #F8FAFC; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1.5;"
        );
        
        Label receiptTitle = new Label("Receipt Details");
        receiptTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        receiptTitle.setStyle("-fx-text-fill: #1E293B;");
        
        // Receipt items
        HBox productReceiptRow = new HBox(15);
        Label productReceiptLabel = new Label("Product:");
        productReceiptLabel.setPrefWidth(80);
        productReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label productReceiptValue = new Label(productName);
        productReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        productReceiptRow.getChildren().addAll(productReceiptLabel, productReceiptValue);
        
        HBox amountReceiptRow = new HBox(15);
        Label amountReceiptLabel = new Label("Amount:");
        amountReceiptLabel.setPrefWidth(80);
        amountReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label amountReceiptValue = new Label(price);
        amountReceiptValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        amountReceiptValue.setStyle("-fx-text-fill: #FFD700;");
        amountReceiptRow.getChildren().addAll(amountReceiptLabel, amountReceiptValue);
        
        HBox cardReceiptRow = new HBox(15);
        Label cardReceiptLabel = new Label("Card:");
        cardReceiptLabel.setPrefWidth(80);
        cardReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label cardReceiptValue = new Label(cardNumber);
        cardReceiptValue.setFont(Font.font("System", 12));
        cardReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        cardReceiptRow.getChildren().addAll(cardReceiptLabel, cardReceiptValue);
        
        HBox timeReceiptRow = new HBox(15);
        Label timeReceiptLabel = new Label("Time:");
        timeReceiptLabel.setPrefWidth(80);
        timeReceiptLabel.setStyle("-fx-text-fill: #64748B;");
        Label timeReceiptValue = new Label(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        timeReceiptValue.setStyle("-fx-text-fill: #1E293B;");
        timeReceiptRow.getChildren().addAll(timeReceiptLabel, timeReceiptValue);
        
        receiptBox.getChildren().addAll(
            receiptTitle,
            productReceiptRow,
            amountReceiptRow,
            cardReceiptRow,
            timeReceiptRow
        );
        
        // Confirmation message
        Label confirmMsg = new Label(
            "A verification code has been sent to your registered email.\n" +
            "Check your email for the product code and activation details."
        );
        confirmMsg.setFont(Font.font("System", 12));
        confirmMsg.setStyle("-fx-text-fill: #475569; -fx-padding: 12;");
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle(
            "-fx-background-color: #E0F2FE; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #7DD3FC; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #0369A1;"
        );
        
        content.getChildren().addAll(successBox, receiptBox, confirmMsg);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Done", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(600);
        
        dialog.showAndWait();
    }
    
    private void showCardDetailsDialog(UserAccount user, String vbn, String expiryDate) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Virtual Card Details");
        dialog.setHeaderText("Full Card Information");
        
        ButtonType okButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okButton);
        
        // Initialize card CVV if not already set (first time only)
        String cardCVV = user.getCardCVV();
        if (cardCVV == null || cardCVV.isBlank()) {
            cardCVV = String.format("%03d", 100 + (int)(Math.random() * 900));
            user.setCardCVV(cardCVV);
        }
        
        // Initialize card expiry date if not already set (first time only)
        String cardExpiryDate = user.getCardExpiryDate();
        if (cardExpiryDate == null || cardExpiryDate.isBlank()) {
            if (expiryDate == null || expiryDate.isBlank()) {
                cardExpiryDate = "11/30"; // Default expiry
            } else {
                cardExpiryDate = expiryDate;
            }
            user.setCardExpiryDate(cardExpiryDate);
        }
        
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
        
        // Expiry and CVV (now using persistent values)
        HBox detailsRow = new HBox(30);
        Label expiryInfo = new Label("EXPIRY: " + cardExpiryDate);
        expiryInfo.setFont(Font.font("System", 11));
        expiryInfo.setStyle("-fx-text-fill: white;");
        
        Label cvvInfo = new Label("CVV: " + cardCVV);
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
            "CVV: %s\n" +
            "Card Type: Mastercard\n" +
            "Status: Active",
            fullCardNumber,
            user != null ? user.getUsername().toUpperCase() : "CARDHOLDER",
            cardExpiryDate,
            cardCVV
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