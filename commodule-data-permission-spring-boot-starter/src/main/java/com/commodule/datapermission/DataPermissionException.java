package com.commodule.datapermission;

public class DataPermissionException extends RuntimeException {

    public DataPermissionException(String message) {
        super(message);
    }

    public DataPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}

