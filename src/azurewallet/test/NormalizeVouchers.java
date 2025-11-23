package azurewallet.test;

import java.io.*;
import java.time.LocalDate;

public class NormalizeVouchers {
    public static void main(String[] args) {
        File f = new File("src/azurewallet/data/vouchers.txt");
        File tmp = new File("src/azurewallet/data/vouchers.tmp");
        try (BufferedReader br = new BufferedReader(new FileReader(f));
             PrintWriter pw = new PrintWriter(new FileWriter(tmp))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length == 3) {
                    pw.println(line + "," + LocalDate.now().plusMonths(1));
                } else {
                    pw.println(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error normalizing vouchers: " + e.getMessage());
            return;
        }
        // replace
        if (!f.delete()) System.err.println("Warning: could not delete original vouchers file");
        if (!tmp.renameTo(f)) System.err.println("Warning: could not rename temp vouchers file");
        System.out.println("Vouchers normalized.");
    }
}
