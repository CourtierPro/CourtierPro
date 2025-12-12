package com.example.courtierprobackend.notifications.presentationlayer;

import com.example.courtierprobackend.notifications.datalayer.Notification;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void toResponseDTO_shouldMapIsReadToRead() {
        // Arrange
        Notification notification = Notification.builder()
                .publicId(UUID.randomUUID().toString())
                .title("Test Title")
                .message("Test Message")
                .isRead(true) // Entity field is 'isRead'
                .createdAt(LocalDateTime.now())
                .recipientId("auth0|123")
                .build();

        // Act
        NotificationResponseDTO dto = mapper.toResponseDTO(notification);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.isRead()).isTrue(); // DTO field 'read' getter is 'isRead()'
        assertThat(dto.getPublicId()).isEqualTo(notification.getPublicId());
    }

    @Test
    void toResponseDTO_shouldMapIsReadFalse() {
        // Arrange
        Notification notification = Notification.builder()
                .publicId(UUID.randomUUID().toString())
                .title("Test Title")
                .message("Test Message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .recipientId("auth0|123")
                .build();

        // Act
        NotificationResponseDTO dto = mapper.toResponseDTO(notification);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.isRead()).isFalse();
    }
}
