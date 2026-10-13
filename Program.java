package CIA_Activity;
import java.util.*;

public class Program {

    // Checks whether the input contains only binary digits.
    static boolean isBinary(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        for (char bit : data.toCharArray()) {
            if (bit != '0' && bit != '1') {
                return false;
            }
        }

        return true;
    }

    // Finds the positions where sender and receiver data differ.
    // Positions are counted from left to right, starting at 1.
    static ArrayList<Integer> findErrors(String sender, String receiver) {
        ArrayList<Integer> errors = new ArrayList<>();

        for (int i = 0; i < sender.length(); i++) {
            if (sender.charAt(i) != receiver.charAt(i)) {
                errors.add(i + 1);
            }
        }

        return errors;
    }

    // Checks whether all detected errors form one consecutive sequence.
    static boolean isBurstError(ArrayList<Integer> errors) {
        if (errors.size() < 2) {
            return false;
        }

        for (int i = 1; i < errors.size(); i++) {
            if (errors.get(i) != errors.get(i - 1) + 1) {
                return false;
            }
        }

        return true;
    }

    // Automatically recommends a technique based on the observed error pattern.
    // This is a project decision rule, not identification of the actual protocol.
    static String chooseTechnique(ArrayList<Integer> errors) {
        if (errors.isEmpty()) {
            return "Parity";
        }

        if (errors.size() == 1) {
            return "Hamming Code";
        }

        if (isBurstError(errors)) {
            return "CRC";
        }

        return "Checksum";
    }

    // Generates an even parity bit for the given data.
    static char getParityBit(String data) {
        int ones = 0;

        for (char bit : data.toCharArray()) {
            if (bit == '1') {
                ones++;
            }
        }

        return (ones % 2 == 0) ? '0' : '1';
    }

    // Verifies whether the complete frame has an even number of 1s.
    static boolean checkEvenParity(String frame) {
        int ones = 0;

        for (char bit : frame.toCharArray()) {
            if (bit == '1') {
                ones++;
            }
        }

        return ones % 2 == 0;
    }

    // Pads the data with trailing zeros to make its length a multiple of 8.
    static String padToByte(String data) {
        while (data.length() % 8 != 0) {
            data += "0";
        }

        return data;
    }

    // Generates an 8-bit one's-complement checksum.
    static String getChecksum(String data) {
        String padded = padToByte(data);
        int sum = 0;

        for (int i = 0; i < padded.length(); i += 8) {
            int value = Integer.parseInt(
                padded.substring(i, i + 8), 2
            );

            sum += value;
            sum = (sum & 255) + (sum >> 8);
        }

        while (sum > 255) {
            sum = (sum & 255) + (sum >> 8);
        }

        int checksum = (~sum) & 255;

        return String.format(
            "%8s", Integer.toBinaryString(checksum)
        ).replace(' ', '0');
    }

    // Verifies a frame containing padded data followed by its checksum.
    static boolean verifyChecksum(String frame) {
        if (frame.length() % 8 != 0) {
            return false;
        }

        int sum = 0;

        for (int i = 0; i < frame.length(); i += 8) {
            int value = Integer.parseInt(
                frame.substring(i, i + 8), 2
            );

            sum += value;
            sum = (sum & 255) + (sum >> 8);
        }

        while (sum > 255) {
            sum = (sum & 255) + (sum >> 8);
        }

        return sum == 255;
    }

    // Calculates the CRC remainder using binary XOR division.
    static String crcRemainder(String data, String polynomial) {
        String dividend =
            data + "0".repeat(polynomial.length() - 1);

        char[] bits = dividend.toCharArray();

        for (int i = 0;
             i <= bits.length - polynomial.length();
             i++) {

            if (bits[i] == '1') {
                for (int j = 0; j < polynomial.length(); j++) {
                    bits[i + j] =
                        bits[i + j] == polynomial.charAt(j)
                        ? '0' : '1';
                }
            }
        }

        int remainderLength = polynomial.length() - 1;

        return new String(bits).substring(
            bits.length - remainderLength
        );
    }

