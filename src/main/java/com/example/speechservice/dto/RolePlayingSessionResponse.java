package com.example.speechservice.dto;

import java.time.LocalDateTime;

/**
 * 롤 플레잉 세션 시작 응답을 담는 DTO 클래스입니다.
 */
public class RolePlayingSessionResponse {
    
    private String sessionId;
    private String difficultyLevel;
    private String aiRole;
    private String userRole;
    private String situation;
    private String aiFirstGreeting;
    private LocalDateTime createdAt;
    
    public RolePlayingSessionResponse() {}
    
    public RolePlayingSessionResponse(String sessionId, String difficultyLevel, String aiRole, 
                                   String userRole, String situation, String aiFirstGreeting, LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.difficultyLevel = difficultyLevel;
        this.aiRole = aiRole;
        this.userRole = userRole;
        this.situation = situation;
        this.aiFirstGreeting = aiFirstGreeting;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    
    public String getAiRole() { return aiRole; }
    public void setAiRole(String aiRole) { this.aiRole = aiRole; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }
    
    public String getAiFirstGreeting() { return aiFirstGreeting; }
    public void setAiFirstGreeting(String aiFirstGreeting) { this.aiFirstGreeting = aiFirstGreeting; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

