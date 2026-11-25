package com.commodule.filestorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageAutoConfigurationTest {

    @TempDir
    Path tempDir;

    @Test
    void selectsMemoryProvider() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class))
                .withPropertyValues(
                        "commodule.file-storage.provider=memory",
                        "commodule.file-storage.base-dir=" + tempDir
                )
                .run(context -> assertThat(context.getBean(FileStorage.class))
                        .isInstanceOf(InMemoryFileStorage.class));
    }

    @Test
    void selectsLocalProviderByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class))
                .withPropertyValues("commodule.file-storage.base-dir=" + tempDir)
                .run(context -> assertThat(context.getBean(FileStorage.class))
                        .isInstanceOf(LocalFileStorage.class));
    }
}