    // Verifies a received CRC frame using the same generator polynomial.
    static boolean verifyCRC(String frame, String polynomial) {
        if (frame.length() < polynomial.length()) {
            return false;
        }

        char[] bits = frame.toCharArray();

        for (int i = 0;
             i <= bits.length - polynomial.length();
             i++) {

            if (bits[i] == '1') {
                for (int j = 0; j < polynomial.length(); j++) {
                    bits[i + j] =
                        bits[i + j] == polynomial.charAt(j)
                        ? '0' : '1';
                }
            }
        }

        int remainderLength = polynomial.length() - 1;

        String remainder = new String(bits).substring(
            bits.length - remainderLength
        );

        for (char bit : remainder.toCharArray()) {
            if (bit != '0') {
                return false;
            }
        }

        return true;
    }

    // Returns true for Hamming parity positions: 1, 2, 4, 8, and so on.
    static boolean isParityPosition(int position) {
        return (position & (position - 1)) == 0;
    }

    // Encodes data using Hamming code with even parity.
    static String hammingEncode(String data) {
        int dataLength = data.length();
        int parityCount = 0;

        while ((1 << parityCount) <
               dataLength + parityCount + 1) {
            parityCount++;
        }

        int codeLength = dataLength + parityCount;
        char[] code = new char[codeLength + 1];

        Arrays.fill(code, '0');

        int dataIndex = 0;

        // Places data bits in non-parity positions.
        for (int pos = codeLength; pos >= 1; pos--) {
            if (!isParityPosition(pos)) {
                code[pos] = data.charAt(dataIndex++);
            }
        }

        // Calculates each even parity bit.
        for (int p = 1; p <= codeLength; p *= 2) {
            int parity = 0;

            for (int pos = 1; pos <= codeLength; pos++) {
                if ((pos & p) != 0 && pos != p) {
                    if (code[pos] == '1') {
                        parity ^= 1;
                    }
                }
            }

            code[p] = (char) ('0' + parity);
        }

        StringBuilder result = new StringBuilder();

        for (int pos = codeLength; pos >= 1; pos--) {
            result.append(code[pos]);
        }

        return result.toString();
    }

    // Corrects a single-bit error and extracts the original data.
    // The input must be a Hamming-encoded codeword.
    static String hammingDecode(String received) {
        int codeLength = received.length();
        char[] code = new char[codeLength + 1];

        for (int pos = 1; pos <= codeLength; pos++) {
            code[pos] = received.charAt(codeLength - pos);
        }

        int syndrome = 0;

        // Calculates the syndrome to locate a single-bit error.
        for (int p = 1; p <= codeLength; p *= 2) {
            int parity = 0;

            for (int pos = 1; pos <= codeLength; pos++) {
                if ((pos & p) != 0 && code[pos] == '1') {
                    parity ^= 1;
                }
            }

            if (parity == 1) {
                syndrome += p;
            }
        }

        if (syndrome > codeLength) {
            return "INVALID";
        }

        // Corrects the bit identified by the syndrome.
        if (syndrome != 0) {
            code[syndrome] =
                code[syndrome] == '1' ? '0' : '1';
        }

        StringBuilder data = new StringBuilder();

        // Removes parity bits to recover the original data.
        for (int pos = codeLength; pos >= 1; pos--) {
            if (!isParityPosition(pos)) {
                data.append(code[pos]);
            }
        }

        return data.toString();
    }

    // Flips the corresponding data bit in a Hamming codeword.
    static String introduceHammingError(
        String codeword, int dataErrorPosition) {

        char[] result = codeword.toCharArray();
        int dataIndex = 0;

        for (int pos = codeword.length(); pos >= 1; pos--) {
            if (!isParityPosition(pos)) {
                if (dataIndex == dataErrorPosition - 1) {
                    int index = codeword.length() - pos;

                    result[index] =
                        result[index] == '0' ? '1' : '0';

                    break;
                }

                dataIndex++;
            }
        }

        return new String(result);
    }

