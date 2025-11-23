package azurewallet.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class CardUtil {

    private CardUtil() {}

    public static String generateLuhn16() {
        java.util.Random rnd = new java.util.Random();
        int[] digits = new int[16];
        for (int i = 0; i < 15; i++) digits[i] = rnd.nextInt(10);
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            int val = digits[14 - i];
            if ((i % 2) == 0) {
                int dbl = val * 2;
                if (dbl > 9) dbl -= 9;
                sum += dbl;
            } else {
                sum += val;
            }
        }
        int check = (10 - (sum % 10)) % 10;
        digits[15] = check;
        StringBuilder sb = new StringBuilder();
        for (int d : digits) sb.append(d);
        return sb.toString();
    }

    public static String formatCardNumber(String num) {
        if (num == null) return "";
        return num.replaceAll("(.{4})", "$1 ").trim();
    }

    public static String generateExpiry() {
        // Use a 5-year validity for generated virtual cards
        LocalDate d = LocalDate.now().plusYears(5);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yy");
        return d.format(fmt);
    }
}
