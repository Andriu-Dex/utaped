package ec.edu.uta.utaped.notifications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="app.notifications.deadline-enabled",havingValue="true",matchIfMissing=true)
public class DeadlineNoticeScheduler {
    private final DeadlineNoticeService service;
    public DeadlineNoticeScheduler(DeadlineNoticeService service) { this.service=service; }
    @Scheduled(fixedDelayString="${app.notifications.deadline-scan-ms:3600000}",initialDelayString="${app.notifications.deadline-scan-ms:3600000}")
    public void run() { service.generate(service.today()); }
}
