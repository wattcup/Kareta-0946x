package ru.gr0946x.db.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0946x.db.entity.User;
import ru.gr0946x.db.repository.UserRepository;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean isNickTaken(String nick) {
        return userRepository.existsByNickIgnoreCase(nick);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByNick(String nick) {
        return userRepository.findByNickIgnoreCase(nick);
    }

    @Transactional
    public User register(String nick, String password) {
        if (nick == null || nick.isBlank() || !Character.isLetter(nick.charAt(0))) {
            throw new IllegalArgumentException("Имя пользователя должно начинаться с буквы");
        }
        var existing = userRepository.existsByNickIgnoreCase(nick);
        if (existing) {
            throw new IllegalArgumentException("Ник " + nick + " уже занят");
        }
        var passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(nick, passwordHash);
        return userRepository.save(newUser);
    }

    @Transactional
    public User login(String nick, String password) {
        Optional<User> existing = userRepository.findByNickIgnoreCase(nick);
        if (existing.isPresent()) {
            var user = existing.get();
            if (BCrypt.checkpw(password, user.getPasswordHash()))
                return existing.get();
        }
        throw new IllegalArgumentException("Неверный ник или пароль");
    }
}