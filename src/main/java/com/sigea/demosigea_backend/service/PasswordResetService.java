package com.sigea.demosigea_backend.service;


import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigea.demosigea_backend.model.PasswordResetToken;
import com.sigea.demosigea_backend.repository.PasswordResetTokenRepository;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Transactional
    public String createPasswordResetToken(String email) {
        tokenRepository.deleteByUserEmail(email);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, email, 15);
        tokenRepository.save(resetToken);

        System.out.println(">>> TOKEN GENERADO PARA " + email + ": " + token);
        return token;
    }

    public boolean validateAndResetPassword(String token, String newPassword) {
        var tokenOptional = tokenRepository.findByToken(token);

        if (tokenOptional.isEmpty()) {
            throw new IllegalArgumentException("El token proporcionado es inválido.");
        }

        PasswordResetToken resetToken = tokenOptional.get();

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("El token de recuperación ha expirado.");
        }

        tokenRepository.delete(resetToken);
        return true;
    }
}
