package com.interviewassistant.service.impl;

import com.interviewassistant.service.PdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class PdfServiceImpl implements PdfService {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    /**
     * 中文字体候选列表，按优先级依次尝试。
     * Windows 11 中文版通常有 msyh.ttf（微软雅黑），
     * 旧版 Windows 可能有 simhei.ttf（黑体）或 simsun.ttc（宋体，TrueType Collection）。
     */
    private static final String[] FONT_CANDIDATES = {
        "C:/Windows/Fonts/msyh.ttf",
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/simsun.ttc,0",
        "C:/Windows/Fonts/msyh.ttc,0"
    };

    private BaseFont loadChineseFont() {
        for (String path : FONT_CANDIDATES) {
            try {
                BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                log.info("成功加载中文字体: {}", path);
                return bf;
            } catch (Exception ignored) {
                // 尝试下一个
            }
        }
        log.warn("未找到任何中文字体文件，PDF 中文将无法正常显示。" +
                 "请将中文字体（如 msyh.ttf）放入 C:/Windows/Fonts/ 目录。");
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("无法加载默认字体", e);
        }
    }

    @Override
    public byte[] generatePdf(String content) {
        if (content == null || content.isBlank()) {
            content = "暂无简历内容";
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont bf = loadChineseFont();

            Font titleFont = new Font(bf, 20, Font.BOLD);
            Font h2Font = new Font(bf, 14, Font.BOLD);
            Font normalFont = new Font(bf, 11, Font.NORMAL);
            Font boldFont = new Font(bf, 11, Font.BOLD);

            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.startsWith("## ")) {
                    document.add(new Paragraph(trimmed.substring(3), h2Font));
                    document.add(Chunk.NEWLINE);
                } else if (trimmed.startsWith("# ")) {
                    document.add(new Paragraph(trimmed.substring(2), titleFont));
                    document.add(Chunk.NEWLINE);
                } else if (trimmed.startsWith("---")) {
                    document.add(new Chunk("\n"));
                } else if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
                    String boldText = trimmed.substring(2, trimmed.length() - 2);
                    Paragraph p = new Paragraph();
                    p.add(new Chunk(boldText, boldFont));
                    document.add(p);
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    String itemText = trimmed.substring(2);
                    Paragraph p = new Paragraph();
                    p.add(new Chunk("  •  " + itemText, normalFont));
                    p.setIndentationLeft(20);
                    document.add(p);
                } else if (trimmed.startsWith("|")) {
                    String cleanLine = trimmed.replace("|", "  ");
                    document.add(new Paragraph(cleanLine, normalFont));
                } else if (!trimmed.isEmpty()) {
                    String cleanLine = trimmed
                            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                            .replaceAll("\\*(.+?)\\*", "$1")
                            .replaceAll("`(.+?)`", "$1")
                            .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1");
                    document.add(new Paragraph(cleanLine, normalFont));
                } else {
                    document.add(new Paragraph(" ", normalFont));
                }
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF生成失败", e);
            throw new RuntimeException("PDF生成失败: " + e.getMessage());
        }
    }
}
