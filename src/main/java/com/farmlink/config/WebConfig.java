package com.farmlink.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    /** Serves uploaded product images at /uploads/**. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of(props.uploadDir()).toAbsolutePath().normalize();
        try {
            // Path.toUri() only ends the URI with "/" when the folder already exists, and Spring needs that "/"
            // to resolve files inside it. So create the folder now and force the slash.
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory " + dir, e);
        }
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    /** Lets query params like ?category=vegetables bind to the VEGETABLES enum. */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new CaseInsensitiveEnumConverterFactory());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static class CaseInsensitiveEnumConverterFactory implements ConverterFactory<String, Enum> {
        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> source.isBlank() ? null : (T) Enum.valueOf(targetType, source.trim().toUpperCase());
        }
    }
}
