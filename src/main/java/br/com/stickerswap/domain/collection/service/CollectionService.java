package br.com.stickerswap.domain.collection.service;

import br.com.stickerswap.api.collection.dto.CollectionFilter;
import br.com.stickerswap.api.collection.dto.CollectionStickerResponse;
import br.com.stickerswap.api.collection.dto.RepeatedStickerResponse;
import br.com.stickerswap.api.collection.dto.SetRepeatedStickerRequest;
import br.com.stickerswap.api.collection.dto.WantedStickerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CollectionService {

    Page<CollectionStickerResponse> listCollection(UUID userId, UUID albumId, String query, CollectionFilter filter, Pageable pageable);

    List<RepeatedStickerResponse> listRepeated(UUID userId, UUID albumId);

    RepeatedStickerResponse setRepeated(UUID userId, UUID stickerId, SetRepeatedStickerRequest req);

    void deleteRepeated(UUID userId, UUID stickerId);

    List<WantedStickerResponse> listWanted(UUID userId, UUID albumId);

    WantedStickerResponse setWanted(UUID userId, UUID stickerId);

    void deleteWanted(UUID userId, UUID stickerId);
}
