package ru.practicum.mainservice.controller.adminApi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/comments/{commentId}")
public class CommentAdminRestController {

    private final CommentService commentService;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long commentId) {
        commentService.deleteById(commentId);
    }
}
