package com.example.Uplink_Backend.modules.user.controller;

import com.example.Uplink_Backend.common.dto.ApiResponse;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.modules.user.dto.*;
import com.example.Uplink_Backend.modules.user.entity.*;
import com.example.Uplink_Backend.modules.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.Uplink_Backend.common.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = {"/api/profile", "/api/profiles"})
@Tag(name = "2. User & Profile Module", description = "Endpoints for Profile management, Academic Info, Skills, Portfolio Projects, Experiences, Certifications, and Achievements")
public class UserProfileController {

    private final UserProfileService profileService;
    private final CloudinaryService cloudinaryService;

    public UserProfileController(UserProfileService profileService, CloudinaryService cloudinaryService) {
        this.profileService = profileService;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(value = {"/avatar", "/me/avatar"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile picture / avatar to Cloudinary")
    public ResponseEntity<ApiResponse<Profile>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = cloudinaryService.uploadAvatarImage(file, userPrincipal.getId());
        Profile updated = profileService.updateAvatar(userPrincipal.getId(), avatarUrl);
        return ResponseEntity.ok(ApiResponse.ok("Avatar updated successfully", updated));
    }

    @GetMapping
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<Profile>> getMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Profile profile = profileService.getOrCreateProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", profile));
    }

    @PutMapping
    @Operation(summary = "Update current user profile and contact details")
    public ResponseEntity<ApiResponse<Profile>> updateMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                @RequestBody ProfileUpdateRequest request) {
        Profile updated = profileService.updateProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    @GetMapping("/portfolio")
    @Operation(summary = "Get full digital portfolio (all academic, skills, projects, and experiences)")
    public ResponseEntity<ApiResponse<DigitalPortfolioDto>> getMyPortfolio(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        DigitalPortfolioDto portfolio = profileService.getDigitalPortfolio(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Digital portfolio retrieved", portfolio));
    }

    @GetMapping("/portfolio/{userId}")
    @Operation(summary = "View public digital portfolio of any candidate/user")
    public ResponseEntity<ApiResponse<DigitalPortfolioDto>> getUserPortfolio(@PathVariable UUID userId) {
        DigitalPortfolioDto portfolio = profileService.getDigitalPortfolio(userId);
        return ResponseEntity.ok(ApiResponse.ok("Candidate portfolio retrieved", portfolio));
    }

    // Education
    @PostMapping("/education")
    @Operation(summary = "Add education record")
    public ResponseEntity<ApiResponse<Education>> addEducation(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                               @Valid @RequestBody Education education) {
        Education created = profileService.addEducation(userPrincipal.getId(), education);
        return ResponseEntity.ok(ApiResponse.created("Education record added", created));
    }

    @GetMapping("/education")
    @Operation(summary = "Get education records for current user")
    public ResponseEntity<ApiResponse<List<Education>>> getEducations(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getEducations(userPrincipal.getId())));
    }

