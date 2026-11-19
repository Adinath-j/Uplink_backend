package com.example.Uplink_Backend.modules.user.service;

import com.example.Uplink_Backend.common.exception.ResourceNotFoundException;
import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import com.example.Uplink_Backend.modules.user.dto.*;
import com.example.Uplink_Backend.modules.user.entity.*;
import com.example.Uplink_Backend.modules.user.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final CertificationRepository certificationRepository;
    private final AchievementRepository achievementRepository;

    public UserProfileService(UserRepository userRepository,
                              ProfileRepository profileRepository,
                              EducationRepository educationRepository,
                              SkillRepository skillRepository,
                              UserSkillRepository userSkillRepository,
                              ProjectRepository projectRepository,
                              ExperienceRepository experienceRepository,
                              CertificationRepository certificationRepository,
                              AchievementRepository achievementRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.educationRepository = educationRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.projectRepository = projectRepository;
        this.experienceRepository = experienceRepository;
        this.certificationRepository = certificationRepository;
        this.achievementRepository = achievementRepository;
    }

    @Cacheable(value = "profiles", key = "#userId")
    public Profile getOrCreateProfile(UUID userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            Profile profile = new Profile(user);
            return profileRepository.save(profile);
        });
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profiles", key = "#userId"),
            @CacheEvict(value = "portfolios", key = "#userId")
    })
    public Profile updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        userRepository.save(user);

        Profile profile = getOrCreateProfile(userId);
        if (request.getHeadline() != null) profile.setHeadline(request.getHeadline());
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getLocation() != null) profile.setLocation(request.getLocation());
        if (request.getProfilePicture() != null) profile.setProfilePicture(request.getProfilePicture());

        return profileRepository.save(profile);
    }

    @Cacheable(value = "portfolios", key = "#userId")
    public DigitalPortfolioDto getDigitalPortfolio(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Profile profile = getOrCreateProfile(userId);
        List<Education> educations = educationRepository.findByUserIdOrderByStartYearDesc(userId);
        List<UserSkill> skills = userSkillRepository.findByUserId(userId);
        List<Project> projects = projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Experience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(userId);
        List<Certification> certifications = certificationRepository.findByUserIdOrderByIssueDateDesc(userId);
        List<Achievement> achievements = achievementRepository.findByUserIdOrderByDateDesc(userId);

        return new DigitalPortfolioDto(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                profile, educations, skills, projects, experiences, certifications, achievements);
    }

    // Education
    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Education addEducation(UUID userId, Education education) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        education.setUser(user);
        return educationRepository.save(education);
    }

    public List<Education> getEducations(UUID userId) {
        return educationRepository.findByUserIdOrderByStartYearDesc(userId);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public void deleteEducation(UUID userId, UUID educationId) {
        Education edu = educationRepository.findById(educationId)
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));
        if (edu.getUser().getId().equals(userId)) {
            educationRepository.delete(edu);
        }
    }

    // Skills
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "skills", key = "#userId"),
            @CacheEvict(value = "portfolios", key = "#userId")
    })
    public UserSkill addSkill(UUID userId, AddSkillRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Skill skill = skillRepository.findByNameIgnoreCase(request.getSkillName().trim())
                .orElseGet(() -> skillRepository.save(new Skill(request.getSkillName().trim(), request.getCategory() != null ? request.getCategory() : "General")));

        UserSkill userSkill = userSkillRepository.findByUserIdAndSkillId(userId, skill.getId())
                .orElseGet(() -> {
                    UserSkill us = new UserSkill();
                    us.setUser(user);
                    us.setSkill(skill);
                    return us;
                });

        userSkill.setProficiency(request.getProficiency());
        userSkill.setYearsOfExp(request.getYearsOfExp());
        return userSkillRepository.save(userSkill);
    }

    @Cacheable(value = "skills", key = "#userId")
    public List<UserSkill> getUserSkills(UUID userId) {
        return userSkillRepository.findByUserId(userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "skills", key = "#userId"),
            @CacheEvict(value = "portfolios", key = "#userId")
    })
    public void deleteSkill(UUID userId, UUID userSkillId) {
        UserSkill us = userSkillRepository.findById(userSkillId)
                .orElseThrow(() -> new ResourceNotFoundException("UserSkill", "id", userSkillId));
        if (us.getUser().getId().equals(userId)) {
            userSkillRepository.delete(us);
        }
    }

    // Projects
    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Project addProject(UUID userId, Project project) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        project.setUser(user);
        return projectRepository.save(project);
    }

    public List<Project> getProjects(UUID userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public void deleteProject(UUID userId, UUID projectId) {
        Project proj = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (proj.getUser().getId().equals(userId)) {
            projectRepository.delete(proj);
        }
    }

    // Experiences
    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Experience addExperience(UUID userId, Experience experience) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        experience.setUser(user);
        return experienceRepository.save(experience);
    }

    public List<Experience> getExperiences(UUID userId) {
        return experienceRepository.findByUserIdOrderByStartDateDesc(userId);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public void deleteExperience(UUID userId, UUID experienceId) {
        Experience exp = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", experienceId));
        if (exp.getUser().getId().equals(userId)) {
            experienceRepository.delete(exp);
        }
    }

    // Certifications
    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Certification addCertification(UUID userId, Certification cert) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        cert.setUser(user);
        return certificationRepository.save(cert);
    }

    public List<Certification> getCertifications(UUID userId) {
        return certificationRepository.findByUserIdOrderByIssueDateDesc(userId);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public void deleteCertification(UUID userId, UUID certId) {
        Certification cert = certificationRepository.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", "id", certId));
        if (cert.getUser().getId().equals(userId)) {
            certificationRepository.delete(cert);
        }
    }

    // Achievements
    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Achievement addAchievement(UUID userId, Achievement achievement) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        achievement.setUser(user);
        return achievementRepository.save(achievement);
    }

    public List<Achievement> getAchievements(UUID userId) {
        return achievementRepository.findByUserIdOrderByDateDesc(userId);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public void deleteAchievement(UUID userId, UUID achievementId) {
        Achievement ach = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", achievementId));
        if (ach.getUser().getId().equals(userId)) {
            achievementRepository.delete(ach);
        }
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Education updateEducation(UUID userId, UUID educationId, Education update) {
        Education edu = educationRepository.findById(educationId)
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));
        if (!edu.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Cannot update another user's education record");
        }
        if (update.getDegree() != null) edu.setDegree(update.getDegree());
        if (update.getCollege() != null) edu.setCollege(update.getCollege());
        if (update.getBranch() != null) edu.setBranch(update.getBranch());
        if (update.getStartYear() != null) edu.setStartYear(update.getStartYear());
        if (update.getEndYear() != null) edu.setEndYear(update.getEndYear());
        if (update.getCgpa() != null) edu.setCgpa(update.getCgpa());
        if (update.getIsCurrent() != null) edu.setIsCurrent(update.getIsCurrent());
        return educationRepository.save(edu);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Project updateProject(UUID userId, UUID projectId, Project update) {
        Project proj = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (!proj.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Cannot update another user's project");
        }
        if (update.getTitle() != null) proj.setTitle(update.getTitle());
        if (update.getDescription() != null) proj.setDescription(update.getDescription());
        if (update.getTechStack() != null) proj.setTechStack(update.getTechStack());
        if (update.getGithubUrl() != null) proj.setGithubUrl(update.getGithubUrl());
        if (update.getLiveUrl() != null) proj.setLiveUrl(update.getLiveUrl());
        return projectRepository.save(proj);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Experience updateExperience(UUID userId, UUID experienceId, Experience update) {
        Experience exp = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", experienceId));
        if (!exp.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Cannot update another user's experience");
        }
        if (update.getTitle() != null) exp.setTitle(update.getTitle());
        if (update.getCompany() != null) exp.setCompany(update.getCompany());
        if (update.getLocation() != null) exp.setLocation(update.getLocation());
        if (update.getStartDate() != null) exp.setStartDate(update.getStartDate());
        if (update.getEndDate() != null) exp.setEndDate(update.getEndDate());
        if (update.getDescription() != null) exp.setDescription(update.getDescription());
        return experienceRepository.save(exp);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Certification updateCertification(UUID userId, UUID certId, Certification update) {
        Certification cert = certificationRepository.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", "id", certId));
        if (!cert.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Cannot update another user's certification");
        }
        if (update.getTitle() != null) cert.setTitle(update.getTitle());
        if (update.getIssuer() != null) cert.setIssuer(update.getIssuer());
        if (update.getIssueDate() != null) cert.setIssueDate(update.getIssueDate());
        if (update.getCredentialUrl() != null) cert.setCredentialUrl(update.getCredentialUrl());
        return certificationRepository.save(cert);
    }

    @Transactional
    @CacheEvict(value = "portfolios", key = "#userId")
    public Achievement updateAchievement(UUID userId, UUID achievementId, Achievement update) {
        Achievement ach = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", achievementId));
        if (!ach.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Cannot update another user's achievement");
        }
        if (update.getTitle() != null) ach.setTitle(update.getTitle());
        if (update.getDescription() != null) ach.setDescription(update.getDescription());
        if (update.getDate() != null) ach.setDate(update.getDate());
        return achievementRepository.save(ach);
    }
}
