package com.example.speechservice.dto;

import com.example.speechservice.entity.SpeechSession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 새로운 롤플레잉 세션 시작을 위한 요청 DTO 클래스.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStartRequest {
    private Long userId;
    private String topic;
    private SpeechSession.DifficultyLevel difficultyLevel;
}
