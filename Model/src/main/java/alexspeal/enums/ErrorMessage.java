package alexspeal.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ErrorMessage {
    USER_NOT_FOUND_BY_ID("Пользователь с userId %d не найден"),
    USER_EXISTS("Пользователь с таким именем уже существует"),
    USER_NOT_FOUND_BY_USERNAME("Пользователь с таким именем не найден"),
    INCORRECT_USER_DATA("Логин или пароль неверны"),
    MEETING_NOT_FOUND("Встреча не найдена"),
    USER_IS_NOT_A_PARTICIPANT("Пользователь не является участником встречи"),
    INCORRECT_STATUS("Некорректный статус ответа"),
    DATE_ALREADY_PASSED("Дата уже прошла: "),
    DATE_IS_NOT_INCLUDED("Выбранная дата %s не входит в доступные дни встречи"),
    NOT_FOUND_AUTHOR("Автор не найден среди участников"),
    INVALID_DAILY_LOAD("Ежедневная нагрузка должна быть положительным числом или null"),
    PREFERRED_WINDOW_INCOMPLETE("Должны быть заданы оба конца желаемого промежутка времени или ни одного"),
    PREFERRED_WINDOW_INVALID_ORDER("Конец желаемого промежутка должен быть позже начала"),
    PREFERRED_WINDOW_TOO_SHORT("Желаемый промежуток (%d мин) короче длительности встречи (%d мин)"),
    PREFERRED_WINDOW_NON_PERSONAL("Желаемый промежуток времени можно задавать только для личных событий"),
    RELOCATION_INFEASIBLE("Не удалось сдвинуть встречу '%s' — нет подходящего слота в её допустимых окнах"),
    FORBIDDEN("Доступ запрещен");
    private final String message;

    public String getMessage(Object... args) {
        return String.format(message, args);
    }
}