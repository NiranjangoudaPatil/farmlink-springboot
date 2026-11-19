package com.farmlink.auth;

import com.farmlink.common.ApiException;
import com.farmlink.notification.NotificationService;
import com.farmlink.security.JwtService;
import com.farmlink.user.Role;
import com.farmlink.user.User;
import com.farmlink.user.UserDto;
import com.farmlink.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final NotificationService notifications;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (r.role() == Role.ADMIN) {
            throw ApiException.badRequest("Role must be FARMER or RETAILER");
        }
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setRole(r.role());
        u.setPhone(r.phone());
        u.setLocation(r.location());
        u.setLatitude(r.latitude());
        u.setLongitude(r.longitude());
        if (r.role() == Role.FARMER) {
            u.setLicenseNumber(r.licenseNumber());
        }
        users.save(u);

        notifications.notify(u, "Welcome to FarmLink, " + u.getFullName() + "!",
                "Your " + u.getRole().name().toLowerCase() + " account is ready.", "WELCOME", null);
        return AuthResponse.of(jwt.generate(u), UserDto.from(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .filter(found -> encoder.matches(r.password(), found.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return AuthResponse.of(jwt.generate(u), UserDto.from(u));
    }
}
