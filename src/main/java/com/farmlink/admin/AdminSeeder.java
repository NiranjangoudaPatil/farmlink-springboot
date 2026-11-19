package com.farmlink.admin;

import com.farmlink.config.AppProperties;
import com.farmlink.user.Role;
import com.farmlink.user.User;
import com.farmlink.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD, because admins cannot self-register. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        if (admin == null || admin.email() == null || admin.email().isBlank()
                || admin.password() == null || admin.password().isBlank()) {
            return;
        }
        String email = admin.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(admin.password()));
        u.setFullName(admin.name());
        u.setRole(Role.ADMIN);
        u.setVerified(true);
        users.save(u);
        log.info("Created admin account {}", email);
    }
}
