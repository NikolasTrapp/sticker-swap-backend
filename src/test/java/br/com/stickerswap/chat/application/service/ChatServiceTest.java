package br.com.stickerswap.chat.application.service;

import br.com.stickerswap.album.domain.model.Sticker;
import br.com.stickerswap.album.infrastructure.persistence.StickerRepository;
import br.com.stickerswap.chat.application.dto.ConversationResponse;
import br.com.stickerswap.chat.application.dto.MessageResponse;
import br.com.stickerswap.chat.domain.model.ChatConversation;
import br.com.stickerswap.chat.domain.model.MessageType;
import br.com.stickerswap.chat.infrastructure.persistence.ChatConversationRepository;
import br.com.stickerswap.chat.infrastructure.persistence.ChatMessageRepository;
import br.com.stickerswap.moderation.application.service.ModerationService;
import br.com.stickerswap.profile.infrastructure.persistence.UserProfileRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatConversationRepository conversationRepo;
    @Mock ChatMessageRepository messageRepo;
    @Mock StickerRepository stickerRepo;
    @Mock UserProfileRepository profileRepo;
    @Mock ModerationService moderationService;

    @InjectMocks ChatServiceImpl chatService;

    static final UUID STICKER_ID = UUID.randomUUID();

    @Test
    void throwsBusinessRule_whenSeekerEqualsHolder() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> chatService.expressInterest(userId, STICKER_ID, userId))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(conversationRepo, stickerRepo);
    }

    @Test
    void normalizePair_smallerUUIDBecomesUserAId() {
        // Force known ordering: A < B
        UUID idA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID idB = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Sticker sticker = sticker(STICKER_ID, "1", "Neymar");
        when(stickerRepo.findByIdAndActive(STICKER_ID, true)).thenReturn(Optional.of(sticker));
        when(conversationRepo.findByUserAIdAndUserBIdAndStickerId(idA, idB, STICKER_ID))
                .thenReturn(Optional.empty());
        when(conversationRepo.save(any())).thenAnswer(inv -> {
            ChatConversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Seeker is B; from B's perspective the "other" is A
        when(profileRepo.findByUserId(idA)).thenReturn(Optional.empty());

        // Seeker is B (larger), holder is A (smaller) — pair should still be (A, B)
        ConversationResponse resp = chatService.expressInterest(idB, STICKER_ID, idA);

        ArgumentCaptor<ChatConversation> captor = ArgumentCaptor.forClass(ChatConversation.class);
        verify(conversationRepo).save(captor.capture());
        assertThat(captor.getValue().getUserAId()).isEqualTo(idA);
        assertThat(captor.getValue().getUserBId()).isEqualTo(idB);
        // otherUserId from B's perspective = A
        assertThat(resp.otherUserId()).isEqualTo(idA);
    }

    @Test
    void injectsSystemIntentMessage_onFirstContact() {
        UUID seekerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID holderId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        when(stickerRepo.findByIdAndActive(STICKER_ID, true))
                .thenReturn(Optional.of(sticker(STICKER_ID, "1", "Neymar")));
        when(conversationRepo.findByUserAIdAndUserBIdAndStickerId(seekerId, holderId, STICKER_ID))
                .thenReturn(Optional.empty());
        when(conversationRepo.save(any())).thenAnswer(inv -> {
            ChatConversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepo.findByUserId(holderId)).thenReturn(Optional.empty());

        chatService.expressInterest(seekerId, STICKER_ID, holderId);

        verify(messageRepo).save(argThat(m -> m.getType() == MessageType.SYSTEM_INTENT
                && m.getSenderUserId() == null));
    }

    @Test
    void returnsExistingConversation_withoutNewMessage() {
        UUID seekerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID holderId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Sticker sticker = sticker(STICKER_ID, "1", "Neymar");
        when(stickerRepo.findByIdAndActive(STICKER_ID, true)).thenReturn(Optional.of(sticker));

        ChatConversation existing = new ChatConversation();
        existing.setId(UUID.randomUUID());
        existing.setUserAId(seekerId);
        existing.setUserBId(holderId);
        existing.setStickerId(STICKER_ID);
        when(conversationRepo.findByUserAIdAndUserBIdAndStickerId(seekerId, holderId, STICKER_ID))
                .thenReturn(Optional.of(existing));
        when(profileRepo.findByUserId(holderId)).thenReturn(Optional.empty());

        ConversationResponse resp = chatService.expressInterest(seekerId, STICKER_ID, holderId);

        assertThat(resp.conversationId()).isEqualTo(existing.getId());
        verify(conversationRepo, never()).save(any());
        verify(messageRepo, never()).save(any());
    }

    @Test
    void sendMessage_throwsBusinessRule_whenBlocked() {
        UUID senderId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ChatConversation conv = new ChatConversation();
        conv.setId(convId);
        conv.setUserAId(senderId);
        conv.setUserBId(otherId);

        when(conversationRepo.findById(convId)).thenReturn(Optional.of(conv));
        when(moderationService.isBlocked(senderId, otherId)).thenReturn(true);

        assertThatThrownBy(() -> chatService.sendMessage(senderId, convId, "oi"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sendMessage_throwsResourceNotFound_forNonParticipant() {
        UUID senderId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ChatConversation conv = new ChatConversation();
        conv.setId(convId);
        conv.setUserAId(UUID.randomUUID());
        conv.setUserBId(UUID.randomUUID());

        when(conversationRepo.findById(convId)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> chatService.sendMessage(senderId, convId, "oi"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendMessage_persistsTextMessage_andReturnsResponse() {
        UUID senderId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ChatConversation conv = new ChatConversation();
        conv.setId(convId);
        conv.setUserAId(senderId);
        conv.setUserBId(otherId);

        when(conversationRepo.findById(convId)).thenReturn(Optional.of(conv));
        when(moderationService.isBlocked(senderId, otherId)).thenReturn(false);
        when(messageRepo.save(any())).thenAnswer(inv -> {
            var m = inv.getArgument(0, br.com.stickerswap.chat.domain.model.ChatMessage.class);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(conversationRepo.save(any())).thenReturn(conv);

        MessageResponse resp = chatService.sendMessage(senderId, convId, "olá!");

        assertThat(resp.type()).isEqualTo(MessageType.TEXT);
        assertThat(resp.body()).isEqualTo("olá!");
        assertThat(resp.senderUserId()).isEqualTo(senderId);
    }

    private Sticker sticker(UUID id, String number, String name) {
        Sticker s = new Sticker();
        s.setId(id);
        s.setNumber(number);
        s.setName(name);
        return s;
    }
}
