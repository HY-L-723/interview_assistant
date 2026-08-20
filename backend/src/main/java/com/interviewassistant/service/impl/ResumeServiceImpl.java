package com.interviewassistant.service.impl;

import com.interviewassistant.common.BusinessException;
import com.interviewassistant.dto.ResumeRequest;
import com.interviewassistant.dto.ResumeResponse;
import com.interviewassistant.entity.Resume;
import com.interviewassistant.entity.User;
import com.interviewassistant.repository.ResumeRepository;
import com.interviewassistant.repository.UserRepository;
import com.interviewassistant.service.AIService;
import com.interviewassistant.service.PdfService;
import com.interviewassistant.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final PdfService pdfService;

    private static final Set<String> ALLOWED_PHOTO_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             UserRepository userRepository,
                             AIService aiService,
                             PdfService pdfService) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.pdfService = pdfService;
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

    @Override
    public byte[] generatePdf(Long userId, Long resumeId) {
        Resume resume = findOwnedResume(userId, resumeId);
        return pdfService.generatePdf(resume.getGeneratedContent());
    }

    @Override
    public String uploadPhoto(Long userId, String originalFilename, byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException("上传照片不能为空");
        }
        // 同时校验用户存在，避免为无效用户写入孤立文件。
        userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));

        String extension = getPhotoExtension(originalFilename);
        try {
            Path photoDirectory = Paths.get(uploadPath, "photos").toAbsolutePath().normalize();
            Files.createDirectories(photoDirectory);
            String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path target = photoDirectory.resolve(filename).normalize();
            if (!target.startsWith(photoDirectory)) {
                throw new BusinessException("非法的文件路径");
            }
            Files.write(target, content);
            return "/uploads/photos/" + filename;
        } catch (IOException e) {
            log.error("照片保存失败: userId={}", userId, e);
            throw new BusinessException("照片上传失败");
        }
    }

    private Resume findOwnedResume(Long userId, Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException("简历不存在"));
        if (!resume.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "无权访问此简历");
        }
        return resume;
    }

    private String getPhotoExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException("照片文件缺少扩展名");
        }
        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_PHOTO_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 JPG、PNG 或 WebP 照片");
        }
        return extension;
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