    @DeleteMapping("/education/{id}")
    @Operation(summary = "Delete education record")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                             @PathVariable UUID id) {
        profileService.deleteEducation(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Education record deleted", null));
    }

    @PutMapping("/education/{id}")
    @Operation(summary = "Update education record")
    public ResponseEntity<ApiResponse<Education>> updateEducation(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                  @PathVariable UUID id,
                                                                  @RequestBody Education education) {
        Education updated = profileService.updateEducation(userPrincipal.getId(), id, education);
        return ResponseEntity.ok(ApiResponse.ok("Education record updated", updated));
    }

    // Skills
    @PostMapping("/skills")
    @Operation(summary = "Add or update a skill with proficiency level")
    public ResponseEntity<ApiResponse<UserSkill>> addSkill(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                           @Valid @RequestBody AddSkillRequest request) {
        UserSkill created = profileService.addSkill(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.created("Skill added to profile", created));
    }

    @GetMapping("/skills")
    @Operation(summary = "Get all skills for current user")
    public ResponseEntity<ApiResponse<List<UserSkill>>> getSkills(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getUserSkills(userPrincipal.getId())));
    }

    @DeleteMapping("/skills/{id}")
    @Operation(summary = "Remove skill from profile")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @PathVariable UUID id) {
        profileService.deleteSkill(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Skill removed", null));
    }

    // Projects
    @PostMapping("/projects")
    @Operation(summary = "Add portfolio project")
    public ResponseEntity<ApiResponse<Project>> addProject(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                           @RequestBody Project project) {
        Project created = profileService.addProject(userPrincipal.getId(), project);
        return ResponseEntity.ok(ApiResponse.created("Project added", created));
    }

    @GetMapping("/projects")
    @Operation(summary = "Get user projects")
    public ResponseEntity<ApiResponse<List<Project>>> getProjects(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProjects(userPrincipal.getId())));
    }

    @DeleteMapping("/projects/{id}")
    @Operation(summary = "Delete portfolio project")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                           @PathVariable UUID id) {
        profileService.deleteProject(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Project deleted", null));
    }

    @PutMapping("/projects/{id}")
    @Operation(summary = "Update portfolio project")
    public ResponseEntity<ApiResponse<Project>> updateProject(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                              @PathVariable UUID id,
                                                              @RequestBody Project project) {
        Project updated = profileService.updateProject(userPrincipal.getId(), id, project);
        return ResponseEntity.ok(ApiResponse.ok("Project updated", updated));
    }

    // Experiences
    @PostMapping("/experiences")
    @Operation(summary = "Add experience record")
    public ResponseEntity<ApiResponse<Experience>> addExperience(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                 @RequestBody Experience experience) {
        Experience created = profileService.addExperience(userPrincipal.getId(), experience);
        return ResponseEntity.ok(ApiResponse.created("Experience added", created));
    }

    @GetMapping("/experiences")
    @Operation(summary = "Get user experiences")
    public ResponseEntity<ApiResponse<List<Experience>>> getExperiences(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getExperiences(userPrincipal.getId())));
    }

    @DeleteMapping("/experiences/{id}")
    @Operation(summary = "Delete experience record")
    public ResponseEntity<ApiResponse<Void>> deleteExperience(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                              @PathVariable UUID id) {
        profileService.deleteExperience(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Experience deleted", null));
    }

    @PutMapping("/experiences/{id}")
    @Operation(summary = "Update experience record")
    public ResponseEntity<ApiResponse<Experience>> updateExperience(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                    @PathVariable UUID id,
                                                                    @RequestBody Experience experience) {
        Experience updated = profileService.updateExperience(userPrincipal.getId(), id, experience);
        return ResponseEntity.ok(ApiResponse.ok("Experience updated", updated));
    }

    // Certifications
    @PostMapping("/certifications")
    @Operation(summary = "Add certification")
    public ResponseEntity<ApiResponse<Certification>> addCertification(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                       @RequestBody Certification certification) {
        Certification created = profileService.addCertification(userPrincipal.getId(), certification);
        return ResponseEntity.ok(ApiResponse.created("Certification added", created));
    }

    @GetMapping("/certifications")
    @Operation(summary = "Get user certifications")
    public ResponseEntity<ApiResponse<List<Certification>>> getCertifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getCertifications(userPrincipal.getId())));
    }

    @DeleteMapping("/certifications/{id}")
    @Operation(summary = "Delete certification")
    public ResponseEntity<ApiResponse<Void>> deleteCertification(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                 @PathVariable UUID id) {
        profileService.deleteCertification(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Certification deleted", null));
    }

    @PutMapping("/certifications/{id}")
    @Operation(summary = "Update certification")
    public ResponseEntity<ApiResponse<Certification>> updateCertification(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @PathVariable UUID id,
                                                                          @RequestBody Certification certification) {
        Certification updated = profileService.updateCertification(userPrincipal.getId(), id, certification);
        return ResponseEntity.ok(ApiResponse.ok("Certification updated", updated));
    }

    // Achievements
    @PostMapping("/achievements")
    @Operation(summary = "Add achievement")
    public ResponseEntity<ApiResponse<Achievement>> addAchievement(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                   @RequestBody Achievement achievement) {
        Achievement created = profileService.addAchievement(userPrincipal.getId(), achievement);
        return ResponseEntity.ok(ApiResponse.created("Achievement added", created));
    }

    @GetMapping("/achievements")
    @Operation(summary = "Get user achievements")
    public ResponseEntity<ApiResponse<List<Achievement>>> getAchievements(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getAchievements(userPrincipal.getId())));
    }

    @DeleteMapping("/achievements/{id}")
    @Operation(summary = "Delete achievement")
    public ResponseEntity<ApiResponse<Void>> deleteAchievement(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                               @PathVariable UUID id) {
        profileService.deleteAchievement(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Achievement deleted", null));
    }

    @PutMapping("/achievements/{id}")
    @Operation(summary = "Update achievement")
    public ResponseEntity<ApiResponse<Achievement>> updateAchievement(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                      @PathVariable UUID id,
                                                                      @RequestBody Achievement achievement) {
        Achievement updated = profileService.updateAchievement(userPrincipal.getId(), id, achievement);
        return ResponseEntity.ok(ApiResponse.ok("Achievement updated", updated));
    }
}
