package com.example.speechservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.speechservice.dto.SessionEndResponse;
import com.example.speechservice.dto.SessionStartRequest;
import com.example.speechservice.dto.SessionStartResponse;
import com.example.speechservice.dto.TalkResponse;
import com.example.speechservice.entity.SpeechSession;
import com.example.speechservice.repository.SpeechSessionRepository;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SpeechSessionRepository speechSessionRepository;

    @Mock
    private OpenAIService openAIService;

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SessionService sessionService;

    private SessionStartRequest validRequest;
    private SpeechSession mockSession;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = SessionStartRequest.builder()
                .userId(1L)
                .topic("travel")
                .difficultyLevel(SpeechSession.DifficultyLevel.BEGINNER)
                .build();

        // Setup mock session
        mockSession = new SpeechSession();
        mockSession.setId(1L);
        mockSession.setSessionId("session_test123");
        mockSession.setUserId(1L);
        mockSession.setTopic("travel");
        mockSession.setDifficultyLevel(SpeechSession.DifficultyLevel.BEGINNER);
        mockSession.setStartTime(LocalDateTime.now());
        mockSession.setCreatedAt(LocalDateTime.now());
        mockSession.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void startSession_createsNewSessionSuccessfully() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(speechSessionRepository.existsBySessionId(anyString())).thenReturn(false);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        SessionStartResponse response = sessionService.startSession(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(mockSession.getSessionId(), response.getSessionId());
        assertEquals(mockSession.getTopic(), response.getTopic());
        assertEquals(mockSession.getDifficultyLevel().name(), response.getDifficultyLevel());
        assertNotNull(response.getAiFirstGreeting());
        
        verify(speechSessionRepository, times(1)).save(any(SpeechSession.class));
        verify(valueOperations, times(1)).set(anyString(), anyList(), eq(Duration.ofHours(1)));
    }

    @Test
    void startSession_generatesUniqueSessionId() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(speechSessionRepository.existsBySessionId(anyString())).thenReturn(true, false);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        sessionService.startSession(validRequest);

        // Then
        verify(speechSessionRepository, atLeastOnce()).existsBySessionId(anyString());
        verify(speechSessionRepository, times(1)).save(any(SpeechSession.class));
    }

    @Test
    void startSession_generatesDifferentGreetingsForDifferentLevels() {
        // Given
        SessionStartRequest intermediateRequest = SessionStartRequest.builder()
                .userId(1L)
                .topic("food")
                .difficultyLevel(SpeechSession.DifficultyLevel.INTERMEDIATE)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(speechSessionRepository.existsBySessionId(anyString())).thenReturn(false);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        SessionStartResponse beginnerResponse = sessionService.startSession(validRequest);
        SessionStartResponse intermediateResponse = sessionService.startSession(intermediateRequest);

        // Then
        assertNotEquals(beginnerResponse.getAiFirstGreeting(), intermediateResponse.getAiFirstGreeting());
        assertTrue(beginnerResponse.getAiFirstGreeting().contains("simple words"));
        assertTrue(intermediateResponse.getAiFirstGreeting().contains("discuss"));
    }

    @Test
    void endSession_endsSessionSuccessfully() {
        // Given
        String sessionId = "session_test123";
        when(speechSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(mockSession));
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        SessionEndResponse response = sessionService.endSession(sessionId, "MANUAL");

        // Then
        assertNotNull(response);
        assertEquals(sessionId, response.getSessionId());
        assertEquals("MANUAL", response.getEndReason());
        assertNotNull(response.getEndTime());
        
        verify(redisTemplate, times(1)).delete(anyString());
        verify(speechSessionRepository, times(1)).save(any(SpeechSession.class));
    }

    @Test
    void endSession_throwsExceptionWhenSessionNotFound() {
        // Given
        String sessionId = "nonexistent_session";
        when(speechSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            sessionService.endSession(sessionId, "MANUAL");
        });
        
        verify(redisTemplate, never()).delete(anyString());
        verify(speechSessionRepository, never()).save(any(SpeechSession.class));
    }

    @Test
    void processTalk_processesUserAudioSuccessfully() throws IOException {
        // Given
        String sessionId = "session_test123";
        String userText = "Hello, how are you?";
        String aiResponse = "I'm doing well, thank you!";
        byte[] audioData = "fake audio data".getBytes();
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(speechSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(mockSession));
        when(openAIService.convertSpeechToText(any(InputStream.class))).thenReturn(userText);
        when(openAIService.getChatResponse(anyList())).thenReturn(aiResponse);
        when(openAIService.convertTextToSpeech(eq(aiResponse), anyString())).thenReturn(new ByteArrayInputStream(audioData));

        // When
        TalkResponse response = sessionService.processTalk(sessionId, new ByteArrayInputStream(audioData));

        // Then
        assertNotNull(response);
        assertEquals(aiResponse, response.getText());
        assertEquals(userText, response.getUserText());
        assertNotNull(response.getAudio());
        
        verify(openAIService, times(1)).convertSpeechToText(any(InputStream.class));
        verify(openAIService, times(1)).getChatResponse(anyList());
        verify(openAIService, times(1)).convertTextToSpeech(eq(aiResponse), eq(mockSession.getDifficultyLevel().name()));
        verify(valueOperations, times(1)).set(anyString(), anyList(), eq(Duration.ofHours(1)));
    }

    @Test
    void processTalk_handlesAudioConversionFailure() throws IOException {
        // Given
        String sessionId = "session_test123";
        when(speechSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(mockSession));
        when(openAIService.convertSpeechToText(any(InputStream.class))).thenThrow(new RuntimeException("Audio conversion failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            sessionService.processTalk(sessionId, new ByteArrayInputStream("test".getBytes()));
        });
        
        verify(openAIService, times(1)).convertSpeechToText(any(InputStream.class));
        verify(openAIService, never()).getChatResponse(anyList());
        verify(openAIService, never()).convertTextToSpeech(anyString(), anyString());
    }

    @Test
    void processTalk_detectsUserIntentToEnd() throws IOException {
        // Given
        String sessionId = "session_test123";
        String userText = "Let's end the conversation";
        String aiResponse = "Thank you for the conversation! [SESSION_ENDED]";
        byte[] audioData = "fake audio data".getBytes();
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(speechSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(mockSession));
        when(openAIService.convertSpeechToText(any(InputStream.class))).thenReturn(userText);
        when(openAIService.getChatResponse(anyList())).thenReturn(aiResponse);
        when(openAIService.convertTextToSpeech(eq(aiResponse), anyString())).thenReturn(new ByteArrayInputStream(audioData));
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        TalkResponse response = sessionService.processTalk(sessionId, new ByteArrayInputStream(audioData));

        // Then
        assertNotNull(response);
        // The AI response should contain [SESSION_ENDED] marker
        assertTrue(response.getText().contains("[SESSION_ENDED]"));
        
        verify(openAIService, times(1)).convertSpeechToText(any(InputStream.class));
        verify(openAIService, times(1)).getChatResponse(anyList());
        verify(openAIService, times(1)).convertTextToSpeech(eq(aiResponse), eq(mockSession.getDifficultyLevel().name()));
    }

    @Test
    void processTalk_checksConversationLimit() throws IOException {
        // Given
        String sessionId = "session_test123";
        String userText = "Another message";
        String aiResponse = "Response";
        byte[] audioData = "fake audio data".getBytes();
        
        // Create messages that exceed conversation limit
        List<ChatMessage> existingMessages = new ArrayList<>();
        for (int i = 0; i < 40; i++) { // 20 conversations (40 messages)
            existingMessages.add(new ChatMessage(ChatMessageRole.USER.value(), "User message " + i));
            existingMessages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), "AI response " + i));
        }
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(speechSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(mockSession));
        when(openAIService.convertSpeechToText(any(InputStream.class))).thenReturn(userText);
        when(openAIService.getChatResponse(anyList())).thenReturn(aiResponse);
        when(openAIService.convertTextToSpeech(eq(aiResponse), anyString())).thenReturn(new ByteArrayInputStream(audioData));
        when(valueOperations.get(anyString())).thenReturn(existingMessages);
        when(speechSessionRepository.save(any(SpeechSession.class))).thenReturn(mockSession);

        // When
        TalkResponse response = sessionService.processTalk(sessionId, new ByteArrayInputStream(audioData));

        // Then
        assertNotNull(response);
        // Session should be auto-ended due to conversation limit
        verify(openAIService, times(1)).convertSpeechToText(any(InputStream.class));
        verify(openAIService, times(1)).getChatResponse(anyList());
        verify(openAIService, times(1)).convertTextToSpeech(eq(aiResponse), eq(mockSession.getDifficultyLevel().name()));
    }
}
