package com.interviewassistant.service.impl;

import com.interviewassistant.common.BusinessException;
import com.interviewassistant.dto.ResumeRequest;
import com.interviewassistant.dto.ResumeResponse;
import com.interviewassistant.entity.Resume;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ResumeRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import com.interviewassistant.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AIService aiService;

    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             UserRepository userRepository,
                             AIService aiService) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
    }

    @Override
    public ResumeResponse generate(Long userId, ResumeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        String prompt = buildPrompt(request);
        log.info("开始生成简历: userId={}, targetPosition={}", userId, request.getTargetPosition());

        String generatedContent = aiService.chat(prompt);

        Resume resume = Resume.builder()
                .user(user)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .education(request.getEducation())
                .experience(request.getExperience())
                .skills(request.getSkills())
                .projects(request.getProjects())
                .photoUrl(request.getPhotoUrl())
                .targetPosition(request.getTargetPosition())
                .generatedContent(generatedContent)
                .build();

        resume = resumeRepository.save(resume);
        log.info("简历生成完成: resumeId={}, userId={}", resume.getId(), userId);

        return ResumeResponse.builder()
                .id(resume.getId())
                .generatedContent(resume.getGeneratedContent())
                .createdAt(resume.getCreatedAt())
                .build();
    }

    @Override
    public List<ResumeResponse> getHistory(Long userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(r -> ResumeResponse.builder()
                        .id(r.getId())
                        .generatedContent(r.getGeneratedContent())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private String buildPrompt(ResumeRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的简历撰写师。请根据以下用户信息，生成一份专业的、结构清晰的简历。\n");
        sb.append("使用 Markdown 格式输出。重要：只生成下面提供了信息的版块，不要编造任何未提供的内容，也不要添加占位符。\n\n");
        sb.append("=== 用户信息 ===\n");

        sb.append("姓名：").append(req.getName()).append("\n");

        if (!isBlank(req.getPhone())) {
            sb.append("电话：").append(req.getPhone()).append("\n");
        }
        if (!isBlank(req.getEmail())) {
            sb.append("邮箱：").append(req.getEmail()).append("\n");
        }
        if (!isBlank(req.getTargetPosition())) {
            sb.append("求职意向：").append(req.getTargetPosition()).append("\n");
        }
        if (!isBlank(req.getEducation())) {
            sb.append("教育背景：\n").append(req.getEducation()).append("\n");
        }
        if (!isBlank(req.getExperience())) {
            sb.append("工作经历：\n").append(req.getExperience()).append("\n");
        }
        if (!isBlank(req.getProjects())) {
            sb.append("项目经历：\n").append(req.getProjects()).append("\n");
        }
        if (!isBlank(req.getSkills())) {
            sb.append("技能特长：").append(req.getSkills()).append("\n");
        }

        sb.append("=== 请生成简历 ===");
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
