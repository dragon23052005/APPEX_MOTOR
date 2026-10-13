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
        registry.addResourceHandler("/assets/cars/**")
            .addResourceLocations("classpath:/static/assets/cars/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());

        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());

        registry.addResourceHandler("/css/**", "/js/**")
            .addResourceLocations("classpath:/static/css/", "classpath:/static/js/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());
    }
}
