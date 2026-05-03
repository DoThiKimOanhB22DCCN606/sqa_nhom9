package com.example.upload_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dq3fcbnk6",
                "api_key", "448352884737144",
                "api_secret", "UHOyk-4DpqCC_sZI5VTS4q9zIEE",
                "secure", true));
    }
}
