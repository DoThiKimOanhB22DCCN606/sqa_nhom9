package com.example.job_service.utils;

import java.util.HashMap;
import java.util.Map;

public class TextTruncateUtil {

    // Giới hạn ký tự mặc định cho các trường
    public static final int DEFAULT_DESCRIPTION_LIMIT = 200;
    public static final int DEFAULT_REQUIREMENTS_LIMIT = 200;
    public static final int DEFAULT_BENEFITS_LIMIT = 200;
    public static final int DEFAULT_REASON_LIMIT = 200;
    public static final int DEFAULT_TITLE_LIMIT = 100;

    /**
     * Truncate text với giới hạn ký tự
     * 
     * @param text  Text cần truncate
     * @param limit Giới hạn ký tự
     * @return Text đã được truncate (thêm "..." nếu vượt quá)
     */
    public static String truncate(String text, int limit) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit) + "...";
    }

    /**
     * Truncate description với giới hạn mặc định
     */
    public static String truncateDescription(String description) {
        return truncate(description, DEFAULT_DESCRIPTION_LIMIT);
    }

    /**
     * Truncate requirements với giới hạn mặc định
     */
    public static String truncateRequirements(String requirements) {
        return truncate(requirements, DEFAULT_REQUIREMENTS_LIMIT);
    }

    /**
     * Truncate benefits với giới hạn mặc định
     */
    public static String truncateBenefits(String benefits) {
        return truncate(benefits, DEFAULT_BENEFITS_LIMIT);
    }

    /**
     * Truncate reason với giới hạn mặc định
     */
    public static String truncateReason(String reason) {
        return truncate(reason, DEFAULT_REASON_LIMIT);
    }

    /**
     * Truncate title với giới hạn mặc định
     */
    public static String truncateTitle(String title) {
        return truncate(title, DEFAULT_TITLE_LIMIT);
    }

    /**
     * Tạo map character limits cho JobPosition
     */
    public static Map<String, Integer> getJobPositionCharacterLimits() {
        Map<String, Integer> limits = new HashMap<>();
        limits.put("title", DEFAULT_TITLE_LIMIT);
        limits.put("description", DEFAULT_DESCRIPTION_LIMIT);
        limits.put("requirements", DEFAULT_REQUIREMENTS_LIMIT);
        limits.put("benefits", DEFAULT_BENEFITS_LIMIT);
        return limits;
    }

    /**
     * Tạo map character limits cho RecruitmentRequest
     */
    public static Map<String, Integer> getRecruitmentRequestCharacterLimits() {
        Map<String, Integer> limits = new HashMap<>();
        limits.put("title", DEFAULT_TITLE_LIMIT);
        limits.put("description", DEFAULT_DESCRIPTION_LIMIT);
        limits.put("requirements", DEFAULT_REQUIREMENTS_LIMIT);
        limits.put("benefits", DEFAULT_BENEFITS_LIMIT);
        limits.put("reason", DEFAULT_REASON_LIMIT);
        return limits;
    }
}
