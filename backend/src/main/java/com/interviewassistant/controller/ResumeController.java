package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.ResumeRequest;
import com.interviewassistant.dto.ResumeResponse;
import com.interviewassistant.service.PdfService;
import com.interviewassistant.service.ResumeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
@Slf4j
public class ResumeController {

    private final ResumeService resumeService;
    private final PdfService pdfService;

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    public ResumeController(ResumeService resumeService, PdfService pdfService) {
        this.resumeService = resumeService;
        this.pdfService = pdfService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Result<ResumeResponse>> generate(@AuthenticationPrincipal Long userId,
                                                            @Valid @RequestBody ResumeRequest request) {
        log.info("生成简历请求: userId={}", userId);
        ResumeResponse response = resumeService.generate(userId, request);
        return ResponseEntity.ok(Result.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<Result<List<ResumeResponse>>> getHistory(@AuthenticationPrincipal Long userId) {
        log.info("查询简历历史: userId={}", userId);
        List<ResumeResponse> history = resumeService.getHistory(userId);
        return ResponseEntity.ok(Result.success(history));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long id) {
        log.info("导出PDF: userId={}, resumeId={}", userId, id);
        List<ResumeResponse> history = resumeService.getHistory(userId);
        ResumeResponse target = history.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("简历不存在或无权访问"));

        byte[] pdfBytes = pdfService.generatePdf(target.getGeneratedContent());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("resume.pdf").build());

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<Result<String>> uploadPhoto(@AuthenticationPrincipal Long userId,
                                                       @RequestParam("file") MultipartFile file) {
        log.info("上传照片: userId={}, fileName={}", userId, file.getOriginalFilename());
        try {
            Path uploadDir = Paths.get(uploadPath, "photos");
            Files.createDirectories(uploadDir);

            String ext = getExtension(file.getOriginalFilename());
            String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path filePath = uploadDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String photoUrl = "/uploads/photos/" + filename;
            return ResponseEntity.ok(Result.success(photoUrl));
        } catch (IOException e) {
            log.error("照片上传失败", e);
            return ResponseEntity.status(500).body(Result.error("照片上传失败"));
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}
