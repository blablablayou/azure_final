package azurewallet.utils;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for card-related operations
 */
public class CardUtil {
    
    /**
     * Format a card number to display format (XXXX XXXX XXXX XXXX)
     * @param cardNumber The full card number
     * @return Formatted card number
     */
    public static String formatCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "";
        }
        
        String cleaned = cardNumber.replaceAll("\\s+", "");
        
        if (cleaned.length() == 16) {
            return cleaned.substring(0, 4) + " " +
                   cleaned.substring(4, 8) + " " +
                   cleaned.substring(8, 12) + " " +
                   cleaned.substring(12, 16);
        }
        
        return cardNumber;
    }
    
    /**
     * Generate a card expiry date (MM/YY) valid for 5 years
     * @return Expiry date in MM/YY format
     */
    public static String generateExpiry() {
        YearMonth expiryMonth = YearMonth.now().plusYears(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        return expiryMonth.format(formatter);
    }
    
    /**
     * Generate a random 3-digit CVV
     * @return Random CVV as string
     */
    public static String generateCVV() {
        return String.format("%03d", (int)(Math.random() * 1000));
    }
    
    /**
     * Validate card number using Luhn algorithm
     * @param cardNumber The card number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCardNumber(String cardNumber) {
        String cleaned = cardNumber.replaceAll("\\s+", "");
        
        if (!cleaned.matches("\\d{16}")) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cleaned.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
}
