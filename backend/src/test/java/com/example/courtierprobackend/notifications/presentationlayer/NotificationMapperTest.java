package com.example.courtierprobackend.notifications.presentationlayer;

import com.example.courtierprobackend.notifications.datalayer.Notification;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
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

    @Test
    void toResponseDTO_withNullInput_returnsNull() {
        assertThat(mapper.toResponseDTO(null)).isNull();
    }

    @Test
    void toResponseList_withNullInput_returnsNull() {
        assertThat(mapper.toResponseList(null)).isNull();
    }

    @Test
    void toResponseList_shouldMapAllItems() {
        Notification first = Notification.builder()
                .publicId(UUID.randomUUID().toString())
                .title("Title 1")
                .message("Message 1")
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .recipientId("auth0|1")
                .build();
        Notification second = Notification.builder()
                .publicId(UUID.randomUUID().toString())
                .title("Title 2")
                .message("Message 2")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .recipientId("auth0|2")
                .build();

        List<NotificationResponseDTO> result = mapper.toResponseList(List.of(first, second));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Title 1");
        assertThat(result.get(1).isRead()).isFalse();
    }
}
