package br.com.stickerswap.profile.infrastructure.web;

import br.com.stickerswap.profile.application.dto.MyProfileResponse;
import br.com.stickerswap.profile.application.dto.PublicProfileResponse;
import br.com.stickerswap.profile.application.dto.UpdateProfileRequest;
import br.com.stickerswap.profile.application.service.ProfileService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Profile", description = "User profile management")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me/profile")
    @Operation(summary = "Get own profile (includes private fields)")
    @SecurityRequirement(name = "bearerAuth")
    public MyProfileResponse getMyProfile() {
        return profileService.getMyProfile(AuthenticatedUser.fromContext().id());
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update own profile")
    @SecurityRequirement(name = "bearerAuth")
    public MyProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateMyProfile(AuthenticatedUser.fromContext().id(), request);
    }

    @GetMapping("/users/{userId}/profile")
    @Operation(summary = "Get public profile of another user")
    @SecurityRequirement(name = "bearerAuth")
    public PublicProfileResponse getPublicProfile(@PathVariable UUID userId) {
        return profileService.getPublicProfile(userId);
    }
}
