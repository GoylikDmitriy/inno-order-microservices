package com.goylik.user_service.service;

/**
 * Service responsible for encryption and decryption
 * of sensitive card data.
 */
public interface CardCryptoService {
    /**
     * Encrypts a card number.
     *
     * @param cardNumber plain card number
     * @return encrypted card number
     */
    String encrypt(String cardNumber);

    /**
     * Decrypts an encrypted card number.
     *
     * @param encryptedCardNumber encrypted card number
     * @return decrypted card number
     */
    String decrypt(String encryptedCardNumber);
}
