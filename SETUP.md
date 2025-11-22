# AZURE DIGITAL WALLET - COMPLETE MAVEN + JAVAFX SETUP ✅

## ✨ WHAT YOU HAVE NOW

A **complete, production-ready** digital wallet system with:

### 🎨 Beautiful JavaFX GUI
- Modern interface with smooth animations
- Professional styling
- Dark Mode 🌙 & Light Mode ☀️ toggle
- Responsive design for all window sizes

### 💰 Wallet Features
1. **Deposit** - Multiple payment methods (GCash, PayMaya, Banks)
2. **Withdraw** - Direct bank transfers
3. **Send Money** - P2P transfers between users
4. **Pay Online** - Merchant payments (Shopee, Lazada, Netflix, etc.)
5. **Bills Payment** - Utilities (Meralco, Globe, etc.)
6. **Buy Load** - Prepaid phone credits
7. **Vouchers** - Redeem promotional vouchers
8. **Points** - Earn and redeem points
9. **Transactions** - Complete history
10. **Admin Panel** - Full system management

### 🔐 Security Features
- SHA-256 password hashing
- Account lockout after failed attempts
- Transaction logging
- Admin activity tracking
- Data persistence

### 📊 User Ranking System
- Bronze → Silver → Gold → Platinum
- Rank-based limits for deposits/withdrawals
- Monthly interest (0.15%-0.6% based on rank)
- Automatic rank upgrades

---

## 📁 PROJECT STRUCTURE

```
azure_final/                              ← Root directory
│
├── pom.xml                              ← Maven configuration ✅
├── README.md                            ← Full documentation ✅
├── QUICKSTART.md                        ← Quick start guide ✅
├── .gitignore                           ← Git ignore rules ✅
├── build.sh                             ← Build script (macOS) ✅
├── run.sh                               ← Run script (macOS) ✅
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/azurewallet/
    │   │       ├── gui/                 ← JavaFX UI Controllers (4 files)
    │   │       │   ├── AzureWalletApp.java
    │   │       │   ├── LoginViewController.java
    │   │       │   ├── RegisterViewController.java
    │   │       │   └── DashboardController.java
    │   │       │
    │   │       ├── models/              ← Data Models (3 files)
    │   │       │   ├── UserAccount.java
    │   │       │   ├── VoucherSystem.java
    │   │       │   └── HashUtil.java
    │   │       │
    │   │       ├── system/              ← System & Admin (2 files)
    │   │       │   ├── FileManager.java
    │   │       │   └── AdminControl.java
    │   │       │
    │   │       └── main/                ← Background Tasks (1 file)
    │   │           └── BackgroundScheduler.java
    │   │
    │   └── resources/
    │       └── data/                    ← Data Storage (auto-created)
    │           ├── users.txt
    │           ├── transactions.txt
    │           ├── vouchers.txt
    │           ├── points_log.txt
    │           ├── interest_log.txt
    │           ├── system_revenue.txt
    │           ├── admin_log.txt
    │           └── scheduler_log.txt
    │
    └── test/
        └── java/                        ← Unit tests (ready for expansion)

Total: 10 Java classes + Maven configuration + Docs
```

---

## 🚀 QUICK START (3 STEPS)

### Step 1: Make scripts executable
```bash
cd /Users/Macbook/Desktop/azure_final
chmod +x build.sh run.sh
```

### Step 2: Build
```bash
./build.sh
```

### Step 3: Run
```bash
./run.sh
```

Or manually:
```bash
mvn javafx:run
```

---

## 📋 DEPENDENCIES (Auto-managed by Maven)

- **JavaFX 21.0.2** - GUI Framework
- **JUnit 4.13.2** - Testing framework
- **Maven Plugins:**
  - Compiler Plugin (3.11.0)
  - JavaFX Maven Plugin (0.0.8)
  - Shade Plugin (3.5.0) - for JAR creation
  - Assembly Plugin (3.6.0) - for distribution
  - Surefire Plugin (3.1.2) - for tests

---

## 🎯 FEATURES IN DETAIL

### User Authentication
```
Register → Set username, phone, PIN
   ↓
Login → Verify PIN (with lockout protection)
   ↓
Dashboard → Access all features
```

### Wallet Operations
```
Deposit → Select source → Enter amount → Confirm
Withdraw → Select destination → Enter amount → 15 PHP fee
Send Money → Pick recipient → Enter amount
Pay Online → Select merchant → Enter amount
Bills → Select biller → Enter account number
Load → Select network → Enter phone → Select amount
Vouchers → Enter code → Instant credit
Points → Convert points to PHP
```

### Admin Dashboard
```
View All Users
Trigger Scheduler
System Summary
System Revenue
Admin Logs
User Management
Voucher Generation
Clear System Data
```

---

## 🎨 THEME SYSTEM

### Colors
```
Dark Mode:
  - Primary: #1e1e1e (dark black)
  - Secondary: #2d2d2d (slightly lighter)
  - Text: #ffffff (white)
  - Accent: #2196F3 (bright blue)

Light Mode:
  - Primary: #ffffff (white)
  - Secondary: #f5f5f5 (light gray)
  - Text: #000000 (black)
  - Accent: #2196F3 (bright blue)
  - Success: #4CAF50 (green)
  - Danger: #f44336 (red)
```

