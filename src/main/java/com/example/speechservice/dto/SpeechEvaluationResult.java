package com.example.speechservice.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 음성 평가 결과를 담는 DTO입니다.
 * AI가 생성한 피드백을 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechEvaluationResult {

    private Long id;
    private String sessionId;
    private String feedback;
    private LocalDateTime createdAt;
}

