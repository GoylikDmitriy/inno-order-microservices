package com.goylik.user_service.service.impl;

import com.goylik.user_service.exception.card.CardHashingException;
import com.goylik.user_service.service.CardHashService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class CardHashServiceImpl implements CardHashService {
    @Override
    public String hash(String cardNumber) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(cardNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new CardHashingException("SHA-256 not available", e);
        }
    }
}
