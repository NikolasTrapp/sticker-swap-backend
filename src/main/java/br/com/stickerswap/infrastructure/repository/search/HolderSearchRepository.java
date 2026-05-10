package br.com.stickerswap.infrastructure.repository.search;

import br.com.stickerswap.api.search.dto.HolderResponse;
import br.com.stickerswap.domain.search.model.HolderSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HolderSearchRepository {
    Page<HolderResponse> findHolders(HolderSearchCriteria criteria, Pageable pageable);
}
