package com.goylik.user_service.service.impl;

import com.goylik.user_service.exception.card.CardCryptoException;
import com.goylik.user_service.service.CardCryptoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CardCryptoServiceImpl implements CardCryptoService {
    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    @Value("${app.payment-card.encryption.base64-key}")
    private String base64Key;

    private SecretKey secretKey;
    private static final SecureRandom RNG = new SecureRandom();

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);

        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalStateException(
                    "Encryption key must be 16, 24 or 32 bytes (AES-128/192/256)."
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encrypt(String cardNumber) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(cardNumber.getBytes());
            byte[] combined = new byte[iv.length + encrypted.length];

            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new CardCryptoException("Failed to encrypt card number", e);
        }
    }

    @Override
    public String decrypt(String encryptedCardNumber) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedCardNumber);

            if (combined.length <= IV_LENGTH) {
                throw new CardCryptoException("Invalid encrypted data format");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] cardNumber = cipher.doFinal(ciphertext);

            return new String(cardNumber);

        } catch (Exception e) {
            throw new CardCryptoException("Failed to decrypt card number", e);
        }
    }
}