package org.fyp.minidrivestoragebe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    
    @Async
    public void sendShareNotification(String recipientEmail, String senderName, 
                                      String fileName, String permissionLevel) {
        log.info("=== MOCK EMAIL ===");
        log.info("To: {}", recipientEmail);
        log.info("Subject: {} shared '{}' with you", senderName, fileName);
        log.info("Body: You have been granted {} access to '{}'", permissionLevel, fileName);
        log.info("==================");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted", e);
        }
        
        log.info("Email sent successfully to {}", recipientEmail);
    }
}
