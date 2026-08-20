package com.interviewassistant.service;

import com.interviewassistant.dto.ResumeRequest;
import com.interviewassistant.dto.ResumeResponse;

import java.util.List;

public interface ResumeService {
    ResumeResponse generate(Long userId, ResumeRequest request);

    List<ResumeResponse> getHistory(Long userId);

    /** 校验简历归属并生成 PDF。 */
    byte[] generatePdf(Long userId, Long resumeId);

    /** 保存用户简历照片并返回可访问路径。 */
    String uploadPhoto(Long userId, String originalFilename, byte[] content);
}
