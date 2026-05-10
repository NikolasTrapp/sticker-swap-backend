package br.com.stickerswap.domain.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(length = 50)
    private String nickname;

    @Column(length = 9)
    private String cep;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(precision = 10, scale = 7)
    private BigDecimal approximateLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal approximateLongitude;

    @Column(nullable = false)
    private boolean showCityStatePublicly = false;

    @Column(nullable = false)
    private boolean useLocationForSearch = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserProfile forUser(UUID userId) {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        return p;
    }
}
