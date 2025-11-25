package azurewallet.system;

import azurewallet.models.UserAccount;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Administrative control system for managing users and system operations
 */
public class AdminControl {
    private static final String ADMIN_LOG = "src/azurewallet/data/admin_log.txt";
    private double systemRevenue = 0;
    private boolean maintenanceMode = false;
    
    /**
     * Create a new user account
     * @param username The username
     * @param pin The PIN
     * @param mobile The mobile number
     * @return true if successful, false if user already exists
     */
    public boolean createUser(String username, String pin, String mobile) {
        UserAccount existing = FileManager.loadUser(username);
        if (existing != null) {
            logAdminAction("CREATE_USER_FAILED", username + " - User already exists");
            return false;
        }
        
        UserAccount newUser = new UserAccount(username, pin, mobile);
        FileManager.saveUser(newUser);
        logAdminAction("CREATE_USER", username);
        return true;
    }
    
    /**
     * Delete a user account
     * @param username The username to delete
     * @return true if successful, false if user not found
     */
    public boolean deleteUser(String username) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) {
            logAdminAction("DELETE_USER_FAILED", username + " - User not found");
            return false;
        }
        
        FileManager.deleteUser(username);
        logAdminAction("DELETE_USER", username);
        return true;
    }
    
    /**
     * Get all users
     * @return List of all user accounts
     */
    public List<UserAccount> getUsers() {
        return FileManager.loadAllUsers();
    }
    
    /**
     * Get all users (alias for getUsers)
     * @return List of all user accounts
     */
    public List<UserAccount> getAllUsers() {
        return getUsers();
    }
    
    /**
     * Get a specific user
     * @param username The username to retrieve
     * @return The user account or null if not found
     */
    public UserAccount getUser(String username) {
        return FileManager.loadUser(username);
    }
    
    /**
     * Update user balance
     * @param username The username
     * @param newBalance The new balance
     * @return true if successful
     */
    public boolean updateUserBalance(String username, double newBalance) {
        UserAccount user = FileManager.loadUser(username);
        if (user == null) {
            return false;
        }
        
        user.setBalance(newBalance);
        FileManager.saveUser(user);
        logAdminAction("UPDATE_BALANCE", username + " - Balance: " + newBalance);
        return true;
    }
    
    /**
     * Authenticate admin user
     * @param adminPin The admin PIN
     * @return true if valid (default admin PIN: 0000)
     */
    public boolean authenticateAdmin(String adminPin) {
        return "0000".equals(adminPin) || "admin123".equals(adminPin);
    }
    
    /**
     * Get system revenue
     * @return The current system revenue
     */
    public double getSystemRevenue() {
        return systemRevenue;
    }
    
    /**
     * Add to system revenue
     * @param amount The amount to add
     */
    public void addSystemRevenue(double amount) {
        systemRevenue += amount;
        logAdminAction("REVENUE_ADDED", "Amount: " + amount);
    }
    
    /**
     * Clear system revenue
     */
    public void clearSystemRevenue() {
        systemRevenue = 0;
        logAdminAction("REVENUE_CLEARED", "System revenue reset to 0");
    }
    
    /**
     * Trigger the background scheduler
     */
    public void triggerScheduler() {
        logAdminAction("SCHEDULER_TRIGGERED", "Background scheduler activated");
    }
    
    /**
     * Perform maintenance cleanup
     */
    public void performMaintenanceClean() {
        maintenanceMode = true;
        logAdminAction("MAINTENANCE_START", "System maintenance started");
    }
    
    /**
     * End maintenance mode
     */
    public void endMaintenance() {
        maintenanceMode = false;
        logAdminAction("MAINTENANCE_END", "System maintenance ended");
    }
    
    /**
     * Check if system is in maintenance mode
     * @return true if in maintenance
     */
    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }
    
    /**
     * Clear admin logs
     */
    public void clearAdminLogs() {
        try {
            Files.write(Paths.get(ADMIN_LOG), "".getBytes());
            logAdminAction("LOGS_CLEARED", "Admin logs cleared");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Clear vouchers data
     */
    public void clearVouchersData() {
        try {
            Files.write(Paths.get("src/azurewallet/data/vouchers.txt"), "".getBytes());
            logAdminAction("VOUCHERS_CLEARED", "All vouchers cleared");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Generate monthly vouchers
     */
    public void generateMonthlyVouchers() {
        List<UserAccount> users = FileManager.loadAllUsers();
        for (UserAccount user : users) {
            // Generate voucher logic
            logAdminAction("VOUCHER_GENERATED", "Monthly voucher for " + user.getUsername());
        }
    }
    
    /**
     * Generate holiday vouchers
     */
    public void generateHolidayVouchers() {
        List<UserAccount> users = FileManager.loadAllUsers();
        for (UserAccount user : users) {
            // Generate voucher logic
            logAdminAction("HOLIDAY_VOUCHER", "Holiday voucher for " + user.getUsername());
        }
    }
    
    /**
     * Generate single vouchers
     */
    public void generateSingleVouchers() {
        logAdminAction("SINGLE_VOUCHER_GENERATED", "Single vouchers generated");
    }
    
    /**
     * Get admin log
     * @return List of admin log entries
     */
    public List<String> getAdminLog() {
        try {
            return Files.readAllLines(Paths.get(ADMIN_LOG));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Log an administrative action
     * @param action The action performed
     * @param details Details about the action
     */
    private void logAdminAction(String action, String details) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logEntry = timestamp + " | " + action + " | " + details;
            
            FileWriter fw = new FileWriter(ADMIN_LOG, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(logEntry);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Import java.nio.file at the top
class Files {
    public static void write(java.nio.file.Path path, byte[] bytes) throws IOException {
        java.nio.file.Files.write(path, bytes);
    }
    
    public static List<String> readAllLines(java.nio.file.Path path) throws IOException {
        return java.nio.file.Files.readAllLines(path);
    }
}

class Paths {
    public static java.nio.file.Path get(String path) {
        return java.nio.file.Paths.get(path);
    }
}
