package com.interviewassistant.service;

import com.interviewassistant.dto.ResumeRequest;
import com.interviewassistant.dto.ResumeResponse;

import java.util.List;

public interface ResumeService {
    ResumeResponse generate(Long userId, ResumeRequest request);

    List<ResumeResponse> getHistory(Long userId);
}
