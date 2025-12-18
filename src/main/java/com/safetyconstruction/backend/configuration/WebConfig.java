package com.safetyconstruction.backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map http://localhost:8080/images/abc.jpg -> thư mục uploads/abc.jpg
        registry.addResourceHandler("/images/**").addResourceLocations("file:uploads/");
    }
}
