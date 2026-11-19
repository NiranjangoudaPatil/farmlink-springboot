package com.farmlink.notification;

import com.farmlink.common.ApiException;
import com.farmlink.common.PageResponse;
import com.farmlink.mail.EmailEvent;
import com.farmlink.user.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the in-app notification and, once the surrounding transaction commits,
 * an email for the same message (see EmailService).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final ApplicationEventPublisher events;

    @Transactional
    public void notify(User user, String title, String message, String type, UUID referenceId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setReferenceId(referenceId);
        repo.save(n);
        events.publishEvent(new EmailEvent(user.getEmail(), title, message));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(UUID userId, Pageable pageable) {
        return PageResponse.of(repo.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(NotificationDto::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDto markRead(UUID userId, UUID id) {
        Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Notification not found");
        }
        n.setRead(true);
        return NotificationDto.from(n);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repo.markAllRead(userId);
    }
}
