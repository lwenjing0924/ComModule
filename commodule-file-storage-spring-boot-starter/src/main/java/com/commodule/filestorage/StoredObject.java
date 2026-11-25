package com.commodule.filestorage;

import java.time.Instant;

public record StoredObject(
        String key,
        String originalFilename,
        String contentType,
        long size,
        Instant createdAt
) {
}

