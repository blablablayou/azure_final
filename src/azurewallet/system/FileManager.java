package azurewallet.system;

import azurewallet.models.UserAccount;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages file I/O operations for the wallet system
 */
public class FileManager {
    private static final String DATA_DIR = "src/azurewallet/data/";
    private static final String USERS_FILE = DATA_DIR + "users.txt";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "transactions.txt";
    private static final String VOUCHERS_FILE = DATA_DIR + "vouchers.txt";
    
    static {
        // Ensure data directory exists
        new File(DATA_DIR).mkdirs();
    }
    
    /**
     * Save a user account to file
     * @param user The user account to save
     */
    public static void saveUser(UserAccount user) {
        try {
            List<String> lines = readAllLines(USERS_FILE);
            
            // Check if user exists and update, otherwise add
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(user.getUsername() + ",")) {
                    lines.set(i, user.toFileFormat());
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                lines.add(user.toFileFormat());
            }
            
            writeToFile(USERS_FILE, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Load all users from file
     * @return List of user accounts
     */
    public static List<UserAccount> loadAllUsers() {
        List<UserAccount> users = new ArrayList<>();
        try {
            List<String> lines = readAllLines(USERS_FILE);
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    UserAccount user = parseUserLine(line);
                    if (user != null) {
                        users.add(user);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    /**
     * Load a specific user by username
     * @param username The username to load
     * @return The user account or null if not found
     */
    public static UserAccount loadUser(String username) {
        try {
            List<String> lines = readAllLines(USERS_FILE);
            for (String line : lines) {
                if (line.startsWith(username + ",")) {
                    return parseUserLine(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Delete a user from file
     * @param username The username to delete
     */
    public static void deleteUser(String username) {
        try {
            List<String> lines = readAllLines(USERS_FILE);
            lines.removeIf(line -> line.startsWith(username + ","));
            writeToFile(USERS_FILE, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Log a transaction
     * @param transactionData The transaction data to log
     */
    public static void logTransaction(String transactionData) {
        try {
            List<String> lines = readAllLines(TRANSACTIONS_FILE);
            lines.add(transactionData);
            writeToFile(TRANSACTIONS_FILE, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Count vouchers for a user
     * @param username The username
     * @return Number of vouchers
     */
    public int countUserVouchers(String username) {
        try {
            List<String> lines = readAllLines(VOUCHERS_FILE);
            int count = 0;
            for (String line : lines) {
                if (line.startsWith(username + ",")) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Show all vouchers for a user
     * @param username The username
     */
    public void showUserVouchers(String username) {
        try {
            List<String> lines = readAllLines(VOUCHERS_FILE);
            for (String line : lines) {
                if (line.startsWith(username + ",")) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Save multiple users
     * @param users List of users to save
     */
    public static void saveUsers(List<UserAccount> users) {
        for (UserAccount user : users) {
            saveUser(user);
        }
    }
    
    /**
     * Clear transaction history for a user
     * @param username The username
     */
    public static void clearTransactionHistory(String username) {
        try {
            List<String> lines = readAllLines(TRANSACTIONS_FILE);
            lines.removeIf(line -> line.contains(username));
            writeToFile(TRANSACTIONS_FILE, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get transaction history for a user
     * @param username The username
     * @return List of transactions
     */
    public static List<String> getTransactionHistory(String username) {
        try {
            List<String> lines = readAllLines(TRANSACTIONS_FILE);
            List<String> userTransactions = new ArrayList<>();
            for (String line : lines) {
                if (line.contains(username)) {
                    userTransactions.add(line);
                }
            }
            return userTransactions;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Remove recent transactions for a user
     * @param username The username
     * @param count Number of recent transactions to remove
     */
    public static void removeRecentTransactions(String username, int count) {
        try {
            List<String> lines = readAllLines(TRANSACTIONS_FILE);
            int removed = 0;
            for (int i = lines.size() - 1; i >= 0 && removed < count; i--) {
                if (lines.get(i).contains(username)) {
                    lines.remove(i);
                    removed++;
                }
            }
            writeToFile(TRANSACTIONS_FILE, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get voucher history for a user
     * @param username The username
     * @return List of voucher transactions
     */
    public static List<String> getVoucherHistory(String username) {
        try {
            List<String> lines = readAllLines(VOUCHERS_FILE);
            List<String> voucherHistory = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith(username)) {
                    voucherHistory.add(line);
                }
            }
            return voucherHistory;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Read last scheduler run timestamp
     * @return The last run timestamp
     */
    public static String readLastSchedulerRun() {
        try {
            List<String> lines = readAllLines("src/azurewallet/data/scheduler_log.txt");
            return lines.isEmpty() ? "Never" : lines.get(lines.size() - 1);
        } catch (IOException e) {
            return "Never";
        }
    }
    
    // Helper methods
    
    private static List<String> readAllLines(String filename) throws IOException {
        if (!Files.exists(Paths.get(filename))) {
            return new ArrayList<>();
        }
        return Files.readAllLines(Paths.get(filename));
    }
    
    private static void writeToFile(String filename, List<String> lines) throws IOException {
        Files.write(Paths.get(filename), String.join("\n", lines).getBytes());
    }
    
    private static UserAccount parseUserLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 7) {
            return null;
        }
        
        try {
            String username = parts[0];
            String pinHash = parts[1];
            String mobile = parts[2];
            double balance = Double.parseDouble(parts[3]);
            int points = Integer.parseInt(parts[4]);
            double totalTransacted = Double.parseDouble(parts[5]);
            String rank = parts[6];
            
            UserAccount user = new UserAccount(username, pinHash, mobile, balance, points, totalTransacted, rank, 0, 0);
            
            // Restore CVV and expiry date if present in the file
            if (parts.length > 9 && !parts[9].isEmpty()) {
                user.setCardCVV(parts[9]);
            }
            if (parts.length > 10 && !parts[10].isEmpty()) {
                user.setCardExpiryDate(parts[10]);
            }
            // Restore virtual bank number (account number) if present in the file
            if (parts.length > 11 && !parts[11].isEmpty()) {
                user.setVirtualBankNumber(parts[11]);
            }
            
            return user;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract unique merchant names from the transactions log.
     * Looks for patterns like "paid <amount> to <merchant>" and returns a sorted list.
     * @return List of merchant names (may be empty)
     */
    public static List<String> getMerchants() {
        try {
            List<String> lines = readAllLines(TRANSACTIONS_FILE);
            Set<String> merchants = new LinkedHashSet<>();
            for (String line : lines) {
                if (line == null || line.isEmpty()) continue;
                // Attempt to parse "... paid <amount> to <merchant>" patterns
                int idx = line.indexOf(" to ");
                if (idx >= 0 && idx + 4 < line.length()) {
                    String tail = line.substring(idx + 4).trim();
                    // strip trailing punctuation
                    tail = tail.replaceAll("[.,;]$", "");
                    if (!tail.isEmpty()) {
                        merchants.add(tail);
                    }
                }
            }
            return new ArrayList<>(merchants);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
