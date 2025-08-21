package com.example.speechservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.speechservice.dto.SpeechEvaluationResult;
import com.example.speechservice.entity.SpeechSession;
import com.example.speechservice.repository.SpeechSessionRepository;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 발화 내용을 평가하고 피드백을 생성하는 서비스입니다.
 * OpenAI ChatCompletion API를 사용하여 발음, 문법, 유창성을 평가합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final OpenAIService openAIService;
    private final SpeechSessionRepository speechSessionRepository;

    /**
     * 사용자의 발화 내용을 평가하고 상세한 피드백을 생성합니다.
     *
     * @param userText 사용자가 발화한 텍스트
     * @param topic 대화 주제
     * @param difficultyLevel 난이도
     * @param speechSession 음성 세션
     * @return 평가 결과
     */
    public SpeechEvaluationResult evaluateSpeech(String userText, String topic, 
                                               SpeechSession.DifficultyLevel difficultyLevel,
                                               SpeechSession speechSession) {
        try {
            // 평가용 프롬프트 생성
            String evaluationPrompt = createEvaluationPrompt(userText, topic, difficultyLevel);
            
            // OpenAI API를 통한 평가 수행
            String aiEvaluation = getAIEvaluation(evaluationPrompt);
            
            // AI 응답에서 피드백 추출
            String feedback = extractFeedback(aiEvaluation);
            
            // SpeechSession의 feedback 필드에 직접 저장
            speechSession.setFeedback(feedback);
            speechSessionRepository.save(speechSession);
            
            // 평가 결과 생성
            SpeechEvaluationResult result = SpeechEvaluationResult.builder()
                    .sessionId(speechSession.getSessionId())
                    .feedback(feedback)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            log.info("Speech evaluation completed for session: {}, feedback saved", 
                    speechSession.getSessionId());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during speech evaluation for session: {}", speechSession.getSessionId(), e);
            // 오류 발생 시 기본 평가 결과 반환
            return createDefaultEvaluation(speechSession.getSessionId());
        }
    }

    /**
     * 평가용 프롬프트를 생성합니다.
     */
    private String createEvaluationPrompt(String userText, String topic, SpeechSession.DifficultyLevel difficultyLevel) {
        return String.format("""
            당신은 영어 교육 전문가입니다. 다음 사용자의 발화를 '%s' 주제에 대해 %s 레벨로 평가해주세요.
            
            사용자 발화: "%s"
            
            다음 형식으로 상세한 평가를 제공해주세요:
            {
                "feedback": "문법적으로 정확하고 자연스러운 표현을 사용했습니다. 어휘 선택이 적절하며 문장 구조가 명확합니다. 전반적으로 유창한 영어 대화가 가능합니다."
            }
            
            평가 기준:
            - 문법: 정확성, 문장 구조, 단어 선택, 시제 일치
            - 어휘: 적절한 단어 선택, 표현의 다양성, 맥락에 맞는 사용
            - 유창성: 자연스러운 흐름, 적절한 휴식, 대화의 연결성
            
            주의사항:
            - 발음에 대한 평가는 하지 마세요 (음성 파일을 직접 들을 수 없음)
            - STT로 변환된 텍스트만을 기준으로 평가하세요
            - 구체적인 예시와 개선 방안을 포함해주세요
            
            건설적인 피드백과 구체적인 개선 방안을 한글로 제공해주세요.
            """, topic, difficultyLevel.name(), userText);
    }

    /**
     * OpenAI API를 통해 평가를 수행합니다.
     */
    private String getAIEvaluation(String prompt) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "당신은 영어 교육 전문가입니다. 요청된 정확한 JSON 형식으로 평가를 제공해주세요."));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
        
        return openAIService.getChatResponse(messages);
    }

    /**
     * AI 응답에서 피드백을 추출합니다.
     */
    private String extractFeedback(String aiResponse) {
        try {
            // JSON에서 피드백 추출
            String feedback = extractField(aiResponse, "feedback");
            if (feedback != null && !feedback.isEmpty()) {
                return feedback;
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI evaluation response", e);
        }
        
        // 기본 피드백 반환
        return "평가 정보를 사용할 수 없습니다. 나중에 다시 시도해주세요.";
    }

    /**
     * JSON 응답에서 텍스트 필드를 추출합니다.
     */
    private String extractField(String response, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 특정 세션의 평가 결과를 조회합니다.
     *
     * @param sessionId 세션 ID
     * @return 해당 세션의 평가 결과
     */
    public SpeechEvaluationResult getSessionEvaluation(String sessionId) {
        SpeechSession session = speechSessionRepository.findBySessionId(sessionId)
                .orElse(null);
        
        if (session == null) {
            return null;
        }
        
        return SpeechEvaluationResult.builder()
                .sessionId(session.getSessionId())
                .feedback(session.getFeedback())
                .createdAt(session.getUpdatedAt())
                .build();
    }
    
    /**
     * 오류 발생 시 기본 평가 결과를 생성합니다.
     */
    private SpeechEvaluationResult createDefaultEvaluation(String sessionId) {
        return SpeechEvaluationResult.builder()
                .sessionId(sessionId)
                .feedback("평가가 일시적으로 불가능합니다. 나중에 다시 시도해주세요.")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
