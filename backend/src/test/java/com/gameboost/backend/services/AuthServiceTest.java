package com.gameboost.backend.services;

import com.gameboost.backend.dto.request.LoginRequest;
import com.gameboost.backend.dto.request.RegisterRequest;
import com.gameboost.backend.dto.request.UpdateUserRequest;
import com.gameboost.backend.dto.response.AuthResponse;
import com.gameboost.backend.models.User;
import com.gameboost.backend.repositories.UserRepository;
import com.gameboost.backend.security.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Tests unitaires")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@gameboost.com")
                .password("encodedPassword")
                .username("testplayer")
                .role(User.Role.JOUEUR)
                .isApproved(false)
                .build();
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        void register_adminRoleIsRejectedBeforePersistence() {
            RegisterRequest request = new RegisterRequest();
            request.setRole(User.Role.ADMIN);
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(userRepository, passwordEncoder, jwtUtils);
        }

        @Test
        void register_missingRoleIsRejectedBeforePersistence() {
            assertThatThrownBy(() -> authService.register(new RegisterRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(userRepository, passwordEncoder, jwtUtils);
        }

        @Test
        @DisplayName("Inscription réussie — retourne AuthResponse avec token")
        void register_success() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@gameboost.com");
            request.setPassword("Password1");
            request.setUsername("newplayer");
            request.setRole(User.Role.JOUEUR);

            when(userRepository.existsByEmail("new@gameboost.com")).thenReturn(false);
            when(userRepository.existsByUsername("newplayer")).thenReturn(false);
            when(passwordEncoder.encode("Password1")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });
            when(jwtUtils.generateToken(eq("new@gameboost.com"), eq("JOUEUR"), eq(false)))
                    .thenReturn("jwt-token");

            AuthResponse response = authService.register(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("new@gameboost.com");
            assertThat(response.getUsername()).isEqualTo("newplayer");
            assertThat(response.getRole()).isEqualTo(User.Role.JOUEUR);
            assertThat(response.getIsApproved()).isFalse();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Inscription avec email existant — lève IllegalStateException")
        void register_duplicateEmail_shouldThrow() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@gameboost.com");
            request.setPassword("Password1");
            request.setUsername("newuser");
            request.setRole(User.Role.JOUEUR);

            when(userRepository.existsByEmail("existing@gameboost.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email déjà utilisé");
        }

        @Test
        @DisplayName("Inscription avec username existant — lève IllegalStateException")
        void register_duplicateUsername_shouldThrow() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@gameboost.com");
            request.setPassword("Password1");
            request.setUsername("existinguser");
            request.setRole(User.Role.JOUEUR);

            when(userRepository.existsByEmail("new@gameboost.com")).thenReturn(false);
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Username déjà utilisé");
        }

        @Test
        @DisplayName("Inscription COACH — rôle COACH dans la réponse")
        void register_asCoach_shouldReturnCoachRole() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("coach@gameboost.com");
            request.setPassword("Password1");
            request.setUsername("newcoach");
            request.setRole(User.Role.COACH);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });
            when(jwtUtils.generateToken(anyString(), anyString(), anyBoolean())).thenReturn("token");

            AuthResponse response = authService.register(request);
            assertThat(response.getRole()).isEqualTo(User.Role.COACH);
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Login réussi — retourne AuthResponse avec token")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@gameboost.com");
            request.setPassword("Password1");

            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password1", "encodedPassword")).thenReturn(true);
            when(jwtUtils.generateToken("test@gameboost.com", "JOUEUR", false)).thenReturn("jwt-token");

            AuthResponse response = authService.login(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("test@gameboost.com");
        }

        @Test
        @DisplayName("Login avec email inexistant — lève BadCredentialsException")
        void login_wrongEmail_shouldThrow() {
            LoginRequest request = new LoginRequest();
            request.setEmail("wrong@mail.com");
            request.setPassword("Password1");

            when(userRepository.findByEmail("wrong@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Login avec mauvais mot de passe — lève BadCredentialsException")
        void login_wrongPassword_shouldThrow() {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@gameboost.com");
            request.setPassword("WrongPassword1");

            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPassword1", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("GetProfile")
    class GetProfileTests {

        @Test
        @DisplayName("Récupérer un profil existant — retourne l'utilisateur")
        void getProfile_existingUser_shouldReturnUser() {
            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));

            User result = authService.getProfile("test@gameboost.com");

            assertThat(result.getEmail()).isEqualTo("test@gameboost.com");
            assertThat(result.getUsername()).isEqualTo("testplayer");
        }

        @Test
        @DisplayName("Récupérer un profil inexistant — lève EntityNotFoundException")
        void getProfile_nonExistingUser_shouldThrow() {
            when(userRepository.findByEmail("unknown@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getProfile("unknown@mail.com"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("UpdateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Mise à jour email — email mis à jour avec succès")
        void updateProfile_changeEmail_shouldUpdate() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("newemail@gameboost.com");

            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("newemail@gameboost.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.updateProfile("test@gameboost.com", request);
            assertThat(result.getEmail()).isEqualTo("newemail@gameboost.com");
        }

        @Test
        @DisplayName("Mise à jour username — username mis à jour avec succès")
        void updateProfile_changeUsername_shouldUpdate() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setUsername("newusername");

            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsername("newusername")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.updateProfile("test@gameboost.com", request);
            assertThat(result.getUsername()).isEqualTo("newusername");
        }

        @Test
        @DisplayName("Mise à jour vers email existant — lève IllegalStateException")
        void updateProfile_duplicateEmail_shouldThrow() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("taken@gameboost.com");

            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("taken@gameboost.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.updateProfile("test@gameboost.com", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email déjà utilisé");
        }

        @Test
        @DisplayName("Mise à jour vers username existant — lève IllegalStateException")
        void updateProfile_duplicateUsername_shouldThrow() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setUsername("takenuser");

            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsername("takenuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.updateProfile("test@gameboost.com", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Username déjà utilisé");
        }

        @Test
        @DisplayName("Mise à jour avec le même email — ne déclenche pas la vérification de doublon")
        void updateProfile_sameEmail_shouldSkipCheck() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("test@gameboost.com"); // same as current

            when(userRepository.findByEmail("test@gameboost.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.updateProfile("test@gameboost.com", request);
            assertThat(result.getEmail()).isEqualTo("test@gameboost.com");
            verify(userRepository, never()).existsByEmail(anyString());
        }
    }
}
