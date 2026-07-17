package ma.darsma.backend.notification.email;

/**
 * Strategy interface for outbound email delivery. {@link MockEmailProvider} backs local/dev/test;
 * a real SMTP-backed provider is added once a delivery provider is chosen (EMAIL_MODE=mock for now).
 */
public interface EmailProvider {

    void send(String to, String subject, String body);
}
