package azurewallet.main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
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
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import azurewallet.utils.CardUtil;
import azurewallet.models.UserAccount;
import azurewallet.system.AdminControl;
import java.util.List;
import java.util.Optional;

/**
 * Main JavaFX application for Azure Digital Wallet
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
        
        // Initialize the backend
        this.azureApp = new AzureDigitalApp();
        this.adminControl = new AdminControl();
        
        root = new BorderPane();
        double fixedWidth = 480.0;
        double fixedHeight = 900.0;
        scene = new Scene(root, fixedWidth, fixedHeight);
        scene.setFill(Color.web("#FFFFFF"));
        java.net.URL css = getClass().getResource("/styles/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        showLoginScreen();

        primaryStage.setTitle("Azure Digital Wallet");
        primaryStage.setScene(scene);
        primaryStage.setWidth(fixedWidth);
        primaryStage.setHeight(fixedHeight);
        primaryStage.setMinWidth(fixedWidth);
        primaryStage.setMinHeight(fixedHeight);
        primaryStage.setMaxWidth(fixedWidth);
        primaryStage.setMaxHeight(fixedHeight);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(e -> {
            azureApp.shutdown();
        });
        primaryStage.show();
    }

    private void showLoginScreen() {
        VBox loginContainer = new VBox(20);
        loginContainer.setAlignment(Pos.TOP_CENTER);
        loginContainer.setPadding(new Insets(140, 0, 12, 0));
        loginContainer.getStyleClass().add("login-container");

        VBox logoBox = new VBox(4);
        logoBox.setAlignment(Pos.CENTER);
        
        Label logo = new Label("Azure");
        logo.setFont(Font.font("System", FontWeight.BOLD, 36));
        logo.setStyle("-fx-text-fill: #FFD700;");
        
        Label appTitle = new Label("Azure Digital Wallet");
        appTitle.setFont(Font.font("Cambria", FontWeight.BOLD, 36));
        appTitle.setStyle("-fx-font-family: 'Cambria'; -fx-text-fill: #FFD700;");
        
        VBox welcomeBox = new VBox(6);
        welcomeBox.setAlignment(Pos.CENTER);
        Label titleLabel = new Label("Welcome Back");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #111827;");
        Label subtitle = new Label("Sign in to continue");
        subtitle.setFont(Font.font("System", 12));
        subtitle.setStyle("-fx-text-fill: #475569;");
        welcomeBox.getChildren().addAll(titleLabel, subtitle);

        VBox formWrapper = new VBox(12);
        formWrapper.setAlignment(Pos.CENTER);
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        styleInputField(usernameField);
        PasswordField pinField = new PasswordField();
        pinField.setPromptText("PIN");
        styleInputField(pinField);
        Button signInBtn = createPrimaryButton("Sign In");
        signInBtn.setStyle(
            "-fx-background-color: #FFD700; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: transparent; -fx-cursor: hand;"
        );
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
        
        loginContainer.getChildren().addAll(
            logoBox, appTitle, welcomeBox, formWrapper, signUpRow
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
        
        if (azureApp.loginUser(username, pin)) {
            currentUser = username;
            showMainScreen();
        } else {
            showAlert("Login Failed", 
                "Invalid username or PIN.", 
                Alert.AlertType.ERROR);
            pinField.clear();
        }
    }
    
    private void showRegistrationScreen() {
        VBox regContainer = new VBox(25);
        regContainer.setAlignment(Pos.CENTER);
        regContainer.setPadding(new Insets(60, 40, 60, 40));
        regContainer.setStyle("-fx-background-color: #FFFFFF;");
        
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Create Account");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: #000000;");
        
        Label subtitle = new Label("Join Azure Digital Wallet");
        subtitle.setFont(Font.font("System", 9));
        subtitle.setStyle("-fx-text-fill: #333333;");
        
        headerBox.getChildren().addAll(title, subtitle);
        
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("Username");
        usernameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        usernameLabel.setStyle("-fx-text-fill: #000000;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");
        styleInputField(usernameField);
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        VBox mobileBox = new VBox(8);
        Label mobileLabel = new Label("Mobile Number");
        mobileLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        mobileLabel.setStyle("-fx-text-fill: #000000;");
        TextField mobileField = new TextField();
        mobileField.setPromptText("09XXXXXXXXX (11 digits)");
        styleInputField(mobileField);
        mobileBox.getChildren().addAll(mobileLabel, mobileField);
        
        VBox pinBox = new VBox(8);
        Label pinLabel = new Label("4-Digit PIN");
        pinLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        pinLabel.setStyle("-fx-text-fill: #000000;");
        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Create 4-digit PIN");
        styleInputField(pinField);
        pinBox.getChildren().addAll(pinLabel, pinField);
        
        Button registerBtn = createPrimaryButton("Create Account");
        registerBtn.setOnAction(e -> handleRegistration(usernameField, mobileField, pinField));
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
        
        UserAccount newUser = azureApp.registerUser(username, pin, mobile);
        if (newUser != null) {
            showAlert("Success", 
                "Account created successfully!\nYou can now login with your credentials.", 
                Alert.AlertType.INFORMATION);
            showLoginScreen();
        } else {
            showAlert("Registration Failed", 
                "Username or mobile already exists, or invalid format.", 
                Alert.AlertType.ERROR);
        }
    }
    
    private void showMainScreen() {
        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: #FFFFFF;");
        
        HBox header = createHeader();
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 20, 100, 20));
        
        VBox balanceCard = createBalanceCard();
        
        content.getChildren().addAll(balanceCard);
        scrollPane.setContent(content);
        
        mainContainer.getChildren().addAll(header, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        root.setCenter(mainContainer);
    }
    
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(50, 20, 15, 20));
        header.setStyle("-fx-background-color: #FFFFFF;");
        
        Circle avatar = new Circle(26);
        avatar.setFill(Color.web("#FFD700"));
        avatar.setStroke(Color.web("#e5e7eb"));
        avatar.setStrokeWidth(1.5);

        StackPane avatarStack = new StackPane(avatar);
        avatarStack.setPrefSize(52, 52);
        avatarStack.setMinSize(52, 52);
        avatarStack.setMaxSize(52, 52);
        avatarStack.setAlignment(Pos.CENTER);

        UserAccount user = azureApp.getUser(currentUser);
        Label title = new Label(user.getUsername());
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #000000;");

        HBox nameBox = new HBox(8, title);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setPadding(new Insets(0, 0, 0, 12));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        MenuButton accountMenu = new MenuButton();
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            currentUser = null;
            showLoginScreen();
        });
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
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFC107); " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-border-color: rgba(37,99,235,0.18); " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);"
        );
        
        UserAccount user = azureApp.getUser(currentUser);
        
        Label tierLabel = new Label(user.getLoyaltyTier() + " Account");
        tierLabel.setFont(Font.font("System", 12));
        
        Label brandLabel = new Label("Azure Wallet ✦");
        brandLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Region cardSpacer = new Region();
        VBox.setVgrow(cardSpacer, Priority.ALWAYS);
        
        Label balanceTitle = new Label("Available Balance");
        balanceTitle.setFont(Font.font("System", 12));
        
        Label balanceAmount = new Label(String.format("₱ %.2f", azureApp.getBalance(currentUser)));
        balanceAmount.setFont(Font.font("System", FontWeight.BOLD, 32));
        
        card.getChildren().addAll(tierLabel, brandLabel, cardSpacer, balanceTitle, balanceAmount);
        return card;
    }
    
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

    public static void main(String[] args) {
        launch(args);
    }
}
