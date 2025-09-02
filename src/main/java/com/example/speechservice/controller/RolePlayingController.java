package com.example.speechservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.speechservice.dto.RolePlayingScenario;
import com.example.speechservice.dto.RolePlayingSessionRequest;
import com.example.speechservice.dto.RolePlayingSessionResponse;
import com.example.speechservice.service.RolePlayingScenarioService;
import com.example.speechservice.service.SpeechSessionService;

/**
 * 롤 플레잉 관련 API를 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/v1/role-playing")
@CrossOrigin(origins = "*")
public class RolePlayingController {
    
    private final RolePlayingScenarioService rolePlayingScenarioService;
    private final SpeechSessionService speechSessionService;
    
    @Autowired
    public RolePlayingController(RolePlayingScenarioService rolePlayingScenarioService, 
                               SpeechSessionService speechSessionService) {
        this.rolePlayingScenarioService = rolePlayingScenarioService;
        this.speechSessionService = speechSessionService;
    }
    
    /**
     * 미리 정의된 롤 플레잉 시나리오 목록을 반환합니다.
     */
    @GetMapping("/scenarios")
    public ResponseEntity<List<RolePlayingScenario>> getScenarios() {
        List<RolePlayingScenario> scenarios = rolePlayingScenarioService.getPredefinedScenarios();
        return ResponseEntity.ok(scenarios);
    }
    
    /**
     * 롤 플레잉 세션을 시작합니다.
     */
    @PostMapping("/start")
    public ResponseEntity<?> startRolePlayingSession(@RequestBody RolePlayingSessionRequest request) {
        try {
            // 요청 로깅 추가
            System.out.println("롤 플레잉 세션 시작 요청: " + request);
            System.out.println("userId: " + request.getUserId());
            System.out.println("difficultyLevel: " + request.getDifficultyLevel());
            System.out.println("scenarioId: " + request.getScenarioId());
            
            RolePlayingSessionResponse response = speechSessionService.startRolePlayingSession(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 오류 로깅 추가
            System.err.println("롤 플레잉 세션 시작 오류: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
