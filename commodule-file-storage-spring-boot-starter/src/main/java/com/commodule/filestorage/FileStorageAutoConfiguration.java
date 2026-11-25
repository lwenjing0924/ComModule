package com.commodule.filestorage;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(
            prefix = "commodule.file-storage",
            name = "provider",
            havingValue = "local",
            matchIfMissing = true
    )
    public FileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(properties);
    }

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "commodule.file-storage", name = "provider", havingValue = "memory")
    public FileStorage inMemoryFileStorage(FileStorageProperties properties) {
        return new InMemoryFileStorage(properties);
    }
}

