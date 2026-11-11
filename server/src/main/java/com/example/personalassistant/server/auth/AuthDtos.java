package com.example.personalassistant.server.auth;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "请输入账号或手机号") String account,
            @NotBlank(message = "请输入密码") String password
    ) {
    }

    public record UserView(
            long id,
            String username,
            String phone,
            String displayName
    ) {
        public static UserView from(User user) {
            return new UserView(user.id(), user.username(), user.phone(), user.displayName());
        }
    }

    public record LoginResponse(
            String token,
            long expiresIn,
            UserView user
    ) {
    }
}
