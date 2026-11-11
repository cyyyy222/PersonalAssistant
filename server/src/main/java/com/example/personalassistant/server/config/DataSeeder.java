package com.example.personalassistant.server.config;

import com.example.personalassistant.server.auth.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByUsername("caiyangyang")) {
            userRepository.insert(
                    "caiyangyang",
                    "13800138000",
                    passwordEncoder.encode("123456789"),
                    "Cai Yangyang"
            );
        }
    }
}
