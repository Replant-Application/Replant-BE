package com.app.replant.domain.notification.repository;

import com.app.replant.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND (:isRead IS NULL OR n.isRead = :isRead)")
    Page<Notification> findByUserIdAndIsRead(@Param("userId") Long userId, @Param("isRead") Boolean isRead, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.id = :notificationId AND n.user.id = :userId")
    Optional<Notification> findByIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);
}
