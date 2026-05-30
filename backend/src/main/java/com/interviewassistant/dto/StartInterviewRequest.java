package com.interviewassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 开始面试请求 DTO。
 */
@Data
public class StartInterviewRequest {

    /** 面试岗位，例如"Java后端开发工程师" */
    @NotBlank(message = "面试岗位不能为空")
    private String position;
}
