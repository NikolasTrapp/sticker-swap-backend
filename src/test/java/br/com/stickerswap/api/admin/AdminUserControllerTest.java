package br.com.stickerswap.api.admin;

import br.com.stickerswap.api.admin.dto.AdminUserResponse;
import br.com.stickerswap.domain.identity.service.AdminUserService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminUserService adminUserService;

    @Test
    void listUsers_ReturnsOk() throws Exception {
        when(adminUserService.listUsers(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void blockUser_ReturnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser adminUser = new AuthenticatedUser(adminId, "admin@test.com", "ADMIN");

        try (var mockedStatic = mockStatic(AuthenticatedUser.class)) {
            mockedStatic.when(AuthenticatedUser::fromContext).thenReturn(adminUser);
            when(adminUserService.blockUser(eq(adminId), eq(userId))).thenReturn(mock(AdminUserResponse.class));

            mockMvc.perform(patch("/admin/users/" + userId + "/block"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void unblockUser_ReturnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.unblockUser(userId)).thenReturn(mock(AdminUserResponse.class));

        mockMvc.perform(patch("/admin/users/" + userId + "/unblock"))
                .andExpect(status().isOk());
    }
}
