package br.com.stickerswap.api.notification;

import br.com.stickerswap.api.notification.dto.NotificationResponse;
import br.com.stickerswap.domain.notification.service.NotificationService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "User notification management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List recent notifications (top 30)")
    public List<NotificationResponse> listNotifications() {
        UUID userId = AuthenticatedUser.fromContext().id();
        return notificationService.listRecent(userId);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    public Map<String, Long> unreadCount() {
        UUID userId = AuthenticatedUser.fromContext().id();
        return Map.of("count", notificationService.countUnread(userId));
    }

    @PutMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a single notification as read")
    public void markAsRead(@PathVariable UUID notificationId) {
        UUID userId = AuthenticatedUser.fromContext().id();
        notificationService.markAsRead(userId, notificationId);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all notifications as read")
    public void markAllAsRead() {
        UUID userId = AuthenticatedUser.fromContext().id();
        notificationService.markAllAsRead(userId);
    }
}
