package ru.gr0946x.db.dto;

import java.time.LocalDateTime;

public record MessageDto(
        Long id,
        String authorNick,
        Long receiverId,
        String content,
        LocalDateTime createdAt,
        boolean isReceived
) {}