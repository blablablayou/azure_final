package azurewallet.test;

import azurewallet.main.AzureDigitalApp;
import azurewallet.models.UserAccount;
import java.util.Random;
import java.util.List;

public class FunctionalTester {
    public static void main(String[] args) {
        try {
            AzureDigitalApp app = new AzureDigitalApp();

            long seed = System.currentTimeMillis() % 10000;
            String user1 = "testuser1" + seed;
            String user2 = "testuser2" + (seed + 1);
            String mobile1 = "09" + (100000000 + new Random().nextInt(900000000));
            String mobile2 = "09" + (200000000 + new Random().nextInt(700000000));

            System.out.println("[TEST] Registering users: " + user1 + ", " + user2);
            boolean r1 = app.registerUser("Test", "User1", user1, "1234", mobile1);
            boolean r2 = app.registerUser("Test", "User2", user2, "1234", mobile2);
            System.out.println("  -> " + user1 + " registered: " + r1);
            System.out.println("  -> " + user2 + " registered: " + r2);

            System.out.println("[TEST] Depositing to " + user1 + " (5000)");
            boolean d1 = app.deposit(user1, 5000.0);
            System.out.println("  -> deposit success: " + d1 + ", balance=" + app.getBalance(user1));

            System.out.println("[TEST] Withdrawing from " + user1 + " (1000)");
            boolean w1 = app.withdraw(user1, 1000.0);
            System.out.println("  -> withdraw success: " + w1 + ", balance=" + app.getBalance(user1));

            System.out.println("[TEST] Sending 500 from " + user1 + " to " + user2);
            boolean s1 = app.sendMoney(user1, user2, 500.0);
            System.out.println("  -> send success: " + s1 + ", sender balance=" + app.getBalance(user1) + ", receiver balance=" + app.getBalance(user2));

            System.out.println("[TEST] Attempt redeem voucher (likely none) for " + user1);
            double rv = app.redeemVoucher(user1, "SOME_TEST_CODE");
            System.out.println("  -> redeem returned: " + rv + ", balance=" + app.getBalance(user1));

            System.out.println("[TEST] Transaction history for " + user1 + ":");
            List<String> txs = app.getFileManager().getTransactionHistory(user1);
            if (txs == null || txs.isEmpty()) {
                System.out.println("  -> (no transactions found)");
            } else {
                txs.forEach(line -> System.out.println("  " + line));
            }

            System.out.println("[TEST] Voucher history for " + user1 + ":");
            List<String> vs = app.getFileManager().getVoucherHistory(user1);
            if (vs == null || vs.isEmpty()) System.out.println("  -> (no vouchers found)");
            else vs.forEach(v -> System.out.println("  " + v));

            app.shutdown();
            System.out.println("[TEST] Completed functional tester.");
        } catch (Exception e) {
            System.err.println("[TEST] Exception during functional test:");
            e.printStackTrace();
            System.exit(2);
        }
    }
}
