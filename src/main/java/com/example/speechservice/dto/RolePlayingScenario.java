package com.example.speechservice.dto;

/**
 * 롤 플레잉 시나리오 정보를 담는 DTO 클래스입니다.
 */
public class RolePlayingScenario {
    
    private String id;
    private String name;
    private String aiRole;
    private String userRole;
    private String situation;
    private String description;
    
    public RolePlayingScenario() {}
    
    public RolePlayingScenario(String id, String name, String aiRole, String userRole, String situation, String description) {
        this.id = id;
        this.name = name;
        this.aiRole = aiRole;
        this.userRole = userRole;
        this.situation = situation;
        this.description = description;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAiRole() { return aiRole; }
    public void setAiRole(String aiRole) { this.aiRole = aiRole; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

