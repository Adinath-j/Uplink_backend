package com.example.Uplink_Backend.modules.qa.controller;

import com.example.Uplink_Backend.common.dto.ApiResponse;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.modules.qa.dto.*;
import com.example.Uplink_Backend.modules.qa.service.QAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
@Tag(name = "13. Q&A Forum", description = "Student Community Questions, Answers, Doubts, and Topic Filtering")
public class QAController {

    private final QAService qaService;

    public QAController(QAService qaService) {
        this.qaService = qaService;
    }

    @PostMapping
    @Operation(summary = "Ask a new student question")
    public ResponseEntity<ApiResponse<QuestionResponseDto>> createQuestion(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponseDto created = qaService.createQuestion(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.created("Question published", created));
    }

    @GetMapping
    @Operation(summary = "Browse questions with feed tab, topic filter, and search keyword")
    public ResponseEntity<ApiResponse<Page<QuestionResponseDto>>> getQuestions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false, defaultValue = "For You") String tab,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<QuestionResponseDto> questions = qaService.getQuestions(userPrincipal.getId(), tab, topic, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Questions retrieved", questions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single question details by ID")
    public ResponseEntity<ApiResponse<QuestionResponseDto>> getQuestionById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        QuestionResponseDto question = qaService.getQuestionById(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Question details retrieved", question));
    }

    @GetMapping("/{id}/answers")
    @Operation(summary = "Get answers for a specific question")
    public ResponseEntity<ApiResponse<List<AnswerResponseDto>>> getAnswers(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        List<AnswerResponseDto> answers = qaService.getAnswers(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Answers retrieved", answers));
    }

    @PostMapping("/{id}/answers")
    @Operation(summary = "Post an answer to a question")
    public ResponseEntity<ApiResponse<AnswerResponseDto>> createAnswer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateAnswerRequest request) {
        AnswerResponseDto answer = qaService.createAnswer(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.created("Answer submitted", answer));
    }

    @PostMapping("/{id}/doubt")
    @Operation(summary = "Toggle 'I have the same doubt' signal on a question")
    public ResponseEntity<ApiResponse<DoubtToggleResponse>> toggleSameDoubt(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        DoubtToggleResponse result = qaService.toggleSameDoubt(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(result.isHasMarked() ? "Marked same doubt" : "Unmarked same doubt", result));
    }

    @PostMapping("/answers/{answerId}/helpful")
    @Operation(summary = "Toggle helpful vote on an answer")
    public ResponseEntity<ApiResponse<Boolean>> toggleHelpful(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID answerId) {
        boolean marked = qaService.toggleHelpful(userPrincipal.getId(), answerId);
        return ResponseEntity.ok(ApiResponse.ok(marked ? "Marked answer as helpful" : "Unmarked helpful", marked));
    }

    @PatchMapping("/answers/{answerId}/accept")
    @Operation(summary = "Mark an answer as accepted (question author only)")
    public ResponseEntity<ApiResponse<AnswerResponseDto>> acceptAnswer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID answerId) {
        AnswerResponseDto accepted = qaService.acceptAnswer(userPrincipal.getId(), answerId);
        return ResponseEntity.ok(ApiResponse.ok("Answer marked as accepted", accepted));
    }
}

