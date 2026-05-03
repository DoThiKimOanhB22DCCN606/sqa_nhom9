package com.example.job_service.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Meta {
    private int page;
    private int pageSize;
    private int pages;
    private long total;
    // Metadata về giới hạn ký tự cho Frontend
    private Map<String, Integer> characterLimits;
}
