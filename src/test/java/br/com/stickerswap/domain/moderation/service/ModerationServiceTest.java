package br.com.stickerswap.domain.moderation.service;

import br.com.stickerswap.domain.moderation.model.ReportReason;
import br.com.stickerswap.domain.moderation.model.UserBlock;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.moderation.UserBlockRepository;
import br.com.stickerswap.infrastructure.repository.moderation.UserReportRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.api.moderation.dto.ReportRequest;
import br.com.stickerswap.api.moderation.dto.ReportResponse;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock UserBlockRepository blockRepo;
    @Mock UserReportRepository reportRepo;
    @Mock UserProfileRepository profileRepo;

    @InjectMocks ModerationServiceImpl moderationService;

    @Test
    void blockUser_throwsBusinessRule_whenSelf() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> moderationService.blockUser(userId, userId))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(blockRepo);
    }

    @Test
    void blockUser_savesBlock_whenNotAlreadyBlocked() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);
        when(blockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        moderationService.blockUser(blockerId, blockedId);

        ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
        verify(blockRepo).save(captor.capture());
        assertThat(captor.getValue().getBlockerId()).isEqualTo(blockerId);
        assertThat(captor.getValue().getBlockedId()).isEqualTo(blockedId);
    }

    @Test
    void blockUser_isIdempotent_whenAlreadyBlocked() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(true);

        moderationService.blockUser(blockerId, blockedId);

        verify(blockRepo, never()).save(any());
    }

    @Test
    void unblockUser_throwsNotFound_whenNotBlocked() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);

        assertThatThrownBy(() -> moderationService.unblockUser(blockerId, blockedId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listBlockedUsers_returnsProfilesForBlockedUsers() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();

        UserBlock block = new UserBlock();
        block.setBlockerId(blockerId);
        block.setBlockedId(blockedId);

        UserProfile profile = UserProfile.forUser(blockedId);
        profile.setNickname("colecionador");
        profile.setCity("Blumenau");
        profile.setState("SC");
        profile.setShowCityStatePublicly(true);

        when(blockRepo.findByBlockerIdOrderByCreatedAtDesc(blockerId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(block)));
        when(profileRepo.findByUserIdIn(Set.of(blockedId))).thenReturn(List.of(profile));

        var page = moderationService.listBlockedUsers(blockerId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).userId()).isEqualTo(blockedId);
        assertThat(page.getContent().get(0).nickname()).isEqualTo("colecionador");
        assertThat(page.getContent().get(0).city()).isEqualTo("Blumenau");
        assertThat(page.getContent().get(0).state()).isEqualTo("SC");
    }

    @Test
    void getMutuallyBlockedIds_combinesBothDirections() {
        UUID userId = UUID.randomUUID();
        UUID blockedByUser = UUID.randomUUID();
        UUID blockerOfUser = UUID.randomUUID();

        when(blockRepo.findBlockedIdsByBlockerId(userId)).thenReturn(List.of(blockedByUser));
        when(blockRepo.findBlockerIdsByBlockedId(userId)).thenReturn(List.of(blockerOfUser));

        Set<UUID> result = moderationService.getMutuallyBlockedIds(userId);

        assertThat(result).containsExactlyInAnyOrder(blockedByUser, blockerOfUser);
    }

    @Test
    void getMutuallyBlockedIds_deduplicates_overlappingBlocks() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        // Both blocked each other
        when(blockRepo.findBlockedIdsByBlockerId(userId)).thenReturn(List.of(otherId));
        when(blockRepo.findBlockerIdsByBlockedId(userId)).thenReturn(List.of(otherId));

        Set<UUID> result = moderationService.getMutuallyBlockedIds(userId);

        assertThat(result).hasSize(1).contains(otherId);
    }

    @Test
    void isBlocked_returnsTrueWhenUserBlockedOther() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(a, b)).thenReturn(true);

        assertThat(moderationService.isBlocked(a, b)).isTrue();
    }

    @Test
    void isBlocked_returnsTrueWhenOtherBlockedUser() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(a, b)).thenReturn(false);
        when(blockRepo.existsByBlockerIdAndBlockedId(b, a)).thenReturn(true);

        assertThat(moderationService.isBlocked(a, b)).isTrue();
    }

    @Test
    void isBlocked_returnsFalseWhenNeitherBlocked() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(blockRepo.existsByBlockerIdAndBlockedId(a, b)).thenReturn(false);
        when(blockRepo.existsByBlockerIdAndBlockedId(b, a)).thenReturn(false);

        assertThat(moderationService.isBlocked(a, b)).isFalse();
    }

    @Test
    void reportUser_throwsBusinessRule_whenSelf() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> moderationService.reportUser(userId, userId,
                new ReportRequest(ReportReason.SPAM, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reportUser_persistsAndReturnsResponse() {
        UUID reporterId = UUID.randomUUID();
        UUID reportedId = UUID.randomUUID();
        when(reportRepo.save(any())).thenAnswer(inv -> {
            var r = inv.getArgument(0, br.com.stickerswap.domain.moderation.model.UserReport.class);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReportResponse resp = moderationService.reportUser(reporterId, reportedId,
                new ReportRequest(ReportReason.HARASSMENT, "mensagens ofensivas"));

        assertThat(resp.reporterId()).isEqualTo(reporterId);
        assertThat(resp.reportedId()).isEqualTo(reportedId);
        assertThat(resp.reason()).isEqualTo(ReportReason.HARASSMENT);
        assertThat(resp.description()).isEqualTo("mensagens ofensivas");
    }
}
