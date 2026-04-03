package com.example.student_portal.controller;

import com.example.student_portal.dto.AuthResponse;
import com.example.student_portal.dto.LoginRequest;
//import com.example.student_portal.dto.LoginResponse;
import com.example.student_portal.dto.RegisterRequest;
import com.example.student_portal.dto.UserResponse;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.security.CustomUserDetails;
import com.example.student_portal.security.JwtService;
import com.example.student_portal.service.PortalUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PortalUserService portalUserService;
    private final JwtService jwtService;

    public AuthController(PortalUserService portalUserService, JwtService jwtService)
    {
        this.portalUserService = portalUserService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("Register endpoint hit");
        PortalUser user = PortalUser.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .build();
        PortalUser savedUser = portalUserService.registerUser(user);
        return new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        PortalUser user = portalUserService.authenticate(
                request.getEmail(),
                request.getPassword()
        );

        String token = jwtService.generateToken(new CustomUserDetails(user));

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole()
        );
    }
}