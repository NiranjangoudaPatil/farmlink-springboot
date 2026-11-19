package com.farmlink.notification;

import com.farmlink.common.ApiResponse;
import com.farmlink.common.PageResponse;
import com.farmlink.security.AuthUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ApiResponse<PageResponse<NotificationDto>> list(@AuthenticationPrincipal AuthUser me,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.list(me.id(), pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(Map.of("unread", service.unreadCount(me.id())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationDto> markRead(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(service.markRead(me.id(), id));
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(Map.of("updated", service.markAllRead(me.id())));
    }
}
