package com.umc.menteup.dto;

import jakarta.validation.constraints.NotBlank;

public class FcmTokenRequest {
    @NotBlank(message = "O totek Firebase é obrigatório")
    private String fcmToken;

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
