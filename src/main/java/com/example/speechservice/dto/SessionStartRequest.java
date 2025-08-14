package com.example.speechservice.dto;

import lombok.Data;
import com.example.speechservice.entity.SpeechSession.DifficultyLevel;

@Data
public class SessionStartRequest {
    private Long kakaoId; // 사용자 식별을 위한 필드
    private String topic;
    private DifficultyLevel difficultyLevel;
}
