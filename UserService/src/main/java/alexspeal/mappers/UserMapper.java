package alexspeal.mappers;

import alexspeal.dto.UserDto;
import alexspeal.entities.UserEntity;


public class UserMapper {
    public static UserDto UserEntityToUserDTO(UserEntity userEntity) {
        return new UserDto(userEntity.getUsername(), userEntity.getPassword());
    }

    public static UserEntity UserDTOToUserEntity(UserDto userDTO) {
        return new UserEntity(userDTO.username(), userDTO.password());
    }
}
