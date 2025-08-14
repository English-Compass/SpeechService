package com.example.speechservice.service;

import com.example.speechservice.dto.SessionStartRequest;
import com.example.speechservice.dto.SessionStartResponse;
import com.example.speechservice.dto.TalkRequest;
import com.example.speechservice.dto.TalkResponse;
import com.example.speechservice.entity.SpeechSession;
import com.example.speechservice.entity.User;
import com.example.speechservice.repository.SpeechSessionRepository;
import com.example.speechservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionService {

    @Autowired
    private SpeechSessionRepository speechSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public SessionStartResponse startSession(SessionStartRequest request) {
        // 1. 사용자 조회 또는 생성
        User user = userRepository.findByKakaoId(request.getKakaoId())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setKakaoId(request.getKakaoId());
                    newUser.setName("User_" + request.getKakaoId()); // 임시 이름
                    // 초기 레벨, 선호 카테고리 등 설정 가능
                    return userRepository.save(newUser);
                });

        // 2. 세션 ID 및 AI 첫 인사 생성
        String sessionId = generateUniqueSessionId();
        String aiFirstGreeting = generateAIFirstGreeting(request.getTopic(), request.getDifficultyLevel());

        // 3. SpeechSession 엔티티 생성 및 저장
        SpeechSession session = new SpeechSession();
        session.setSessionId(sessionId);
        session.setUser(user);
        session.setTopic(request.getTopic());
        session.setDifficultyLevel(request.getDifficultyLevel());
        session.setAiFirstGreeting(aiFirstGreeting);

        SpeechSession savedSession = speechSessionRepository.save(session);

        // 4. 응답 DTO 생성 및 반환
        return SessionStartResponse.builder()
                .sessionId(savedSession.getSessionId())
                .topic(savedSession.getTopic())
                .difficultyLevel(savedSession.getDifficultyLevel().name())
                .aiFirstGreeting(savedSession.getAiFirstGreeting())
                .createdAt(savedSession.getCreatedAt())
                .build();
    }

    @Transactional
    public TalkResponse processTalk(String sessionId, TalkRequest request) {
        // 1. 세션 조회
        SpeechSession session = speechSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        // TODO: 2. 이전 대화 내역 조회 (Redis 또는 DB)

        // TODO: 3. 사용자 발화와 대화 내역을 ChatCompletion API로 전송

        // 4. (임시) AI 응답 생성 - 에코
        String aiResponseText = "You said: '" + request.getUserText() + "'. I am a simple echo bot for now.";

        // TODO: 5. 대화 내역 업데이트 (사용자 발화 + AI 응답)

        // 6. 응답 반환
        return new TalkResponse(aiResponseText);
    }

    private String generateUniqueSessionId() {
        String sessionId;
        do {
            sessionId = "session_" + UUID.randomUUID().toString().substring(0, 8);
        } while (speechSessionRepository.existsBySessionId(sessionId));
        return sessionId;
    }

    private String generateAIFirstGreeting(String topic, SpeechSession.DifficultyLevel difficultyLevel) {
        String greeting = switch (difficultyLevel) {
            case BEGINNER -> "Hello! Let's talk about " + topic + ". I'm ready to start. How are you?";
            case INTERMEDIATE -> "Hi there! I'm looking forward to discussing " + topic + " with you. Are you ready?";
            case ADVANCED -> "Greetings. I'm prepared for an in-depth conversation about " + topic + ". Let's begin.";
        };
        return greeting;
    }
}
