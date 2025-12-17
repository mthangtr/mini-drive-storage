package org.fyp.minidrivestoragebe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email notification service (Mock implementation)
 * In production, integrate with actual email provider (SendGrid, AWS SES, etc.)
 */
@Service
@Slf4j
public class EmailService {
    
    /**
     * Send share notification email asynchronously
     */
    @Async
    public void sendShareNotification(String recipientEmail, String senderName, 
                                      String fileName, String permissionLevel) {
        // Mock implementation - log instead of sending actual email
        log.info("=== MOCK EMAIL ===");
        log.info("To: {}", recipientEmail);
        log.info("Subject: {} shared '{}' with you", senderName, fileName);
        log.info("Body: You have been granted {} access to '{}'", permissionLevel, fileName);
        log.info("==================");
        
        // Simulate email sending delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted", e);
        }
        
        log.info("Email sent successfully to {}", recipientEmail);
    }
    
    /**
     * Send generic notification email
     */
    @Async
    public void sendEmail(String to, String subject, String body) {
        log.info("=== MOCK EMAIL ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        log.info("==================");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted", e);
        }
    }
}
