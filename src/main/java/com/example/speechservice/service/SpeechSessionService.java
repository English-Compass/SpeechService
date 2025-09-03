package com.example.speechservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.speechservice.dto.RolePlayingScenario;
import com.example.speechservice.dto.RolePlayingSessionRequest;
import com.example.speechservice.dto.RolePlayingSessionResponse;
import com.example.speechservice.dto.SessionEndResponse;
import com.example.speechservice.dto.SessionStartRequest;
import com.example.speechservice.dto.SessionStartResponse;
import com.example.speechservice.dto.TalkResponse;
import com.example.speechservice.entity.SpeechSession;
import com.example.speechservice.repository.SpeechSessionRepository;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * 롤플레잉 음성 세션의 핵심 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 음성 세션 생성 및 관리, AI 대화 진행, 대화 기록 저장 등의 기능을 수행합니다.
 */
@Service
@Slf4j
public class SpeechSessionService {

    private final SpeechSessionRepository speechSessionRepository;
    private final OpenAIService openAIService;
    private final EvaluationService evaluationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RolePlayingScenarioService rolePlayingScenarioService;

    public SpeechSessionService(SpeechSessionRepository speechSessionRepository,
                          OpenAIService openAIService,
                          EvaluationService evaluationService,
                          RedisTemplate<String, Object> redisTemplate,
                          RolePlayingScenarioService rolePlayingScenarioService) {
        this.speechSessionRepository = speechSessionRepository;
        this.openAIService = openAIService;
        this.evaluationService = evaluationService;
        this.redisTemplate = redisTemplate;
        this.rolePlayingScenarioService = rolePlayingScenarioService;
    }

    /**
     * Redis에 저장될 채팅 기록의 키를 생성하는 헬퍼 메서드.
     * 각 세션의 채팅 기록은 고유한 Redis 키로 관리됩니다.
     *
     * @param sessionId 세션 ID
     * @return Redis 키 문자열
     */
    private String getChatHistoryKey(String sessionId) {
        return "chat_history:" + sessionId;
    }

