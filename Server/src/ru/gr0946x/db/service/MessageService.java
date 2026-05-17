package ru.gr0946x.db.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0946x.db.dto.MessageDto;
import ru.gr0946x.db.entity.Message;
import ru.gr0946x.db.entity.User;
import ru.gr0946x.db.repository.MessageRepository;

import java.util.List;

/**
 * Сервисный слой для управления публикациями.
 * <p>
 * Отвечает за создание постов, получение ленты пользователя
 * и поиск по содержимому. Все операции выполняются в рамках
 * транзакций для обеспечения целостности данных.
 *
 * @author Маклецов С. В.
 * @see Message
 * @see MessageRepository
 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;

    /**
     * Создаёт экземпляр сервиса с внедрённым репозиторием.
     * <p>
     * Использует конструкторную инъекцию — рекомендуемый способ
     * внедрения зависимостей в Spring для обязательных компонентов.
     *
     * @param messageRepository репозиторий для работы с {@link ru.gr0946x.db.entity.Message}
     */
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Создаёт и сохраняет новую публикацию от имени пользователя.
     * <p>
     * Метод выполняется в транзакции: при успешном завершении
     * пост сохраняется в базе, при ошибке — изменения откатываются.
     *
     * @param author  пользователь-автор публикации
     * @param content текстовое содержимое поста
     * @return сохранённая сущность {@link Message} с присвоенным ID
     * @throws IllegalArgumentException если автор или контент невалидны
     */
    @Transactional
    public Message createMessage(User author, Long receiverId, String content) {
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
        return messageRepository.save(message);
    }

    /**
     * Возвращает ленту публикаций пользователя,
     * отсортированную по дате (новые сверху).
     * <p>
     * Аннотация {@code @Transactional(readOnly = true)} оптимизирует
     * транзакцию только для операций чтения, что повышает производительность.
     *
     * @param author пользователь, чью ленту получаем
     * @return список публикаций (может быть пустым)
     */
//    @Transactional(readOnly = true)
//    public List<Message> getUserFeed(User author) {
//        return messageRepository
//                .findByAuthorOrderByCreatedAtDesc(author);
//    }
//
//    @Transactional(readOnly = true)
//    public List<Message> getUserFeedWithInitializedAuthors(User author) {
//        var posts = postRepository.findByAuthorOrderByCreatedAtDesc(author);
//        // Инициализация прокси: обращение к полю внутри транзакции
//        posts.forEach(post -> post.getAuthor().getNick());
//        return posts;
//    }
//
//    @Transactional(readOnly = true)
//    public List<MessageDto> getUserFeedAsDto(User author) {
//        return postRepository.findByAuthorOrderByCreatedAtDesc(author)
//                .stream()
//                .map(p -> new MessageDto(
//                        p.getId(),
//                        p.getAuthor().getNick(), // безопасно: внутри транзакции
//                        p.getContent(),
//                        p.getCreatedAt()
//                ))
//                .toList();
//    }

    /**
     * Ищет публикации пользователя по фрагменту текста.
     * <p>
     * Поиск выполняется без учёта регистра и поддерживает
     * частичное совпадение слов внутри сообщения.
     *
     * @param author   пользователь, чьи публикации ищем
     * @param fragment искомый фрагмент текста
     * @return список найденных публикаций
     */
    @Transactional(readOnly = true)
    public List<Message> searchPosts(User author, String fragment) {
        return messageRepository
                .searchByContentFragment(fragment, author);
    }
}