package com.example.speechservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.speechservice.dto.SpeechEvaluationResult;
import com.example.speechservice.entity.SpeechSession;
import com.example.speechservice.repository.SpeechSessionRepository;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private OpenAIService openAIService;

    @Mock
    private SpeechSessionRepository speechSessionRepository;

    @InjectMocks
    private EvaluationService evaluationService;

    private SpeechSession mockSession;
    private String testUserText;
    private String testTopic;

    @BeforeEach
    void setUp() {
        mockSession = new SpeechSession();
        mockSession.setId(1L);
        mockSession.setSessionId("session_test123");
        mockSession.setUserId(1L);
        mockSession.setTopic("travel");
        mockSession.setDifficultyLevel(SpeechSession.DifficultyLevel.BEGINNER);
        mockSession.setStartTime(LocalDateTime.now());
        mockSession.setCreatedAt(LocalDateTime.now());
        mockSession.setUpdatedAt(LocalDateTime.now());

        testUserText = "Hello, I like to travel to different countries.";
        testTopic = "travel";
    }

    @Test
    void evaluateSpeech_createsEvaluationSuccessfully() {
        // Given
        String mockAIEvaluation = "This is a good response. Grammar: Good. Vocabulary: Appropriate for beginner level.";
        when(openAIService.getChatResponse(anyList())).thenReturn(mockAIEvaluation);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        SpeechEvaluationResult result = evaluationService.evaluateSpeech(
            testUserText, testTopic, SpeechSession.DifficultyLevel.BEGINNER, mockSession
        );

        // Then
        assertNotNull(result);
        assertEquals(mockSession.getSessionId(), result.getSessionId());
        assertNotNull(result.getFeedback());
        assertNotNull(result.getCreatedAt());
        
        verify(openAIService, times(1)).getChatResponse(anyList());
        verify(speechSessionRepository, times(1)).save(mockSession);
    }

    @Test
    void evaluateSpeech_generatesAppropriatePromptForBeginnerLevel() {
        // Given
        String mockAIEvaluation = "Evaluation result";
        when(openAIService.getChatResponse(anyList())).thenReturn(mockAIEvaluation);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        evaluationService.evaluateSpeech(
            testUserText, testTopic, SpeechSession.DifficultyLevel.BEGINNER, mockSession
        );

        // Then
        verify(openAIService, times(1)).getChatResponse(anyList());
    }

    @Test
    void evaluateSpeech_generatesAppropriatePromptForIntermediateLevel() {
        // Given
        String mockAIEvaluation = "Evaluation result";
        when(openAIService.getChatResponse(anyList())).thenReturn(mockAIEvaluation);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        evaluationService.evaluateSpeech(
            testUserText, testTopic, SpeechSession.DifficultyLevel.INTERMEDIATE, mockSession
        );

        // Then
        verify(openAIService, times(1)).getChatResponse(anyList());
    }

    @Test
    void evaluateSpeech_generatesAppropriatePromptForAdvancedLevel() {
        // Given
        String mockAIEvaluation = "Evaluation result";
        when(openAIService.getChatResponse(anyList())).thenReturn(mockAIEvaluation);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        evaluationService.evaluateSpeech(
            testUserText, testTopic, SpeechSession.DifficultyLevel.ADVANCED, mockSession
        );

        // Then
        verify(openAIService, times(1)).getChatResponse(anyList());
    }

    @Test
    void evaluateSpeech_handlesOpenAIFailure() {
        // Given
        when(openAIService.getChatResponse(anyList())).thenThrow(new RuntimeException("OpenAI API failed"));

        // When
        SpeechEvaluationResult result = evaluationService.evaluateSpeech(
                testUserText, testTopic, SpeechSession.DifficultyLevel.BEGINNER, mockSession
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.getFeedback());
        // Should return default evaluation when OpenAI fails
        assertTrue(result.getFeedback().contains("평가"));
        
        verify(openAIService, times(1)).getChatResponse(anyList());
        verify(speechSessionRepository, never()).save(any(SpeechSession.class));
    }

    @Test
    void evaluateSpeech_savesFeedbackToSession() {
        // Given
        String mockAIEvaluation = "This is excellent! Grammar: Perfect. Vocabulary: Rich and varied.";
        when(openAIService.getChatResponse(anyList())).thenReturn(mockAIEvaluation);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        evaluationService.evaluateSpeech(
            testUserText, testTopic, SpeechSession.DifficultyLevel.BEGINNER, mockSession
        );

        // Then
        verify(speechSessionRepository, times(1)).save(argThat(session -> 
            session.getFeedback() != null && 
            !session.getFeedback().isEmpty()
        ));
    }

    @Test
    void evaluateSpeech_handlesEmptyUserText() {
        // Given
        String emptyUserText = "";
        String mockAIEvaluation = "Evaluation result";
        when(openAIService.getChatResponse(anyList())).thenReturn(mockAIEvaluation);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        SpeechEvaluationResult result = evaluationService.evaluateSpeech(
            emptyUserText, testTopic, SpeechSession.DifficultyLevel.BEGINNER, mockSession
        );

        // Then
        assertNotNull(result);
        verify(openAIService, times(1)).getChatResponse(anyList());
        verify(speechSessionRepository, times(1)).save(mockSession);
    }

    @Test
    void evaluateSpeech_handlesSpecialCharactersInText() {
        // Given
        String specialText = "Hello! How are you? I like traveling to places like Paris, France.";
        String mockAIEvaluation = "Evaluation result";
        when(openAIService.getChatResponse(anyList())).thenReturn(mockAIEvaluation);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        SpeechEvaluationResult result = evaluationService.evaluateSpeech(
            specialText, testTopic, SpeechSession.DifficultyLevel.BEGINNER, mockSession
        );

        // Then
        assertNotNull(result);
        verify(openAIService, times(1)).getChatResponse(anyList());
        verify(speechSessionRepository, times(1)).save(mockSession);
    }
}
