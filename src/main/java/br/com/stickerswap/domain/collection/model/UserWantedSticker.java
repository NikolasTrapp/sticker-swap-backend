package br.com.stickerswap.domain.collection.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_wanted_stickers")
@Getter
@Setter
@NoArgsConstructor
public class UserWantedSticker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Column(name = "sticker_id", nullable = false)
    private UUID stickerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
