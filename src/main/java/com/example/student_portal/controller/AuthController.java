package com.example.student_portal.controller;

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
    public PortalUser register(@RequestBody PortalUser user) {
        return portalUserService.registerUser(user);
    }
}