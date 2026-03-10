package com.sgms.sgms_backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }

    @PostConstruct
    public void initUploads() {
        try {
            Files.createDirectories(Paths.get("./uploads"));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory");
        }
    }
}