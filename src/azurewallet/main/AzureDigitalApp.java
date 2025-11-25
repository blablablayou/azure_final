package azurewallet.main;

import azurewallet.models.UserAccount;
import azurewallet.system.FileManager;
import java.util.List;

/**
 * Core Azure Digital Wallet application logic
 */
public class AzureDigitalApp {
    private FileManager fileManager = new FileManager();
    private BackgroundScheduler scheduler;
    
    public AzureDigitalApp() {
        this.scheduler = new BackgroundScheduler();
    }
    
    /**
     * Verify user login credentials
     * @param username The username
     * @param pin The PIN to verify
     * @return true if credentials are valid
     */
    public boolean loginUser(String username, String pin) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) {
            return false;
        }
        
        return user.verifyPin(pin);
    }
    
    /**
     * Get a user account by username
     * @param username The username
     * @return The user account or null if not found
     */
    public UserAccount getUser(String username) {
        return FileManager.loadUser(username);
    }
    
    /**
     * Get all users
     * @return List of all user accounts
     */
    public List<UserAccount> getUsers() {
        return FileManager.loadAllUsers();
    }
    
    /**
     * Create a new user account
     * @param username The username
     * @param pin The PIN
     * @param mobile The mobile number
     * @return The created user account
     */
    public UserAccount registerUser(String username, String pin, String mobile) {
        UserAccount user = new UserAccount(username, pin, mobile);
        
        // Generate unique account number for every new user at registration time
        // Format: 16-digit number (e.g., 4532123456783456)
        String uniqueAccountNumber = generateUniqueAccountNumber();
        user.setVirtualBankNumber(uniqueAccountNumber);
        
        // Generate unique CVV for every new user at registration time
        String uniqueCVV = String.format("%03d", 100 + (int)(Math.random() * 900));
        user.setCardCVV(uniqueCVV);
        
        // Set default expiry date (11/30)
        user.setCardExpiryDate("11/30");
        
        FileManager.saveUser(user);
        return user;
    }
    
    /**
     * Generate a unique 16-digit account number
     * @return A unique 16-digit account number
     */
    private String generateUniqueAccountNumber() {
        // Generate random 16-digit account number (4532XXXXXXXXXXXX format)
        StringBuilder accountNumber = new StringBuilder("4532");
        for (int i = 0; i < 12; i++) {
            accountNumber.append((int)(Math.random() * 10));
        }
        return accountNumber.toString();
    }
    
    /**
     * Update a user account
     * @param user The user account to update
     */
    public void updateUser(UserAccount user) {
        FileManager.saveUser(user);
    }
    
    /**
     * Delete a user account
     * @param username The username to delete
     */
    public void deleteUser(String username) {
        FileManager.deleteUser(username);
    }
    
    /**
     * Log a transaction
     * @param transactionData The transaction data
     */
    public void logTransaction(String transactionData) {
        FileManager.logTransaction(transactionData);
    }
    
    /**
     * Get the file manager instance
     * @return FileManager instance
     */
    public FileManager getFileManager() {
        return fileManager;
    }
    
    /**
     * Get the balance of a user
     * @param username The username
     * @return The balance or 0 if user not found
     */
    public double getBalance(String username) {
        UserAccount user = FileManager.loadUser(username);
        return user != null ? user.getBalance() : 0;
    }
    
    /**
     * Deposit money to a user's account
     * @param username The username
     * @param amount The amount to deposit
     * @param source The source of the deposit
     * @return true if successful
     */
    public boolean deposit(String username, double amount, String source) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) return false;
        
        user.deposit(amount);
        FileManager.saveUser(user);
        logTransaction(username + ": Deposited " + amount + " from " + source);
        return true;
    }
    
    /**
     * Withdraw money from a user's account
     * @param username The username
     * @param amount The amount to withdraw
     * @param method The withdrawal method
     * @return true if successful
     */
    public boolean withdraw(String username, double amount, String method) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        FileManager.saveUser(user);
        logTransaction(username + ": Withdrew " + amount + " via " + method);
        return true;
    }
    
    /**
     * Send money between users
     * @param fromUsername Sender username
     * @param toUsername Recipient username
     * @param amount The amount to send
     * @return true if successful
     */
    public boolean sendMoney(String fromUsername, String toUsername, double amount) {
        UserAccount sender = FileManager.loadUser(fromUsername);
        UserAccount recipient = FileManager.loadUser(toUsername);
        
        if (sender == null || recipient == null || sender.getBalance() < amount) {
            return false;
        }
        
        sender.withdraw(amount);
        recipient.deposit(amount);
        FileManager.saveUser(sender);
        FileManager.saveUser(recipient);
        logTransaction(fromUsername + " sent " + amount + " to " + toUsername);
        return true;
    }
    
    /**
     * Redeem a voucher
     * @param username The username
     * @param voucherId The voucher ID
     * @return true if successful
     */
    public boolean redeemVoucher(String username, String voucherId) {
        // Implement voucher redemption logic
        return true;
    }
    
    /**
     * Get fixed merchants
     * @return List of fixed merchants
     */
    public List<String> getFixedMerchants() {
        // Prefer merchants discovered from transaction logs, otherwise fall back to defaults
        List<String> merchants = FileManager.getMerchants();
        if (merchants == null || merchants.isEmpty()) {
            return List.of("Restaurant A", "Cafe B", "Shop C");
        }
        return merchants;
    }
    
    /**
     * Pay online to a merchant
     * @param username The username
     * @param merchant The merchant name
     * @param amount The payment amount
     * @return true if successful
     */
    public boolean payOnline(String username, String merchant, double amount) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        user.addTotalTransacted(amount);
        FileManager.saveUser(user);
        logTransaction(username + " paid " + amount + " to " + merchant);
        return true;
    }
    
    /**
     * Redeem points
     * @param username The username
     * @param points The number of points to redeem
     * @return true if successful
     */
    public boolean redeemPoints(String username, int points) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getPoints() < points) return false;
        
        double value = points * 0.1;
        user.redeemPoints(points, value);
        FileManager.saveUser(user);
        logTransaction(username + " redeemed " + points + " points");
        return true;
    }
    
    /**
     * Process bills payment
     * @param username The username
     * @param biller The biller name
     * @param referenceId The reference ID
     * @param amount The payment amount
     * @return true if successful
     */
    public boolean billsPayment(String username, String biller, String referenceId, double amount) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        FileManager.saveUser(user);
        logTransaction(username + " paid bill to " + biller + ": " + amount);
        return true;
    }
    
    /**
     * Buy load/credit
     * @param username The username
     * @param network The network provider
     * @param mobileNumber The mobile number
     * @param amount The load amount
     * @return true if successful
     */
    public boolean buyLoad(String username, String network, String mobileNumber, double amount) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        FileManager.saveUser(user);
        logTransaction(username + " bought " + amount + " load from " + network);
        return true;
    }
    
    /**
     * Change user PIN
     * @param username The username
     * @param currentPin The current PIN
     * @param newPin The new PIN
     * @return true if successful
     */
    public boolean changePin(String username, String currentPin, String newPin) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) return false;
        
        if (user.changePin(currentPin, newPin)) {
            FileManager.saveUser(user);
            return true;
        }
        return false;
    }
    
    /**
     * Get the scheduler
     * @return BackgroundScheduler instance
     */
    public BackgroundScheduler getScheduler() {
        return scheduler;
    }
    
    /**
     * Shutdown the application
     */
    public void shutdown() {
        // Cleanup resources
        if (scheduler != null) {
            scheduler.stop();
        }
    }
}
