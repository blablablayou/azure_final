package azurewallet.main;

import azurewallet.models.UserAccount;
import azurewallet.system.FileManager;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Background scheduler for regular tasks like interest calculation
 */
public class BackgroundScheduler {
    private Timer timer;
    private boolean running = false;
    
    public BackgroundScheduler() {
        this.timer = new Timer();
    }
    
    /**
     * Start the scheduler
     */
    public void start() {
        if (!running) {
            running = true;
            // Schedule daily tasks
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    applyMonthlyInterest();
                }
            }, 0, 86400000); // Every 24 hours
        }
    }
    
    /**
     * Stop the scheduler
     */
    public void stop() {
        running = false;
        if (timer != null) {
            timer.cancel();
        }
    }
    
    /**
     * Apply monthly interest to all accounts
     */
    private void applyMonthlyInterest() {
        List<UserAccount> users = FileManager.loadAllUsers();
        for (UserAccount user : users) {
            user.applyMonthlyInterest();
            FileManager.saveUser(user);
        }
    }
    
    /**
     * Check if scheduler is running
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
}
