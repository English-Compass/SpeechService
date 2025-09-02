package com.example.speechservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.speechservice.dto.SessionEndResponse;
import com.example.speechservice.dto.SessionStartRequest;
import com.example.speechservice.dto.SessionStartResponse;
import com.example.speechservice.dto.SpeechEvaluationResult;
import com.example.speechservice.dto.TalkResponse;
import com.example.speechservice.service.EvaluationService;
import com.example.speechservice.service.SpeechSessionService;

/**
 * 롤플레잉 음성 세션과 관련된 HTTP 요청을 처리하는 REST 컨트롤러입니다.
 * 음성 세션 시작, 대화 진행 등의 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/speech-sessions") // 모든 음성 세션 관련 엔드포인트의 기본 경로
@CrossOrigin(origins = "*") // CORS 허용 (개발 단계에서 모든 출처 허용)
public class SpeechSessionController {
    
    private final SpeechSessionService speechSessionService;
    private final EvaluationService evaluationService;
    
    public SpeechSessionController(SpeechSessionService speechSessionService, EvaluationService evaluationService) {
        this.speechSessionService = speechSessionService;
        this.evaluationService = evaluationService;
    }
    
    /**
     * 새로운 롤플레잉 세션을 시작하는 엔드포인트입니다.
     * 클라이언트로부터 SessionStartRequest를 받아 세션을 초기화하고 AI의 첫 인사를 반환합니다.
     *
     * @param request 세션 시작 요청 데이터 (사용자 ID, 주제, 난이도 등)
     * @return 생성된 세션 정보 및 AI의 첫 인사를 포함하는 ResponseEntity (HTTP 200 OK)
     */
    @PostMapping("/role-playing")
    public ResponseEntity<SessionStartResponse> startSession(@RequestBody SessionStartRequest request) {
        SessionStartResponse response = speechSessionService.startSession(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 기존 롤플레잉 세션에서 사용자의 음성 발화를 처리하고 AI의 응답을 반환하는 엔드포인트입니다.
     * 메모리 스트림을 직접 사용하여 딜레이를 최소화합니다.
     * 사용자의 음성 파일을 받아 텍스트로 변환하고, AI와 대화를 진행한 후 AI의 음성 응답을 반환합니다.
     *
     * @param sessionId 현재 진행 중인 세션의 고유 ID
     * @param audio 사용자가 녹음한 음성 파일 (MultipartFile)
     * @return AI가 생성한 텍스트 응답을 포함하는 ResponseEntity (HTTP 200 OK)
     */
    @PostMapping("/role-playing/{sessionId}/talk")
    public ResponseEntity<TalkResponse> handleTalk(
            @PathVariable String sessionId,
            @RequestParam("audio") MultipartFile audio) {
        try {
            // MultipartFile을 InputStream으로 변환하여 메모리에서 직접 처리
            TalkResponse response = speechSessionService.processTalk(sessionId, audio.getInputStream());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 음성 파일 처리 중 예외 발생 시 서버 에러 (HTTP 500) 응답을 반환합니다.
            // 실제 서비스에서는 에러 로깅, 사용자에게 더 상세한 에러 메시지 제공 등의 처리가 필요합니다.
            e.printStackTrace(); // 개발 중 디버깅을 위해 스택 트레이스 출력
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 세션의 음성 평가 결과를 조회하는 엔드포인트입니다.
     * 사용자의 발화 내용에 대한 AI 평가 결과(발음, 문법, 유창성 점수 및 피드백)를 반환합니다.
     *
     * @param sessionId 조회할 세션의 고유 ID
     * @return 해당 세션의 평가 결과 목록을 포함하는 ResponseEntity
     */
    @GetMapping("/role-playing/{sessionId}/evaluations")
    public ResponseEntity<SpeechEvaluationResult> getSessionEvaluations(@PathVariable String sessionId) {
        SpeechEvaluationResult evaluation = evaluationService.getSessionEvaluation(sessionId);
        return ResponseEntity.ok(evaluation);
    }
    
    /**
     * 롤플레잉 세션을 종료하는 엔드포인트입니다.
     * 사용자가 수동으로 세션을 종료할 때 사용됩니다.
     *
     * @param sessionId 종료할 세션의 고유 ID
     * @return 세션 종료 결과를 포함하는 ResponseEntity
     */
    @PostMapping("/role-playing/{sessionId}/end")
    public ResponseEntity<SessionEndResponse> endSession(@PathVariable String sessionId) {
        SessionEndResponse response = speechSessionService.endSession(sessionId, "MANUAL");
        return ResponseEntity.ok(response);
    }

    /**
     * 모든 세션의 평가 결과를 페이지네이션으로 조회하는 엔드포인트입니다.
     * 사용자가 이전 세션들의 피드백을 확인할 때 사용됩니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이지네이션된 평가 결과 목록을 포함하는 ResponseEntity
     */
    @GetMapping("/evaluations")
    public ResponseEntity<Page<SpeechEvaluationResult>> getAllEvaluations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<SpeechEvaluationResult> evaluations = evaluationService.getAllEvaluations(page, size);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * 서비스의 상태를 확인하기 위한 헬스 체크 엔드포인트입니다.
     * 서비스가 정상적으로 작동하는지 확인할 때 사용됩니다.
     *
     * @return 서비스 상태 메시지를 포함하는 ResponseEntity (HTTP 200 OK)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Session Service is running!");
    }
}
