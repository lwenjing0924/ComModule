package com.commodule.filestorage;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFileStorageTest {

    @Test
    void storesAndDeletesFile() throws Exception {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setMaxFileSize(DataSize.ofKilobytes(1));
        InMemoryFileStorage storage = new InMemoryFileStorage(properties);

        StoredObject stored = storage.store(
                new ByteArrayInputStream("memory".getBytes(StandardCharsets.UTF_8)),
                "memory.txt",
                "text/plain"
        );

        assertThat(storage.exists(stored.key())).isTrue();
        StoredFile loaded = storage.load(stored.key()).orElseThrow();
        assertThat(loaded.resource().getInputStream().readAllBytes())
                .isEqualTo("memory".getBytes(StandardCharsets.UTF_8));
        assertThat(loaded.resource().getFilename()).isEqualTo("memory.txt");
        assertThat(storage.delete(stored.key())).isTrue();
        assertThat(storage.exists(stored.key())).isFalse();
    }
}

