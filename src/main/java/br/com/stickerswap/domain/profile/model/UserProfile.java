package br.com.stickerswap.domain.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
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

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static UserProfile forUser(UUID userId) {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        return p;
    }
}
