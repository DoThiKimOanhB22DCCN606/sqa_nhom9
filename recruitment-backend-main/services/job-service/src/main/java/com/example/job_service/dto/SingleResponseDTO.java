package com.example.job_service.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleResponseDTO<T> {
    private T data;
    // Metadata về giới hạn ký tự cho Frontend
    private Map<String, Integer> characterLimits;

    public SingleResponseDTO(T data) {
        this.data = data;
    }

    public SingleResponseDTO(T data, Map<String, Integer> characterLimits) {
        this.data = data;
        this.characterLimits = characterLimits;
    }
}









