package br.com.stickerswap.domain.search.service;

import br.com.stickerswap.api.search.dto.HolderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface SearchService {

    Page<HolderResponse> findHolders(UUID searcherId, UUID albumId, UUID stickerId,
                                     Set<UUID> excludedUserIds, Pageable pageable);
}
