package com.example.personalassistant.server.auth;

import com.example.personalassistant.server.auth.AuthDtos.LoginRequest;
import com.example.personalassistant.server.auth.AuthDtos.LoginResponse;
import com.example.personalassistant.server.auth.AuthDtos.UserView;
import com.example.personalassistant.server.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.account(), request.password()));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me(@RequestAttribute("userId") long userId) {
        return ApiResponse.ok(authService.me(userId));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }
}
