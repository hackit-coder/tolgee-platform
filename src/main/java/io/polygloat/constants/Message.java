package io.polygloat.constants;

import lombok.Getter;

public enum Message {
    API_KEY_NOT_FOUND(),
    BAD_CREDENTIALS(),
    CAN_NOT_REVOKE_OWN_PERMISSIONS(),
    DATA_CORRUPTED(),
    INVITATION_CODE_DOES_NOT_EXIST_OR_EXPIRED(),
    LANGUAGE_ABBREVIATION_EXISTS(),
    LANGUAGE_NAME_EXISTS(),
    LANGUAGE_NOT_FOUND(),
    OPERATION_NOT_PERMITTED(),
    PERMISSION_NOT_FOUND(),
    REGISTRATIONS_NOT_ALLOWED(),
    REPOSITORY_NOT_FOUND(),
    RESOURCE_NOT_FOUND(),
    SCOPE_NOT_FOUND(),
    SOURCE_EXISTS(),
    SOURCE_NOT_FROM_REPOSITORY(),
    SOURCE_TEXT_IS_REQUIRED(),
    THIRD_PARTY_AUTH_ERROR_MESSAGE(),
    THIRD_PARTY_AUTH_NO_EMAIL(),
    THIRD_PARTY_AUTH_UNKNOWN_ERROR(),
    THIRD_PARTY_UNAUTHORIZED(),
    TRANSLATION_TEXT_IS_REQUIRED(),
    USERNAME_ALREADY_EXISTS(),
    USERNAME_OR_PASSWORD_INVALID(),
    USER_ALREADY_HAS_PERMISSIONS(),
    USER_NOT_FOUND(),
    VALIDATION_ERROR,
    LANGUAGE_CAN_NOT_CONTAIN_COMMA;

    String code;

    Message(String code) {
        this.code = code;
    }

    Message() {
        this.code = this.name().toLowerCase();
    }

    public String getCode() {
        return code;
    }
}
