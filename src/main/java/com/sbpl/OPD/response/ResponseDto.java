package com.sbpl.OPD.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Setter
@Getter
public class ResponseDto {
    private boolean response;
    private String message;
    private HttpStatus status;
    private Object data;
    private LocalDateTime timestamp;

    // @Override
    // public String toString() {
    //     return "ResponseDto{" +
    //             "response=" + response +
    //             ", message='" + message + '\'' +
    //             ", status=" + status +
    //             ", data=" + data +
    //             ", timestamp=" + timestamp +
    //             '}';
    // }
}