package br.com.stickerswap.domain.collection.service;

import br.com.stickerswap.api.collection.dto.RepeatedStickerResponse;
import br.com.stickerswap.api.collection.dto.SetRepeatedStickerRequest;
import br.com.stickerswap.api.collection.dto.WantedStickerResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionService {

    List<RepeatedStickerResponse> listRepeated(UUID userId, UUID albumId);

    RepeatedStickerResponse setRepeated(UUID userId, UUID stickerId, SetRepeatedStickerRequest req);

    void deleteRepeated(UUID userId, UUID stickerId);

    List<WantedStickerResponse> listWanted(UUID userId, UUID albumId);

    WantedStickerResponse setWanted(UUID userId, UUID stickerId);

    void deleteWanted(UUID userId, UUID stickerId);
}
