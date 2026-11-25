package com.commodule.filestorage;

import org.springframework.core.io.Resource;

public record StoredFile(StoredObject metadata, Resource resource) {
}

