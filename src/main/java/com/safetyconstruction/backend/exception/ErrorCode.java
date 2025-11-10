package com.safetyconstruction.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_EMAIL(1008, "Email is not valid", HttpStatus.BAD_REQUEST),
    PROJECT_NAME_EXISTED(1009, "Project's name existed", HttpStatus.BAD_REQUEST),
    PROJECT_NOT_FOUND(1010, "Project not found", HttpStatus.NOT_FOUND),
    CAMERA_NOT_FOUND(1011, "Camera not found", HttpStatus.NOT_FOUND),
    ALERT_NOT_FOUND(1012, "Alert not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_NOT_FOUND(1012, "Notification not found", HttpStatus.NOT_FOUND),
    ROLE_EXISTED(1013, "Role existed", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1014, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(1015, "Permission not found", HttpStatus.NOT_FOUND),
    ACCOUNT_LOCKED(1016, "Account is locked", HttpStatus.FORBIDDEN);

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;
}
