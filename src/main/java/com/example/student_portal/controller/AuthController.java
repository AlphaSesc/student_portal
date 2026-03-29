package com.example.student_portal.controller;

import com.example.student_portal.dto.LoginRequest;
import com.example.student_portal.dto.LoginResponse;
import com.example.student_portal.dto.RegisterRequest;
import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.service.PortalUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PortalUserService portalUserService;

    public AuthController(PortalUserService portalUserService) {
        this.portalUserService = portalUserService;
    }

    @PostMapping("/register")
    public PortalUser register(@RequestBody RegisterRequest request) {
        System.out.println("Register endpoint hit");
        PortalUser user = PortalUser.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .build();
        return portalUserService.registerUser(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        PortalUser user = portalUserService.authenticate(
                request.getEmail(),
                request.getPassword()
        );

        return new LoginResponse("Login successful for: " + user.getEmail());
    }
}