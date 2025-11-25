package azurewallet.system;

import azurewallet.models.UserAccount;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Administrative control system for managing users and system operations
 */
public class AdminControl {
    private static final String ADMIN_LOG = "src/azurewallet/data/admin_log.txt";
    private double systemRevenue = 0;
    private boolean maintenanceMode = false;
    
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
    
    public List<UserAccount> getUsers() {
        return FileManager.loadAllUsers();
    }
    
    public List<UserAccount> getAllUsers() {
        return getUsers();
    }
    
    public UserAccount getUser(String username) {
        return FileManager.loadUser(username);
    }
    
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
    
    public boolean authenticateAdmin(String adminPin) {
        return "0000".equals(adminPin) || "1234".equals(adminPin);
    }
    
    public double getSystemRevenue() {
        return systemRevenue;
    }
    
    public void addSystemRevenue(double amount) {
        systemRevenue += amount;
        logAdminAction("REVENUE_ADDED", "Amount: " + amount);
    }
    
    public void clearSystemRevenue() {
        systemRevenue = 0;
        logAdminAction("REVENUE_CLEARED", "System revenue reset to 0");
    }
    
    public void triggerScheduler() {
        logAdminAction("SCHEDULER_TRIGGERED", "Background scheduler activated");
    }
    
    public void performMaintenanceClean() {
        maintenanceMode = true;
        logAdminAction("MAINTENANCE_START", "System maintenance started");
    }
    
    public void endMaintenance() {
        maintenanceMode = false;
        logAdminAction("MAINTENANCE_END", "System maintenance ended");
    }
    
    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }
    
    public void clearAdminLogs() {
        try {
            Files.write(Paths.get(ADMIN_LOG), "".getBytes());
            logAdminAction("LOGS_CLEARED", "Admin logs cleared");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void clearVouchersData() {
        try {
            Files.write(Paths.get("src/azurewallet/data/vouchers.txt"), "".getBytes());
            logAdminAction("VOUCHERS_CLEARED", "All vouchers cleared");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void generateMonthlyVouchers() {
        List<UserAccount> users = FileManager.loadAllUsers();
        for (UserAccount user : users) {
            logAdminAction("VOUCHER_GENERATED", "Monthly voucher for " + user.getUsername());
        }
    }
    
    public void generateHolidayVouchers() {
        List<UserAccount> users = FileManager.loadAllUsers();
        for (UserAccount user : users) {
            logAdminAction("HOLIDAY_VOUCHER", "Holiday voucher for " + user.getUsername());
        }
    }
    
    public void generateSingleVouchers() {
        logAdminAction("SINGLE_VOUCHER_GENERATED", "Single vouchers generated");
    }
    
    public List<String> getAdminLog() {
        try {
            return Files.readAllLines(Paths.get(ADMIN_LOG));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
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
