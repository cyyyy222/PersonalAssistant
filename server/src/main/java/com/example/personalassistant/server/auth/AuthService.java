package com.example.personalassistant.server.auth;

import com.example.personalassistant.server.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.example.personalassistant.server.auth.AuthDtos.LoginResponse;
import static com.example.personalassistant.server.auth.AuthDtos.UserView;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expirationSeconds;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expirationSeconds = expirationSeconds;
    }

    public LoginResponse login(String account, String password) {
        String normalized = account == null ? "" : account.trim();
        User user = userRepository.findByAccount(normalized)
                .orElseThrow(() -> new ApiException(401, "账号或密码错误"));

        if (user.status() != 1 || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new ApiException(401, "账号或密码错误");
        }

        String token = jwtService.createToken(user.id(), user.username());
        return new LoginResponse(token, expirationSeconds, UserView.from(user));
    }

    public UserView me(long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.status() == 1)
                .map(UserView::from)
                .orElseThrow(() -> new ApiException(401, "登录已失效，请重新登录"));
    }
}
