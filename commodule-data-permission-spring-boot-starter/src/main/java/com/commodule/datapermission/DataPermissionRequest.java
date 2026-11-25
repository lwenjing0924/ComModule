package com.commodule.datapermission;

public record DataPermissionRequest(String resource, String method, String ownerField) {
}

