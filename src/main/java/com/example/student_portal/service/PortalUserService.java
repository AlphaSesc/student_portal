package com.example.student_portal.service;

import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.repository.PortalUserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PortalUserService {

    private final PortalUserRepository portalUserRepository;

    public PortalUserService(PortalUserRepository portalUserRepository) {
        this.portalUserRepository = portalUserRepository;
    }

    public PortalUser registerUser(PortalUser user) {
        return portalUserRepository.save(user);
    }

    public Optional<PortalUser> findByUsername(String email) {
        return portalUserRepository.findByEmail(email);
    }
}