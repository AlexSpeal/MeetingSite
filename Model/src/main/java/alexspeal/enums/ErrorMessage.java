package alexspeal.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ErrorMessage {
    USER_NOT_FOUND_BY_ID("Пользователь с id %d не найден"),
    USER_EXISTS("Пользователь с таким именем уже существует"),
    USER_NOT_FOUND_BY_USERNAME("Пользователь с таким именем не найден"),
    INCORRECT_USER_DATA("Логин или пароль неверны"),
    MEETING_NOT_FOUND("Встреча не найдена"),
    USER_IS_NOT_A_PARTICIPANT("Пользователь не является участником встречи"),
    INCORRECT_STATUS("Некорректный статус ответа"),
    DATE_ALREADY_PASSED("Дата уже прошла: "),
    DATE_IS_NOT_INCLUDED("Выбранная дата %s не входит в доступные дни встречи");

    private final String message;

    public String getMessage(Object... args) {
        return String.format(message, args);
    }
}