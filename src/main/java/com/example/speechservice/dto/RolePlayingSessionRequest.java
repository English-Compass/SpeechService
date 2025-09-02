package com.example.speechservice.dto;

/**
 * 롤 플레잉 세션 시작 요청을 담는 DTO 클래스입니다.
 */
public class RolePlayingSessionRequest {
    
    private String userId;
    private String difficultyLevel;
    private String scenarioId;        // 미리 정의된 시나리오 ID (cafe, restaurant 등)
    private String customAiRole;      // 사용자 정의 AI 역할
    private String customUserRole;    // 사용자 정의 사용자 역할
    private String customSituation;   // 사용자 정의 상황
    
    public RolePlayingSessionRequest() {}
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    
    public String getCustomAiRole() { return customAiRole; }
    public void setCustomAiRole(String customAiRole) { this.customAiRole = customAiRole; }
    
    public String getCustomUserRole() { return customUserRole; }
    public void setCustomUserRole(String customUserRole) { this.customUserRole = customUserRole; }
    
    public String getCustomSituation() { return customSituation; }
    public void setCustomSituation(String customSituation) { this.customSituation = customSituation; }
    
    /**
     * 미리 정의된 시나리오를 사용하는지 확인합니다.
     */
    public boolean isPredefinedScenario() {
        return scenarioId != null && !scenarioId.isEmpty() && !"custom".equals(scenarioId);
    }
    
    /**
     * 사용자 정의 시나리오를 사용하는지 확인합니다.
     */
    public boolean isCustomScenario() {
        // scenarioId가 "custom", "null" 문자열이거나 null이면서, custom 필드들이 모두 유효한 경우
        boolean isScenarioIdCustom = scenarioId == null || scenarioId.isEmpty() || "null".equals(scenarioId) || "custom".equals(scenarioId);
        boolean hasCustomAiRole = customAiRole != null && !customAiRole.isEmpty() && !"null".equals(customAiRole);
        boolean hasCustomUserRole = customUserRole != null && !customUserRole.isEmpty() && !"null".equals(customUserRole);
        boolean hasCustomSituation = customSituation != null && !customSituation.isEmpty() && !"null".equals(customSituation);
        
        return isScenarioIdCustom && hasCustomAiRole && hasCustomUserRole && hasCustomSituation;
    }
    
    @Override
    public String toString() {
        return "RolePlayingSessionRequest{" +
               "userId='" + userId + '\'' +
               ", difficultyLevel='" + difficultyLevel + '\'' +
               ", scenarioId='" + scenarioId + '\'' +
               ", customAiRole='" + customAiRole + '\'' +
               ", customUserRole='" + customUserRole + '\'' +
               ", customSituation='" + customSituation + '\'' +
               '}';
    }
}

