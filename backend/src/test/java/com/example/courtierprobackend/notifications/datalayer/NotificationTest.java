package com.example.courtierprobackend.notifications.datalayer;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void prePersist_SetsPublicIdAndCreatedAt_WhenNull() {
        Notification notification = new Notification();
        notification.prePersist();

        assertThat(notification.getPublicId()).isNotNull();
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void prePersist_DoesNotOverwrite_WhenfieldsPresent() {
        String existingId = "existing-id";
        LocalDateTime existingDate = LocalDateTime.now().minusDays(1);
        
        Notification notification = new Notification();
        notification.setPublicId(existingId);
        notification.setCreatedAt(existingDate);
        
        notification.prePersist();

        assertThat(notification.getPublicId()).isEqualTo(existingId);
        assertThat(notification.getCreatedAt()).isEqualTo(existingDate);
    }
    
    @Test
    void builder_WorksCorrectly() {
        Notification n = Notification.builder()
            .title("Title")
            .message("Message")
            .build();
            
        assertThat(n.getTitle()).isEqualTo("Title");
        assertThat(n.getMessage()).isEqualTo("Message");
    }
}
