package user.service;

import dto.user.NewUserRequest;
import dto.user.UserResponse;
import dto.user.UserShortDto;
import org.springframework.data.domain.Pageable;
import user.dal.entity.User;

import java.util.List;

public interface UserService {
    UserResponse createUser(NewUserRequest userRequest);

    List<UserResponse> getUsers(List<Long> ids, Pageable pageable);

    UserShortDto getUserById(Long userId);

    void deleteUser(Long userId);

    void checkUserExists(Long userId);

    User getUserEntityById(Long userId);
}