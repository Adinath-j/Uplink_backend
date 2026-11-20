package com.example.Uplink_Backend.modules.resume.controller;

import com.example.Uplink_Backend.common.dto.ApiResponse;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.modules.resume.dto.ResumeDto;
import com.example.Uplink_Backend.modules.resume.dto.ResumeGenerateRequest;
import com.example.Uplink_Backend.modules.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = {"/api/resume", "/api/resumes"})
@Tag(name = "11. Resume Module", description = "Endpoints for AI Resume Generation, LaTeX Compilation, PDF Export, and Version Management")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(value = {"", "/generate"})
    @Operation(summary = "Generate customized resume with OpenHTMLtoPDF compilation, Cloudinary upload, and PDF URL")
    public ResponseEntity<ApiResponse<ResumeDto>> generateResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ResumeGenerateRequest request) {
        ResumeDto resume = resumeService.generateResume(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Resume generated successfully", resume));
    }

    @GetMapping
    @Operation(summary = "Get all generated resumes for current user")
    public ResponseEntity<ApiResponse<List<ResumeDto>>> getMyResumes(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ResumeDto> list = resumeService.getUserResumes(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resumes retrieved successfully", list));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete generated resume document")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        resumeService.deleteResume(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Resume document deleted successfully", null));
    }

    @GetMapping("/download/{userId}")
    @Operation(summary = "Download compiled LaTeX / mock PDF for candidate resume")
    public ResponseEntity<byte[]> downloadResume(@PathVariable UUID userId, @RequestParam(defaultValue = "1") int version) {
        String content = "% Resume compiled by Uplink Platform for User " + userId + " (Version " + version + ")";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume_v" + version + ".tex\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
