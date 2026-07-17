package ma.darsma.backend.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a real SMTP provider (EMAIL_MODE=mock, no delivery provider chosen yet).
 * Logs the email instead of sending it.
 */
@Component
public class MockEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(MockEmailProvider.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("Mock email to={} subject=\"{}\" body=\"{}\"", to, subject, body);
    }
}
