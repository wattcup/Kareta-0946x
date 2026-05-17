package ru.gr0946x.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gr0946x.db.entity.Message;
import ru.gr0946x.db.entity.User;

import java.util.List;

/**
 * Репозиторий для работы с публикациями.
 * <p>
 * Наследует стандартные CRUD-операции от {@link JpaRepository}
 * и добавляет специфичные методы выборки для социальной сети.
 * <p>
 * Spring Data JPA автоматически реализует этот интерфейс во время выполнения,
 * генерируя необходимые SQL-запросы на основе имён методов и аннотаций.
 *
 * @author Маклецов С. В.
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Возвращает все публикации конкретного автора,
     * отсортированные по дате создания (новые сверху).
     * <p>
     * Генерирует запрос:
     * {@code SELECT p FROM Post p WHERE p.author = ?1 ORDER BY p.createdAt DESC}
     *
     * @param author пользователь-автор публикаций
     * @return список постов, отсортированный по убыванию даты
     */
    List<Message> findByAuthorOrderByCreatedAtDesc(User author);

    /**
     * Ищет публикации автора, содержащие заданный фрагмент текста.
     * <p>
     * Использует кастомный JPQL-запрос с оператором {@code LIKE}
     * для полнотекстового поиска без учёта регистра.
     * <p>
     * Параметры запроса передаются через аннотацию {@code @Param},
     * что обеспечивает безопасность от SQL-инъекций.
     *
     * @param fragment искомый фрагмент текста
     * @param author   пользователь, чьи публикации ищем
     * @return список найденных публикаций (может быть пустым)
     */
//    @Query("SELECT p FROM Post p WHERE p.content " +
//            "LIKE %:fragment% AND p.author = :author")
//    List<Message> searchByContentFragment(
//            @Param("fragment") String fragment,
//            @Param("author") User author
//    ); надо удалить

    //Метод получения истории чата между двумя пользователями:
    @Query("SELECT m FROM Message m WHERE (m.author.id = :user1Id AND m.receiverId = :user2Id) " +
            "OR (m.author.id = :user2Id AND m.receiverId = :user1Id) ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    //Метод поиска по фрагменту текста внутри конкретного чата (Пункт 10 задания):
    @Query("SELECT m FROM Message m WHERE ((m.author.id = :user1Id AND m.receiverId = :user2Id) " +
            "OR (m.author.id = :user2Id AND m.receiverId = :user1Id)) AND LOWER(m.content) " +
            "LIKE LOWER(CONCAT('%', :fragment, '%')) ORDER BY m.createdAt ASC")
    List<Message> searchInChat(
            @Param("user1Id") Long user1Id,
            @Param("user2Id") Long user2Id,
            @Param("fragment") String fragment
    );
}