package com.demo.nimn.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private int status;
    private String message;
    private String code; // 디버깅용
}