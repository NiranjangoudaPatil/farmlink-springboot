package com.farmlink.mail;

import com.farmlink.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends emails off the request thread and only after the DB transaction committed,
 * so a rolled-back order never produces an email. Disabled by default (just logs).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmail(EmailEvent event) {
        if (!props.mail().enabled()) {
            log.info("[email disabled] to={} subject={}", event.to(), event.subject());
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Email enabled but no SMTP host configured (spring.mail.host); skipping {}", event.subject());
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(props.mail().from());
            m.setTo(event.to());
            m.setSubject(event.subject());
            m.setText(event.body());
            sender.send(m);
        } catch (Exception e) {
            log.error("Failed to send email to {}", event.to(), e);
        }
    }
}
