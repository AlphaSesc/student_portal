package com.example.student_portal.service;

import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.exception.InvalidCredentialsException;
import com.example.student_portal.exception.UserNotFoundException;
import com.example.student_portal.exception.ResourceAlreadyExistsException;
import com.example.student_portal.repository.PortalUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PortalUserService {

    private final PortalUserRepository portalUserRepository;
    private final PasswordEncoder passwordEncoder;

    public PortalUserService(PortalUserRepository portalUserRepository, PasswordEncoder passwordEncoder) {
        this.portalUserRepository = portalUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PortalUser registerUser(PortalUser user) {
        if (portalUserRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("Email already registered");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return portalUserRepository.save(user);
    }

    public Optional<PortalUser> findByUsername(String email) {
        return portalUserRepository.findByEmail(email);
    }

    public PortalUser authenticate(String email, String password) {
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return user;
    }
}