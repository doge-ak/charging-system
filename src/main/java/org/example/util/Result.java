package org.example.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "ok", data);
    }

    public static Result<Void> success() {
        return new Result<>(200, "ok", null);
    }
}
