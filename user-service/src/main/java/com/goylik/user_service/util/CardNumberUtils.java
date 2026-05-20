package com.goylik.user_service.util;

/**
 * Utility class for working with payment card numbers.
 * <p>
 * Provides methods for validating card numbers using the Luhn algorithm.
 *
 * @version 1.0
 */
public final class CardNumberUtils {
    private CardNumberUtils() {}

    /**
     * Validates a card number.
     * <p>
     * The validation checks:
     * <ul>
     *     <li>card number is not null</li>
     *     <li>card number contains exactly 16 digits</li>
     *     <li>card number passes the Luhn algorithm</li>
     * </ul>
     *
     * @param cardNumber the card number to validate
     * @return {@code true} if the card number is valid, otherwise {@code false}
     */
    public static boolean validate(String cardNumber) {
        return cardNumber != null
                && cardNumber.matches("\\d{16}")
                && calculateLuhnSum(cardNumber) % 10 == 0;
    }

    /**
     * Calculates the checksum of a card number using the Luhn algorithm.
     *
     * @param cardNumber the card number
     * @return calculated checksum value
     */
    private static int calculateLuhnSum(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = cardNumber.charAt(i) - '0';

            if (alternate) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum;
    }
}