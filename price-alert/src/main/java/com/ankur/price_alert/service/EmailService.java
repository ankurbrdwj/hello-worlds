package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.strategy.AlertStrategyFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Email implementation of NotificationService.
 * Follows Dependency Inversion Principle - implements NotificationService interface.
 * Follows Single Responsibility Principle - handles email notifications only.
 */
@Service
public class EmailService implements NotificationService {

    private final AlertStrategyFactory strategyFactory;

    public EmailService(AlertStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    @Override
    public void sendPriceAlertNotification(PriceAlert alert, double currentPrice) {
        try {
            String subject = buildAlertSubject(alert, currentPrice);
            String body = buildAlertEmailBody(alert, currentPrice);

            send(alert.getUserEmail(), subject, body);

            System.out.println("✅ Price alert sent to: " + alert.getUserEmail());

        } catch (Exception e) {
            System.err.println("⚠️ Failed to send price alert email: " + e.getMessage());
            throw e; // Re-throw to handle in calling service
        }
    }
    @Override
    public void send(String to, String subject, String body) {
        try {
            // Option 1: Using Spring Boot's JavaMailSender
            //sendEmailWithSpringMail(to, subject, body);

            // Option 2: Using external service (uncomment if using SendGrid, AWS SES, etc.)
            // sendEmailWithExternalService(to, subject, body);

        } catch (Exception e) {
            System.err.println("⚠️ Email delivery failed to " + to + ": " + e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }
    private String buildAlertSubject(PriceAlert alert, double currentPrice) {
        return String.format("🚨 Price Alert: %s %s $%.2f",
                alert.getSymbol(),
                getAlertActionText(alert.getAlertType()),
                currentPrice);
    }
    /**
     * Build email body for price alerts
     */
    private String buildAlertEmailBody(PriceAlert alert, double currentPrice) {
        StringBuilder body = new StringBuilder();

        body.append("<!DOCTYPE html>");
        body.append("<html><body style='font-family: Arial, sans-serif;'>");
        body.append("<h2 style='color: #d32f2f;'>🚨 Price Alert Triggered!</h2>");

        body.append("<div style='background-color: #f5f5f5; padding: 20px; border-radius: 8px; margin: 20px 0;'>");
        body.append("<h3>Alert Details:</h3>");
        body.append("<table style='width: 100%; border-collapse: collapse;'>");

        addTableRow(body, "Symbol", alert.getSymbol());
        addTableRow(body, "Current Price", String.format("$%.2f", currentPrice));
        addTableRow(body, "Alert Condition",
                alert.getAlertType().toValue() + " $" + String.format("%.2f", alert.getThreshold()));
        addTableRow(body, "Triggered At",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        body.append("</table>");
        body.append("</div>");

        // Add some market context
        body.append("<div style='margin: 20px 0;'>");
        body.append("<p><strong>What happened?</strong></p>");
        body.append("<p>").append(getAlertExplanation(alert, currentPrice)).append("</p>");
        body.append("</div>");

        if (alert.isOneTime()) {
            body.append("<div style='background-color: #fff3cd; padding: 10px; border-radius: 4px; margin: 20px 0;'>");
            body.append("<p><strong>Note:</strong> This was a one-time alert and has been automatically deactivated.</p>");
            body.append("</div>");
        }

        body.append("<hr style='margin: 30px 0;'>");
        body.append("<p style='color: #666; font-size: 14px;'>");
        body.append("Best regards,<br>");
        body.append("Your Price Alert System<br>");
        body.append("<em>This is an automated message. Please do not reply to this email.</em>");
        body.append("</p>");

        body.append("</body></html>");

        return body.toString();
    }
    /**
     * Helper method to add table rows to email HTML
     */
    private void addTableRow(StringBuilder body, String label, String value) {
        body.append("<tr>");
        body.append("<td style='padding: 8px; font-weight: bold; width: 40%;'>").append(label).append(":</td>");
        body.append("<td style='padding: 8px;'>").append(value).append("</td>");
        body.append("</tr>");
    }

    /**
     * Get action text for alert type using strategy pattern.
     */
    private String getAlertActionText(AlertType alertType) {
        return strategyFactory.getActionText(alertType);
    }

    /**
     * Get explanation for triggered alert using strategy pattern.
     */
    private String getAlertExplanation(PriceAlert alert, double currentPrice) {
        return strategyFactory.getExplanation(alert, currentPrice);
    }
    private String buildEmailBody(PriceAlert alert, double currentPrice) {
        return String.format("""
            Hello,
            
            Your price alert for %s has been triggered!
            
            Alert Details:
            • Symbol: %s
            • Current Price: $%.2f
            • Alert Condition: %s $%.2f
            • Triggered At: %s
            
            Best regards,
            Price Alert System
            """,
                alert.getSymbol(),
                alert.getSymbol(),
                currentPrice,
                alert.getAlertType().toValue(),
                alert.getThreshold(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
   /* private void sendEmailWithSpringMail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML content
            helper.setFrom("alerts@yourcompany.com");

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
         // For demo purposes - just log the email
        System.out.println("📧 EMAIL SENT TO: " + to);
        System.out.println("📋 SUBJECT: " + subject);
        System.out.println("📄 BODY: " + body);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
*/

}
