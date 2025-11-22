package com.azurewallet.gui;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import com.azurewallet.models.UserAccount;
import java.text.DecimalFormat;
import java.io.BufferedReader;
import java.io.FileReader;

public class DashboardController {
    private Scene scene;
    private AzureWalletApp app;
    private UserAccount account;
    private DecimalFormat df = new DecimalFormat("#,##0.00");
    private VBox centerContent;

    public DashboardController(AzureWalletApp app, UserAccount account) {
        this.app = app;
        this.account = account;
        this.scene = createDashboard();
    }

    private Scene createDashboard() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + AzureWalletApp.getPrimaryColor() + ";");

        root.setTop(createHeader());
        root.setLeft(createSidebar());

        centerContent = new VBox();
        centerContent.setStyle("-fx-background-color: " + AzureWalletApp.getPrimaryColor() + ";");
        centerContent.setPadding(new Insets(20));
        centerContent.setStyle("-fx-background-color: " + AzureWalletApp.getPrimaryColor() + ";");
        showDashboard();
        
        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setStyle("-fx-background-color: " + AzureWalletApp.getPrimaryColor() + "; -fx-control-inner-background: " + AzureWalletApp.getPrimaryColor() + ";");
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);

        return new Scene(root, 1000, 750);
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: " + AzureWalletApp.getSecondaryColor() + "; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.SPACE_BETWEEN);

        Label title = new Label("🔷 AZURE DIGITAL WALLET");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getAccentColor() + ";");

        HBox rightBox = new HBox(15);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label userInfo = new Label("👤 " + account.getUsername().toUpperCase());
        userInfo.setStyle("-fx-text-fill: " + AzureWalletApp.getTextColor() + "; -fx-font-size: 12;");

        Button logoutBtn = new Button("LOGOUT");
        logoutBtn.setStyle("-fx-padding: 8 15; -fx-background-color: " + AzureWalletApp.getDangerColor() + "; -fx-text-fill: white; -fx-font-weight: bold;");
        logoutBtn.setOnAction(e -> app.switchToLogin());

        rightBox.getChildren().addAll(userInfo, logoutBtn);
        header.getChildren().addAll(title, rightBox);

        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(15));
        sidebar.setStyle("-fx-background-color: " + AzureWalletApp.getSecondaryColor() + "; -fx-border-color: #444; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(180);

        Label menuLabel = new Label("MENU");
        menuLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getAccentColor() + ";");

        String[][] menuItems = {
            {"Dashboard", "dashboard"},
            {"Deposit", "deposit"},
            {"Withdraw", "withdraw"},
            {"Send Money", "send"},
            {"Pay Online", "payonline"},
            {"Bills Payment", "bills"},
            {"Buy Load", "load"},
            {"Redeem Voucher", "voucher"},
            {"Redeem Points", "points"},
            {"Transactions", "transactions"}
        };

        sidebar.getChildren().add(menuLabel);
        for (String[] item : menuItems) {
            Button btn = createMenuButton(item[0], item[1]);
            sidebar.getChildren().add(btn);
        }

        return sidebar;
    }

    private Button createMenuButton(String text, String action) {
        Button btn = new Button(text);
        btn.setPrefWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-padding: 10; -fx-background-color: " + AzureWalletApp.getAccentColor() + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btn.setOnAction(e -> handleMenuAction(action));
        return btn;
    }

    private void handleMenuAction(String action) {
        centerContent.getChildren().clear();
        switch (action) {
            case "dashboard" -> showDashboard();
            case "deposit" -> showDepositView();
            case "withdraw" -> showWithdrawView();
            case "send" -> showSendView();
            case "payonline" -> showPayOnlineView();
            case "bills" -> showBillsView();
            case "load" -> showLoadView();
            case "voucher" -> showVoucherView();
            case "points" -> showPointsView();
            case "transactions" -> showTransactionsView();
        }
    }

    private void showDashboard() {
        VBox dashboard = new VBox(15);
        dashboard.setPadding(new Insets(10));

        // Balance Card
        VBox balanceCard = createBalanceCard();
        dashboard.getChildren().add(balanceCard);

        // Quick Stats
        HBox statsBox = createStatsBox();
        dashboard.getChildren().add(statsBox);

        centerContent.getChildren().add(dashboard);
    }

    private VBox createBalanceCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: " + AzureWalletApp.getAccentColor() + "; -fx-background-radius: 10;");

        Label balanceTitle = new Label("Current Balance");
        balanceTitle.setStyle("-fx-font-size: 14; -fx-text-fill: rgba(255,255,255,0.8);");

        Label balanceAmount = new Label("PHP " + df.format(account.getBalance()));
        balanceAmount.setStyle("-fx-font-size: 42; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox infoBox = new HBox(40);
        infoBox.setPadding(new Insets(20, 0, 0, 0));

        VBox rankBox = new VBox(5);
        Label rankLabel = new Label("RANK");
        rankLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11;");
        Label rankValue = new Label(account.getRank().toUpperCase());
        rankValue.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        rankBox.getChildren().addAll(rankLabel, rankValue);

        VBox pointsBox = new VBox(5);
        Label pointsLabel = new Label("POINTS");
        pointsLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11;");
        Label pointsValue = new Label(account.getPoints() + " PTS");
        pointsValue.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        pointsBox.getChildren().addAll(pointsLabel, pointsValue);

        infoBox.getChildren().addAll(rankBox, pointsBox);
        card.getChildren().addAll(balanceTitle, balanceAmount, infoBox);

        return card;
    }

    private HBox createStatsBox() {
        HBox box = new HBox(15);

        String[] stats = {
            "Total Transacted\nPHP " + df.format(account.getTotalTransacted()),
            "Vouchers\n" + app.getFileManager().countUserVouchers(account.getUsername()),
            "Deposit Limit\nPHP " + df.format(account.getDepositLimit()),
            "Withdraw Limit\nPHP " + df.format(account.getWithdrawLimit())
        };

        for (String stat : stats) {
            VBox statCard = new VBox();
            statCard.setPadding(new Insets(15));
            statCard.setStyle("-fx-background-color: " + AzureWalletApp.getSecondaryColor() + "; -fx-border-radius: 5;");
            statCard.setAlignment(Pos.CENTER);
            statCard.setPrefWidth(150);

            Label statLabel = new Label(stat);
            statLabel.setStyle("-fx-text-fill: " + AzureWalletApp.getTextColor() + "; -fx-font-size: 11; -fx-text-alignment: center;");
            statLabel.setWrapText(true);
            statCard.getChildren().add(statLabel);
            box.getChildren().add(statCard);
        }

        return box;
    }

    private void showDepositView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("💰 DEPOSIT");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.setStyle(getTextFieldStyle());
        amountField.setPrefWidth(300);

        ComboBox<String> sourceCombo = new ComboBox<>();
        sourceCombo.getItems().addAll("GCash", "PayMaya", "Bank of the Philippine Island", "Banco De Oro", "Metrobank");
        sourceCombo.setStyle("-fx-font-size: 12;");

        Button depositBtn = new Button("PROCEED DEPOSIT");
        depositBtn.setStyle("-fx-padding: 12; -fx-font-size: 14; -fx-background-color: " + AzureWalletApp.getSuccessColor() + "; -fx-text-fill: white;");
        depositBtn.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String source = sourceCombo.getValue();
                if (source == null) {
                    showAlert("Error", "Select a deposit source", Alert.AlertType.WARNING);
                    return;
                }
                handleDeposit(amount, source);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid amount", Alert.AlertType.ERROR);
            }
        });

        view.getChildren().addAll(title, new Label("Amount"), amountField, new Label("Deposit Source"), sourceCombo, depositBtn);
        centerContent.getChildren().add(view);
    }

    private void handleDeposit(double amount, String source) {
        String reference = "AZR-" + (int)(Math.random() * 900000 + 100000);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Deposit Instructions");
        alert.setContentText("Send PHP " + df.format(amount) + " via " + source + "\n\nReference Code: " + reference + "\n\nEnter reference code in payment message.");
        alert.showAndWait();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Confirm Deposit");
        dialog.setHeaderText(null);
        dialog.setContentText("Type DONE once you've sent the payment:");
        
        if (dialog.showAndWait().isPresent() && dialog.getResult().equalsIgnoreCase("DONE")) {
            if (amount > account.getDepositLimit()) {
                showAlert("Error", "Deposit exceeds your limit", Alert.AlertType.ERROR);
                return;
            }
            String prevRank = account.getRank();
            account.deposit(amount);
            account.addTotalTransacted(amount);
            app.getFileManager().logTransaction(account.getUsername(), "Deposit via " + source, amount);
            
            int pointsEarned = (int) (amount / 1000);
            if (pointsEarned > 0) {
                account.addPoints(pointsEarned);
                app.getFileManager().logPoints(account.getUsername(), "earned", pointsEarned, "from deposit");
            }
            
            if (!account.getRank().equals(prevRank)) {
                showAlert("Rank Up!", "Congratulations! Your rank upgraded to " + account.getRank(), Alert.AlertType.INFORMATION);
            }
            
            app.getFileManager().saveUsers(app.getUsers());
            showAlert("Success", "Deposit successful! +PHP " + df.format(amount), Alert.AlertType.INFORMATION);
            showDashboard();
        }
    }

    private void showWithdrawView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("💸 WITHDRAW");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.setStyle(getTextFieldStyle());

        ComboBox<String> destCombo = new ComboBox<>();
        destCombo.getItems().addAll("GCash", "PayMaya", "BPI", "BDO", "Metrobank");

        TextField nameField = new TextField();
        nameField.setPromptText("Account holder name");
        nameField.setStyle(getTextFieldStyle());

        TextField accountField = new TextField();
        accountField.setPromptText("Account number");
        accountField.setStyle(getTextFieldStyle());

        Button withdrawBtn = new Button("PROCESS WITHDRAWAL");
        withdrawBtn.setStyle("-fx-padding: 12; -fx-background-color: " + AzureWalletApp.getDangerColor() + "; -fx-text-fill: white;");
        withdrawBtn.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String dest = destCombo.getValue();
                double totalAmount = amount + 15.0;
                
                if (dest == null) {
                    showAlert("Error", "Select destination", Alert.AlertType.WARNING);
                    return;
                }
                if (totalAmount > account.getBalance()) {
                    showAlert("Error", "Insufficient balance (includes PHP 15 fee)", Alert.AlertType.ERROR);
                    return;
                }
                if (amount > account.getWithdrawLimit()) {
                    showAlert("Error", "Exceeds withdrawal limit", Alert.AlertType.ERROR);
                    return;
                }
                
                account.withdraw(totalAmount);
                app.getFileManager().logTransaction(account.getUsername(), "Withdraw to " + dest, amount);
                app.getFileManager().logSystemRevenue(15.0);
                app.getFileManager().saveUsers(app.getUsers());
                
                showAlert("Success", "Withdrawn PHP " + df.format(amount) + " (Fee: PHP 15.00)", Alert.AlertType.INFORMATION);
                showDashboard();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid amount", Alert.AlertType.ERROR);
            }
        });

        view.getChildren().addAll(title, new Label("Amount"), amountField, new Label("Destination"), destCombo, new Label("Account Holder"), nameField, new Label("Account Number"), accountField, withdrawBtn);
        centerContent.getChildren().add(view);
    }

    private void showSendView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("📤 SEND MONEY");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        TextField recipientField = new TextField();
        recipientField.setPromptText("Recipient username");
        recipientField.setStyle(getTextFieldStyle());

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setStyle(getTextFieldStyle());

        Button sendBtn = new Button("SEND MONEY");
        sendBtn.setStyle("-fx-padding: 12; -fx-font-size: 14; -fx-background-color: " + AzureWalletApp.getAccentColor() + "; -fx-text-fill: white;");
        sendBtn.setOnAction(e -> {
            try {
                String recipient = recipientField.getText().toLowerCase();
                double amount = Double.parseDouble(amountField.getText());

                if (!app.getUsers().containsKey(recipient)) {
                    showAlert("Error", "Recipient not found", Alert.AlertType.ERROR);
                    return;
                }
                if (amount > account.getSendLimit()) {
                    showAlert("Error", "Exceeds send limit", Alert.AlertType.ERROR);
                    return;
                }
                if (amount > account.getBalance()) {
                    showAlert("Error", "Insufficient balance", Alert.AlertType.ERROR);
                    return;
                }

                UserAccount receiver = app.getUsers().get(recipient);
                account.withdraw(amount);
                receiver.deposit(amount);
                app.getFileManager().logTransaction(account.getUsername(), "Sent to " + recipient, amount);
                app.getFileManager().logTransaction(recipient, "Received from " + account.getUsername(), amount);
                app.getFileManager().saveUsers(app.getUsers());

                showAlert("Success", "Sent PHP " + df.format(amount) + " to " + recipient, Alert.AlertType.INFORMATION);
                showDashboard();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid amount", Alert.AlertType.ERROR);
            }
        });

        view.getChildren().addAll(title, new Label("Recipient"), recipientField, new Label("Amount"), amountField, sendBtn);
        centerContent.getChildren().add(view);
    }

    private void showPayOnlineView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("🛍️ PAY ONLINE");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        ComboBox<String> merchantCombo = new ComboBox<>();
        merchantCombo.getItems().addAll("Shopee", "Lazada", "Netflix", "Spotify", "Steam", "FoodPanda", "GrabFood");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setStyle(getTextFieldStyle());

        Button payBtn = new Button("PAY NOW");
        payBtn.setStyle("-fx-padding: 12; -fx-background-color: " + AzureWalletApp.getSuccessColor() + "; -fx-text-fill: white;");
        payBtn.setOnAction(e -> {
            try {
                String merchant = merchantCombo.getValue();
                double amount = Double.parseDouble(amountField.getText());

                if (merchant == null) {
                    showAlert("Error", "Select merchant", Alert.AlertType.WARNING);
                    return;
                }
                if (amount > account.getSendLimit()) {
                    showAlert("Error", "Exceeds payment limit", Alert.AlertType.ERROR);
                    return;
                }
                if (amount > account.getBalance()) {
                    showAlert("Error", "Insufficient balance", Alert.AlertType.ERROR);
                    return;
                }

                account.withdraw(amount);
                app.getFileManager().logTransaction(account.getUsername(), "Paid to " + merchant, amount);
                app.getFileManager().saveUsers(app.getUsers());

                showAlert("Success", "Payment of PHP " + df.format(amount) + " sent to " + merchant, Alert.AlertType.INFORMATION);
                showDashboard();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid amount", Alert.AlertType.ERROR);
            }
        });

        view.getChildren().addAll(title, new Label("Merchant"), merchantCombo, new Label("Amount"), amountField, payBtn);
        centerContent.getChildren().add(view);
    }

    private void showBillsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("💡 BILLS PAYMENT");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        ComboBox<String> billerCombo = new ComboBox<>();
        billerCombo.getItems().addAll("Meralco", "Maynilad", "Globe", "Smart", "Converge ICT", "PLDT");

        TextField accountField = new TextField();
        accountField.setPromptText("Account Number");
        accountField.setStyle(getTextFieldStyle());

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setStyle(getTextFieldStyle());

        Button payBtn = new Button("PAY BILL");
        payBtn.setStyle("-fx-padding: 12; -fx-background-color: " + AzureWalletApp.getAccentColor() + "; -fx-text-fill: white;");
        payBtn.setOnAction(e -> {
            try {
                String biller = billerCombo.getValue();
                String acct = accountField.getText();
                double amount = Double.parseDouble(amountField.getText());

                if (biller == null) {
                    showAlert("Error", "Select biller", Alert.AlertType.WARNING);
                    return;
                }
                if (amount > account.getBalance()) {
                    showAlert("Error", "Insufficient balance", Alert.AlertType.ERROR);
                    return;
                }

                account.withdraw(amount);
                app.getFileManager().logTransaction(account.getUsername(), "Bills Payment to " + biller + " (Acct: " + acct + ")", amount);
                app.getFileManager().saveUsers(app.getUsers());

                showAlert("Success", "Payment successful!", Alert.AlertType.INFORMATION);
                showDashboard();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid amount", Alert.AlertType.ERROR);
            }
        });

        view.getChildren().addAll(title, new Label("Biller"), billerCombo, new Label("Account"), accountField, new Label("Amount"), amountField, payBtn);
        centerContent.getChildren().add(view);
    }

    private void showLoadView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("📱 BUY PREPAID LOAD");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        ComboBox<String> networkCombo = new ComboBox<>();
        networkCombo.getItems().addAll("Globe/TM", "Smart/TNT", "DITO");

        TextField numberField = new TextField();
        numberField.setPromptText("Mobile number (11 digits)");
        numberField.setStyle(getTextFieldStyle());

        ComboBox<Integer> loadCombo = new ComboBox<>();
        loadCombo.getItems().addAll(50, 100, 150, 200);

        Button buyBtn = new Button("BUY LOAD");
        buyBtn.setStyle("-fx-padding: 12; -fx-background-color: " + AzureWalletApp.getSuccessColor() + "; -fx-text-fill: white;");
        buyBtn.setOnAction(e -> {
            String network = networkCombo.getValue();
            String number = numberField.getText();
            Integer amount = loadCombo.getValue();

            if (network == null || amount == null) {
                showAlert("Error", "Fill all fields", Alert.AlertType.WARNING);
                return;
            }
            if (!number.matches("^09\\d{9}$")) {
                showAlert("Error", "Invalid mobile number", Alert.AlertType.ERROR);
                return;
            }
            if (amount > account.getBalance()) {
                showAlert("Error", "Insufficient balance", Alert.AlertType.ERROR);
                return;
            }

            account.withdraw(amount);
            app.getFileManager().logTransaction(account.getUsername(), "Prepaid Load (" + network + " - " + number + ")", amount);
            app.getFileManager().saveUsers(app.getUsers());

            showAlert("Success", "Load purchased successfully!", Alert.AlertType.INFORMATION);
            showDashboard();
        });

        view.getChildren().addAll(title, new Label("Network"), networkCombo, new Label("Mobile Number"), numberField, new Label("Load Amount"), loadCombo, buyBtn);
        centerContent.getChildren().add(view);
    }

    private void showVoucherView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("🎟️ REDEEM VOUCHER");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        TextField codeField = new TextField();
        codeField.setPromptText("Voucher Code");
        codeField.setStyle(getTextFieldStyle());

        Button redeemBtn = new Button("REDEEM");
        redeemBtn.setStyle("-fx-padding: 12; -fx-background-color: " + AzureWalletApp.getAccentColor() + "; -fx-text-fill: white;");
        redeemBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                showAlert("Error", "Enter voucher code", Alert.AlertType.WARNING);
                return;
            }

            double value = com.azurewallet.models.VoucherSystem.redeemVoucher(account, code, app.getFileManager());
            if (value > 0) {
                app.getFileManager().saveUsers(app.getUsers());
                showAlert("Success", "Voucher redeemed! +PHP " + df.format(value), Alert.AlertType.INFORMATION);
                showDashboard();
            } else {
                showAlert("Error", "Invalid or expired voucher", Alert.AlertType.ERROR);
            }
        });

        view.getChildren().addAll(title, new Label("Voucher Code"), codeField, redeemBtn);
        centerContent.getChildren().add(view);
    }

    private void showPointsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("⭐ REDEEM POINTS");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        Label availableLabel = new Label("Available Points: " + account.getPoints());
        availableLabel.setStyle("-fx-font-size: 14; -fx-text-fill: " + AzureWalletApp.getAccentColor() + ";");

        TextField pointsField = new TextField();
        pointsField.setPromptText("Points to redeem");
        pointsField.setStyle(getTextFieldStyle());

        Button redeemBtn = new Button("REDEEM POINTS");
        redeemBtn.setStyle("-fx-padding: 12; -fx-background-color: " + AzureWalletApp.getSuccessColor() + "; -fx-text-fill: white;");
        redeemBtn.setOnAction(e -> {
            try {
                int pts = Integer.parseInt(pointsField.getText());
                if (pts <= 0 || pts > account.getPoints()) {
                    showAlert("Error", "Invalid points", Alert.AlertType.ERROR);
                    return;
                }

                double value = pts * 1.0;
                account.redeemPoints(pts, value);
                app.getFileManager().logPoints(account.getUsername(), "redeemed", pts, "converted to PHP " + df.format(value));
                app.getFileManager().saveUsers(app.getUsers());

                showAlert("Success", "Redeemed " + pts + " points = PHP " + df.format(value), Alert.AlertType.INFORMATION);
                showDashboard();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid input", Alert.AlertType.ERROR);
            }
        });

        view.getChildren().addAll(title, availableLabel, new Label("Points"), pointsField, redeemBtn);
        centerContent.getChildren().add(view);
    }

    private void showTransactionsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("📋 TRANSACTION HISTORY");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");

        TextArea transactionArea = new TextArea();
        transactionArea.setEditable(false);
        transactionArea.setStyle("-fx-control-inner-background: " + AzureWalletApp.getSecondaryColor() + "; -fx-text-fill: " + AzureWalletApp.getTextColor() + ";");
        transactionArea.setPrefHeight(400);
        transactionArea.setWrapText(true);

        StringBuilder transactions = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/data/transactions.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(account.getUsername())) {
                    transactions.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            transactions.append("No transactions found");
        }

        if (transactions.length() == 0) {
            transactions.append("No transactions yet");
        }

        transactionArea.setText(transactions.toString());
        view.getChildren().addAll(title, transactionArea);
        centerContent.getChildren().add(view);
    }

    private String getTextFieldStyle() {
        String bgColor = AzureWalletApp.isDarkMode ? "#3a3a3a" : "#e8e8e8";
        String textColor = AzureWalletApp.getTextColor();
        return "-fx-padding: 10; -fx-font-size: 12; -fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-border-color: " + AzureWalletApp.getAccentColor() + "; -fx-border-width: 0 0 2 0;";
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
