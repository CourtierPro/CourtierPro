package com.example.courtierprobackend.audit.systemalert.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SystemAlertRepository extends JpaRepository<SystemAlert, Long> {
    List<SystemAlert> findByActiveTrueOrderByCreatedAtDesc();
}