package com.interviewassistant.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;
    private String currentPassword;
    private String newPassword;
}
