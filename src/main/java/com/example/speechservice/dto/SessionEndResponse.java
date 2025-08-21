package com.example.speechservice.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 세션 종료 응답을 위한 DTO 클래스.
 * 세션 종료 시 세션 정보와 총 소요 시간 등을 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEndResponse {
    private String sessionId;           // 세션 ID
    private String topic;               // 대화 주제
    private String difficultyLevel;     // 난이도 레벨
    private LocalDateTime startTime;    // 시작 시간
    private LocalDateTime endTime;      // 종료 시간
    private Duration totalDuration;     // 총 소요 시간
    private String endReason;           // 종료 사유 (MANUAL, CONVERSATION_LIMIT, TIME_LIMIT)
    private String message;             // 종료 메시지
}

