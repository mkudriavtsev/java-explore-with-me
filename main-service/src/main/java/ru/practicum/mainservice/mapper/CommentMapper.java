package ru.practicum.mainservice.mapper;

import org.mapstruct.*;
import ru.practicum.mainservice.dto.comment.CommentDto;
import ru.practicum.mainservice.dto.comment.CommentRequestDto;
import ru.practicum.mainservice.model.Comment;

import java.util.List;

@Mapper(uses = {UserMapper.class})
public interface CommentMapper {


    @Mapping(target = "author", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "event", ignore = true)
    Comment toEntity(CommentRequestDto dto);

    @Mapping(target = "event", source = "event.id")
    CommentDto toDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "author", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchComment(CommentRequestDto dto, @MappingTarget Comment comment);

    List<CommentDto> toDtoList(List<Comment> comments);
}
