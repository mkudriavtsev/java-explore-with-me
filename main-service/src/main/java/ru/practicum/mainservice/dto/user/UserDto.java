package ru.practicum.mainservice.dto.user;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UserDto {
    private Long id;
    @NotBlank
    @Size(min = 3, max = 100)
    private String name;
    @Email
    @NotBlank
    @Size(min = 3, max = 100)
    private String email;
}
