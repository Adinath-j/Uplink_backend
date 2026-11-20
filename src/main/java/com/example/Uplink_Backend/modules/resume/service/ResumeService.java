package com.example.Uplink_Backend.modules.resume.service;

import com.example.Uplink_Backend.common.exception.ResourceNotFoundException;
import com.example.Uplink_Backend.common.service.CloudinaryService;
import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import com.example.Uplink_Backend.modules.ocr.entity.Document;
import com.example.Uplink_Backend.modules.ocr.repository.DocumentRepository;
import com.example.Uplink_Backend.modules.resume.dto.ResumeDto;
import com.example.Uplink_Backend.modules.resume.dto.ResumeGenerateRequest;
import com.example.Uplink_Backend.modules.user.dto.DigitalPortfolioDto;
import com.example.Uplink_Backend.modules.user.entity.*;
import com.example.Uplink_Backend.modules.user.service.UserProfileService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);

    private final UserProfileService profileService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public ResumeService(UserProfileService profileService,
                         DocumentRepository documentRepository,
                         UserRepository userRepository,
                         CloudinaryService cloudinaryService) {
        this.profileService = profileService;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public ResumeDto generateResume(UUID userId, ResumeGenerateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        DigitalPortfolioDto portfolio = profileService.getDigitalPortfolio(userId);

        String template = request.getTemplate() != null ? request.getTemplate().toUpperCase() : "MODERN";
        String targetRole = request.getTargetRole() != null && !request.getTargetRole().isBlank()
                ? request.getTargetRole().trim()
                : (portfolio.getProfile() != null && portfolio.getProfile().getHeadline() != null
                ? portfolio.getProfile().getHeadline()
                : "Software Engineer");

        // Count previous resumes for versioning
        List<Document> previousResumes = documentRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .filter(d -> "RESUME".equalsIgnoreCase(d.getType()))
                .collect(Collectors.toList());
        int newVersion = previousResumes.size() + 1;

        String safeName = user.getFullName() != null
                ? user.getFullName().trim().replaceAll("[^a-zA-Z0-9]", "_")
                : "Candidate";
        String fileName = safeName + "_Resume_v" + newVersion + "_" + template.toLowerCase() + ".pdf";

        // 1. Build ATS-compliant HTML
        String html = buildResumeHtml(portfolio, request, template, targetRole);

        // 2. Render to crisp PDF bytes with OpenHTMLtoPDF
        byte[] pdfBytes = renderHtmlToPdf(html);

        // 3. Upload to Cloudinary (raw resource_type, preserving .pdf extension)
        String folder = "uplink/resumes/" + userId;
        String pdfUrl = cloudinaryService.uploadRawFile(pdfBytes, fileName, folder);

        // 4. Generate matching LaTeX source
        String latex = buildLatexSource(portfolio, request, targetRole);

        // 5. Persist RESUME Document row
        Document doc = new Document(user, fileName, pdfUrl, "RESUME");
        Document savedDoc = documentRepository.save(doc);

        Instant now = Instant.now();
        return new ResumeDto(
                savedDoc.getId(),
                user.getId(),
                newVersion,
                template,
                targetRole,
                fileName,
                latex,
                pdfUrl,
                now
        );
    }

    @Transactional(readOnly = true)
    public List<ResumeDto> getUserResumes(UUID userId) {
        List<Document> docs = documentRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .filter(d -> "RESUME".equalsIgnoreCase(d.getType()))
                .collect(Collectors.toList());

        List<ResumeDto> list = new ArrayList<>();
        int count = docs.size();
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            int version = count - i;
            list.add(new ResumeDto(
                    d.getId(),
                    userId,
                    version,
                    "MODERN",
                    "Resume Version " + version,
                    d.getName(),
                    "",
                    d.getFileUrl(),
                    d.getUploadedAt()
            ));
        }
        return list;
    }

    @Transactional
    public void deleteResume(UUID userId, UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        if (!doc.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You are not authorized to delete this document");
        }

        cloudinaryService.deleteAsset(doc.getFileUrl(), "raw");
        documentRepository.delete(doc);
    }

    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to render PDF using OpenHTMLtoPDF: {}", e.getMessage(), e);
            throw new RuntimeException("PDF rendering failed: " + e.getMessage(), e);
        }
    }

    private String buildResumeHtml(DigitalPortfolioDto p, ResumeGenerateRequest req, String template, String targetRole) {
        boolean isClassic = "CLASSIC".equalsIgnoreCase(template);
        boolean isAcademic = "ACADEMIC".equalsIgnoreCase(template);
        boolean isExecutive = "EXECUTIVE".equalsIgnoreCase(template);

        String fontFamily = isClassic ? "'Times New Roman', Times, serif" : "Arial, Helvetica, sans-serif";
        String primaryColor = isClassic ? "#111827" : (isExecutive ? "#0F172A" : (isAcademic ? "#1E3A8A" : "#1E40AF"));
        String accentColor = isClassic ? "#374151" : "#2563EB";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\" />\n");
        sb.append("<title>Resume - ").append(escapeHtml(p.getFullName())).append("</title>\n");
        sb.append("<style>\n");
        sb.append("@page { size: letter; margin: 0.55in; }\n");
        sb.append("body { font-family: ").append(fontFamily).append("; font-size: 10pt; line-height: 1.35; color: #1F2937; margin: 0; padding: 0; }\n");
        sb.append(".header { text-align: center; margin-bottom: 12px; border-bottom: 2px solid ").append(primaryColor).append("; padding-bottom: 8px; }\n");
        sb.append(".name { font-size: 20pt; font-weight: bold; color: ").append(primaryColor).append("; text-transform: uppercase; letter-spacing: 0.5px; margin: 0; }\n");
        sb.append(".target-role { font-size: 11pt; font-weight: 600; color: ").append(accentColor).append("; margin-top: 3px; }\n");
        sb.append(".contact { font-size: 9pt; color: #4B5563; margin-top: 4px; }\n");
        sb.append(".section { margin-top: 10px; margin-bottom: 8px; }\n");
        sb.append(".section-title { font-size: 11pt; font-weight: bold; text-transform: uppercase; color: ").append(primaryColor).append("; border-bottom: 1px solid #D1D5DB; padding-bottom: 2px; margin-bottom: 6px; letter-spacing: 0.5px; }\n");
        sb.append(".item-header { width: 100%; margin-bottom: 2px; }\n");
        sb.append(".item-title { font-weight: bold; font-size: 10pt; color: #111827; }\n");
        sb.append(".item-subtitle { font-style: italic; font-size: 9.5pt; color: #374151; }\n");
        sb.append(".item-date { text-align: right; font-size: 9pt; color: #4B5563; }\n");
        sb.append(".item-desc { font-size: 9pt; color: #374151; margin-top: 2px; margin-bottom: 6px; }\n");
        sb.append("ul { margin: 2px 0 6px 16px; padding: 0; }\n");
        sb.append("li { font-size: 9pt; color: #374151; margin-bottom: 2px; }\n");
        sb.append(".skills-grid { font-size: 9pt; line-height: 1.4; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        // Header
        sb.append("<div class=\"header\">\n");
        sb.append("  <div class=\"name\">").append(escapeHtml(p.getFullName())).append("</div>\n");
        sb.append("  <div class=\"target-role\">").append(escapeHtml(targetRole)).append("</div>\n");
        sb.append("  <div class=\"contact\">");
        sb.append(escapeHtml(p.getEmail()));
        if (p.getPhone() != null && !p.getPhone().isBlank()) {
            sb.append(" &bull; ").append(escapeHtml(p.getPhone()));
        }
        if (p.getProfile() != null && p.getProfile().getLocation() != null && !p.getProfile().getLocation().isBlank()) {
            sb.append(" &bull; ").append(escapeHtml(p.getProfile().getLocation()));
        }
        sb.append("</div>\n");
        sb.append("</div>\n");

        // Professional Summary
        if (p.getProfile() != null && p.getProfile().getBio() != null && !p.getProfile().getBio().isBlank()) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Professional Summary</div>\n");
            sb.append("  <div class=\"item-desc\">").append(escapeHtml(p.getProfile().getBio())).append("</div>\n");
            sb.append("</div>\n");
        }

        // Education
        if (p.getEducation() != null && !p.getEducation().isEmpty()) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Education</div>\n");
            for (Education edu : p.getEducation()) {
                sb.append("  <table style=\"width: 100%;\">\n    <tr>\n");
                sb.append("      <td class=\"item-title\">").append(escapeHtml(edu.getCollege())).append("</td>\n");
                sb.append("      <td class=\"item-date\">").append(edu.getStartYear()).append(" &ndash; ")
                        .append(Boolean.TRUE.equals(edu.getIsCurrent()) ? "Present" : String.valueOf(edu.getEndYear()))
                        .append("</td>\n    </tr>\n    <tr>\n");
                sb.append("      <td class=\"item-subtitle\" colspan=\"2\">").append(escapeHtml(edu.getDegree()))
                        .append(edu.getBranch() != null ? " in " + escapeHtml(edu.getBranch()) : "")
                        .append(edu.getCgpa() != null ? " &bull; CGPA: " + edu.getCgpa() : "")
                        .append("</td>\n    </tr>\n  </table>\n");
            }
            sb.append("</div>\n");
        }

        // Technical Skills
        if (p.getSkills() != null && !p.getSkills().isEmpty()) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Technical Skills</div>\n");
            String skillList = p.getSkills().stream()
                    .map(us -> us.getSkill() != null ? us.getSkill().getName() : "")
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
            sb.append("  <div class=\"skills-grid\"><strong>Proficiencies &amp; Tools:</strong> ")
                    .append(escapeHtml(skillList)).append("</div>\n");
            sb.append("</div>\n");
        }

        // Experience
        if (p.getExperiences() != null && !p.getExperiences().isEmpty()) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Work Experience</div>\n");
            for (Experience exp : p.getExperiences()) {
                sb.append("  <table style=\"width: 100%;\">\n    <tr>\n");
                sb.append("      <td class=\"item-title\">").append(escapeHtml(exp.getTitle()))
                        .append(" &ndash; <span class=\"item-subtitle\">").append(escapeHtml(exp.getCompany())).append("</span></td>\n");
                sb.append("      <td class=\"item-date\">")
                        .append(exp.getStartDate() != null ? exp.getStartDate().toString() : "")
                        .append(" &ndash; ")
                        .append(exp.getEndDate() != null ? exp.getEndDate().toString() : "Present")
                        .append("</td>\n    </tr>\n  </table>\n");
                if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                    sb.append("  <div class=\"item-desc\">").append(escapeHtml(exp.getDescription())).append("</div>\n");
                }
            }
            sb.append("</div>\n");
        }

        // Projects
        if (req.isIncludeProjects() && p.getProjects() != null && !p.getProjects().isEmpty()) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Featured Projects</div>\n");
            for (Project proj : p.getProjects()) {
                sb.append("  <table style=\"width: 100%;\">\n    <tr>\n");
                sb.append("      <td class=\"item-title\">").append(escapeHtml(proj.getTitle())).append("</td>\n");
                if (proj.getGithubUrl() != null && !proj.getGithubUrl().isBlank()) {
                    sb.append("      <td class=\"item-date\">").append(escapeHtml(proj.getGithubUrl())).append("</td>\n");
                }
                sb.append("    </tr>\n  </table>\n");
                if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
                    sb.append("  <div class=\"item-desc\">").append(escapeHtml(proj.getDescription())).append("</div>\n");
                }
                if (proj.getTechStack() != null && !proj.getTechStack().isBlank()) {
                    sb.append("  <div style=\"font-size: 8.5pt; color: #4B5563;\"><em>Tech Stack: ")
                            .append(escapeHtml(proj.getTechStack())).append("</em></div>\n");
                }
            }
            sb.append("</div>\n");
        }

        // Certifications
        if (req.isIncludeCertifications() && p.getCertifications() != null && !p.getCertifications().isEmpty()) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Certifications &amp; Licenses</div>\n");
            for (Certification cert : p.getCertifications()) {
                sb.append("  <table style=\"width: 100%;\">\n    <tr>\n");
                sb.append("      <td class=\"item-title\">").append(escapeHtml(cert.getTitle()))
                        .append(" &ndash; <span class=\"item-subtitle\">").append(escapeHtml(cert.getIssuer())).append("</span></td>\n");
                sb.append("      <td class=\"item-date\">")
                        .append(cert.getIssueDate() != null ? cert.getIssueDate().toString() : "")
                        .append("</td>\n    </tr>\n  </table>\n");
            }
            sb.append("</div>\n");
        }

        // Achievements
        if (req.isIncludeAchievements() && p.getAchievements() != null && !p.getAchievements().isEmpty()) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Honors &amp; Awards</div>\n");
            for (Achievement ach : p.getAchievements()) {
                sb.append("  <table style=\"width: 100%;\">\n    <tr>\n");
                sb.append("      <td class=\"item-title\">").append(escapeHtml(ach.getTitle())).append("</td>\n");
                sb.append("      <td class=\"item-date\">").append(ach.getDate() != null ? ach.getDate().toString() : "").append("</td>\n");
                sb.append("    </tr>\n  </table>\n");
                if (ach.getDescription() != null && !ach.getDescription().isBlank()) {
                    sb.append("  <div class=\"item-desc\">").append(escapeHtml(ach.getDescription())).append("</div>\n");
                }
            }
            sb.append("</div>\n");
        }

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private String buildLatexSource(DigitalPortfolioDto p, ResumeGenerateRequest req, String targetRole) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\documentclass[letterpaper,10pt]{article}\n");
        sb.append("\\usepackage{latexsym,titlesec,marvosym,graphicx,amsmath}\n");
        sb.append("\\usepackage[empty]{fullpage}\n");
        sb.append("\\usepackage[hidelinks]{hyperref}\n\n");
        sb.append("\\begin{document}\n\n");

        // Header
        sb.append("\\begin{center}\n");
        sb.append("  {\\Huge \\scshape ").append(escapeLatex(p.getFullName())).append("} \\\\\n");
        sb.append("  \\large ").append(escapeLatex(targetRole)).append(" \\\\\n");
        sb.append("  \\small ").append(p.getEmail());
        if (p.getPhone() != null && !p.getPhone().isBlank()) sb.append(" $|$ ").append(p.getPhone());
        if (p.getProfile() != null && p.getProfile().getLocation() != null && !p.getProfile().getLocation().isBlank()) {
            sb.append(" $|$ ").append(p.getProfile().getLocation());
        }
        sb.append("\n\\end{center}\n\n");

        // Summary
        if (p.getProfile() != null && p.getProfile().getBio() != null && !p.getProfile().getBio().isBlank()) {
            sb.append("\\section*{Professional Summary}\n");
            sb.append(escapeLatex(p.getProfile().getBio())).append("\n\n");
        }

        // Education
        if (p.getEducation() != null && !p.getEducation().isEmpty()) {
            sb.append("\\section*{Education}\n\\begin{itemize}\n");
            for (Education edu : p.getEducation()) {
                sb.append("  \\item \\textbf{").append(escapeLatex(edu.getCollege())).append("} -- ")
                        .append(escapeLatex(edu.getDegree())).append(" in ").append(escapeLatex(edu.getBranch()))
                        .append(" (").append(edu.getStartYear()).append(" -- ")
                        .append(Boolean.TRUE.equals(edu.getIsCurrent()) ? "Present" : edu.getEndYear()).append(")")
                        .append(edu.getCgpa() != null ? " [CGPA: " + edu.getCgpa() + "]" : "")
                        .append("\n");
            }
            sb.append("\\end{itemize}\n\n");
        }

        // Skills
        if (p.getSkills() != null && !p.getSkills().isEmpty()) {
            sb.append("\\section*{Technical Skills}\n");
            String skillList = p.getSkills().stream()
                    .map(us -> us.getSkill() != null ? us.getSkill().getName() : "")
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
            sb.append("\\textbf{Core Competencies:} ").append(escapeLatex(skillList)).append("\n\n");
        }

        // Experience
        if (p.getExperiences() != null && !p.getExperiences().isEmpty()) {
            sb.append("\\section*{Experience}\n\\begin{itemize}\n");
            for (Experience exp : p.getExperiences()) {
                sb.append("  \\item \\textbf{").append(escapeLatex(exp.getTitle())).append("} at ")
                        .append(escapeLatex(exp.getCompany())).append(" (").append(exp.getStartDate()).append(" -- ")
                        .append(exp.getEndDate() != null ? exp.getEndDate().toString() : "Present").append(")\\\\\n")
                        .append("  ").append(escapeLatex(exp.getDescription() != null ? exp.getDescription() : "")).append("\n");
            }
            sb.append("\\end{itemize}\n\n");
        }

        // Projects
        if (req.isIncludeProjects() && p.getProjects() != null && !p.getProjects().isEmpty()) {
            sb.append("\\section*{Projects}\n\\begin{itemize}\n");
            for (Project proj : p.getProjects()) {
                sb.append("  \\item \\textbf{").append(escapeLatex(proj.getTitle())).append("} \\\\\n")
                        .append("  ").append(escapeLatex(proj.getDescription() != null ? proj.getDescription() : "")).append("\n");
                if (proj.getTechStack() != null && !proj.getTechStack().isBlank()) {
                    sb.append("  \\textit{Tech Stack: ").append(escapeLatex(proj.getTechStack())).append("}\n");
                }
            }
            sb.append("\\end{itemize}\n\n");
        }

        // Certifications
        if (req.isIncludeCertifications() && p.getCertifications() != null && !p.getCertifications().isEmpty()) {
            sb.append("\\section*{Certifications}\n\\begin{itemize}\n");
            for (Certification cert : p.getCertifications()) {
                sb.append("  \\item \\textbf{").append(escapeLatex(cert.getTitle())).append("} -- ")
                        .append(escapeLatex(cert.getIssuer())).append("\n");
            }
            sb.append("\\end{itemize}\n\n");
        }

        // Achievements
        if (req.isIncludeAchievements() && p.getAchievements() != null && !p.getAchievements().isEmpty()) {
            sb.append("\\section*{Honors \\& Awards}\n\\begin{itemize}\n");
            for (Achievement ach : p.getAchievements()) {
                sb.append("  \\item \\textbf{").append(escapeLatex(ach.getTitle())).append("} (")
                        .append(ach.getDate() != null ? ach.getDate().toString() : "").append(")\\\\\n")
                        .append("  ").append(escapeLatex(ach.getDescription() != null ? ach.getDescription() : "")).append("\n");
            }
            sb.append("\\end{itemize}\n\n");
        }

        sb.append("\\end{document}\n");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeLatex(String text) {
        if (text == null) return "";
        return text.replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_");
    }
}