    /**
     * 새로운 롤플레잉 세션을 시작합니다.
     * 고유한 세션 ID를 발급하며, AI의 첫 인사를 생성합니다.
     * 생성된 세션 정보와 AI의 첫 인사를 Redis에 저장합니다.
     *
     * @param request 세션 시작 요청 데이터 (사용자 ID, 주제, 난이도 등)
     * @return 생성된 세션의 상세 정보를 담은 SessionStartResponse 객체
     */
    @Transactional
    public SessionStartResponse startSession(SessionStartRequest request) {
        // 1. 세션 ID 및 AI 첫 인사 생성: 고유한 세션 ID를 생성하고, 설정된 주제와 난이도에 맞는 AI의 첫 인사를 생성합니다.
        String sessionId = generateUniqueSessionId();
        String aiFirstGreeting = generateAIFirstGreeting(request.getTopic(), request.getDifficultyLevel());

        // 2. SpeechSession 엔티티 생성 및 저장: 세션 정보를 데이터베이스에 저장합니다.
        SpeechSession session = new SpeechSession();
        session.setSessionId(sessionId);
        session.setUserId(request.getUserId());
        session.setTopic(request.getTopic());
        session.setDifficultyLevel(request.getDifficultyLevel());

        SpeechSession savedSession = speechSessionRepository.save(session);

        // 3. 새 세션 시작 시 AI 첫 인사를 Redis에 저장: 초기 대화 기록으로 AI의 첫 인사를 Redis에 저장하고, 1시간의 만료 시간을 설정합니다.
        List<ChatMessage> initialMessages = new ArrayList<>();
        initialMessages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), aiFirstGreeting));
        redisTemplate.opsForValue().set(getChatHistoryKey(sessionId), initialMessages, Duration.ofHours(1));

        // 4. 응답 DTO 생성 및 반환: 생성된 세션 정보를 응답 객체로 빌드하여 반환합니다.
        return SessionStartResponse.builder()
                .sessionId(savedSession.getSessionId())
                .topic(savedSession.getTopic())
                .difficultyLevel(savedSession.getDifficultyLevel().name())
                .aiFirstGreeting(aiFirstGreeting)
                .createdAt(savedSession.getCreatedAt())
                .build();
    }
    
    /**
     * 롤 플레잉 세션을 시작합니다.
     */
    @Transactional
    public RolePlayingSessionResponse startRolePlayingSession(RolePlayingSessionRequest request) {
        log.info("시나리오 타입 확인 - isPredefinedScenario: {}, isCustomScenario: {}", 
                request.isPredefinedScenario(), request.isCustomScenario());
        log.info("요청 데이터 - scenarioId: '{}', customAiRole: '{}', customUserRole: '{}', customSituation: '{}'", 
                request.getScenarioId(), request.getCustomAiRole(), request.getCustomUserRole(), request.getCustomSituation());
        
        // 1. 시나리오 정보 가져오기
        RolePlayingScenario scenario;
        if (request.isPredefinedScenario()) {
            log.info("미리 정의된 시나리오 사용");
            scenario = rolePlayingScenarioService.findScenarioById(request.getScenarioId());
        } else if (request.isCustomScenario()) {
            log.info("사용자 정의 시나리오 사용");
            scenario = rolePlayingScenarioService.createCustomScenario(
                request.getCustomAiRole(), 
                request.getCustomUserRole(), 
                request.getCustomSituation()
            );
        } else {
            log.error("유효하지 않은 시나리오 정보");
            throw new IllegalArgumentException("유효하지 않은 시나리오 정보입니다.");
        }
        
        log.info("생성된 시나리오: {}", scenario);
        
        if (scenario == null) {
            log.error("시나리오가 null입니다. 요청 데이터를 다시 확인해주세요.");
            throw new IllegalStateException("시나리오 생성에 실패했습니다.");
        }
        
        // 2. 동적 상황 생성 및 AI 첫 인사 생성
        String dynamicSituation = rolePlayingScenarioService.generateDynamicSituation(
            scenario.getAiRole(), 
            scenario.getUserRole(), 
            scenario.getSituation()
        );
        
        String sessionId = generateUniqueSessionId();
        String aiFirstGreeting = generateRolePlayingAIFirstGreeting(scenario, request.getDifficultyLevel());
        
        // 3. SpeechSession 엔티티 생성 및 저장
        SpeechSession session = new SpeechSession();
        session.setSessionId(sessionId);
        
        // userId 처리 - 숫자 형식이면 Long으로 변환, 아니면 기본값 사용
        Long userId;
        try {
            userId = Long.parseLong(request.getUserId());
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 기본값 사용
            userId = 1L;
        }
        session.setUserId(userId);
        
        session.setTopic("role-playing:" + scenario.getId());
        session.setDifficultyLevel(SpeechSession.DifficultyLevel.valueOf(request.getDifficultyLevel()));
        
        // 사용자 정의 시나리오의 경우, 역할과 상황 정보를 세션에 직접 저장
        if (request.isCustomScenario()) {
            session.setAiRole(scenario.getAiRole());
            session.setUserRole(scenario.getUserRole());
            session.setSituation(scenario.getSituation());
        }

        SpeechSession savedSession = speechSessionRepository.save(session);
        
        // 4. AI 첫 인사를 Redis에 저장 (aiFirstGreeting이 null이 아닌 경우에만)
        List<ChatMessage> initialMessages = new ArrayList<>();
        if (aiFirstGreeting != null) {
            initialMessages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), aiFirstGreeting));
        }
        redisTemplate.opsForValue().set(getChatHistoryKey(sessionId), initialMessages, Duration.ofHours(1));
        
        // 5. 응답 DTO 생성 및 반환 (동적 상황 포함)
        return new RolePlayingSessionResponse(
            savedSession.getSessionId(),
            request.getDifficultyLevel(),
            scenario.getAiRole(),
            scenario.getUserRole(),
            dynamicSituation,  // 동적으로 생성된 상황 사용
            aiFirstGreeting,
            savedSession.getCreatedAt()
        );
    }
    
    /**
     * 롤 플레잉 시나리오에 맞는 AI의 첫 인사를 생성합니다.
     */
    private String generateRolePlayingAIFirstGreeting(RolePlayingScenario scenario, String difficultyLevel) {
        log.info("=== AI First Greeting Debug ===");
        log.info("AI Role: '{}', Scenario ID: '{}'", scenario.getAiRole(), scenario.getId());
        log.info("Scenario type check - isPredefined: {}", isPredefinedScenarioById(scenario.getId()));
        
        // 정해진 시나리오인지 확인 (시나리오 ID로 판단)
        if (isPredefinedScenarioById(scenario.getId())) {
            log.info("정해진 시나리오로 인식됨: {}", scenario.getId());
            String greeting = generatePredefinedScenarioGreeting(scenario.getAiRole(), difficultyLevel);
            log.info("생성된 첫 인사: '{}'", greeting);
            return greeting;
        } else {
            log.info("사용자 정의 시나리오로 인식됨: {}", scenario.getId());
            log.info("사용자 정의 역할은 AI 첫 인사 없음 (null 반환)");
            // 사용자 정의 역할은 AI 첫 인사 없음 (null 반환)
            return null;
        }
    }
    
    /**
     * 정해진 시나리오인지 확인하는 헬퍼 메서드 (시나리오 ID로 판단)
     */
    private boolean isPredefinedScenarioById(String scenarioId) {
        log.info("isPredefinedScenarioById 체크: '{}'", scenarioId);
        
        // 정해진 시나리오 ID 목록
        boolean isPredefined = "cafe".equals(scenarioId) || "restaurant".equals(scenarioId) || 
               "hotel".equals(scenarioId) || "shop".equals(scenarioId) || 
               "doctor".equals(scenarioId) || "airport".equals(scenarioId) || 
               "bank".equals(scenarioId);
               
        log.info("isPredefined 결과: {}", isPredefined);
        return isPredefined;
    }
    
    /**
     * 정해진 시나리오용 통일된 첫 인사 생성
     */
    private String generatePredefinedScenarioGreeting(String aiRole, String difficultyLevel) {
        // 모든 정해진 시나리오는 통일된 인사 사용
        return "Hello! How can I help you?";
    }
    

    
    /**
     * 일반 세션을 위한 시스템 프롬프트를 생성합니다.
     */
    private String generateRegularSystemPrompt(SpeechSession session) {
        return String.format(
            "You are a friendly English tutor for %s level students. " +
            "Your user wants to practice speaking about %s. " +
            "Use vocabulary and grammar appropriate for %s level. " +
            "Keep your responses encouraging and educational. " +
            "**USER SPEECH CORRECTION POLICY - SMART CORRECTIONS ONLY:** " +
            "**CORRECT ONLY when you detect:** " +
            "1. **Major grammatical errors**: Wrong verb tense, missing articles, subject-verb agreement " +
            "2. **Significant vocabulary misuse**: Wrong word choice that completely changes meaning " +
            "3. **Unclear/incomplete sentences**: When the user's meaning is hard to understand " +
            "4. **Obvious STT errors**: Clear transcription mistakes (e.g., 'I like to eat' vs 'I like to it') " +
            "**DO NOT correct:** " +
            "- Minor pronunciation variations " +
            "- Informal/casual expressions " +
            "- Regional English differences " +
            "- When the user's meaning is clear despite small errors " +
            "- Every single mistake (be selective!) " +
            "**Correction approach:** " +
            "- Be gentle and encouraging " +
            "- Use: 'You can also say: [corrected version]' or 'A more natural way: [corrected version]' " +
            "- Don't interrupt the conversation flow " +
            "- Focus on helping, not criticizing " +
            "**STRICT LEVEL GUIDELINES - Follow these EXACTLY:** " +
            "For BEGINNER level: " +
            "- Use ONLY basic, everyday words (hello, good, food, like, want, go, see, eat, drink, work, home, family, friend) " +
            "- Keep sentences VERY short (3-5 words maximum) " +
            "- Use simple present tense ONLY " +
            "- Use repetition and encouragement (like: 'Yes! Good! You can say: I like pizza.') " +
            "For INTERMEDIATE level: " +
            "- Use common vocabulary and varied sentence structures " +
            "- Include past and future tenses " +
            "- Sentences can be 5-10 words " +
            "- Encourage natural conversation flow " +
            "For ADVANCED level: " +
            "- Use sophisticated vocabulary and complex sentence structures " +
            "- Include idioms, expressions, and cultural references " +
            "- Encourage deeper discussions and abstract thinking " +
            "- Sentences can be 10+ words with multiple clauses",
            session.getDifficultyLevel().name(),
            session.getTopic(),
            session.getDifficultyLevel().name()
        );
    }
    
    /**
     * 세션에 맞는 시스템 프롬프트를 가져오거나 생성합니다.
     */
    private String getSystemPrompt(SpeechSession session, List<ChatMessage> messages) {
        // 첫 메시지이거나 시스템 프롬프트가 없는 경우에만 새로 생성
        if (messages.isEmpty() || !"system".equals(messages.get(0).getRole())) {
            log.info("새 시스템 메시지 추가");
            String prompt;
            if (session.getTopic() != null && "role-playing:custom".equals(session.getTopic())) {
                log.info("사용자 정의 롤플레잉 세션 감지 - 전용 프롬프트 생성");
                prompt = generateCustomRolePlayingSystemPrompt(session);
            } else if (session.getTopic() != null && session.getTopic().startsWith("role-playing:")) {
                log.info("사전 정의 롤플레잉 세션 감지 - 롤플레잉 시스템 프롬프트 생성");
                prompt = generatePredefinedRolePlayingSystemPrompt(session);
            } else {
                log.info("일반 세션 감지 - 일반 시스템 프롬프트 생성");
                prompt = generateRegularSystemPrompt(session);
            }
            messages.add(0, new ChatMessage("system", prompt));
            return prompt;
        }
        return messages.get(0).getContent();
    }

    /**
     * 사전 정의된 롤 플레잉 세션을 위한 시스템 프롬프트를 생성합니다.
     */
    private String generatePredefinedRolePlayingSystemPrompt(SpeechSession session) {
        RolePlayingScenario scenario;

        // 사용자 정의 시나리오인 경우, 세션에 저장된 정보를 사용
        if ("role-playing:custom".equals(session.getTopic()) && session.getAiRole() != null) {
            scenario = new RolePlayingScenario(
                "custom",
                "사용자 정의",
                session.getAiRole(),
                session.getUserRole(),
                session.getSituation(),
                "사용자가 정의한 롤 플레잉 시나리오"
            );
        } else {
            // 미리 정의된 시나리오인 경우, ID로 조회
            String scenarioId = session.getTopic().substring("role-playing:".length());
            scenario = rolePlayingScenarioService.findScenarioById(scenarioId);
        }
        
        if (scenario == null) {
            // 시나리오를 찾을 수 없는 경우 기본 프롬프트 사용
            log.warn("시나리오 정보를 찾을 수 없어 일반 시스템 프롬프트를 사용합니다. SessionId: {}", session.getSessionId());
            return generateRegularSystemPrompt(session);
        }

        return String.format(
            "--- CRITICAL INSTRUCTIONS ---\n" +
            "You are NOT an English teacher, tutor, or language instructor. You are a REAL %s in this REAL situation.\n" +
            "NEVER mention English practice, learning, tutoring, or language skills.\n" +
            "NEVER ask about English practice or offer language help.\n" +
            "NEVER say things like 'How can I help you with your English?'\n" +
            "Respond exactly as a real %s would in this real situation. Stay completely in character at all times.\n" +
            "--- Context ---\n" +
            "Your Role: %s\n" +
            "User's Role: %s\n" +
            "Situation: %s\n" +
            "--- Language Level ---\n" +
            "Use %s level vocabulary and sentence structures, but do it as part of your natural character speech, not as teaching.\n" +
            "--- Correction Policy ---\n" +
            "Only correct the user if they make a major error that completely breaks understanding. If you must correct, be gentle: 'You can also say: [correction]'",
            scenario.getAiRole(),
            scenario.getAiRole(),
            scenario.getAiRole(),
            scenario.getUserRole(),
            scenario.getSituation(),
            session.getDifficultyLevel().name()
        );
    }

    /**
     * 사용자 정의 롤 플레잉 세션을 위한, 순수 역할극 전용 시스템 프롬프트를 생성합니다.
     */
    private String generateCustomRolePlayingSystemPrompt(SpeechSession session) {
        if (session.getAiRole() == null || session.getUserRole() == null || session.getSituation() == null) {
            log.warn("사용자 정의 롤플레잉 정보가 세션에 없어 일반 프롬프트를 사용합니다. SessionId: {}", session.getSessionId());
            return generateRegularSystemPrompt(session);
        }

        return String.format(
            "--- CRITICAL ROLE-PLAYING INSTRUCTIONS ---\n" +
            "You are a character in a role-playing scenario. You are NOT an AI assistant or a language tutor.\n" +
            "Your ONLY job is to play your character authentically.\n" +
            "**You MUST respond in English.**\n" +
            "NEVER break character for any reason.\n" +
            "NEVER mention that this is a role-play, a simulation, or for practice.\n" +
            "Treat this as a real conversation. Respond naturally based on your character and the situation.\n" +
            "--- Your Character ---\n" +
            "Your Role: %s\n" +
            "User's Role: %s\n" +
            "Situation: %s\n" +
            "--- Language Style ---\n" +
            "Use %s level English vocabulary and sentence structures as part of your natural character speech.",
            session.getAiRole(),
            session.getUserRole(),
            session.getSituation(),
            session.getDifficultyLevel().name()
        );
    }

    /**
     * 기존 롤플레잉 세션에서 사용자의 음성 발화를 처리하고 AI의 응답을 반환합니다.
     * 메모리 스트림을 직접 사용하여 딜레이를 최소화합니다.
     * 음성 스트림을 텍스트로 변환하고, Redis에서 이전 대화 기록을 불러와 AI와 대화를 진행한 후,
     * 업데이트된 대화 기록을 다시 Redis에 저장하고 AI의 응답을 반환합니다.
     *
     * @param sessionId 현재 진행 중인 세션의 고유 ID
     * @param audioStream 사용자가 녹음한 음성 데이터 스트림
     * @return AI가 생성한 텍스트 응답을 담은 TalkResponse 객체
     */
    @Transactional
    public TalkResponse processTalk(String sessionId, InputStream audioStream) {
        // 1. 세션 조회: 주어진 세션 ID로 데이터베이스에서 SpeechSession을 조회합니다.
        SpeechSession session = speechSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        // 2. 음성 스트림을 텍스트로 변환: OpenAI Whisper API를 사용하여 사용자의 음성 발화를 텍스트로 변환합니다. (메모리에서 직접 처리)
        String userText = openAIService.convertSpeechToText(audioStream);
        log.info("사용자 발화 변환 완료: {}", userText);

        // 3. 대화 히스토리 불러오기 (Redis): Redis에서 현재 세션의 이전 대화 기록을 불러옵니다.
        //    기록이 없으면 새로운 ArrayList를 생성합니다.
        @SuppressWarnings("unchecked")
        List<ChatMessage> messages = Optional.ofNullable((List<ChatMessage>) redisTemplate.opsForValue().get(getChatHistoryKey(sessionId)))
                                            .orElse(new ArrayList<>());

        // 4. 시스템 메시지 추가 또는 교체
        log.info("=== System Prompt Debug ===");
        log.info("Messages size: {}, isEmpty: {}", messages.size(), messages.isEmpty());
        if (!messages.isEmpty()) {
            log.info("First message role: {}", messages.get(0).getRole());
        }
        log.info("Session topic: {}", session.getTopic());
        
        getSystemPrompt(session, messages);

        // 5. 사용자의 현재 발화 추가: 변환된 사용자의 텍스트를 대화 기록에 추가합니다.
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), userText));

        // 6. OpenAI API를 통해 AI 응답 받아오기: 업데이트된 대화 기록을 기반으로 OpenAI ChatCompletion API를 호출하여 AI의 응답을 생성합니다.
        String aiResponseText = openAIService.getChatResponse(messages);

        // 7. 새로운 대화(사용자 발화 + AI 응답)를 Redis에 저장: AI의 응답을 대화 기록에 추가하고,
        //    업데이트된 대화 기록을 Redis에 다시 저장하며, 1시간의 만료 시간을 설정합니다.
        messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), aiResponseText));
        redisTemplate.opsForValue().set(getChatHistoryKey(sessionId), messages, Duration.ofHours(1));

        // 8. 대화 길이 제한 체크 및 자동 종료
        checkConversationLimit(sessionId, messages);

        // 9. AI의 텍스트 응답을 음성으로 변환: 난이도에 맞는 음성 속도로 OpenAI Speech API를 사용하여 AI의 텍스트 응답을 음성 데이터로 변환합니다.
        //    InputStream으로 반환되는 음성 데이터를 Base64로 인코딩하여 TalkResponse에 포함시킵니다.
        String audioDataBase64;
        try (InputStream audioInputStream = openAIService.convertTextToSpeech(aiResponseText, session.getDifficultyLevel().name())) {
            audioDataBase64 = Base64.getEncoder().encodeToString(audioInputStream.readAllBytes());
        } catch (IOException e) {
            log.error("AI 음성 변환 실패: {}", sessionId, e);
            audioDataBase64 = ""; // 음성 변환 실패 시 빈 문자열로 처리
        }

        // 10. 비동기로 사용자 발화 평가 수행: 사용자의 발화 내용을 평가하고 피드백을 생성합니다.
        //    이는 대화 응답과 별개로 백그라운드에서 수행되어 사용자 경험을 방해하지 않습니다.
        CompletableFuture.runAsync(() -> {
            try {
                evaluationService.evaluateSpeech(userText, session.getTopic(), 
                                               session.getDifficultyLevel(), session);
                log.info("Speech evaluation completed for session: {}", sessionId);
            } catch (Exception e) {
                log.error("Error during speech evaluation for session: {}", sessionId, e);
            }
        });

                        // 11. 응답 반환: AI의 텍스트 응답과 Base64 인코딩된 음성 응답을 TalkResponse 객체로 묶어 반환합니다.
                return new TalkResponse(aiResponseText, audioDataBase64, userText, "evaluating");
    }

    /**
     * 기존 MultipartFile을 사용하는 메소드 (하위 호환성을 위해 유지)
     *
     * @param sessionId 현재 진행 중인 세션의 고유 ID
     * @param audio 사용자가 녹음한 음성 파일 (MultipartFile)
     * @return AI가 생성한 텍스트 응답을 담은 TalkResponse 객체
     * @throws IOException 음성 파일을 텍스트로 변환하는 중 발생할 수 있는 입출력 예외
     */
    @Transactional
    public TalkResponse processTalk(String sessionId, MultipartFile audio) throws IOException {
        // MultipartFile을 InputStream으로 변환하여 새로운 메소드 호출
        return processTalk(sessionId, audio.getInputStream());
    }

    /**
     * 세션을 종료하는 메서드입니다.
     * 세션 종료 시간을 설정하고, Redis에서 대화 기록을 삭제하며, 세션 상태를 업데이트합니다.
     *
     * @param sessionId 종료할 세션 ID
     * @param endReason 종료 사유 (MANUAL, CONVERSATION_LIMIT, TIME_LIMIT)
     * @return 세션 종료 결과를 담은 SessionEndResponse 객체
     */
    @Transactional
    public SessionEndResponse endSession(String sessionId, String endReason) {
        // 1. 세션 조회
        SpeechSession session = speechSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        // 2. 세션 종료 시간 설정
        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);

        // 3. Redis에서 대화 기록 삭제
        redisTemplate.delete(getChatHistoryKey(sessionId));

        // 4. 세션 저장
        SpeechSession savedSession = speechSessionRepository.save(session);

        // 5. 종료 메시지 생성
        String message = switch (endReason) {
            case "MANUAL" -> "사용자가 세션을 종료했습니다.";
            case "CONVERSATION_LIMIT" -> "대화 길이 제한으로 세션이 종료되었습니다.";
            case "TIME_LIMIT" -> "시간 제한으로 세션이 종료되었습니다.";
            default -> "세션이 종료되었습니다.";
        };

        // 6. 응답 생성 및 반환
        return SessionEndResponse.builder()
                .sessionId(savedSession.getSessionId())
                .topic(savedSession.getTopic())
                .difficultyLevel(savedSession.getDifficultyLevel().name())
                .startTime(savedSession.getStartTime())
                .endTime(savedSession.getEndTime())
                .totalDuration(Duration.between(savedSession.getStartTime(), savedSession.getEndTime()))
                .endReason(endReason)
                .message(message)
                .build();
    }

    /**
     * 고유한 세션 ID를 생성하는 헬퍼 메서드.
     * 중복되지 않는 세션 ID가 생성될 때까지 반복하여 보장합니다.
     *
     * @return 고유한 세션 ID 문자열
     */
    private String generateUniqueSessionId() {
        String sessionId;
        do {
            sessionId = "session_" + UUID.randomUUID().toString().substring(0, 8);
        } while (speechSessionRepository.existsBySessionId(sessionId));
        return sessionId;
    }

    /**
     * 대화 길이 제한을 체크하고 필요시 세션을 자동 종료하는 헬퍼 메서드.
     * 18번째 대화에서 경고 메시지를 추가하고, 20번째 대화에서 자동 종료합니다.
     *
     * @param sessionId 세션 ID
     * @param messages 현재 대화 기록
     */
    private void checkConversationLimit(String sessionId, List<ChatMessage> messages) {
        // 시스템 메시지를 제외한 실제 대화 교환 횟수 계산 (사용자 + AI = 2개씩)
        int conversationCount = (messages.size() - 1) / 2; // -1은 시스템 메시지 제외
        
        if (conversationCount >= 18) {
            // 18번째 대화에서 경고 메시지 추가
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "세션이 곧 종료됩니다. 마무리하시겠습니까?"));
            
            // Redis에 경고 메시지가 포함된 대화 기록 저장
            redisTemplate.opsForValue().set(getChatHistoryKey(sessionId), messages, Duration.ofHours(1));
        }
        
        if (conversationCount >= 20) {
            // 20번째 대화에서 자동 종료
            log.info("Session {} reached conversation limit (20), auto-ending", sessionId);
            endSession(sessionId, "CONVERSATION_LIMIT");
        }
    }



    /**
     * AI의 첫 인사를 생성하는 헬퍼 메서드.
     * 세션의 주제와 난이도에 따라 다른 첫 인사를 제공합니다.
     *
     * @param topic 세션의 주제
     * @param difficultyLevel 세션의 난이도 (BEGINNER, INTERMEDIATE, ADVANCED)
     * @return AI의 첫 인사 텍스트
     */
    private String generateAIFirstGreeting(String topic, SpeechSession.DifficultyLevel difficultyLevel) {
        String greeting = switch (difficultyLevel) {
            case BEGINNER -> "Hi! I'm your English friend. Let's talk about " + topic + " in simple words. How are you today?";
            case INTERMEDIATE -> "Hello there! I'm excited to discuss " + topic + " with you. Are you ready for our conversation?";
            case ADVANCED -> "Greetings! I'm looking forward to an engaging discussion about " + topic + ". Let's dive into this topic together.";
        };
        return greeting;
    }
    
    /**
     * 10분이 경과한 세션을 자동으로 종료하는 스케줄러 메서드입니다.
     * 5분마다 실행되어 장시간 실행 중인 세션을 안전장치로 종료합니다.
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행 (300,000ms = 5분)
    public void checkAndEndLongRunningSessions() {
        try {
            log.info("Checking for long-running sessions...");
            
            // 활성 세션 조회 (endTime이 null인 세션들)
            List<SpeechSession> activeSessions = speechSessionRepository.findByEndTimeIsNull();
            
            LocalDateTime now = LocalDateTime.now();
            int endedCount = 0;
            
            for (SpeechSession session : activeSessions) {
                Duration sessionDuration = Duration.between(session.getStartTime(), now);
                
                if (sessionDuration.toMinutes() >= 10) { // 10분 이상 실행된 세션
                    log.info("Session {} has been running for {} minutes, auto-ending due to time limit", 
                            session.getSessionId(), sessionDuration.toMinutes());
                    
                    endSession(session.getSessionId(), "TIME_LIMIT");
                    endedCount++;
                }
            }
            
            if (endedCount > 0) {
                log.info("Auto-ended {} sessions due to time limit", endedCount);
            }
            
        } catch (Exception e) {
            log.error("Error during long-running session check", e);
        }
    }
}
