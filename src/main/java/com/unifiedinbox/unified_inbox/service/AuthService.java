
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.entity.UserEntity;
import com.unifiedinbox.unified_inbox.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity register(String username, String password) {

        if (username == null ||
                !username.matches("[a-zA-Z0-9_]{3,30}")) {
            throw new IllegalArgumentException(
                    "Username must be between 3 and 30 characters and contain only Latin letters, numbers, or underscores."
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long.\""
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "This username is already taken."
            );
        }

        String passwordHash = passwordEncoder.encode(password);

        UserEntity user = new UserEntity(username, passwordHash);

        return userRepository.save(user);
    }
}
