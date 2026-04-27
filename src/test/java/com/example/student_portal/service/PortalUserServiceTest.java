package com.example.student_portal.service;

import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.exception.InvalidCredentialsException;
import com.example.student_portal.exception.ResourceAlreadyExistsException;
import com.example.student_portal.exception.UserNotFoundException;
import com.example.student_portal.repository.PortalUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalUserServiceTest {

    @Mock
    private PortalUserRepository portalUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PortalUserService portalUserService;

    private PortalUser portalUser;

    @BeforeEach
    void setUp() {
        portalUser = PortalUser.builder()
                .email("student@example.com")
                .password("plain-password")
                .role(UserRole.STUDENT)
                .build();
    }

    @Test
    void registerUserShouldEncodePasswordAndSaveUser() {
        PortalUser savedUser = PortalUser.builder()
                .id(1L)
                .email(portalUser.getEmail())
                .password("encoded-password")
                .role(portalUser.getRole())
                .build();

        when(portalUserRepository.findByEmail(portalUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(portalUserRepository.save(portalUser)).thenReturn(savedUser);

        PortalUser result = portalUserService.registerUser(portalUser);

        assertSame(savedUser, result);
        assertEquals("encoded-password", portalUser.getPassword());
        verify(passwordEncoder).encode("plain-password");
        verify(portalUserRepository).save(portalUser);
    }

    @Test
    void registerUserShouldRejectDuplicateEmail() {
        when(portalUserRepository.findByEmail(portalUser.getEmail())).thenReturn(Optional.of(portalUser));

        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> portalUserService.registerUser(portalUser)
        );

        assertEquals("Email already registered", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void authenticateShouldReturnUserWhenPasswordMatches() {
        PortalUser savedUser = PortalUser.builder()
                .id(2L)
                .email(portalUser.getEmail())
                .password("encoded-password")
                .role(UserRole.STUDENT)
                .build();

        when(portalUserRepository.findByEmail(portalUser.getEmail())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);

        PortalUser result = portalUserService.authenticate(portalUser.getEmail(), "plain-password");

        assertSame(savedUser, result);
        verify(passwordEncoder).matches("plain-password", "encoded-password");
    }

    @Test
    void authenticateShouldThrowWhenPasswordDoesNotMatch() {
        PortalUser savedUser = PortalUser.builder()
                .id(2L)
                .email(portalUser.getEmail())
                .password("encoded-password")
                .role(UserRole.STUDENT)
                .build();

        when(portalUserRepository.findByEmail(portalUser.getEmail())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> portalUserService.authenticate(portalUser.getEmail(), "wrong-password")
        );

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void authenticateShouldThrowWhenUserDoesNotExist() {
        when(portalUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> portalUserService.authenticate("missing@example.com", "plain-password")
        );

        assertEquals("User not found", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void findByUsernameShouldReturnUserByEmail() {
        when(portalUserRepository.findByEmail(portalUser.getEmail()))
                .thenReturn(Optional.of(portalUser));

        Optional<PortalUser> result = portalUserService.findByUsername(portalUser.getEmail());

        assertEquals(Optional.of(portalUser), result);
    }
}
