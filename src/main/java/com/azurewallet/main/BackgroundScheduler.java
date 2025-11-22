package com.azurewallet.main;

import java.time.LocalDate;
import java.util.Map;
import com.azurewallet.models.UserAccount;
import com.azurewallet.models.VoucherSystem;
import com.azurewallet.system.FileManager;

public class BackgroundScheduler {
    private final FileManager fileManager;
    private final Map<String, UserAccount> users;
    private LocalDate lastRunDate;

    public BackgroundScheduler(FileManager fileManager, Map<String, UserAccount> users) {
        this.fileManager = fileManager;
        this.users = users;
        this.lastRunDate = LocalDate.now().minusDays(1);
    }

    public void runScheduler() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastRunDate)) {
            applyMonthlyInterest();
            VoucherSystem.generateMonthlyVouchers(users);
            fileManager.logSchedulerRun();
            lastRunDate = today;
        }
    }

    private void applyMonthlyInterest() {
        for (UserAccount acc : users.values()) {
            double before = acc.getBalance();
            acc.applyMonthlyInterest();
            double added = acc.getBalance() - before;
            if (added > 0) {
                fileManager.logInterest(acc.getUsername(), added);
            }
        }
        fileManager.saveUsers(users);
        System.out.println("Monthly interest applied to all users.");
    }
}
