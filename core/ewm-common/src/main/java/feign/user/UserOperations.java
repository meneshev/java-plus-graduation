package feign.user;

import dto.user.UserShortDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface UserOperations {
    @GetMapping("/{id}")
    UserShortDto getById(@PathVariable("id") Long id);
}
