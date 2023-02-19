package ru.practicum.mainservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.mainservice.dto.comment.CommentDto;
import ru.practicum.mainservice.dto.comment.NewCommentDto;
import ru.practicum.mainservice.dto.comment.UpdateCommentDto;
import ru.practicum.mainservice.exception.ConflictException;
import ru.practicum.mainservice.exception.NotFoundException;
import ru.practicum.mainservice.mapper.CommentMapper;
import ru.practicum.mainservice.mapper.UserMapper;
import ru.practicum.mainservice.model.Comment;
import ru.practicum.mainservice.model.Event;
import ru.practicum.mainservice.model.EventState;
import ru.practicum.mainservice.model.User;
import ru.practicum.mainservice.repository.CommentRepository;
import ru.practicum.mainservice.repository.EventRepository;
import ru.practicum.mainservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @InjectMocks
    private CommentServiceImpl commentService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Spy
    @InjectMocks
    private CommentMapper commentMapper = Mappers.getMapper(CommentMapper.class);
    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void create_whenInvoked_thenSavedComment() {
        NewCommentDto dto = new NewCommentDto("Test comment");
        Long userId = 1L;
        Long eventId = 1L;
        Comment commentToSave = commentMapper.toEntity(dto);
        commentToSave.setId(1L);
        commentToSave.setAuthor(getAuthor(userId));
        commentToSave.setEvent(getEvent(eventId));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(getEvent(eventId)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(getAuthor(userId)));
        doAnswer(invocationOnMock -> {
            Comment comment = invocationOnMock.getArgument(0, Comment.class);
            comment.setId(1L);
            return comment;
        }).when(commentRepository).save(any(Comment.class));
        CommentDto expectedDto = commentMapper.toDto(commentToSave);

        CommentDto actualDto = commentService.create(dto, userId, eventId);

        assertEquals(expectedDto, actualDto);
    }

    @Test
    void create_whenEventNotFound_thenNotFoundExceptionThrown() {
        NewCommentDto dto = new NewCommentDto("Test comment");
        Long userId = 1L;
        Long eventId = 0L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.create(dto, userId, eventId));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void create_whenAuthorNotFound_thenNotFoundExceptionThrown() {
        NewCommentDto dto = new NewCommentDto("Test comment");
        Long userId = 0L;
        Long eventId = 1L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(getEvent(eventId)));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.create(dto, userId, eventId));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void create_whenEventStateNotPublished_thenConflictExceptionThrown() {
        NewCommentDto dto = new NewCommentDto("Test comment");
        Long userId = 0L;
        Long eventId = 1L;
        Event event = getEvent(eventId);
        event.setState(EventState.PENDING);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> commentService.create(dto, userId, eventId));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void patch_whenInvoked_thenUpdateComment() {
        Long userId = 1L;
        Long commentId = 1L;
        UpdateCommentDto dto = new UpdateCommentDto();
        dto.setId(commentId);
        dto.setText("Updated Text");
        Comment updatedComment = getComment(1L);
        updatedComment.setText("Updated Text");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(getComment(commentId)));
        when(commentRepository.saveAndFlush(any(Comment.class))).thenReturn(updatedComment);
        CommentDto expectedDto = commentMapper.toDto(updatedComment);

        CommentDto actualDto = commentService.patch(dto, userId);

        assertEquals(expectedDto, actualDto);
    }

    @Test
    void patch_whenEventNotFound_thenNotFoundExceptionThrown() {
        Long userId = 1L;
        Long commentId = 1L;
        UpdateCommentDto dto = new UpdateCommentDto();
        dto.setId(commentId);
        dto.setText("Updated Text");
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.patch(dto, userId));
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void patch_whenNotAuthorUpdate_thenConflictExceptionThrown() {
        Long userId = 2L;
        Long commentId = 1L;
        UpdateCommentDto dto = new UpdateCommentDto();
        dto.setId(commentId);
        dto.setText("Updated Text");
        Comment comment = getComment(1L);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(ConflictException.class,
                () -> commentService.patch(dto, userId));
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void patch_whenEventStateNotPublished_thenConflictExceptionThrown() {
        Long userId = 1L;
        Long commentId = 1L;
        UpdateCommentDto dto = new UpdateCommentDto();
        dto.setId(commentId);
        dto.setText("Updated Text");
        Comment comment = getComment(1L);
        comment.getEvent().setState(EventState.PENDING);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(ConflictException.class,
                () -> commentService.patch(dto, userId));
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    @Test
    void getOwn() {
        Long userId = 1L;
        List<Comment> comments = List.of(getComment(1L));
        List<CommentDto> expectedDtos = commentMapper.toDtoList(comments);
        when(commentRepository.findAllByAuthorId(userId)).thenReturn(comments);

        List<CommentDto> actualDtos = commentService.getOwn(userId);

        assertEquals(expectedDtos, actualDtos);
    }

    @Test
    void getEventComments() {
        Long eventId = 1L;
        List<Comment> comments = List.of(getComment(1L));
        List<CommentDto> expectedDtos = commentMapper.toDtoList(comments);
        when(commentRepository.findAllByEventId(eq(eventId), any(PageRequest.class))).thenReturn(new PageImpl<>(comments));

        List<CommentDto> actualDtos = commentService.getEventComments(eventId, 0, 10);

        assertEquals(expectedDtos, actualDtos);
    }

    @Test
    void deleteById_whenCommentFound_thenCommentDeleted() {
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(Boolean.TRUE);

        commentService.deleteById(commentId);

        verify(commentRepository).deleteById(commentId);
    }

    @Test
    void deleteById_whenCommentNotFound_thenNotFoundExceptionThrown() {
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(Boolean.FALSE);

        assertThrows(NotFoundException.class,
                () -> commentService.deleteById(commentId));
    }

    @Test
    void search() {
        String text = "text";
        List<Comment> comments = List.of(getComment(1L));
        List<CommentDto> expectedDtos = commentMapper.toDtoList(comments);
        when(commentRepository.search(text)).thenReturn(comments);

        List<CommentDto> actualDtos = commentService.search(text);

        assertEquals(expectedDtos, actualDtos);
    }

    @Test
    void patchByAdmin_whenInvoked_thenUpdateComment() {
        Long commentId = 1L;
        UpdateCommentDto dto = new UpdateCommentDto();
        dto.setId(commentId);
        dto.setText("Updated Text");
        Comment updatedComment = getComment(1L);
        updatedComment.setText("Updated Text");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(getComment(commentId)));
        when(commentRepository.saveAndFlush(any(Comment.class))).thenReturn(updatedComment);
        CommentDto expectedDto = commentMapper.toDto(updatedComment);

        CommentDto actualDto = commentService.patchByAdmin(dto);

        assertEquals(expectedDto, actualDto);
    }

    private User getAuthor(Long userId) {
        User author = new User();
        author.setId(userId);
        author.setName("Nil Armstrong");
        author.setEmail("nil@armstrong.com");
        return author;
    }

    private Event getEvent(Long eventId) {
        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);
        return event;
    }

    private Comment getComment(Long commentId) {
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setText("Test text");
        comment.setCreated(LocalDateTime.now());
        comment.setAuthor(getAuthor(1L));
        comment.setEvent(getEvent(1L));
        return comment;
    }
}
