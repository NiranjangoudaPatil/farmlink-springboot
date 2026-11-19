package com.farmlink.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the "app.*" block in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        String uploadDir,
        List<String> corsOrigins,
        Razorpay razorpay,
        Mail mail,
        Admin admin) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Razorpay(String keyId, String keySecret) {
        public boolean configured() {
            return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        }
    }

    public record Mail(boolean enabled, String from) {}

    public record Admin(String email, String password, String name) {}
}
