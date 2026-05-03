package com.example.statistics_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response<T> {
    private int statusCode;
    private String error;
    private Object message;
    private T data;

}
