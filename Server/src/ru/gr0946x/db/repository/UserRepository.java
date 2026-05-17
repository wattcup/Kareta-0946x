package ru.gr0946x.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gr0946x.db.entity.User;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link User} в базе данных.
 * <p>
 * Наследует стандартные CRUD-операции от {@link JpaRepository}:
 * {@code save()}, {@code findById()}, {@code findAll()}, {@code delete()},
 * а также методы пагинации и сортировки.
 * <p>
 * Spring Data JPA автоматически реализует этот интерфейс во время выполнения,
 * генерируя необходимые SQL-запросы на основе имён методов.
 *
 * @author ВашИмя
 * @version 1.0
 * @see JpaRepository
 * @see ru.smak.db.service.UserService
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Находит пользователя по псевдониму без учёта регистра символов.
     * <p>
     * Spring Data JPA автоматически формирует запрос с {@code UPPER()} или
     * {@code ILIKE} (в зависимости от диалекта БД) для
     * регистронезависимого сравнения.
     *
     * @param nick псевдоним для поиска
     * @return {@link Optional} с найденным пользователем или
     *         {@link Optional#empty()}, если пользователь не найден
     */
    Optional<User> findByNickIgnoreCase(String nick);

    /**
     * Проверяет существование пользователя с заданным псевдонимом
     * без учёта регистра символов.
     * <p>
     * Оптимизировано для быстрой проверки: генерирует запрос
     * {@code SELECT EXISTS(...)} или {@code SELECT COUNT(...) > 0},
     * не загружая полную сущность из базы данных.
     *
     * @param nick псевдоним для проверки
     * @return {@code true}, если пользователь с таким ником уже существует
     */
    boolean existsByNickIgnoreCase(String nick);
}