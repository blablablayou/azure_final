package azurewallet.main;

import azurewallet.models.UserAccount;
import azurewallet.models.VoucherSystem;
import azurewallet.system.FileManager;
import azurewallet.system.AdminControl;
import java.util.*;
import java.io.Console;
import java.text.DecimalFormat;

public class AzureDigitalApp {
    private final FileManager fileManager;
    private final Map<String, UserAccount> users;
    private final BackgroundScheduler scheduler;
    private final Scanner sc = new Scanner(System.in);
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private static final double WITHDRAW_FEE = 15.0;
    private static final String[] FIXED_MERCHANTS = {
        "Shopee", "Lazada", "Netflix", "Spotify", "Steam", "FoodPanda", "GrabFood"
    };

    public AzureDigitalApp() {
        fileManager = new FileManager();
        users = fileManager.loadUsers();
        scheduler = new BackgroundScheduler(fileManager, users);
        scheduler.runScheduler();
    }

    public void start() {
        while (true) {
            System.out.println("\n+==================================================+");
            System.out.println("|          WELCOME TO AZURE DIGITAL WALLET         |");
            System.out.println("+==================================================+");
            System.out.println("| [1] USER                                         |");
            System.out.println("| [2] ADMIN                                        |");
            System.out.println("| [0] EXIT                                         |");
            System.out.println("+--------------------------------------------------+");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> userMainMenu();
                case "2" -> adminMenu();
                case "0" -> {
                    fileManager.saveUsers(users);
                    System.out.println("System exited successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void userMainMenu() {
        while (true) {
            System.out.println("\n+==================================================+");
            System.out.println("|                     USER MENU                    |");
            System.out.println("+==================================================+");
            System.out.println("| [1] Register                                     |");
            System.out.println("| [2] Login                                        |");
            System.out.println("| [0/B] Back                                       |");
            System.out.println("+--------------------------------------------------+");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            switch (choice.toUpperCase()) {
                case "1" -> register();
                case "2" -> login();
                case "0", "B" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- USER REGISTRATION ---");
        System.out.print("Enter username: ");
        String username = sc.nextLine().trim().toLowerCase();
        if (users.containsKey(username)) {
            System.out.println("Username already exists.");
            return;
        }

        System.out.print("Enter mobile number (11 digits, start with 09): ");
        String mobile = sc.nextLine().trim();
        if (!mobile.matches("^09\\d{9}$")) {
            System.out.println("Invalid phone number. Must start with 09 and contain 11 digits.");
            return;
        }
        for (UserAccount u : users.values()) {
            if (u.getMobile().equals(mobile)) {
                System.out.println("This mobile number is already registered.");
                return;
            }
        }

        System.out.print("Enter 4-digit PIN: ");
        String pin = sc.nextLine().trim();
        if (!pin.matches("\\d{4}")) {
            System.out.println("PIN must be 4 digits.");
            return;
        }

        UserAccount newUser = new UserAccount(username, pin, mobile);
        users.put(username, newUser);
        fileManager.saveUsers(users);
        System.out.println("Registration successful.");
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String username = sc.nextLine().trim().toLowerCase();
        if (!users.containsKey(username)) {
            System.out.println("User not found.");
            return;
        }

        UserAccount acc = users.get(username);
        if (acc.isLocked()) {
            long minsLeft = (acc.getLockEndTime() - System.currentTimeMillis()) / 60000;
            System.out.println("Account is locked. Try again in " + Math.max(minsLeft, 1) + " minute(s).");
            return;
        }

        System.out.print("Enter PIN: ");
        Console console = System.console();
        String pin;
        if (console != null) {
            char[] pinArray = console.readPassword();
            pin = new String(pinArray);
        } else {
            pin = sc.nextLine().trim();
        }
        if (!acc.verifyPin(pin)) {
            acc.registerFailedAttempt();
            fileManager.saveUsers(users);
            System.out.println("Incorrect PIN.");
            return;
        }

        acc.resetLock();
        fileManager.saveUsers(users);
        acc.viewVoucherNotification(fileManager);

        showUserDashboard(acc);
        userMenu(acc);
    }

    private void showUserDashboard(UserAccount acc) {
        System.out.println("\n+==================================================+");
        System.out.println("|                  USER DASHBOARD                  |");
        System.out.println("+==================================================+");
        System.out.printf("| Username : %-35s   |\n", acc.getUsername());
        System.out.printf("| Rank     : %-35s   |\n", acc.getRank());
        System.out.printf("| Balance  : PHP %-30s    |\n", df.format(acc.getBalance()));
        System.out.printf("| Points   : %-35d   |\n", acc.getPoints());
        System.out.printf("| Vouchers : %-35d   |\n", fileManager.countUserVouchers(acc.getUsername()));
        System.out.println("+==================================================+");
    }

    private void userMenu(UserAccount acc) {
        while (true) {
            System.out.println("\n+==================================================+");
            System.out.println("|                   WALLET MENU                    |");
            System.out.println("+==================================================+");
            System.out.println("| [1] Deposit                                      |");
            System.out.println("| [2] Withdraw                                     |");
            System.out.println("| [3] Pay Online                                   |");
            System.out.println("| [4] Send Money to User                           |");
            System.out.println("| [5] Redeem Voucher                               |");
            System.out.println("| [6] Redeem Points                                |");
            System.out.println("| [7] View Balance                                 |");
            System.out.println("| [8] View Transactions                            |");
            System.out.println("| [9] View My Vouchers                             |");
            System.out.println("| [10] Bills Payment                               |");
            System.out.println("| [0/B] Logout                                     |");
            System.out.println("+--------------------------------------------------+");
            System.out.print("Choose: ");
            String ch = sc.nextLine();

            switch (ch.toUpperCase()) {
                case "1" -> deposit(acc);
                case "2" -> withdraw(acc);
                case "3" -> payOnline(acc);
                case "4" -> sendMoney(acc);
                case "5" -> redeemVoucher(acc);
                case "6" -> redeemPoints(acc);
                case "7" -> acc.displayBalance();
                case "8" -> fileManager.showTransactions(acc.getUsername());
                case "9" -> acc.viewMyVouchers(fileManager);              // <-- ensured here
                case "10" -> billsPayment(acc);
                case "0", "B" -> {
                    fileManager.saveUsers(users);
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private boolean confirmAction() {
        System.out.print("\nProceed? (Y/N): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        return confirm.equals("Y");
    }

    private void deposit(UserAccount acc) {
        System.out.print("Enter amount to deposit (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equalsIgnoreCase("0") || input.equalsIgnoreCase("B")) return;

        double amount = Double.parseDouble(input);

        System.out.println("Choose Deposit Source:");
        System.out.println("[1] GCash");
        System.out.println("[2] PayMaya");
        System.out.println("[3] Bank of the Philippine Island");
        System.out.println("[4] Banco De Oro");
        System.out.println("[5] Metrobank");
        System.out.print("Choose: ");
        String source = sc.nextLine().trim();

        String reference = "AZR-" + (int)(Math.random() * 900000 + 100000);

        switch (source) {
            case "1" -> {
                System.out.print("\nSend PHP " + df.format(amount) + " to Azure Wallet using GCash.");
                System.out.println("\nEnter this Reference Code in GCash message: REFERENCE " + reference);
                System.out.print("\nType DONE once you sent the GCash payment: ");

                while (true) {
                    String confirm = sc.nextLine().trim();
                    if (confirm.equalsIgnoreCase("DONE")) {
                        System.out.println("GCash payment confirmed.");
                        break;
                    }
                    System.out.println("Please type DONE to continue: ");
                }
            }
            
            case "2" -> {
                System.out.print("\nSend PHP " + df.format(amount) + " to Azure Wallet using PayMaya.");
                System.out.println("\nEnter this Reference Code in PayMaya message: REFERENCE " + reference);
                System.out.print("\nType DONE once you sent the PayMaya payment: ");

                while (true) {
                    String confirm = sc.nextLine().trim();
                    if (confirm.equalsIgnoreCase("DONE")) {
                        System.out.println("PayMaya payment confirmed.");
                        break;
                    }
                    System.out.println("Please type DONE to continue: ");
                }
            }

            case "3" -> {
                System.out.print("\nSend PHP " + df.format(amount) + " via BPI Transfer to 3rd Party.");
                System.out.println("\nAttach reference code: REFERENCE " + reference);
                System.out.print("\nType DONE once you sent the BPI payment: ");

                while (true) {
                    String confirm = sc.nextLine().trim();
                    if (confirm.equalsIgnoreCase("DONE")) {
                        System.out.println("BPI payment confirmed.");
                        break;
                    }
                    System.out.println("Please type DONE to continue: ");
                }
            }

            case "4" -> {
                System.out.print("\nOpen BDO Online Banking -> Send Money.");
                System.out.println("\nAttach reference code: REFERENCE " + reference);
                System.out.print("\nType DONE once you sent the BDO payment: ");

                while (true) {
                    String confirm = sc.nextLine().trim();
                    if (confirm.equalsIgnoreCase("DONE")) {
                        System.out.println("BDO Transfer Verified!.");
                        break;
                    }
                    System.out.println("Please type DONE to continue: ");
                }
            }

            case "5" -> {
                System.out.print("\nTransfer using Metrobank Direct.");
                System.out.println("\nAttach reference code: REFERENCE " + reference);
                System.out.print("\nType DONE once Metrobank transfer is complete: ");

                while (true) {
                    String confirm = sc.nextLine().trim();
                    if (confirm.equalsIgnoreCase("DONE")) {
                        System.out.println("Metrobank Payment Confirmed!");
                        break;
                    }
                    System.out.println("Please type DONE to continue: ");
                }
            }
        }

        String finalReference = reference;

        if (!confirmAction()) return;

        String prevRank = acc.getRank();
        if (acc.getBalance() + amount > acc.getDepositLimit()) {
            System.out.println("Deposit exceeds your rank limit of PHP " + df.format(acc.getDepositLimit()));
            return;
        }

        acc.deposit(amount);
        acc.addTotalTransacted(amount);
        checkRankUp(acc, prevRank);

        fileManager.logTransaction(acc.getUsername(), "Deposit", amount);

        int pointsEarned = (int) (amount / 1000);
        if (pointsEarned > 0) {
            acc.addPoints(pointsEarned);
            fileManager.logPoints(acc.getUsername(), "earned", pointsEarned, "from deposit");
            System.out.println("You earned " + pointsEarned + " points from this deposit!");
        }

        fileManager.saveUsers(users);
        System.out.println("Deposit successful. Balance: PHP " + df.format(acc.getBalance()));
    }

    private void withdraw(UserAccount acc) {
        System.out.print("Enter amount to withdraw (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equalsIgnoreCase("0") || input.equalsIgnoreCase("B")) return;

        double amount = Double.parseDouble(input);

        System.out.println("Choose Withdrawal Destination:");
        System.out.println("[1] GCash");
        System.out.println("[2] PayMaya");
        System.out.println("[3] Bank of the Philippine Island");
        System.out.println("[4] Banco De Oro");
        System.out.println("[5] Metrobank");
        System.out.print("Choose: ");
        String dest = sc.nextLine().trim();

        String destination = "";
        if (!dest.equals("1")) {
            System.out.print("Enter account holder name: ");
            String accountName = sc.nextLine();
            System.out.print("Enter account number: ");
            String accountNumber = sc.nextLine();

            destination = switch (dest) {
                case "1" -> "GCash - " + accountName + " (" + accountNumber + ")";
                case "2" -> "PayMaya - " + accountName + " (" + accountNumber + ")";
                case "3" -> "BPI - " + accountName + " (" + accountNumber + ")";
                case "4" -> "BDO - " + accountName + " (" + accountNumber + ")";
                case "5" -> "Metrobank - " + accountName + " (" + accountNumber + ")";
                default -> "Unknown";
            };
        } else {
            destination = "Azure Wallet";
        }
        if (!confirmAction()) return;

        if (amount <= 0 || amount > acc.getWithdrawLimit()) {
            System.out.println("Invalid or exceeds limit (" + df.format(acc.getWithdrawLimit()) + ")");
            return;
        }
        double totalAmount = amount + WITHDRAW_FEE;
        if (totalAmount > acc.getBalance()) {
            System.out.println("Insufficient balance including fee of PHP 15.00.");
            return;
        }
        acc.withdraw(totalAmount);
        fileManager.logTransaction(acc.getUsername(), "Withdraw", amount);
        fileManager.logSystemRevenue(WITHDRAW_FEE);
        fileManager.saveUsers(users);
        System.out.println("Withdraw successful. PHP 15.00 fee applied. New balance: PHP " + df.format(acc.getBalance()));
    }

    private void payOnline(UserAccount acc) {
        System.out.print("Select merchant to pay (0/B to go back):\n");
        for (int i = 0; i < FIXED_MERCHANTS.length; i++) {
            System.out.println("[" + (i + 1) + "] " + FIXED_MERCHANTS[i]);
        }
        System.out.print("Enter number or type merchant name: ");
        String merchantInput = sc.nextLine().trim();

        String merchant;
        try {
            int m = Integer.parseInt(merchantInput);
            merchant = FIXED_MERCHANTS[m - 1];
        } catch (NumberFormatException e) {
            merchant = merchantInput;
        }
        if (merchant.equalsIgnoreCase("0") || merchant.equalsIgnoreCase("B")) return;

        System.out.print("Enter amount: ");
        double amount = Double.parseDouble(sc.nextLine());
        if (!confirmAction()) return;

        if (amount <= 0 || amount > acc.getSendLimit()) {
            System.out.println("Invalid or exceeds limit (" + df.format(acc.getSendLimit()) + ")");
            return;
        }
        if (amount > acc.getBalance()) {
            System.out.println("Insufficient balance.");
            return;
        }
        acc.withdraw(amount);
        fileManager.logTransaction(acc.getUsername(), "Paid to " + merchant, amount);
        fileManager.saveUsers(users);
        System.out.println("Payment of PHP " + df.format(amount) + " to " + merchant + " successful.");
    }

    private void sendMoney(UserAccount acc) {
        System.out.print("Enter recipient username (0/B to go back): ");
        String recipient = sc.nextLine().trim().toLowerCase();
        if (recipient.equalsIgnoreCase("0") || recipient.equalsIgnoreCase("B")) return;

        if (!users.containsKey(recipient)) {
            System.out.println("Recipient not found.");
            return;
        }

        System.out.println("Choose Transfer Method:");
        System.out.println("[1] Azure User");
        System.out.println("[2] GCash");
        System.out.println("[3] PayMaya");
        System.out.println("[4] BPI");
        System.out.println("[5] BDO");
        System.out.println("[6] Metrobank");
        System.out.print("Choose: ");
        String method = sc.nextLine().trim();

        String accountName = "";
        String accountNumber = "";

        if (!method.equals("1")) {
            System.out.print("Enter account holder name: ");
            accountName = sc.nextLine().trim();

            System.out.print("Enter account number: ");
            accountNumber = sc.nextLine().trim();
        }
        System.out.print("Enter amount to send: ");
        double amount = Double.parseDouble(sc.nextLine());
        if (!confirmAction()) return;

        if (amount <= 0 || amount > acc.getSendLimit()) {
            System.out.println("Invalid or exceeds limit (" + df.format(acc.getSendLimit()) + ")");
            return;
        }
        if (amount > acc.getBalance()) {
            System.out.println("Insufficient balance.");
            return;
        }

        UserAccount receiver = users.get(recipient);
        acc.withdraw(amount);
        receiver.deposit(amount);
        String destination = switch (method) {
            case "1" -> recipient;
            case "2" -> "GCash - " + accountName + " (" + accountNumber + ")";
            case "3" -> "PayMaya - " + accountName + " (" + accountNumber + ")";
            case "4" -> "BPI - " + accountName + " (" + accountNumber + ")";
            case "5" -> "BDO - " + accountName + " (" + accountNumber + ")";
            case "6" -> "Metrobank - " + accountName + " (" + accountNumber + ")";
            default -> recipient;
        };
        fileManager.logTransaction(acc.getUsername(), "Sent to " + destination, amount);
        fileManager.logTransaction(recipient, "Received from " + acc.getUsername(), amount);
        fileManager.saveUsers(users);
        System.out.println("Successfully sent PHP " + df.format(amount) + " to " + recipient + ".");
    }

    private void redeemVoucher(UserAccount acc) {
        System.out.print("Enter voucher code (0/B to go back): ");
        String code = sc.nextLine().trim();
        if (code.equalsIgnoreCase("0") || code.equalsIgnoreCase("B")) return;
        if (!confirmAction()) return;

        double value = VoucherSystem.redeemVoucher(acc, code, fileManager);
        if (value > 0) {
            fileManager.saveUsers(users);
            System.out.println("Voucher redeemed successfully! +PHP " + df.format(value));
        } else {
            System.out.println("Invalid or expired voucher.");
        }
    }

    private void redeemPoints(UserAccount acc) {
        System.out.print("Enter points to redeem (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equalsIgnoreCase("0") || input.equalsIgnoreCase("B")) return;
        if (!confirmAction()) return;

        int pts = Integer.parseInt(input);
        if (pts <= 0 || pts > acc.getPoints()) {
            System.out.println("Invalid points.");
            return;
        }
        double value = pts * 1.0;
        acc.redeemPoints(pts, value);
        fileManager.logPoints(acc.getUsername(), "redeemed", pts, "converted to PHP " + df.format(value));
        fileManager.saveUsers(users);
        System.out.println("Redeemed " + pts + " points = PHP " + df.format(value));
    }

    private void adminMenu() {
        AdminControl admin = new AdminControl(fileManager, users, scheduler);
        admin.menu(sc);
    }

    private void billsPayment(UserAccount acc) {
        System.out.print("\n--- BILLS PAYMENT ---");
        String[] billers = {"Meralco", "Maynilad", "Globe", "Smart", "Converge ICT", "PLDT"};
        System.out.println("\nSelect Biller:");
        for (int i = 0; i < billers.length; i++) {
            System.out.println("[" + (i + 1) + "] " + billers[i]);
        }
        System.out.print("Choose: ");
        int ch = Integer.parseInt(sc.nextLine());

        String biller = billers[ch - 1];

        System.out.print("Enter Account Number: ");
        String acct = sc.nextLine();
        System.out.print("Enter Amount: ");
        double amount = Double.parseDouble(sc.nextLine());

        if (!confirmAction()) return;
        if (amount > acc.getBalance()) {
            System.out.println("Insufficient balance.");
            return;
        }

        acc.withdraw(amount);
        fileManager.logTransaction(acc.getUsername(), "Bills Payment to " + biller + " (Acct: " + acct + ")", amount);
        fileManager.saveUsers(users);
        System.out.println("Payment successful!");
    }

    // Helper for rank-up notification
    private void checkRankUp(UserAccount acc, String prevRank) {
        if (!acc.getRank().equals(prevRank)) {
            System.out.println("Congratulations! Your account rank has been upgraded to " + acc.getRank() + "!");
        }
    }
}