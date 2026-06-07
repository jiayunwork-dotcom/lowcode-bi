package com.lowcode.bi.service;

import java.util.List;
import java.util.Map;

public interface EmailService {

    boolean sendEmail(String to, String subject, String body);

    boolean sendEmail(List<String> to, String subject, String body);

    boolean sendEmailWithAttachment(String to, String subject, String body,
                                    byte[] attachment, String attachmentName,
                                    String attachmentType);

    boolean sendEmailWithAttachment(List<String> to, String subject, String body,
                                    byte[] attachment, String attachmentName,
                                    String attachmentType);

    boolean sendHtmlEmail(String to, String subject, String htmlBody);

    boolean sendHtmlEmail(List<String> to, String subject, String htmlBody);

    boolean sendDashboardReport(List<String> recipients, String subject,
                                String body, byte[] screenshot,
                                String dashboardLink, boolean dataTimeout);

    void sendNotification(List<String> recipients, String subject,
                          String message, boolean isSuccess);
}
