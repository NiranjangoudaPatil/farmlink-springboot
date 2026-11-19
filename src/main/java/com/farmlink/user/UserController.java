package com.farmlink.user;

import com.farmlink.common.ApiResponse;
import com.farmlink.security.AuthUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(userService.me(me.id()));
    }

    @PutMapping("/me")
    public ApiResponse<UserDto> update(@AuthenticationPrincipal AuthUser me,
                                       @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.update(me.id(), request));
    }
}
