package alexspeal.mappers;

import alexspeal.dto.UserDetailsDto;
import alexspeal.dto.UserDto;
import alexspeal.entities.UserEntity;


public final class UserMapper {

    public static UserEntity UserDTOToUserEntity(UserDto userDTO) {
        return new UserEntity(userDTO.username(), userDTO.password());

    }

    public static UserDetailsDto toUserDetailsDto(UserEntity user) {
        return new UserDetailsDto(
                user.getId(),
                user.getUsername()
        );
    }
}