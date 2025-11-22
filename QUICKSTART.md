# Quick Start Guide - Azure Digital Wallet

## Step 1: Verify Project Structure
```
azure_final/
├── pom.xml ✓
├── README.md ✓
├── .gitignore ✓
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/azurewallet/
    │   │       ├── gui/ ✓ (JavaFX Controllers)
    │   │       ├── models/ ✓ (Data Models)
    │   │       ├── system/ ✓ (File & Admin)
    │   │       └── main/ ✓ (Scheduler)
    │   └── resources/
    │       └── data/ ✓ (Data Storage)
    └── test/
        └── java/
```

## Step 2: Prerequisites

Ensure you have:
- **Java 17+** installed
- **Maven 3.8+** installed
- **Terminal/Command Prompt** available

Check versions:
```bash
java -version
mvn -v
```

## Step 3: Build the Project

Navigate to project directory:
```bash
cd /Users/Macbook/Desktop/azure_final
```

Build using Maven:
```bash
mvn clean package -DskipTests
```

This will:
- Download dependencies
- Compile Java code
- Create JAR files in `target/` directory

## Step 4: Run the Application

### Option A: Using Maven
```bash
mvn javafx:run
```

### Option B: Using Java directly
```bash
java --module-path $(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)/org/openjfx/javafx-controls/21.0.2 --add-modules javafx.controls,javafx.fxml -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) com.azurewallet.gui.AzureWalletApp
```

### Option C: Using compiled JAR
```bash
java -jar target/azure-wallet-app.jar
```

## Step 5: First Use

### Register New Account:
1. Click "Don't have an account? Register"
2. Enter:
   - Username (lowercase)
   - Mobile number (09xxxxxxxxx format)
   - 4-digit PIN
3. Click "CREATE ACCOUNT"

### Login:
1. Enter username
2. Enter PIN (4 digits)
3. Click "LOGIN"

### Test Features:
- View Dashboard
- Deposit test amount
- Check transaction history
- Toggle Dark/Light mode
- Redeem points/vouchers

## Step 6: Admin Access

While logged in as any user:
1. Click menu option for "Admin"
2. Enter password: `admin123`
3. Access admin features:
   - View all users
   - Generate vouchers
   - Check system revenue
   - View activity logs

## Common Issues & Solutions

### Issue: Maven not found
**Solution:** Add Maven to PATH or use full path

### Issue: Java version mismatch
**Solution:** Ensure Java 17+ is your default
```bash
java -version
```

### Issue: Dependencies not downloading
**Solution:** Check internet connection and run:
```bash
mvn clean install
```

### Issue: JavaFX module not found
**Solution:** Ensure pom.xml has correct JavaFX dependency version

### Issue: Data directory doesn't exist
**Solution:** Will be created automatically on first run

## Project Statistics

- **Total Java Files:** 10
- **GUI Views:** 4 (Login, Register, Dashboard)
- **Features:** 10+ wallet operations
- **Themes:** 2 (Dark/Light)
- **Security:** SHA-256 hashing
- **Database:** File-based (TXT files)

## File Descriptions

| File | Purpose |
|------|---------|
| `AzureWalletApp.java` | Main application, theme management |
| `LoginViewController.java` | Login screen UI |
| `RegisterViewController.java` | Registration screen UI |
| `DashboardController.java` | Main wallet operations UI |
| `UserAccount.java` | User data model |
| `VoucherSystem.java` | Voucher generation/redemption |
| `FileManager.java` | Data persistence |
| `AdminControl.java` | Admin panel operations |
| `BackgroundScheduler.java` | Scheduled tasks |
| `HashUtil.java` | Password hashing utility |

## Dark/Light Mode

Click the theme toggle button in header:
- **Dark Mode:** `🌙 Dark` button (light text on dark background)
- **Light Mode:** `☀️ Light` button (dark text on white background)

Theme persists during session, resets on app restart.

## Testing Checklist

- [ ] Build successfully
- [ ] App launches without errors
- [ ] Can register new user
- [ ] Can login with created account
- [ ] Dashboard displays correctly
- [ ] Dark mode toggles
- [ ] Deposit operation works
- [ ] Withdraw operation works
- [ ] Send money to another user
- [ ] Transaction history shows entries
- [ ] Admin panel accessible
- [ ] Can generate vouchers

## Maintenance

### Clear All Data
Delete files in `src/main/resources/data/`:
```bash
rm src/main/resources/data/*.txt
```

### View Logs
```bash
cat src/main/resources/data/transactions.txt
cat src/main/resources/data/admin_log.txt
```

### Clean Build
```bash
mvn clean
```

## Support

For issues or questions:
1. Check README.md
2. Review error messages
3. Ensure all dependencies installed
4. Verify Java/Maven versions

## Next Steps

1. ✅ Build the project
2. ✅ Run the application
3. ✅ Test all features
4. ✅ Explore admin panel
5. ✅ Customize colors/themes
6. ✅ Deploy or share

---

**Happy Banking! 🏦💰**

Version 1.0.0 | Azure Digital Wallet
