package br.com.stickerswap.profile.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 50, message = "must be between 2 and 50 characters") String nickname,
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "must be a valid Brazilian ZIP code (e.g. 01310-100)") String cep,
        @Size(max = 100) String city,
        @Pattern(regexp = "[A-Z]{2}", message = "must be a 2-letter state code (e.g. SP)") String state,
        Boolean showCityStatePublicly,
        Boolean useLocationForSearch
) {}
