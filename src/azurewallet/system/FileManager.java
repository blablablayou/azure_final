package azurewallet.system;

import java.io.*;
import java.util.*;
import azurewallet.models.UserAccount;
import java.text.DecimalFormat;

public class FileManager {

    private static final String DATA_DIR = System.getProperty("user.dir") + "/src/azurewallet/data/";
    private static final String USERS_FILE = DATA_DIR + "users.txt";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "transactions.txt";
    private static final String VOUCHERS_FILE = DATA_DIR + "vouchers.txt";
    private static final String VOUCHER_LOG_FILE = DATA_DIR + "voucher_log.txt";
    private static final String POINTS_LOG_FILE = DATA_DIR + "points_log.txt";
    private static final String INTEREST_LOG_FILE = DATA_DIR + "interest_log.txt";
    private static final String SYSTEM_REVENUE_FILE = DATA_DIR + "system_revenue.txt";
    private static final String SCHEDULER_FILE = DATA_DIR + "scheduler_log.txt";
    private static final String MAINTENANCE_FILE = DATA_DIR + "maintenance.flag";

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public FileManager() {
        try {
            File folder = new File(DATA_DIR);
            if (!folder.exists()) folder.mkdirs();

            new File(USERS_FILE).createNewFile();
            new File(TRANSACTIONS_FILE).createNewFile();
            new File(VOUCHERS_FILE).createNewFile();
            new File(VOUCHER_LOG_FILE).createNewFile();
            new File(POINTS_LOG_FILE).createNewFile();
            new File(INTEREST_LOG_FILE).createNewFile();
            new File(SYSTEM_REVENUE_FILE).createNewFile();
            new File(SCHEDULER_FILE).createNewFile();
            new File(MAINTENANCE_FILE).createNewFile();

            System.out.println("+----------------------------------------------------------+");
            System.out.println("| Data directory initialized: " + DATA_DIR);
            System.out.println("+----------------------------------------------------------+");
        } catch (IOException e) {
            System.out.println("| Error initializing data files: " + e.getMessage());
        }
    }

    // ====================== MAINTENANCE HELPERS ======================

