package com.example.tmdt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Value("${dung.upload-file.base-uri}")
    private String baseURI;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:./uploads/");
        registry.addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/img/**")
            .addResourceLocations("classpath:/static/img/");
        registry.addResourceHandler("/fonts/**")
            .addResourceLocations("classpath:/static/fonts/");
        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/client/**")
            .addResourceLocations("classpath:/static/client/");
    }

}

