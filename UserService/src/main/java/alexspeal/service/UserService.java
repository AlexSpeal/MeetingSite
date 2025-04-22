package alexspeal.service;

import alexspeal.dto.UserDetailsDto;
import alexspeal.dto.UserDto;
import alexspeal.entities.UserEntity;
import alexspeal.enums.ErrorMessage;
import alexspeal.mappers.UserMapper;
import alexspeal.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<UserEntity> findUserEntityByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserDetailsDto findById(Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage()));
        return new UserDetailsDto(user.getId(), user.getUsername());

    }

    public UserDetailsDto findByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow(
                () -> new NoSuchElementException(ErrorMessage.USER_NOT_FOUND_BY_ID.getMessage()));
        return new UserDetailsDto(user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return new User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>()
        );
    }

    public void createNewUser(UserDto userDTO) {
        UserDto userWithPasswordDTO = new UserDto(userDTO.username(), passwordEncoder.encode(userDTO.password()));
        UserEntity userEntity = UserMapper.UserDTOToUserEntity(userWithPasswordDTO);
        userRepository.save(userEntity);
    }

}
