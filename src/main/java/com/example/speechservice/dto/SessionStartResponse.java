package com.example.speechservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionStartResponse {
    private String sessionId;
    private String topic;
    private String difficultyLevel;
    private String aiFirstGreeting;
    private LocalDateTime createdAt;
}
