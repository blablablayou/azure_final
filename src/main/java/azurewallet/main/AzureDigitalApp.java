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
    
    public boolean loginUser(String username, String pin) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) {
            return false;
        }
        
        return user.verifyPin(pin);
    }
    
    public UserAccount getUser(String username) {
        return FileManager.loadUser(username);
    }
    
    public List<UserAccount> getUsers() {
        return FileManager.loadAllUsers();
    }
    
    public UserAccount registerUser(String username, String pin, String mobile) {
        UserAccount user = new UserAccount(username, pin, mobile);
        FileManager.saveUser(user);
        return user;
    }
    
    public void updateUser(UserAccount user) {
        FileManager.saveUser(user);
    }
    
    public void deleteUser(String username) {
        FileManager.deleteUser(username);
    }
    
    public void logTransaction(String transactionData) {
        FileManager.logTransaction(transactionData);
    }
    
    public FileManager getFileManager() {
        return fileManager;
    }
    
    public double getBalance(String username) {
        UserAccount user = FileManager.loadUser(username);
        return user != null ? user.getBalance() : 0;
    }
    
    public boolean deposit(String username, double amount, String source) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) return false;
        
        user.deposit(amount);
        FileManager.saveUser(user);
        logTransaction(username + ": Deposited " + amount + " from " + source);
        return true;
    }
    
    public boolean withdraw(String username, double amount, String method) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        FileManager.saveUser(user);
        logTransaction(username + ": Withdrew " + amount + " via " + method);
        return true;
    }
    
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
    
    public boolean redeemVoucher(String username, String voucherId) {
        return true;
    }
    
    public List<String> getFixedMerchants() {
        return List.of("Restaurant A", "Cafe B", "Shop C");
    }
    
    public boolean payOnline(String username, String merchant, double amount) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        user.addTotalTransacted(amount);
        FileManager.saveUser(user);
        logTransaction(username + " paid " + amount + " to " + merchant);
        return true;
    }
    
    public boolean redeemPoints(String username, int points) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getPoints() < points) return false;
        
        double value = points * 0.1;
        user.redeemPoints(points, value);
        FileManager.saveUser(user);
        logTransaction(username + " redeemed " + points + " points");
        return true;
    }
    
    public boolean billsPayment(String username, String biller, String referenceId, double amount) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        FileManager.saveUser(user);
        logTransaction(username + " paid bill to " + biller + ": " + amount);
        return true;
    }
    
    public boolean buyLoad(String username, String network, String mobileNumber, double amount) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null || user.getBalance() < amount) return false;
        
        user.withdraw(amount);
        FileManager.saveUser(user);
        logTransaction(username + " bought " + amount + " load from " + network);
        return true;
    }
    
    public boolean changePin(String username, String currentPin, String newPin) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) return false;
        
        if (user.changePin(currentPin, newPin)) {
            FileManager.saveUser(user);
            return true;
        }
        return false;
    }
    
    public BackgroundScheduler getScheduler() {
        return scheduler;
    }
    
    public void shutdown() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }
}
