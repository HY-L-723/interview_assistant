package com.interviewassistant.controller;

import com.interviewassistant.common.Result;
import com.interviewassistant.dto.ResumeRequest;
import com.interviewassistant.dto.ResumeResponse;
import com.interviewassistant.service.ResumeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resume")
@Slf4j
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
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
        byte[] pdfBytes = resumeService.generatePdf(userId, id);

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
            return ResponseEntity.ok(Result.success(
                    resumeService.uploadPhoto(userId, file.getOriginalFilename(), file.getBytes())));
        } catch (java.io.IOException e) {
            throw new com.interviewassistant.common.BusinessException("读取上传照片失败");
        }
    }
}
