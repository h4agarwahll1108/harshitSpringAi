package ai.serviceImpl;

import ai.dto.*;
import ai.exception.ServiceProvisioningException;
import ai.model.RefreshToken;
import ai.model.User;
import ai.repository.EmailService;
import ai.repository.RefreshTokenRepository;
import ai.repository.UserRepository;
import ai.security.JwtService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final long REFRESH_TOKEN_EXPIRATION = 7L * 24 * 60 * 60 * 1000;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ServiceProvisioningException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ServiceProvisioningException("Email already exists");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .enabled(true)
                .build();
        userRepository.save(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        try {
            sendRegistrationEmail(user);
        } catch (Exception e) {
            log.error("User registered but registration email failed. email={}",user.getEmail(),e);
        }
        return generateAuthResponse(userDetails, user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        return generateAuthResponse(userDetails, user);
    }

    private AuthResponse generateAuthResponse(UserDetails userDetails, User user) {
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiration() / 1000)
                .build();
    }

    private String createRefreshToken(User user) {
        // Remove old refresh tokens.
        refreshTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new ServiceProvisioningException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ServiceProvisioningException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        // Rotate refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = createRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiration() / 1000)
                .build();
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private void sendRegistrationEmail(User user) {
        EmailRequest emailRequest = EmailRequest.builder()
                .to(user.getEmail())
                .subject("Registration Successful")
                .templateName("registration-success")
                .variables(Map.of(
                        "userName", user.getUsername(),
                        "email", user.getEmail()
                ))
                .build();
        emailService.send(emailRequest);
    }
}
