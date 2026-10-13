package com.apexmotors.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/models/**")
            .addResourceLocations("classpath:/static/models/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());

        registry.addResourceHandler("/vendor/**")
            .addResourceLocations("classpath:/static/vendor/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
