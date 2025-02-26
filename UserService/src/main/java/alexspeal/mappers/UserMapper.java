package alexspeal.mappers;

import alexspeal.dto.UserDTO;
import alexspeal.entities.UserEntity;


public class UserMapper {
    public static UserDTO UserEntityToUserDTO(UserEntity userEntity) {
        return new UserDTO(userEntity.getUsername(), userEntity.getPassword());
    }

    public static UserEntity UserDTOToUserEntity(UserDTO userDTO) {
        return new UserEntity(userDTO.username(), userDTO.password());
    }
}
