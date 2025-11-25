package azurewallet.test;

import azurewallet.system.FileManager;
import azurewallet.system.AdminControl;
import azurewallet.main.BackgroundScheduler;
import java.util.Map;
import azurewallet.models.UserAccount;

public class GenerateSingleVoucherTest {
    public static void main(String[] args) {
        FileManager fm = new FileManager();
        Map<String, UserAccount> users = fm.loadUsers();
        BackgroundScheduler sched = new BackgroundScheduler(fm, users);
        AdminControl ac = new AdminControl(fm, users, sched);

        System.out.println("Generating single vouchers for all users (test)...");
        ac.generateSingleVouchers();
        System.out.println("Done. Check data/vouchers.txt for entries with expiry.");
    }
}
