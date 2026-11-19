package com.farmlink.user;

import com.farmlink.common.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;

    public User getEntity(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID id) {
        return UserDto.from(getEntity(id));
    }

    @Transactional
    public UserDto update(UUID id, UpdateProfileRequest r) {
        User u = getEntity(id);
        if (r.fullName() != null) u.setFullName(r.fullName().trim());
        if (r.phone() != null) u.setPhone(r.phone());
        if (r.location() != null) u.setLocation(r.location());
        if (r.avatarUrl() != null) u.setAvatarUrl(r.avatarUrl());
        if (r.licenseNumber() != null && u.getRole() == Role.FARMER) u.setLicenseNumber(r.licenseNumber());
        if (r.latitude() != null) u.setLatitude(r.latitude());
        if (r.longitude() != null) u.setLongitude(r.longitude());
        return UserDto.from(u);
    }
}
