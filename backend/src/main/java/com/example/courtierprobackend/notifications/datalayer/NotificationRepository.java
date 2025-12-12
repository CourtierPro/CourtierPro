package com.example.courtierprobackend.notifications.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId);

    Optional<Notification> findByPublicId(String publicId);
}
