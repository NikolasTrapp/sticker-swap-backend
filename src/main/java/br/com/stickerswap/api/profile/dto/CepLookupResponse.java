package br.com.stickerswap.api.profile.dto;

import br.com.stickerswap.domain.profile.service.CepGeocodeService;

public record CepLookupResponse(
        String cep,
        String city,
        String state,
        boolean found
) {
    public static CepLookupResponse found(String cep, CepGeocodeService.CepLocation location) {
        return new CepLookupResponse(cep, location.city(), location.state(), true);
    }

    public static CepLookupResponse notFound(String cep) {
        return new CepLookupResponse(cep, null, null, false);
    }
}
