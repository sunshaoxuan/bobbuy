package com.bobbuy.api;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final RequestLoggingInterceptor requestLoggingInterceptor;

  public WebConfig(RequestLoggingInterceptor requestLoggingInterceptor) {
    this.requestLoggingInterceptor = requestLoggingInterceptor;
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:4173")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(requestLoggingInterceptor).addPathPatterns("/api/**");
  }

  @Bean
  public MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
    return resolver;
  }
}