    // Injects observed data errors into a frame whose payload starts
    // at the same positions as the original data.
    static String introduceErrors(
        String frame, ArrayList<Integer> errors) {

        char[] result = frame.toCharArray();

        for (int position : errors) {
            int index = position - 1;

            if (index >= 0 && index < result.length) {
                result[index] =
                    result[index] == '0' ? '1' : '0';
            }
        }

        return new String(result);
    }

    // Displays the automatic analysis and runs the selected demonstration.
    static void runProject(String sender, String receiver) {
        ArrayList<Integer> errors = findErrors(sender, receiver);
        String technique = chooseTechnique(errors);

        System.out.println("\n===== TRANSMISSION REPORT =====");
        System.out.println("Sender Data       : " + sender);
        System.out.println("Receiver Data     : " + receiver);
        System.out.println("Error Detected    : " +
            (errors.isEmpty() ? "No" : "Yes"));
        System.out.println("Error Count       : " + errors.size());
        System.out.println("Error Positions   : " +
            (errors.isEmpty() ? "None" : errors));
        System.out.println("Recommended Tech. : " + technique);

        if (errors.isEmpty()) {
            String frame = sender + getParityBit(sender);

            System.out.println("\n--- Parity Demonstration ---");
            System.out.println("Transmitted Frame : " + frame);
            System.out.println("Parity Check      : " +
                (checkEvenParity(frame) ? "PASS" : "FAIL"));
            System.out.println("Recovered Data    : " + sender);
            System.out.println("Correction Status : No correction needed");
        }
        else if (technique.equals("Hamming Code")) {
            System.out.println("\n--- Hamming Code Demonstration ---");

            String encoded = hammingEncode(sender);

            String corrupted = introduceHammingError(
                encoded, errors.get(0)
            );

            String corrected = hammingDecode(corrupted);

            System.out.println("Encoded Codeword  : " + encoded);
            System.out.println("Corrupted Word    : " + corrupted);
            System.out.println("Recovered Data    : " + corrected);

            System.out.println("Correction Status : " +
                (corrected.equals(sender) ? "SUCCESS" : "FAILED"));
        }
        else if (technique.equals("CRC")) {
            System.out.println("\n--- CRC Demonstration ---");

            String polynomial = "1011";

            String frame =
                sender + crcRemainder(sender, polynomial);

            String corrupted = introduceErrors(frame, errors);

            boolean passed = verifyCRC(corrupted, polynomial);

            System.out.println("Generator         : " + polynomial);
            System.out.println("Transmitted Frame : " + frame);
            System.out.println("Corrupted Frame   : " + corrupted);
            System.out.println("CRC Check         : " +
                (passed ? "PASS" : "ERROR DETECTED"));
            System.out.println(
                "Correction Status : CRC detects errors; it does not correct them."
            );
        }
        else {
            System.out.println("\n--- Checksum Demonstration ---");

            String padded = padToByte(sender);
            String frame = padded + getChecksum(sender);

            String corrupted = introduceErrors(frame, errors);

            boolean passed = verifyChecksum(corrupted);

            System.out.println("Transmitted Frame : " + frame);
            System.out.println("Corrupted Frame   : " + corrupted);
            System.out.println("Checksum Check    : " +
                (passed ? "PASS" : "ERROR DETECTED"));
            System.out.println(
                "Correction Status : Checksum detects errors; it does not correct them."
            );
        }

        System.out.println("===============================");
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println(
            "Dynamic Error Detection and Correction System"
        );

        System.out.print("Enter Sender Data Bits: ");
        String sender = sc.next();

        System.out.print("Enter Receiver Data Bits: ");
        String receiver = sc.next();

        if (!isBinary(sender) || !isBinary(receiver)) {
            System.out.println(
                "Invalid input! Enter binary digits (0 and 1) only."
            );
        }
        else if (sender.length() != receiver.length()) {
            System.out.println(
                "Sender and receiver data must have the same length."
            );
        }
        else {
            runProject(sender, receiver);
        }

        sc.close();
    }
}
