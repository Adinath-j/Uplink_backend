package com.example.Uplink_Backend.modules.resume.dto;

import java.time.Instant;
import java.util.UUID;

public class ResumeDto {
    private UUID id;
    private UUID documentId;
    private UUID userId;
    private int version;
    private String template;
    private String targetJobTitle;
    private String fileName;
    private String latexSource;
    private String pdfUrl;
    private String pdfDownloadUrl;
    private Instant createdAt;
    private Instant generatedAt;

    public ResumeDto() {}

    public ResumeDto(UUID id, UUID userId, int version, String template, String targetJobTitle,
                     String fileName, String latexSource, String pdfUrl, Instant createdAt) {
        this.id = id;
        this.documentId = id;
        this.userId = userId;
        this.version = version;
        this.template = template;
        this.targetJobTitle = targetJobTitle;
        this.fileName = fileName;
        this.latexSource = latexSource;
        this.pdfUrl = pdfUrl;
        this.pdfDownloadUrl = pdfUrl;
        this.createdAt = createdAt;
        this.generatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
        this.documentId = id;
    }

    public UUID getDocumentId() {
        return documentId != null ? documentId : id;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
        if (this.id == null) this.id = documentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getTargetJobTitle() {
        return targetJobTitle;
    }

    public void setTargetJobTitle(String targetJobTitle) {
        this.targetJobTitle = targetJobTitle;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLatexSource() {
        return latexSource;
    }

    public void setLatexSource(String latexSource) {
        this.latexSource = latexSource;
    }

    public String getPdfUrl() {
        return pdfUrl != null ? pdfUrl : pdfDownloadUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
        this.pdfDownloadUrl = pdfUrl;
    }

    public String getPdfDownloadUrl() {
        return pdfDownloadUrl != null ? pdfDownloadUrl : pdfUrl;
    }

    public void setPdfDownloadUrl(String pdfDownloadUrl) {
        this.pdfDownloadUrl = pdfDownloadUrl;
        if (this.pdfUrl == null) this.pdfUrl = pdfDownloadUrl;
    }

    public Instant getCreatedAt() {
        return createdAt != null ? createdAt : generatedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        this.generatedAt = createdAt;
    }

    public Instant getGeneratedAt() {
        return generatedAt != null ? generatedAt : createdAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
        if (this.createdAt == null) this.createdAt = generatedAt;
    }
}
