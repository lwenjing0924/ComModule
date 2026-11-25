package com.commodule.filestorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesLoadsAndDeletesFile() throws Exception {
        LocalFileStorage storage = storage(DataSize.ofMegabytes(1));
        byte[] content = "hello file".getBytes(StandardCharsets.UTF_8);

        StoredObject stored = storage.store(
                new ByteArrayInputStream(content),
                "hello.txt",
                "text/plain"
        );

        assertThat(storage.exists(stored.key())).isTrue();
        assertThat(stored.originalFilename()).isEqualTo("hello.txt");
        assertThat(stored.size()).isEqualTo(content.length);

        StoredFile loaded = storage.load(stored.key()).orElseThrow();
        assertThat(loaded.resource().getInputStream().readAllBytes()).isEqualTo(content);
        assertThat(loaded.metadata().contentType()).isEqualTo("text/plain");

        assertThat(storage.delete(stored.key())).isTrue();
        assertThat(storage.exists(stored.key())).isFalse();
    }

    @Test
    void rejectsPathTraversal() {
        LocalFileStorage storage = storage(DataSize.ofMegabytes(1));

        assertThatThrownBy(() -> storage.load("../outside.txt"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void rejectsOversizedFile() {
        LocalFileStorage storage = storage(DataSize.ofBytes(4));

        assertThatThrownBy(() -> storage.store(
                new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8)),
                "large.txt",
                "text/plain"
        )).isInstanceOf(FileStorageException.class);
        assertThat(temporaryFileCount()).isZero();
    }

    private LocalFileStorage storage(DataSize maxSize) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBaseDir(tempDir);
        properties.setMaxFileSize(maxSize);
        LocalFileStorage storage = new LocalFileStorage(properties);
        storage.initialize();
        return storage;
    }

    private long temporaryFileCount() {
        try (Stream<Path> files = java.nio.file.Files.walk(tempDir)) {
            return files
                    .filter(path -> path.getFileName().toString().contains(".part-"))
                    .count();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

