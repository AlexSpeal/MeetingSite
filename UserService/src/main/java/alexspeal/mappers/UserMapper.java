package alexspeal.mappers;

import alexspeal.dto.UserDetailsDto;
import alexspeal.dto.UserDto;
import alexspeal.entities.UserEntity;


public final class UserMapper {

    public static UserEntity UserDTOToUserEntity(UserDto userDTO) {
        UserEntity entity = new UserEntity(userDTO.username(), userDTO.password());
        entity.setTimezone(userDTO.timezone());
        entity.setDailyLoadMinutes(userDTO.dailyLoadMinutes());
        return entity;
    }

    public static UserDetailsDto toUserDetailsDto(UserEntity user) {
        return new UserDetailsDto(
                user.getId(),
                user.getUsername()
        );
    }
}
