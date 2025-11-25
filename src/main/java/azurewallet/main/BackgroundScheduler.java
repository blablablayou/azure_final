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
    
    public void start() {
        if (!running) {
            running = true;
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    applyMonthlyInterest();
                }
            }, 0, 86400000);
        }
    }
    
    public void stop() {
        running = false;
        if (timer != null) {
            timer.cancel();
        }
    }
    
    private void applyMonthlyInterest() {
        List<UserAccount> users = FileManager.loadAllUsers();
        for (UserAccount user : users) {
            user.applyMonthlyInterest();
            FileManager.saveUser(user);
        }
    }
    
    public boolean isRunning() {
        return running;
    }
}
