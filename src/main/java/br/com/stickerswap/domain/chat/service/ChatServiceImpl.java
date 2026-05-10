package br.com.stickerswap.domain.chat.service;

import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.api.chat.dto.ConversationResponse;
import br.com.stickerswap.api.chat.dto.MessageResponse;
import br.com.stickerswap.domain.chat.model.ChatConversation;
import br.com.stickerswap.domain.chat.model.ChatMessage;
import br.com.stickerswap.domain.chat.model.MessageType;
import br.com.stickerswap.infrastructure.repository.chat.ChatConversationRepository;
import br.com.stickerswap.infrastructure.repository.chat.ChatMessageRepository;
import br.com.stickerswap.domain.moderation.service.ModerationService;
import br.com.stickerswap.domain.notification.service.NotificationService;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final StickerRepository stickerRepo;
    private final UserProfileRepository profileRepo;
    private final ModerationService moderationService;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public ConversationResponse expressInterest(UUID seekerId, UUID stickerId, UUID holderId) {
        if (seekerId.equals(holderId)) {
            throw new BusinessRuleException("Você não pode iniciar uma conversa consigo mesmo.");
        }

        Sticker sticker = stickerRepo.findByIdAndActive(stickerId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker", stickerId));

        // Normalize pair: smaller UUID → userAId
        UUID userAId = seekerId.compareTo(holderId) < 0 ? seekerId : holderId;
        UUID userBId = seekerId.compareTo(holderId) < 0 ? holderId : seekerId;

        Optional<ChatConversation> existing = conversationRepo
                .findByUserAIdAndUserBIdAndStickerId(userAId, userBId, stickerId);

        ChatConversation conversation;
        if (existing.isPresent()) {
            conversation = existing.get();
        } else {
            conversation = new ChatConversation();
            conversation.setUserAId(userAId);
            conversation.setUserBId(userBId);
            conversation.setStickerId(stickerId);
            conversation = conversationRepo.save(conversation);

            ChatMessage intent = new ChatMessage();
            intent.setConversationId(conversation.getId());
            intent.setSenderUserId(null);
            intent.setType(MessageType.SYSTEM_INTENT);
            intent.setBody("Um usuário demonstrou interesse em trocar pela figurinha " +
                           sticker.getCode() + " — " + sticker.getName() + ".");
            messageRepo.save(intent);

            notificationService.notifyInterest(holderId, seekerId, conversation.getId(), stickerId);
        }

        UUID otherUserId = seekerId.equals(userAId) ? userBId : userAId;
        String otherNickname = profileRepo.findByUserId(otherUserId)
                .map(UserProfile::getNickname).orElse(null);

        return toResponse(conversation, otherUserId, otherNickname, sticker);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ConversationResponse> listConversations(UUID userId) {
        List<ChatConversation> conversations = conversationRepo.findAllForUser(userId);
        if (conversations.isEmpty()) return List.of();

        Set<UUID> stickerIds = conversations.stream()
                .map(ChatConversation::getStickerId).collect(Collectors.toSet());
        Set<UUID> otherUserIds = conversations.stream()
                .map(c -> c.getUserAId().equals(userId) ? c.getUserBId() : c.getUserAId())
                .collect(Collectors.toSet());

        Map<UUID, Sticker> stickers = stickerRepo.findAllById(stickerIds).stream()
                .collect(Collectors.toMap(Sticker::getId, s -> s));
        Map<UUID, String> nicknames = profileRepo.findByUserIdIn(otherUserIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getNickname));

        return conversations.stream().map(c -> {
            UUID otherUserId = c.getUserAId().equals(userId) ? c.getUserBId() : c.getUserAId();
            return toResponse(c, otherUserId, nicknames.get(otherUserId), stickers.get(c.getStickerId()));
        }).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<MessageResponse> listMessages(UUID userId, UUID conversationId, Pageable pageable) {
        ChatConversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        requireParticipant(userId, conversation);
        return messageRepo.findByConversationIdOrderBySentAtAsc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    @Override
    public MessageResponse sendMessage(UUID senderId, UUID conversationId, String body) {
        ChatConversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        requireParticipant(senderId, conversation);
        UUID otherId = conversation.getUserAId().equals(senderId)
                ? conversation.getUserBId() : conversation.getUserAId();
        if (moderationService.isBlocked(senderId, otherId)) {
            throw new BusinessRuleException("Não é possível enviar mensagens para um usuário bloqueado.");
        }

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderUserId(senderId);
        msg.setType(MessageType.TEXT);
        msg.setBody(body);
        msg = messageRepo.save(msg);

        // Touch updatedAt so the conversation rises to the top of listings
        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepo.save(conversation);

        notificationService.notifyMessage(otherId, senderId, conversationId, conversation.getStickerId());

        return toMessageResponse(msg);
    }

    private void requireParticipant(UUID userId, ChatConversation c) {
        if (!c.getUserAId().equals(userId) && !c.getUserBId().equals(userId)) {
            throw new ResourceNotFoundException("Conversation", c.getId());
        }
    }

    private ConversationResponse toResponse(ChatConversation c, UUID otherUserId,
                                             String otherNickname, Sticker sticker) {
        return new ConversationResponse(
                c.getId(), otherUserId, otherNickname,
                sticker != null ? sticker.getId() : c.getStickerId(),
                sticker != null ? sticker.getCode() : null,
                sticker != null ? sticker.getName() : null,
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(ChatMessage m) {
        return new MessageResponse(m.getId(), m.getConversationId(),
                m.getSenderUserId(), m.getType(), m.getBody(), m.getSentAt());
    }
}
