package com.example.courtierprobackend.notifications.presentationlayer;

import com.example.courtierprobackend.notifications.datalayer.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponseDTO toResponseDTO(Notification notification);

    List<NotificationResponseDTO> toResponseList(List<Notification> notifications);
}
