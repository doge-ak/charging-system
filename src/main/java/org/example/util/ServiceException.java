package org.example.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ServiceException extends RuntimeException {
    private int code;
    private String msg;
}
