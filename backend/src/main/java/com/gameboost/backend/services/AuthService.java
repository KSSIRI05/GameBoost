package com.gameboost.backend.services;

import com.gameboost.backend.dto.request.LoginRequest;
import com.gameboost.backend.dto.request.RegisterRequest;
import com.gameboost.backend.dto.request.UpdateUserRequest;
import com.gameboost.backend.dto.response.AuthResponse;
import com.gameboost.backend.models.User;
import com.gameboost.backend.repositories.UserRepository;
import com.gameboost.backend.security.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() != User.Role.JOUEUR && request.getRole() != User.Role.COACH)
            throw new IllegalArgumentException("L'inscription publique est réservée aux joueurs et aux coachs");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalStateException("Email déjà utilisé");
        if (userRepository.existsByUsername(request.getUsername()))
            throw new IllegalStateException("Username déjà utilisé");

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .role(request.getRole())
                .isApproved(false)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user, jwtUtils.generateToken(user.getEmail(), user.getRole().name(), user.getIsApproved()));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new BadCredentialsException("Email ou mot de passe incorrect");

        return buildAuthResponse(user, jwtUtils.generateToken(user.getEmail(), user.getRole().name(), user.getIsApproved()));
    }

    public User getProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
    }

    public User updateProfile(String email, UpdateUserRequest request) {
        User user = getProfile(email);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail()))
                throw new IllegalStateException("Email déjà utilisé");
            user.setEmail(request.getEmail());
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername()))
                throw new IllegalStateException("Username déjà utilisé");
            user.setUsername(request.getUsername());
        }

        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .isApproved(user.getIsApproved())
                .build();
    }
}
