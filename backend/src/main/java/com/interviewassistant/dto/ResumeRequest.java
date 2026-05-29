package com.interviewassistant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResumeRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String education;

    private String experience;

    private String skills;

    private String projects;

    private String photoUrl;

    private String targetPosition;
}
