package com.example.Uplink_Backend.modules.qa.service;

import com.example.Uplink_Backend.common.exception.ResourceNotFoundException;
import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import com.example.Uplink_Backend.modules.qa.dto.*;
import com.example.Uplink_Backend.modules.qa.entity.*;
import com.example.Uplink_Backend.modules.qa.repository.*;
import com.example.Uplink_Backend.modules.user.entity.Education;
import com.example.Uplink_Backend.modules.user.repository.EducationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QAService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionDoubtRepository doubtRepository;
    private final AnswerHelpfulRepository helpfulRepository;
    private final UserRepository userRepository;
    private final EducationRepository educationRepository;

    public QAService(QuestionRepository questionRepository,
                     AnswerRepository answerRepository,
                     QuestionDoubtRepository doubtRepository,
                     AnswerHelpfulRepository helpfulRepository,
                     UserRepository userRepository,
                     EducationRepository educationRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.doubtRepository = doubtRepository;
        this.helpfulRepository = helpfulRepository;
        this.userRepository = userRepository;
        this.educationRepository = educationRepository;
    }

    @Transactional
    public QuestionResponseDto createQuestion(UUID userId, CreateQuestionRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String tags = request.getTopicTags() != null ? String.join(", ", request.getTopicTags()) : "";
        Question question = new Question(author, request.getTitle().trim(), request.getDescription().trim(), tags);
        Question saved = questionRepository.save(question);
        return mapToQuestionDto(saved, userId);
    }

    public Page<QuestionResponseDto> getQuestions(UUID userId, String tab, String topic, String search, Pageable pageable) {
        Page<Question> page;

        String cleanTopic = (topic != null && !topic.equalsIgnoreCase("All") && !topic.isBlank()) ? topic.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        if (cleanTopic != null || cleanSearch != null) {
            page = questionRepository.searchQuestions(cleanTopic, cleanSearch, pageable);
        } else if ("Unanswered".equalsIgnoreCase(tab)) {
            page = questionRepository.findByIsUnansweredTrue(pageable);
        } else if ("Trending".equalsIgnoreCase(tab)) {
            page = questionRepository.findTrending(pageable);
        } else if ("My Questions".equalsIgnoreCase(tab)) {
            page = questionRepository.findByAuthorId(userId, pageable);
        } else {
            page = questionRepository.findAll(pageable);
        }

        List<QuestionResponseDto> dtos = page.getContent().stream()
                .map(q -> mapToQuestionDto(q, userId))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public QuestionResponseDto getQuestionById(UUID userId, UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));
        return mapToQuestionDto(question, userId);
    }

    public List<AnswerResponseDto> getAnswers(UUID userId, UUID questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Question", "id", questionId);
        }

        return answerRepository.findByQuestionIdOrderByIsAcceptedDescHelpfulCountDescCreatedAtAsc(questionId)
                .stream()
                .map(a -> mapToAnswerDto(a, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public AnswerResponseDto createAnswer(UUID userId, UUID questionId, CreateAnswerRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        Answer answer = new Answer(question, author, request.getContent().trim());
        Answer saved = answerRepository.save(answer);

        // Update question stats
        int count = (int) answerRepository.countByQuestionId(questionId);
        question.setAnswerCount(count);
        question.setUnanswered(count == 0);
        question.setUpdatedAt(Instant.now());
        questionRepository.save(question);

        return mapToAnswerDto(saved, userId);
    }

    @Transactional
    public DoubtToggleResponse toggleSameDoubt(UUID userId, UUID questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        boolean alreadyMarked = doubtRepository.existsByQuestionIdAndUserId(questionId, userId);
        boolean currentlyMarked;

        if (alreadyMarked) {
            doubtRepository.deleteByQuestionIdAndUserId(questionId, userId);
            currentlyMarked = false;
        } else {
            doubtRepository.save(new QuestionDoubt(question, user));
            currentlyMarked = true;
        }

        int count = (int) doubtRepository.countByQuestionId(questionId);
        question.setSameDoubtCount(count);
        questionRepository.save(question);

        return new DoubtToggleResponse(currentlyMarked, count);
    }

    @Transactional
    public boolean toggleHelpful(UUID userId, UUID answerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "id", answerId));

        boolean alreadyMarked = helpfulRepository.existsByAnswerIdAndUserId(answerId, userId);
        boolean currentlyMarked;

        if (alreadyMarked) {
            helpfulRepository.deleteByAnswerIdAndUserId(answerId, userId);
            currentlyMarked = false;
        } else {
            helpfulRepository.save(new AnswerHelpful(answer, user));
            currentlyMarked = true;
        }

        int count = (int) helpfulRepository.countByAnswerId(answerId);
        answer.setHelpfulCount(count);
        answerRepository.save(answer);

        return currentlyMarked;
    }

    @Transactional
    public AnswerResponseDto acceptAnswer(UUID userId, UUID answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "id", answerId));

        Question question = answer.getQuestion();
        if (!question.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Only the question author can accept an answer");
        }

        answer.setAccepted(!answer.isAccepted());
        Answer saved = answerRepository.save(answer);
        return mapToAnswerDto(saved, userId);
    }

    private QuestionResponseDto mapToQuestionDto(Question q, UUID userId) {
        QuestionResponseDto dto = new QuestionResponseDto();
        dto.setId(q.getId().toString());
        dto.setTitle(q.getTitle());
        dto.setDescription(q.getDescription());

        List<String> tags = new ArrayList<>();
        if (q.getTopicTags() != null && !q.getTopicTags().isBlank()) {
            tags = Arrays.stream(q.getTopicTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        dto.setTopicTags(tags);

        dto.setAuthorId(q.getAuthor().getId().toString());
        dto.setAuthorName(q.getAuthor().getFullName());
        dto.setCreatedAt(q.getCreatedAt().toString());
        dto.setAnswerCount(q.getAnswerCount());
        dto.setSameDoubtCount(q.getSameDoubtCount());
        dto.setUnanswered(q.isUnanswered());

        // Resolve academic context (e.g. "Computer Science • 2027")
        List<Education> eduList = educationRepository.findByUserIdOrderByStartYearDesc(q.getAuthor().getId());
        if (eduList != null && !eduList.isEmpty()) {
            Education edu = eduList.get(0);
            dto.setAuthorAcademicContext(edu.getCollege() != null ? edu.getCollege() : "Engineering Student");
        } else {
            dto.setAuthorAcademicContext("Campus Student");
        }

        dto.setHasUserMarkedSameDoubt(doubtRepository.existsByQuestionIdAndUserId(q.getId(), userId));
        return dto;
    }

    private AnswerResponseDto mapToAnswerDto(Answer a, UUID userId) {
        AnswerResponseDto dto = new AnswerResponseDto();
        dto.setId(a.getId().toString());
        dto.setQuestionId(a.getQuestion().getId().toString());
        dto.setAuthorId(a.getAuthor().getId().toString());
        dto.setAuthorName(a.getAuthor().getFullName());
        dto.setAuthorRole("Student / Contributor");
        dto.setContent(a.getContent());
        dto.setCreatedAt(a.getCreatedAt().toString());
        dto.setHelpfulCount(a.getHelpfulCount());
        dto.setAccepted(a.isAccepted());
        dto.setHasUserMarkedHelpful(helpfulRepository.existsByAnswerIdAndUserId(a.getId(), userId));
        return dto;
    }
}
