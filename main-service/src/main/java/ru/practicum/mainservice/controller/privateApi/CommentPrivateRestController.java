package ru.practicum.mainservice.controller.privateApi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.dto.comment.CommentDto;
import ru.practicum.mainservice.dto.comment.CommentRequestDto;
import ru.practicum.mainservice.service.CommentService;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/comments")
public class CommentPrivateRestController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(@Valid @RequestBody CommentRequestDto dto,
                             @RequestParam Long eventId,
                             @PathVariable Long userId) {
        return commentService.create(dto, userId, eventId);
    }

    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto patch(@Valid @RequestBody CommentRequestDto dto,
                            @PathVariable Long userId,
                            @PathVariable Long commentId) {
        return commentService.patch(dto, userId, commentId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> getOwn(@PathVariable Long userId) {
        return commentService.getOwn(userId);
    }
}
