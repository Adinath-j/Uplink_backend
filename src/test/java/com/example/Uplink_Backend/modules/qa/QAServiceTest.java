package com.example.Uplink_Backend.modules.qa;

import com.example.Uplink_Backend.modules.auth.entity.User;
import com.example.Uplink_Backend.modules.auth.repository.UserRepository;
import com.example.Uplink_Backend.modules.qa.dto.*;
import com.example.Uplink_Backend.modules.qa.entity.*;
import com.example.Uplink_Backend.modules.qa.repository.*;
import com.example.Uplink_Backend.modules.qa.service.QAService;
import com.example.Uplink_Backend.modules.user.repository.EducationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QAServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionDoubtRepository doubtRepository;

    @Mock
    private AnswerHelpfulRepository helpfulRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EducationRepository educationRepository;

    @InjectMocks
    private QAService qaService;

    private User sampleUser;
    private Question sampleQuestion;
    private UUID userId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        questionId = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setId(userId);
        sampleUser.setFullName("Aditya Verma");
        sampleUser.setEmail("aditya@campus.edu");

        sampleQuestion = new Question(sampleUser, "How does indexing work?", "B-Tree vs Hash index", "Database, DSA");
        sampleQuestion.setId(questionId);
    }

    @Test
    void testCreateQuestion() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(questionRepository.save(any(Question.class))).thenReturn(sampleQuestion);
        when(educationRepository.findByUserIdOrderByStartYearDesc(userId)).thenReturn(Collections.emptyList());

        CreateQuestionRequest req = new CreateQuestionRequest("How does indexing work?", "B-Tree vs Hash index", List.of("Database", "DSA"));
        QuestionResponseDto result = qaService.createQuestion(userId, req);

        assertNotNull(result);
        assertEquals("How does indexing work?", result.getTitle());
        assertEquals(sampleUser.getFullName(), result.getAuthorName());
        verify(questionRepository, times(1)).save(any(Question.class));
    }

    @Test
    void testGetQuestions() {
        when(questionRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(sampleQuestion)));
        when(educationRepository.findByUserIdOrderByStartYearDesc(userId)).thenReturn(Collections.emptyList());

        Page<QuestionResponseDto> page = qaService.getQuestions(userId, "For You", null, null, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("How does indexing work?", page.getContent().get(0).getTitle());
    }

    @Test
    void testToggleSameDoubtAddsDoubtWhenNotMarked() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(sampleQuestion));
        when(doubtRepository.existsByQuestionIdAndUserId(questionId, userId)).thenReturn(false);
        when(doubtRepository.countByQuestionId(questionId)).thenReturn(1L);

        DoubtToggleResponse response = qaService.toggleSameDoubt(userId, questionId);

        assertTrue(response.isHasMarked());
        assertEquals(1, response.getCount());
        verify(doubtRepository, times(1)).save(any(QuestionDoubt.class));
    }

    @Test
    void testToggleSameDoubtRemovesDoubtWhenAlreadyMarked() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(sampleQuestion));
        when(doubtRepository.existsByQuestionIdAndUserId(questionId, userId)).thenReturn(true);
        when(doubtRepository.countByQuestionId(questionId)).thenReturn(0L);

        DoubtToggleResponse response = qaService.toggleSameDoubt(userId, questionId);

        assertFalse(response.isHasMarked());
        assertEquals(0, response.getCount());
        verify(doubtRepository, times(1)).deleteByQuestionIdAndUserId(questionId, userId);
    }
}

