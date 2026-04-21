package com.notifyhub.service;

import com.notifyhub.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String         fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender  = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Sends an HTML notification email to the given recipient.
     *
     * @param toEmail   recipient email address
     * @param event     the notification event that triggered this email
     */
    public void sendNotification(String toEmail, NotificationEvent event) {
        try {
            MimeMessage     mime    = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("NotifyHub — " + formatEventType(event.getEventType()));
            helper.setText(buildHtml(event), true);

            mailSender.send(mime);
            log.info("Email sent to={} eventId={}", toEmail, event.getId());

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Failed to send email to={} eventId={} | {} | cause: {}",
                    toEmail, event.getId(), e.getMessage(), cause.getMessage(), e);
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private String formatEventType(NotificationEvent.EventType type) {
        // e.g. ORDER_PLACED → Order Placed
        String[] words = type.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private String buildHtml(NotificationEvent event) {
        String eventType = formatEventType(event.getEventType());
        String payload   = event.getPayload() != null ? event.getPayload() : "{}";
        String tenantId  = event.getTenantId() != null ? event.getTenantId().toString() : "—";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>NotifyHub</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="560" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">

                          <!-- Header -->
                          <tr>
                            <td style="background:#1e293b;padding:24px 32px;">
                              <p style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:-0.3px;">
                                NotifyHub
                              </p>
                              <p style="margin:4px 0 0;color:#94a3b8;font-size:13px;">
                                Real-Time Event Notification
                              </p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 4px;color:#6b7280;font-size:12px;font-weight:600;
                                         text-transform:uppercase;letter-spacing:0.5px;">
                                New Event
                              </p>
                              <h2 style="margin:0 0 24px;color:#111827;font-size:22px;font-weight:700;">
                                %s
                              </h2>

                              <!-- Meta -->
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;margin-bottom:24px;">
                                <tr>
                                  <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;">
                                    <span style="color:#6b7280;font-size:12px;">Event ID</span><br/>
                                    <span style="color:#111827;font-size:13px;font-family:monospace;">%s</span>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;">
                                    <span style="color:#6b7280;font-size:12px;">Tenant</span><br/>
                                    <span style="color:#111827;font-size:13px;font-family:monospace;">%s</span>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 16px;">
                                    <span style="color:#6b7280;font-size:12px;">Status</span><br/>
                                    <span style="background:#dcfce7;color:#16a34a;font-size:12px;font-weight:600;
                                                 padding:2px 8px;border-radius:4px;">QUEUED</span>
                                  </td>
                                </tr>
                              </table>

                              <!-- Payload -->
                              <p style="margin:0 0 8px;color:#6b7280;font-size:12px;font-weight:600;
                                         text-transform:uppercase;letter-spacing:0.5px;">
                                Payload
                              </p>
                              <pre style="margin:0;background:#0f172a;color:#e2e8f0;font-size:12px;
                                          padding:16px;border-radius:8px;overflow-x:auto;white-space:pre-wrap;
                                          word-break:break-all;">%s</pre>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background:#f9fafb;padding:16px 32px;border-top:1px solid #e5e7eb;">
                              <p style="margin:0;color:#9ca3af;font-size:11px;text-align:center;">
                                You received this because you subscribed to
                                <strong>%s</strong> events on the EMAIL channel.<br/>
                                NotifyHub &mdash; Real-Time Event-Driven Notification System
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(eventType, event.getId(), tenantId, payload, eventType);
    }
}