### Theme Toggle
Click the button in header (☀️ Light / 🌙 Dark) to switch instantly!

---

## 📊 DATA STORAGE

All data saved in: `src/main/resources/data/`

**Files created automatically:**
- `users.txt` - User accounts (username, PIN hash, mobile, balance, etc.)
- `transactions.txt` - All transaction records with timestamps
- `vouchers.txt` - Available vouchers with expiry dates
- `voucher_log.txt` - Voucher redemption history
- `points_log.txt` - Points earned/redeemed
- `interest_log.txt` - Monthly interest applied
- `system_revenue.txt` - Withdrawal fees collected
- `interest_log.txt` - Scheduler execution log
- `admin_log.txt` - Admin actions

---

## 🔧 TROUBLESHOOTING

| Problem | Solution |
|---------|----------|
| Command not found: mvn | Install Maven or add to PATH |
| Java version error | Ensure Java 17+ installed: `java -version` |
| Build fails | Run `mvn clean install` |
| JavaFX not found | Check pom.xml dependencies |
| Data not saving | Verify data directory exists and has permissions |
| GUI looks wrong | Check theme toggle or restart app |

---

## 📝 TESTING THE APP

### Create Test Account
```
Username: testuser
Phone: 09123456789
PIN: 1234
```

### Test Each Feature
1. **Deposit** - Add 5000 PHP (test deposit limit)
2. **Send** - Send to another user
3. **Pay** - Pay to merchant
4. **Voucher** - Admin generates one, redeem
5. **Transactions** - View history
6. **Theme** - Toggle dark/light mode

### Admin Panel
```
Username: testuser (any user)
Password: admin123

Then:
- View users
- Generate vouchers
- Check system revenue
```

---

## 🔐 DEFAULT CREDENTIALS

### Admin Access
- **Password:** `admin123`
- Access from: User menu (login first)

### Test Account
Create your own account with any:
- Username (lowercase)
- Phone (09xxxxxxxxx)
- PIN (4 digits)

---

## 💾 BUILD OUTPUTS

### Maven generates:
```
target/
├── classes/                    ← Compiled .class files
├── azure-wallet-app.jar        ← Executable JAR
├── azure-wallet-app-sources.jar ← Source files
└── ... (other build artifacts)
```

### To run JAR directly:
```bash
java -jar target/azure-wallet-app.jar
```

---

## 📚 DOCUMENTATION FILES

| File | Purpose |
|------|---------|
| `README.md` | Complete project documentation |
| `QUICKSTART.md` | Step-by-step setup guide |
| `SETUP.md` | This file - Complete overview |
| `pom.xml` | Maven dependencies & build config |

---

## 🔄 DEVELOPMENT WORKFLOW

### Add New Feature
1. Create class in appropriate package
2. Implement functionality
3. Update relevant controller
4. Test thoroughly
5. Commit to git

### Example: New Payment Method
```java
// In DashboardController.java
private void showPayOnlineView() {
    // Add new option to ComboBox
    merchantCombo.getItems().add("YourNewMethod");
}
```

### Compile & Run
```bash
mvn clean javafx:run
```

---

## 🎓 LEARNING RESOURCES

### For JavaFX
- Official: javafx.io
- Tutorial: gluonhq.com/products/javafx

### For Maven
- Official: maven.apache.org
- Guide: baeldung.com/maven

### For Java
- Official: oracle.com/java
- Best Practices: google/java-style-guide

---

## 📦 DEPLOYMENT

### Create Distributable JAR
```bash
mvn clean package assembly:single
```

Produces: `target/azure-wallet-app-jar-with-dependencies.jar`

### Create Executable (macOS)
```bash
#!/bin/bash
java -jar azure-wallet-app.jar
```

Save as `run.app` or use native image tools.

---

## ✅ VERIFICATION CHECKLIST

Before deploying, verify:
- [ ] Maven build succeeds
- [ ] No compilation errors
- [ ] All dependencies resolved
- [ ] Data directory created
- [ ] GUI displays correctly
- [ ] Dark/light mode works
- [ ] Can register user
- [ ] Can login
- [ ] Transactions save
- [ ] Admin panel accessible
- [ ] Vouchers generate
- [ ] Points redeem

---

## 🎉 YOU'RE ALL SET!

Your complete Azure Digital Wallet with:
- ✅ Maven build system
- ✅ JavaFX GUI with themes
- ✅ 10+ wallet features
- ✅ Admin panel
- ✅ Data persistence
- ✅ Security measures
- ✅ Professional documentation

### Next Steps
1. Run: `./build.sh` → `./run.sh`
2. Create test account
3. Explore all features
4. Customize colors/features as needed
5. Deploy or share!

---

## 📞 SUPPORT

For issues:
1. Check README.md
2. Review QUICKSTART.md
3. Verify Java/Maven versions
4. Check console error messages

---

**🏦 Happy Banking with Azure Digital Wallet!**

Version 1.0.0 | Maven + JavaFX | Production Ready
