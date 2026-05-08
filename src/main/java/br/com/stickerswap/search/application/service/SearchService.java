package br.com.stickerswap.search.application.service;

import br.com.stickerswap.search.application.dto.HolderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface SearchService {

    Page<HolderResponse> findHolders(UUID searcherId, UUID albumId, UUID stickerId,
                                     Set<UUID> excludedUserIds, Pageable pageable);
}