    /**
     * Set maintenance mode flag (persisted as a small file). If enabled, login screen
     * and other GUI components can check this and display a maintenance notification.
     */
    public void setMaintenanceMode(boolean enabled) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MAINTENANCE_FILE))) {
            pw.print(enabled ? "1" : "0");
        } catch (IOException e) {
            System.out.println("| Error setting maintenance flag.                        |");
        }
    }

    /**
     * Read whether maintenance mode is enabled.
     */
    public boolean isMaintenanceMode() {
        try (BufferedReader br = new BufferedReader(new FileReader(MAINTENANCE_FILE))) {
            String val = br.readLine();
            return val != null && (val.trim().equals("1") || val.trim().equalsIgnoreCase("true"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Clear vouchers file (used for maintenance clean).
     */
    public void clearVouchers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHERS_FILE))) {
            pw.print("");
        } catch (IOException e) {
            System.out.println("| Error clearing vouchers file.                          |");
        }
    }

    // ====================== USER MANAGEMENT ======================

    public Map<String, UserAccount> loadUsers() {
        Map<String, UserAccount> users = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 9) {
                    UserAccount acc = new UserAccount(
                        p[0], p[1], p[2],
                        Double.parseDouble(p[3]),
                        Integer.parseInt(p[4]),
                        Double.parseDouble(p[5]),
                        p[6],
                        Integer.parseInt(p[7]),
                        Long.parseLong(p[8])
                    );
                    users.put(p[0], acc);
                }
            }
        } catch (IOException e) {
            System.out.println("| Error loading users.                                    |");
        }
        return users;
    }

    public void saveUsers(Map<String, UserAccount> users) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (UserAccount u : users.values()) {
                pw.println(u.toFileFormat());
            }
        } catch (IOException e) {
            System.out.println("| Error saving users.                                     |");
        }
    }

    // ====================== TRANSACTION LOGS ======================

    public void logTransaction(String username, String type, double amount) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + ": " + type + " - PHP " + df.format(amount));
        } catch (IOException e) {
            System.out.println("| Error logging transaction.                              |");
        }
    }

    public void logPoints(String username, String action, int points, String note) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(POINTS_LOG_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + " " + action + " " + points + " points (" + note + ")");
        } catch (IOException e) {
            System.out.println("| Error logging points.                                   |");
        }
    }

    public void logInterest(String username, double amount) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(INTEREST_LOG_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + ": +PHP " + df.format(amount));
        } catch (IOException e) {
            System.out.println("| Error logging interest.                                 |");
        }
    }

    public void logVoucher(String username, String code, double value) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHER_LOG_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + " redeemed " + code + " (PHP " + df.format(value) + ")");
        } catch (IOException e) {
            System.out.println("| Error logging voucher redemption.                       |");
        }
    }

    public void logSystemRevenue(double fee) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SYSTEM_REVENUE_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - +PHP " + df.format(fee));
        } catch (IOException e) {
            System.out.println("| Error logging system revenue.                           |");
        }
    }

    // ====================== DATA READING ======================

    public double readSystemRevenue() {
        double total = 0.0;
        try (BufferedReader br = new BufferedReader(new FileReader(SYSTEM_REVENUE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("PHP")) {
                    String amt = line.substring(line.indexOf("PHP") + 4).replace(",", "");
                    total += Double.parseDouble(amt);
                }
            }
        } catch (Exception ignored) {}
        return total;
    }
    
    public int getTotalUsersCount() {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            while (br.readLine() != null) count++;
        } catch (IOException e) {}
        return count;
    }

    public int getTotalVouchersCount() {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            while (br.readLine() != null) count++;
        } catch (IOException e) {}
        return count;
    }

    // ====================== DISPLAY HELPERS ======================

    public void showTransactions(String username) {
        System.out.println("+==========================================================+");
        System.out.println("|                    TRANSACTION HISTORY                   |");
        System.out.println("+==========================================================+");
        try (BufferedReader br = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(username)) System.out.println("| " + line);
            }
        } catch (IOException e) {
            System.out.println("| Error reading transactions.                             |");
        }
        System.out.println("+==========================================================+");
    }

    /**
     * Return transaction history lines for a user as a List<String> (for GUI display)
     */
    public List<String> getTransactionHistory(String username) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(username)) lines.add(line);
            }
        } catch (IOException e) {
            // ignore and return what we have
        }
        return lines;
    }

    /**
     * Remove the most recent `count` transaction entries for a specific user.
     * This reads the transactions file, removes up to `count` lines that
     * contain the username (starting from the end/newest), and writes the
     * remaining lines back to the file.
     */
    public void removeRecentTransactions(String username, int count) {
        try {
            List<String> all = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
                String line;
                while ((line = br.readLine()) != null) all.add(line);
            }

            // collect indices of lines that belong to the user
            List<Integer> userIdx = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).contains(username)) userIdx.add(i);
            }

            // determine which indices to remove (last `count` entries)
            Set<Integer> toRemove = new HashSet<>();
            for (int r = 0; r < count && !userIdx.isEmpty(); r++) {
                int idx = userIdx.remove(userIdx.size() - 1);
                toRemove.add(idx);
            }

            // write back lines except those toRemove
            try (PrintWriter pw = new PrintWriter(new FileWriter(TRANSACTIONS_FILE))) {
                for (int i = 0; i < all.size(); i++) {
                    if (!toRemove.contains(i)) pw.println(all.get(i));
                }
            }
        } catch (IOException e) {
            System.out.println("| Error removing recent transactions: " + e.getMessage());
        }
    }

    public void showUserVouchers(String username) {
        System.out.println("+==========================================================+");
        System.out.println("|                       MY VOUCHERS                        |");
        System.out.println("+==========================================================+");
        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(username + ",")) System.out.println("| " + line);
            }
        } catch (IOException e) {
            System.out.println("| Error reading vouchers.                                 |");
        }
        System.out.println("+==========================================================+");
    }

    /**
     * Return voucher history lines for a user as a List<String> (for GUI display)
     */
    public List<String> getVoucherHistory(String username) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(username + ",") || line.contains(username + ",")) lines.add(line);
            }
        } catch (IOException e) {
            // ignore and return what we have
        }
        return lines;
    }

    public int countUserVouchers(String username) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(username + ",")) count++;
            }
        } catch (IOException e) {}
        return count;
    }
    // ====================== SCHEDULER LOGS ======================

    public void logSchedulerRun() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SCHEDULER_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - Scheduler executed");
        } catch (IOException e) {
            System.out.println("| Error logging scheduler.                                |");
        }
    }

    public String readLastSchedulerRun() {
        String last = "N/A";
        try (BufferedReader br = new BufferedReader(new FileReader(SCHEDULER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) last = line;
        } catch (IOException e) {}
        return last;
    }

    /**
     * Clear all system revenue records.
     */
    public void clearSystemRevenue() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SYSTEM_REVENUE_FILE))) {
            pw.print("");
        } catch (IOException e) {
            System.out.println("| Error clearing system revenue.                          |");
        }
    }
    
    public void clearTransactionHistory(String username) {
        try {
            List<String> all = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
                String line;
                while ((line = br.readLine()) != null) all.add(line);
            }

            // Remove all transactions for the user
            try (PrintWriter pw = new PrintWriter(new FileWriter(TRANSACTIONS_FILE))) {
                for (String line : all) {
                    if (!line.contains(username + ":")) {
                        pw.println(line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("| Error clearing transaction history: " + e.getMessage());
        }
    }
}
