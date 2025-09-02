package com.example.speechservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
            
            // AI 응답 파싱하여 feedback과 recommendedDifficulty 모두 추출
            SpeechEvaluationResult parsedResult = parseAIEvaluationResponse(aiEvaluation);
            
            // 로그에 파싱된 피드백과 추천 난이도 출력
            log.info("Parsed Feedback: {}", parsedResult.getFeedback());
            log.info("Parsed Recommended Difficulty: {}", parsedResult.getRecommendedDifficulty());
            
            // AI가 추천한 난이도 로그 출력
            log.info("AI Recommended Difficulty: {}", parsedResult.getRecommendedDifficulty());
            
            // SpeechSession의 feedback 필드에 직접 저장 (feedback만)
            speechSession.setFeedback(parsedResult.getFeedback());
            speechSessionRepository.save(speechSession);
            
            // 평가 결과 생성 (검증된 recommendedDifficulty 포함)
            parsedResult.setSessionId(speechSession.getSessionId());
            parsedResult.setCreatedAt(LocalDateTime.now()); 
            
            log.info("Speech evaluation completed for session: {}, feedback saved", 
                    speechSession.getSessionId());
            
            return parsedResult;
            
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
        // 롤 플레잉 세션인지 확인
        boolean isRolePlaying = topic != null && topic.startsWith("role-playing:");
        
        if (isRolePlaying) {
            return createRolePlayingEvaluationPrompt(userText, difficultyLevel);
        } else {
            return createRegularEvaluationPrompt(userText, difficultyLevel);
        }
    }
    
    /**
     * 롤 플레잉 세션용 평가 프롬프트를 생성합니다.
     */
    private String createRolePlayingEvaluationPrompt(String userText, SpeechSession.DifficultyLevel difficultyLevel) {
        return String.format("""
            당신은 영어 교육 전문가입니다. 롤 플레잉 대화에서 사용자의 발화를 %s 레벨 기준으로 평가해주세요.
            
            사용자 발화: "%s"
            
            다음 JSON 형식으로 상세하고 유용한 피드백을 제공해주세요. 'feedback' 필드에는 사용자 발화에 대한 구체적인 평가, 개선 방안, 그리고 추천 난이도를 모두 포함해야 합니다.
            {
                "feedback": "평가 내용...\\n\\n추천 난이도: [권장난이도]"
            }
            
            **롤 플레잉 특별 평가 기준:**
            - **역할극 몰입도**: 대화 상황에 적절한 표현 사용
            - **자연스러운 대화**: 실제 상황에서 사용할 수 있는 표현
            - **역할에 맞는 어휘**: 상황에 적합한 단어와 표현 선택
            
            **자연스러운 문장 제안 (중요):**
            - 사용자가 사용한 표현보다 더 자연스럽고 적절한 대안 문장을 제시해주세요
            - "더 자연스럽게 표현하면..." 형태로 시작하여 구체적인 예시를 들어주세요
            - 롤 플레잉 상황에 맞는 실용적인 표현을 제안해주세요
            
            **난이도 기준 (반드시 이 기준을 따라주세요):**
            
            **초급 (BEGINNER):**
            - 문장 길이: 3-5단어 이하
            - 어휘: 일상생활 기본 단어만 (hello, food, like, want, go, see, eat, drink, work, home, family, friend)
            - 문법: 현재시제만, 단순한 주어+동사 구조
            - 표현: 매우 기본적인 의사소통 가능
            
            **중급 (INTERMEDIATE):**
            - 문장 길이: 5-10단어
            - 어휘: 일반적인 어휘, 동의어 사용
            - 문법: 과거/미래시제, 복합문, 전치사구
            - 표현: 자연스러운 대화, 감정/의견 표현 가능
            
            **고급 (ADVANCED):**
            - 문장 길이: 10단어 이상
            - 어휘: 고급 어휘, 관용구, 전문용어
            - 문법: 복잡한 문장구조, 가정법, 수동태
            - 표현: 추상적 개념, 논리적 설명, 문화적 맥락 이해
            
            평가 기준:
            - 문법: 정확성, 문장 구조, 단어 선택, 시제 일치
            - 어휘: 적절한 단어 선택, 표현의 다양성, 맥락에 맞는 사용
            - 유창성: 자연스러운 흐름, 적절한 휴식, 대화의 연결성
            - **롤 플레잉 적합성**: 상황에 맞는 표현, 자연스러운 대화
            
            주의사항:
            - 발음에 대한 평가는 하지 마세요 (음성 파일을 직접 들을 수 없음)
            - STT로 변환된 텍스트만을 기준으로 평가하세요
            - 롤 플레잉 상황을 고려하여 실용적인 피드백을 제공하세요
            - 위의 난이도 기준을 정확히 적용하여 평가하세요
            - 구체적인 예시와 개선 방안을 포함해주세요
            - 평가 결과는 "~수준을 추천합니다" 형태로 작성하세요 (예: "중급 수준을 추천합니다")
            
            **난이도 추천 규칙 (절대적으로 따라야 함):**
            - 현재 사용자 난이도: %s
            - **중요**: 사용자의 실제 영어 실력이 현재 난이도보다 높을 때만 상위 난이도 추천
            - **초급 사용자**: 영어 실력이 초급 수준을 벗어났을 때만 중급 또는 고급 추천
            - **중급 사용자**: 영어 실력이 중급 수준을 벗어났을 때만 고급 추천
            - **고급 사용자**: 더 이상 상위 난이도가 없음
            - **평가 기준**: 위의 난이도 기준에 따라 사용자 발화를 정확히 평가한 후, 그 결과에 따라 추천
            - **잘못된 추천 금지**: 단순히 "높은 난이도"가 아니라, 실제 실력 평가 결과에 따른 추천만
            
            **추천 난이도 표시 규칙:**
            - 상위 난이도로 추천할 때만 "추천 난이도: [권장난이도]" 형태로 포함
            - "현재 수준 유지" 또는 "현재 난이도 유지" 같은 추천은 하지 마세요
            - 추천이 없을 때는 해당 문구를 아예 포함하지 마세요
            - **중요**: 난이도는 반드시 한글로 표현하세요 (초급, 중급, 고급)
            
            **롤 플레잉 피드백 특별 요구사항:**
            - 대화 중에는 교정하지 않았지만, 세션 종료 후에는 구체적인 개선 방안을 제시해주세요
            - "더 자연스럽게 표현하면..." 형태로 실용적인 대안 문장을 제안해주세요
            - 롤 플레잉 상황에서 실제로 사용할 수 있는 표현을 중심으로 피드백을 제공해주세요
            
            건설적인 피드백과 구체적인 개선 방안을 한글로 제공해주세요.
            """, difficultyLevel.name(), userText, difficultyLevel.name());
    }
    
    /**
     * 일반 세션용 평가 프롬프트를 생성합니다.
     */
    private String createRegularEvaluationPrompt(String userText, SpeechSession.DifficultyLevel difficultyLevel) {
        return String.format("""
            당신은 영어 교육 전문가입니다. 다음 사용자의 발화를 %s 레벨 기준으로 평가해주세요.
            
            사용자 발화: "%s"
            
            다음 JSON 형식으로 상세하고 유용한 피드백을 제공해주세요. 'feedback' 필드에는 사용자 발화에 대한 구체적인 평가, 개선 방안, 그리고 추천 난이도를 모두 포함해야 합니다.
            {
                "feedback": "평가 내용...\\n\\n추천 난이도: [권장난이도]"
            }
            
            **난이도 기준 (반드시 이 기준을 따라주세요):**
            
            **초급 (BEGINNER):**
            - 문장 길이: 3-5단어 이하
            - 어휘: 일상생활 기본 단어만 (hello, food, like, want, go, see, eat, drink, work, home, family, friend)
            - 문법: 현재시제만, 단순한 주어+동사 구조
            - 표현: 매우 기본적인 의사소통 가능
            
            **중급 (INTERMEDIATE):**
            - 문장 길이: 5-10단어
            - 어휘: 일반적인 어휘, 동의어 사용
            - 문법: 과거/미래시제, 복합문, 전치사구
            - 표현: 자연스러운 대화, 감정/의견 표현 가능
            
            **고급 (ADVANCED):**
            - 문장 길이: 10단어 이상
            - 어휘: 고급 어휘, 관용구, 전문용어
            - 문법: 복잡한 문장구조, 가정법, 수동태
            - 표현: 추상적 개념, 논리적 설명, 문화적 맥락 이해
            
            평가 기준:
            - 문법: 정확성, 문장 구조, 단어 선택, 시제 일치
            - 어휘: 적절한 단어 선택, 표현의 다양성, 맥락에 맞는 사용
            - 유창성: 자연스러운 흐름, 적절한 휴식, 대화의 연결성
            
            주의사항:
            - 발음에 대한 평가는 하지 마세요 (음성 파일을 직접 들을 수 없음)
            - STT로 변환된 텍스트만을 기준으로 평가하세요
            - 주제나 내용의 관련성은 평가하지 마세요 - 오직 영어 실력만 평가하세요
            - 위의 난이도 기준을 정확히 적용하여 평가하세요
            - 구체적인 예시와 개선 방안을 포함해주세요
            - 평가 결과는 "~수준을 추천합니다" 형태로 작성하세요 (예: "중급 수준을 추천합니다")
            
            **난이도 추천 규칙 (절대적으로 따라야 함):**
            - 현재 사용자 난이도: %s
            - **중요**: 사용자의 실제 영어 실력이 현재 난이도보다 높을 때만 상위 난이도 추천
            - **초급 사용자**: 영어 실력이 초급 수준을 벗어났을 때만 중급 또는 고급 추천
            - **중급 사용자**: 영어 실력이 중급 수준을 벗어났을 때만 고급 추천
            - **고급 사용자**: 더 이상 상위 난이도가 없음
            - **평가 기준**: 위의 난이도 기준에 따라 사용자 발화를 정확히 평가한 후, 그 결과에 따라 추천
            - **잘못된 추천 금지**: 단순히 "높은 난이도"가 아니라, 실제 실력 평가 결과에 따른 추천만
            
            **추천 난이도 표시 규칙:**
            - 상위 난이도로 추천할 때만 "추천 난이도: [권장난이도]" 형태로 포함
            - "현재 수준 유지" 또는 "현재 난이도 유지" 같은 추천은 하지 마세요
            - 추천이 없을 때는 해당 문구를 아예 포함하지 마세요
            - **중요**: 난이도는 반드시 한글로 표현하세요 (초급, 중급, 고급)
            
            건설적인 피드백과 구체적인 개선 방안을 한글로 제공해주세요.
            """, difficultyLevel.name(), userText, difficultyLevel.name());
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
     * AI 응답을 파싱하여 피드백을 추출합니다.
     */
    private SpeechEvaluationResult parseAIEvaluationResponse(String aiResponse) {
        String feedback = extractField(aiResponse, "feedback");

        if (feedback == null || feedback.isEmpty()) {
            log.warn("Feedback field not found or empty in AI response: {}", aiResponse);
            feedback = "평가 정보를 사용할 수 없습니다. 나중에 다시 시도해주세요."; // 기본값
        }

        return SpeechEvaluationResult.builder()
                .feedback(feedback)
                .recommendedDifficulty(null) // 더 이상 사용하지 않음
                .build();
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
                .recommendedDifficulty(null) // 현재는 세션에 recommendedDifficulty가 직접 저장되지 않음
                .createdAt(session.getUpdatedAt())
                .build();
    }
    
    /**
     * 모든 세션의 평가 결과를 페이지네이션으로 조회합니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이지네이션된 평가 결과 목록
     */
    public Page<SpeechEvaluationResult> getAllEvaluations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SpeechSession> sessions = speechSessionRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        return sessions.map(session -> SpeechEvaluationResult.builder()
                .sessionId(session.getSessionId())
                .feedback(session.getFeedback())
                .recommendedDifficulty(null)
                .createdAt(session.getCreatedAt())
                .build());
    }

    /**
     * 오류 발생 시 기본 평가 결과를 생성합니다.
     */
    private SpeechEvaluationResult createDefaultEvaluation(String sessionId) {
        return SpeechEvaluationResult.builder()
                .sessionId(sessionId)
                .feedback("평가가 일시적으로 불가능합니다. 나중에 다시 시도해주세요.")
                .recommendedDifficulty(null) // 기본 평가 결과에도 recommendedDifficulty 추가
                .createdAt(LocalDateTime.now())
                .build();
    }
}
