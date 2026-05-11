package br.com.stickerswap.api.profile;

import br.com.stickerswap.api.profile.dto.CepLookupResponse;
import br.com.stickerswap.api.profile.dto.MyProfileResponse;
import br.com.stickerswap.api.profile.dto.PublicProfileResponse;
import br.com.stickerswap.domain.profile.service.ProfileService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProfileService profileService;

    private UUID userId;
    private MockedStatic<AuthenticatedUser> mockedAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        AuthenticatedUser auth = new AuthenticatedUser(userId, "u@e.com", "USER");
        mockedAuth = mockStatic(AuthenticatedUser.class);
        mockedAuth.when(AuthenticatedUser::fromContext).thenReturn(auth);
    }

    @AfterEach
    void tearDown() {
        mockedAuth.close();
    }

    @Test
    @DisplayName("dado usuário autenticado, quando GET /me/profile, então retorna 200 com perfil próprio")
    void givenAuthenticatedUser_whenGetMyProfile_thenReturns200() throws Exception {
        // Arrange
        MyProfileResponse profile = new MyProfileResponse(userId, "nickname", "01310-100", "São Paulo", "SP", null, null, true, true);
        when(profileService.getMyProfile(userId)).thenReturn(profile);

        // Act / Assert
        mockMvc.perform(get("/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("nickname"));
    }

    @Test
    @DisplayName("dado usuário autenticado e payload válido, quando PUT /me/profile, então retorna 200 com perfil atualizado")
    void givenAuthenticatedUserAndValidBody_whenUpdateMyProfile_thenReturns200() throws Exception {
        // Arrange
        MyProfileResponse updated = new MyProfileResponse(userId, "novo-nick", null, null, null, null, null, false, false);
        when(profileService.updateMyProfile(eq(userId), any())).thenReturn(updated);

        // Act / Assert
        mockMvc.perform(put("/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"novo-nick\",\"showCityStatePublicly\":false,\"useLocationForSearch\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("novo-nick"));
    }

    @Test
    @DisplayName("dado CEP válido, quando GET /ceps/{cep}, então retorna 200 com resultado")
    void givenValidCep_whenLookupCep_thenReturns200() throws Exception {
        // Arrange
        CepLookupResponse cepResp = new CepLookupResponse("01310-100", "São Paulo", "SP", true);
        when(profileService.lookupCep("01310-100")).thenReturn(cepResp);

        // Act / Assert
        mockMvc.perform(get("/ceps/{cep}", "01310-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("São Paulo"));
    }

    @Test
    @DisplayName("dado ID de outro usuário, quando GET /users/{userId}/profile, então retorna 200 com perfil público")
    void givenOtherUserId_whenGetPublicProfile_thenReturns200() throws Exception {
        // Arrange
        UUID targetId = UUID.randomUUID();
        PublicProfileResponse pub = new PublicProfileResponse(targetId, "other-nick", "São Paulo", "SP");
        when(profileService.getPublicProfile(targetId)).thenReturn(pub);

        // Act / Assert
        mockMvc.perform(get("/users/{userId}/profile", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("other-nick"));
    }
}
