package ru.gr0946x.db.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0946x.db.dto.MessageDto;
import ru.gr0946x.db.entity.Message;
import ru.gr0946x.db.entity.User;
import ru.gr0946x.db.repository.MessageRepository;

import java.util.List;


@Service
public class MessageService {

    private final MessageRepository messageRepository;


    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Message createMessage(User author, Long receiverId, String content, boolean isReceived) {
        if (author == null || author.getId() == null) {
            throw new IllegalArgumentException(
                    "Автор должен быть сохранён в БД"
            );
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Содержимое поста не может быть пустым"
            );
        }
        Message message = new Message(author, receiverId, content);
        message.setReceived(isReceived);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getChatHistory(User user1, Long user2) {
        List<Message> messages = messageRepository.findChatHistory(user1.getId(), user2);
        return messages.stream()
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getAuthor().getNick(),
                        m.getReceiverId(),
                        m.getContent(),
                        m.getCreatedAt(),
                        m.isReceived()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageDto> searchMessagesInChat(User user1, Long user2, String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return List.of();
        }
        List<Message> messages = messageRepository.searchInChat(user1.getId(), user2, fragment);
        return messages.stream()
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getAuthor().getNick(),
                        m.getReceiverId(),
                        m.getContent(),
                        m.getCreatedAt(),
                        m.isReceived()
                ))
                .toList();
    }

    @Transactional
    public void markAsRead(Long authorId, Long receiverId) {
        messageRepository.markAsRead(authorId, receiverId);
    }
}