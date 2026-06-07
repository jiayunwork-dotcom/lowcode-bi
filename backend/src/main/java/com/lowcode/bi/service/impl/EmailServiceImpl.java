package com.lowcode.bi.service.impl;

import com.lowcode.bi.service.EmailService;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.properties.mail.smtp.from:}")
    private String fromDisplay;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Override
    public boolean sendEmail(String to, String subject, String body) {
        return sendEmail(List.of(to), subject, body);
    }

    @Override
    @Async("emailExecutor")
    public boolean sendEmail(List<String> to, String subject, String body) {
        if (!mailEnabled) {
            log.info("邮件功能已禁用，跳过发送邮件到: {}", to);
            return true;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(to.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("邮件发送成功，收件人: {}", to);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendEmailWithAttachment(String to, String subject, String body,
                                           byte[] attachment, String attachmentName,
                                           String attachmentType) {
        return sendEmailWithAttachment(List.of(to), subject, body,
                attachment, attachmentName, attachmentType);
    }

    @Override
    @Async("emailExecutor")
    public boolean sendEmailWithAttachment(List<String> to, String subject, String body,
                                           byte[] attachment, String attachmentName,
                                           String attachmentType) {
        if (!mailEnabled) {
            log.info("邮件功能已禁用，跳过发送邮件到: {}", to);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getFromAddress());
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(body);

            ByteArrayResource resource = new ByteArrayResource(attachment);
            helper.addAttachment(attachmentName, resource, attachmentType);

            mailSender.send(message);
            log.info("带附件邮件发送成功，收件人: {}, 附件: {}", to, attachmentName);
            return true;
        } catch (Exception e) {
            log.error("带附件邮件发送失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        return sendHtmlEmail(List.of(to), subject, htmlBody);
    }

    @Override
    @Async("emailExecutor")
    public boolean sendHtmlEmail(List<String> to, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.info("邮件功能已禁用，跳过发送邮件到: {}", to);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getFromAddress());
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTML邮件发送成功，收件人: {}", to);
            return true;
        } catch (Exception e) {
            log.error("HTML邮件发送失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async("emailExecutor")
    public boolean sendDashboardReport(List<String> recipients, String subject,
                                       String body, byte[] screenshot,
                                       String dashboardLink, boolean dataTimeout) {
        if (!mailEnabled) {
            log.info("邮件功能已禁用，跳过发送仪表板报告到: {}", recipients);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getFromAddress());
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);

            String htmlContent = buildDashboardReportHtml(body, dashboardLink, dataTimeout);
            helper.setText(htmlContent, true);

            if (!dataTimeout && screenshot != null && screenshot.length > 0) {
                String imageCid = "dashboard-image-" + System.currentTimeMillis();

                MimeBodyPart imagePart = new MimeBodyPart();
                imagePart.setContentID("<" + imageCid + ">");
                imagePart.setDisposition(MimeBodyPart.INLINE);

                DataSource imageSource = new ByteArrayDataSource(
                        new ByteArrayInputStream(screenshot), "image/png");
                imagePart.setDataHandler(new DataHandler(imageSource));
                imagePart.setFileName("dashboard.png");

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(htmlContent.replace("CID_IMAGE", "cid:" + imageCid),
                        "text/html; charset=UTF-8");

                jakarta.mail.Multipart multipart = new jakarta.mail.internet.MimeMultipart("related");
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(imagePart);

                message.setContent(multipart);

                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(
                        new ByteArrayDataSource(screenshot, "image/png")));
                attachmentPart.setFileName("dashboard_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png");
                multipart.addBodyPart(attachmentPart);
            }

            mailSender.send(message);
            log.info("仪表板报告邮件发送成功，收件人: {}, 数据超时: {}", recipients, dataTimeout);
            return true;
        } catch (Exception e) {
            log.error("仪表板报告邮件发送失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async("emailExecutor")
    public void sendNotification(List<String> recipients, String subject,
                                 String message, boolean isSuccess) {
        if (!mailEnabled || recipients == null || recipients.isEmpty()) {
            return;
        }

        String icon = isSuccess ? "✅" : "❌";
        String color = isSuccess ? "#28a745" : "#dc3545";

        String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: %s; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                        <h2 style="margin: 0;">%s %s</h2>
                    </div>
                    <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 8px 8px;">
                        <p style="font-size: 16px; line-height: 1.6; color: #333;">%s</p>
                        <p style="font-size: 12px; color: #999; margin-top: 20px;">
                            发送时间: %s
                        </p>
                    </div>
                </div>
                """.formatted(
                        color,
                        icon,
                        subject,
                        message,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );

        sendHtmlEmail(recipients, subject, htmlBody);
    }

    private String buildDashboardReportHtml(String body, String dashboardLink, boolean dataTimeout) {
        StringBuilder html = new StringBuilder();

        html.append("""
                <div style="font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto;">
                    <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px 8px 0 0;">
                        <h1 style="margin: 0; font-size: 28px;">📊 仪表板定时报告</h1>
                        <p style="margin: 10px 0 0 0; opacity: 0.9;">
                            生成时间: %s
                        </p>
                    </div>
                    <div style="background: #ffffff; padding: 30px; border: 1px solid #e9ecef; border-radius: 0 0 8px 8px;">
                """.formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        if (body != null && !body.isEmpty()) {
            html.append("<div style=\"margin-bottom: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px;\">")
                .append(body.replace("\n", "<br>"))
                .append("</div>");
        }

        if (dataTimeout) {
            html.append("""
                    <div style="background: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                        <h3 style="margin: 0 0 10px 0;">⚠️ 数据加载超时</h3>
                        <p style="margin: 0;">仪表板数据加载超时（超过30秒），请点击下方链接手动查看最新数据。</p>
                    </div>
                    """);
        } else {
            html.append("""
                    <div style="text-align: center; margin: 20px 0;">
                        <img src="CID_IMAGE" alt="Dashboard Screenshot" style="max-width: 100%; border: 1px solid #e9ecef; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    </div>
                    """);
        }

        if (dashboardLink != null && !dashboardLink.isEmpty()) {
            html.append("""
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="display: inline-block; background: #667eea; color: white; padding: 15px 40px; text-decoration: none; border-radius: 50px; font-size: 16px; font-weight: bold;">
                            🔗 查看完整仪表板
                        </a>
                    </div>
                    """.formatted(dashboardLink));
        }

        html.append("""
                    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #e9ecef; color: #999; font-size: 12px;">
                        <p>此邮件由低代码BI平台自动发送，请勿直接回复。</p>
                        <p>如有任何问题，请联系系统管理员。</p>
                    </div>
                </div>
            </div>
            """);

        return html.toString();
    }

    private String getFromAddress() {
        if (fromDisplay != null && !fromDisplay.isEmpty()) {
            return fromDisplay + " <" + fromEmail + ">";
        }
        return fromEmail;
    }
}
