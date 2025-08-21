package com.example.speechservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI의 텍스트 응답 및 음성 데이터를 Base64 인코딩된 문자열로 포함하는 응답 DTO 클래스.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TalkResponse {
    private String text; // AI의 텍스트 응답 (aiText에서 변경)
    private String audio; // AI 응답에 해당하는 음성 데이터 (audioDataBase64에서 변경)
    private String userText; // 사용자 음성 텍스트 (STT 결과)
    private String evaluationStatus; // 음성 평가 상태 ("evaluating", "completed", "failed")
}
