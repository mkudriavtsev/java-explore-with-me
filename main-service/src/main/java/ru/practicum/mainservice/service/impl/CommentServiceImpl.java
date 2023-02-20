package ru.practicum.mainservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.dto.comment.CommentDto;
import ru.practicum.mainservice.dto.comment.NewCommentDto;
import ru.practicum.mainservice.dto.comment.UpdateCommentDto;
import ru.practicum.mainservice.exception.ConflictException;
import ru.practicum.mainservice.exception.NotFoundException;
import ru.practicum.mainservice.mapper.CommentMapper;
import ru.practicum.mainservice.model.Comment;
import ru.practicum.mainservice.model.Event;
import ru.practicum.mainservice.model.EventState;
import ru.practicum.mainservice.model.User;
import ru.practicum.mainservice.repository.CommentRepository;
import ru.practicum.mainservice.repository.EventRepository;
import ru.practicum.mainservice.repository.UserRepository;
import ru.practicum.mainservice.service.CommentService;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final String EVENT_NOT_FOUND = "Event with id %d not found";
    private static final String USER_NOT_FOUND = "User with id %d not found";
    private static final String COMMENT_NOT_FOUND = "Comment with id %d not found";
    private static final Sort SORT_BY_CREATED_TIME = Sort.by(Sort.Direction.DESC, "created");

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto create(NewCommentDto dto, Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, eventId));
        });
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Comments can only be posted on published events");
        }
        User author = userRepository.findById(userId).orElseThrow(() -> {
            throw new NotFoundException(String.format(USER_NOT_FOUND, userId));
        });
        Comment comment = commentMapper.toEntity(dto);
        comment.setAuthor(author);
        comment.setEvent(event);
        Comment savedComment = commentRepository.save(comment);
        log.info("Comment with id {} created", savedComment.getId());
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto patch(UpdateCommentDto dto, Long userId) {
        Comment comment = commentRepository.findById(dto.getId()).orElseThrow(() -> {
            throw new NotFoundException(String.format(COMMENT_NOT_FOUND, dto.getId()));
        });
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Only the author of the comment can make changes");
        }
        if (comment.getEvent().getState() != EventState.PUBLISHED) {
            throw new ConflictException("Changes to comments can only be made for published events");
        }
        commentMapper.patchComment(dto, comment);
        log.info("Comment with id {} updated", comment.getId());
        Comment updatedComment = commentRepository.saveAndFlush(comment);
        return commentMapper.toDto(updatedComment);
    }

    @Override
    public List<CommentDto> getOwn(Long userId) {
        List<Comment> comments = commentRepository.findAllByAuthorId(userId);
        return commentMapper.toDtoList(comments);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from / size, size, SORT_BY_CREATED_TIME);
        List<Comment> comments = commentRepository.findAllByEventId(eventId, page).getContent();
        return commentMapper.toDtoList(comments);
    }

    @Override
    @Transactional
    public void deleteById(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(String.format(COMMENT_NOT_FOUND, commentId));
        }
        commentRepository.deleteById(commentId);
        log.info("Comment with id {} deleted", commentId);
    }

    @Override
    public List<CommentDto> search(String text) {
        if (text.isBlank()) {
            return Collections.emptyList();
        }
        List<Comment> comments = commentRepository.search(text);
        return commentMapper.toDtoList(comments);
    }

    @Override
    @Transactional
    public CommentDto patchByAdmin(UpdateCommentDto dto) {
        Comment comment = commentRepository.findById(dto.getId()).orElseThrow(() -> {
            throw new NotFoundException(String.format(COMMENT_NOT_FOUND, dto.getId()));
        });
        commentMapper.patchComment(dto, comment);
        log.info("Comment with id {} updated", comment.getId());
        Comment updatedComment = commentRepository.saveAndFlush(comment);
        return commentMapper.toDto(updatedComment);
    }
}
