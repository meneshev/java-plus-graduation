package user.controller;

import dto.user.UserShortDto;
import feign.user.UserOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class InnerUserController implements UserOperations {
    private final UserService userService;

    @GetMapping("/{id}")
    public UserShortDto getById(Long id) {
        return userService.getUserById(id);
    }
}