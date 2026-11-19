package com.farmlink.security;

import com.farmlink.user.Role;
import java.util.UUID;

/** The authenticated caller, rebuilt from the JWT on every request (no DB hit). */
public record AuthUser(UUID id, String email, Role role) {}
