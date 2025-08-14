package com.example.speechservice.controller;

import com.example.speechservice.dto.SessionStartRequest;
import com.example.speechservice.dto.SessionStartResponse;
import com.example.speechservice.dto.TalkRequest;
import com.example.speechservice.dto.TalkResponse;
import com.example.speechservice.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@CrossOrigin(origins = "*")
public class SessionController {
    
    @Autowired
    private SessionService sessionService;
    
    @PostMapping("/role-playing")
    public ResponseEntity<SessionStartResponse> startSession(@RequestBody SessionStartRequest request) {
        SessionStartResponse response = sessionService.startSession(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/role-playing/{sessionId}/talk")
    public ResponseEntity<TalkResponse> handleTalk(
            @PathVariable String sessionId,
            @RequestBody TalkRequest request) {
        TalkResponse response = sessionService.processTalk(sessionId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Session Service is running!");
    }
}
