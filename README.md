# Azure Digital Wallet - JavaFX GUI Application

A modern digital wallet system with a beautiful JavaFX GUI supporting both **dark and light themes**.

## Project Structure

```
azure-digital-wallet/
├── pom.xml                          # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/azurewallet/
│   │   │       ├── gui/             # JavaFX GUI Controllers
│   │   │       │   ├── AzureWalletApp.java
│   │   │       │   ├── LoginViewController.java
│   │   │       │   ├── RegisterViewController.java
│   │   │       │   └── DashboardController.java
│   │   │       ├── models/          # Data Models
│   │   │       │   ├── UserAccount.java
│   │   │       │   ├── VoucherSystem.java
│   │   │       │   └── HashUtil.java
│   │   │       ├── system/          # File & Admin Management
│   │   │       │   ├── FileManager.java
│   │   │       │   └── AdminControl.java
│   │   │       └── main/            # Main Application Logic
│   │   │           └── BackgroundScheduler.java
│   │   └── resources/
│   │       └── data/                # Data storage
│   └── test/
│       └── java/
└── target/                          # Compiled output
```

## Features

✅ **Modern JavaFX GUI**
- Clean, intuitive interface
- Responsive design
- Professional styling

✅ **Dark/Light Mode Toggle**
- Switch between themes instantly
- Persists across all screens
- Beautiful color schemes

✅ **User Authentication**
- Secure PIN-based login
- Account lockout after 3 failed attempts
- User registration with validation

✅ **Complete Wallet Features**
- 💰 Deposit (GCash, PayMaya, Bank Transfers)
- 💸 Withdraw with transaction fees
- 📤 Send Money to other users
- 🛍️ Pay Online (Shopee, Lazada, etc.)
- 💡 Bills Payment (Meralco, Globe, etc.)
- 📱 Buy Prepaid Load
- 🎟️ Redeem Vouchers
- ⭐ Redeem Points
- 📋 Transaction History

✅ **User Ranking System**
- Bronze → Silver → Gold → Platinum
- Rank-based transaction limits
- Monthly interest based on rank

✅ **Admin Control Panel**
- View all users and transactions
- Manual scheduler trigger
- System revenue tracking
- User management
- Voucher generation

✅ **Data Persistence**
- File-based database
- Transaction logging
- Interest tracking
- Admin activity logs

## Requirements

- **Java 17+**
- **Maven 3.8+**
- **JavaFX 21.0.2+**

## Installation & Setup

### 1. Clone or Download Project
```bash
cd /Users/Macbook/Desktop/azure_final
```

### 2. Build with Maven
```bash
mvn clean package
```

### 3. Run the Application
```bash
mvn javafx:run
```

Or run the compiled JAR:
```bash
java -cp target/azure-wallet-app.jar com.azurewallet.gui.AzureWalletApp
```

## Default Admin Credentials

- **Username:** (Any registered user account)
- **Admin Password:** `admin123`

## Initial Users (Create or Use Demo)

1. Register a new account with username and PIN
2. Deposit initial balance
3. Test all features

## Usage Guide

### User Operations
1. **Register** → Create account with username, phone, 4-digit PIN
2. **Login** → Enter credentials
3. **Dashboard** → View balance, points, rank, and statistics
4. **Deposit** → Add funds from various payment methods
5. **Withdraw** → Transfer to bank or e-wallet
6. **Send Money** → P2P transfers to other users
7. **Pay Online** → Purchase from merchants
8. **Redeem Vouchers/Points** → Earn rewards

### Admin Operations
1. Login as any user, then access admin panel
2. View system statistics
3. Generate vouchers
4. Manage users
5. Check transaction logs

## Data Files

All user data is stored in `src/main/resources/data/`:
- `users.txt` - User accounts
- `transactions.txt` - Transaction history
- `vouchers.txt` - Available vouchers
- `points_log.txt` - Points activity
- `interest_log.txt` - Interest applications
- `system_revenue.txt` - System fees collected
- `admin_log.txt` - Admin activity
- `scheduler_log.txt` - Scheduled events

## Theme Customization

To change theme colors, edit the color constants in `AzureWalletApp.java`:
```java
public static String getAccentColor() {
    return "#2196F3"; // Blue
}
```

## Technology Stack

- **JavaFX 21.0.2** - GUI Framework
- **Maven** - Build Management
- **Java 17** - Programming Language
- **File I/O** - Data Storage

## Security Features

- SHA-256 password hashing
- Account lockout mechanism
- PIN verification
- Transaction logging
- Admin access control

## Future Enhancements

- 🔐 Database integration (MySQL, PostgreSQL)
- 📱 Mobile companion app
- 🌐 Web dashboard
- 💳 Real payment gateway integration
- 📊 Advanced analytics
- 🔔 Push notifications

## Troubleshooting

### Data Directory Not Found
```bash
mkdir -p src/main/resources/data
```

### JavaFX Runtime Issues
Ensure Java 17+ is installed:
```bash
java -version
```

### Maven Build Fails
```bash
mvn clean install
```

## License

This project is open-source and available for educational purposes.

## Developer

**Azure Digital Wallet Team**
- Version 1.0.0
- Built with ❤️ using JavaFX

---

**Happy Banking! 🏦💰**
