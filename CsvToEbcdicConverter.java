import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

public class CsvToEbcdicConverter {
    
    // Field constants from copybook
    private static final int CUSTOMER_ID_LENGTH = 7;
    private static final int CUSTOMER_NAME_LENGTH = 20;
    private static final int TRANSACTION_AMT_LENGTH = 5;
    private static final int TRANSACTION_DATE_LENGTH = 10;
    private static final int ACCOUNT_STATUS_LENGTH = 1;
    
    // Position constants
    private static final int NAME_POS = CUSTOMER_ID_LENGTH;
    private static final int AMOUNT_POS = NAME_POS + CUSTOMER_NAME_LENGTH;
    private static final int DATE_POS = AMOUNT_POS + TRANSACTION_AMT_LENGTH;
    private static final int STATUS_POS = DATE_POS + TRANSACTION_DATE_LENGTH;
    
    private static final Charset EBCDIC = Charset.forName("Cp037");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("0000000.00");

    public static void main(String[] args) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("customers.csv"));
             OutputStream writer = new FileOutputStream("customers.ebc")) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                if (fields.length != 5) {
                    throw new IOException("Invalid CSV format in line: " + line);
                }
                
                byte[] record = new byte[43];
                
                // 1. Customer ID (7 bytes)
                String id = padOrTruncate(fields[0], CUSTOMER_ID_LENGTH, '0', false);
                System.arraycopy(id.getBytes(EBCDIC), 0, record, 0, CUSTOMER_ID_LENGTH);
                
                // 2. Customer Name (20 bytes)
                String name = padOrTruncate(fields[1], CUSTOMER_NAME_LENGTH, ' ', true);
                System.arraycopy(name.getBytes(EBCDIC), 0, record, NAME_POS, CUSTOMER_NAME_LENGTH);
                
                // 3. Transaction Amount (5 bytes packed)
                double amount = Double.parseDouble(fields[2]);
                byte[] packed = packDecimal(AMOUNT_FORMAT.format(amount), 9);
                System.arraycopy(packed, 0, record, AMOUNT_POS, TRANSACTION_AMT_LENGTH);
                
                // 4. Transaction Date (10 bytes)
                String date = padOrTruncate(fields[3], TRANSACTION_DATE_LENGTH, ' ', true);
                System.arraycopy(date.getBytes(EBCDIC), 0, record, DATE_POS, TRANSACTION_DATE_LENGTH);
                
                // 5. Account Status (1 byte)
                String status = padOrTruncate(fields[4], ACCOUNT_STATUS_LENGTH, ' ', true);
                System.arraycopy(status.getBytes(EBCDIC), 0, record, STATUS_POS, ACCOUNT_STATUS_LENGTH);
                
                writer.write(record);
            }
        }
    }

    private static String padOrTruncate(String input, int length, char padChar, boolean rightPad) {
        if (input.length() > length) {
            return input.substring(0, length);
        }
        if (input.length() < length) {
            String format = rightPad ? "%-" + length + "s" : "%" + length + "s";
            return String.format(format, input).replace(' ', padChar);
        }
        return input;
    }

    private static byte[] packDecimal(String number, int totalDigits) {
        String cleanNumber = number.replace(".", "").replace("-", "");
        int length = (totalDigits + 1) / 2;
        byte[] packed = new byte[length];
        
        for (int i = 0; i < cleanNumber.length(); i++) {
            int digit = Character.getNumericValue(cleanNumber.charAt(i));
            if (i % 2 == 0) {
                packed[i/2] = (byte) (digit << 4);
            } else {
                packed[i/2] |= (byte) digit;
            }
        }
        
        // Add sign in last nibble (0xC for positive, 0xD for negative)
        packed[length-1] |= (number.contains("-") ? 0x0D : 0x0C);
        return packed;
    }
}
