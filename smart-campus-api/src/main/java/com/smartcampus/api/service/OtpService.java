package com.smartcampus.api.service;

import com.smartcampus.api.model.OtpToken;
import com.smartcampus.api.model.User;
import com.smartcampus.api.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void generateAndSendOtp(User user, String tempPassword) {
        Optional<OtpToken> existingToken = otpTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            LocalDateTime cooldownThreshold = LocalDateTime.now().minusSeconds(COOLDOWN_SECONDS);
            if (existingToken.get().getCreatedAt().isAfter(cooldownThreshold)) {
                throw new RuntimeException("Please wait " + COOLDOWN_SECONDS + " seconds before requesting another code.");
            }
        }

        otpTokenRepository.deleteByUser(user);

        String plainOtp = String.valueOf(secureRandom.nextInt(900_000) + 100_000);
        String hashedOtp = passwordEncoder.encode(plainOtp);

        OtpToken otpToken = new OtpToken();
        otpToken.setUser(user);
        otpToken.setOtpCode(hashedOtp);
        otpToken.setTempPassword(tempPassword);
        otpToken.setExpiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpToken.setAttempts(0);
        otpTokenRepository.save(otpToken);

        emailService.sendOtpEmail(user.getEmail(), plainOtp, OTP_EXPIRY_MINUTES);
    }

    public Optional<OtpToken> verifyOtpAndGetToken(User user, String otpCode) {
        Optional<OtpToken> otpTokenOpt = otpTokenRepository.findByUser(user);

        if (otpTokenOpt.isEmpty()) {
            return Optional.empty();
        }

        OtpToken token = otpTokenOpt.get();

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            otpTokenRepository.delete(token);
            return Optional.empty();
        }

        if (token.getAttempts() >= MAX_ATTEMPTS) {
            otpTokenRepository.delete(token);
            throw new RuntimeException("Maximum verification attempts exceeded. Please request a new code.");
        }

        if (passwordEncoder.matches(otpCode, token.getOtpCode())) {
            otpTokenRepository.delete(token);
            return Optional.of(token);
        } else {
            token.setAttempts(token.getAttempts() + 1);
            otpTokenRepository.save(token);
            return Optional.empty();
        }
    }
}
