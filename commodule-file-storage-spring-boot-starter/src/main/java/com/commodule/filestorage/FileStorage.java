package com.commodule.filestorage;

import java.io.InputStream;
import java.util.Optional;

public interface FileStorage {

    StoredObject store(InputStream inputStream, String originalFilename, String contentType);

    Optional<StoredFile> load(String key);

    boolean delete(String key);

    boolean exists(String key);
}

