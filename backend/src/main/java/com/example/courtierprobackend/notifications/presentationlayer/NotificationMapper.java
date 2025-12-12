package com.example.courtierprobackend.notifications.presentationlayer;

import com.example.courtierprobackend.notifications.datalayer.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @org.mapstruct.Mapping(target = "isRead", source = "read")
    NotificationResponseDTO toResponseDTO(Notification notification);

    List<NotificationResponseDTO> toResponseList(List<Notification> notifications);
}
