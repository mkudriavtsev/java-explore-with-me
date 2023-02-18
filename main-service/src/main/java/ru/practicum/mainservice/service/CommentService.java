package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.comment.CommentDto;
import ru.practicum.mainservice.dto.comment.CommentRequestDto;

import java.util.List;

public interface CommentService {

    CommentDto create(CommentRequestDto dto, Long userId, Long eventId);

    CommentDto patch(CommentRequestDto dto, Long userId, Long commentId);

    List<CommentDto> getOwn(Long userId);

    List<CommentDto> getEventComments(Long eventId, Integer from, Integer size);

    void deleteById(Long commentId);

    List<CommentDto> search(String text, Integer from, Integer size);
}
