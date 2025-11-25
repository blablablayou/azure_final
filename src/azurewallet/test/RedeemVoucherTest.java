package azurewallet.test;

import azurewallet.main.AzureDigitalApp;

public class RedeemVoucherTest {
    public static void main(String[] args) {
        AzureDigitalApp app = new AzureDigitalApp();
        String user = "testuser15140";
        String code = "VCHR-TESTUSER15140-7345";
        System.out.println("Before redeem, balance=" + app.getBalance(user));
        double v = app.redeemVoucher(user, code);
        System.out.println("Redeem returned: " + v);
        System.out.println("After redeem, balance=" + app.getBalance(user));
        app.shutdown();
    }
}
