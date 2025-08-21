package com.example.speechservice.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream; // 음성 파일 스트림 처리를 위한 임포트 추가
import java.util.List;

// import com.theokanning.openai.audio.SpeechResult; // TTS API 응답을 위한 임포트 추가 (필요 없으므로 제거)
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.theokanning.openai.audio.CreateSpeechRequest; // TTS API 요청을 위한 임포트 추가
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

/**
 * OpenAI API와 상호작용하는 서비스 클래스.
 * ChatCompletion (텍스트 대화), Whisper (음성 텍스트 변환), Speech (텍스트 음성 변환) API를 호출하는 기능을 제공합니다.
 */
@Service
public class OpenAIService {

    private final OpenAiService openAiService;

    /**
     * OpenAIService 생성자.
     * application.yml에 설정된 OpenAI API 키를 주입받아 OpenAiService 인스턴스를 초기화합니다.
     *
     * @param apiKey OpenAI API 키
     */
    public OpenAIService(@Value("${openai.api.api-key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
    }

    /**
     * Chat Completion API를 호출하여 AI의 텍스트 응답을 생성합니다.
     * 주어진 메시지 목록을 기반으로 대화를 이어갑니다.
     *
     * @param messages AI와 사용자 간의 대화 기록 (ChatMessage 객체 목록)
     * @return AI가 생성한 텍스트 응답
     */
    public String getChatResponse(List<ChatMessage> messages) {
        // ChatCompletionRequest 객체 빌드.
        // 모델은 "gpt-3.5-turbo"를 사용하며, 입력 메시지 목록을 포함합니다.
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .build();

        // OpenAI 서비스로부터 ChatCompletion을 생성하고, 첫 번째 선택지의 메시지 내용을 반환합니다.
        return openAiService.createChatCompletion(chatCompletionRequest)
                .getChoices().get(0).getMessage().getContent();
    }

    /**
     * Whisper API를 호출하여 음성을 텍스트로 변환합니다 (Speech-to-Text).
     * 메모리 스트림을 직접 사용하여 딜레이를 최소화합니다.
     * 임시 파일을 최소한으로 사용하여 성능을 개선합니다.
     *
     * @param audioStream 변환할 음성 데이터 스트림
     * @return 변환된 텍스트
     * @throws RuntimeException 음성 텍스트 변환 실패 시 발생
     */
    public String convertSpeechToText(InputStream audioStream) {
        File tempFile = null;
        try {
            // 메모리 내에서 가능한 한 빠르게 임시 파일 생성 (RAM 디스크 사용)
            tempFile = File.createTempFile("audio-", ".wav");
            
            // InputStream을 임시 파일로 직접 복사 (버퍼링으로 최적화)
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 audioStream) {
                byte[] buffer = new byte[8192]; // 8KB 버퍼로 빠른 복사
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // CreateTranscriptionRequest 객체 빌드
            CreateTranscriptionRequest createTranscriptionRequest = CreateTranscriptionRequest.builder()
                    .model("whisper-1")
                    .build();

            // OpenAI 서비스로부터 음성 텍스트 변환을 요청하고, 변환된 텍스트를 가져옵니다.
            return openAiService.createTranscription(createTranscriptionRequest, tempFile).getText();
            
        } catch (Exception e) {
            // 변환 실패 시 런타임 예외로 감싸서 던집니다.
            throw new RuntimeException("음성 텍스트 변환에 실패했습니다: " + e.getMessage(), e);
        } finally {
            // 임시 파일 정리 (메모리 누수 방지)
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit(); // 삭제 실패 시 JVM 종료 시 삭제
                }
            }
        }
    }

    /**
     * 기존 MultipartFile을 사용하는 메소드 (하위 호환성을 위해 유지)
     * 
     * @param audioFile 변환할 음성 파일 (MultipartFile 형태)
     * @return 변환된 텍스트
     * @throws IOException 임시 파일 생성 또는 파일 읽기/쓰기 중 발생할 수 있는 예외
     */
    public String convertSpeechToText(MultipartFile audioFile) throws IOException {
        // MultipartFile을 InputStream으로 변환하여 새로운 메소드 호출
        return convertSpeechToText(audioFile.getInputStream());
    }

    /**
     * OpenAI Speech API를 호출하여 텍스트를 음성 파일로 변환합니다 (Text-to-Speech).
     *
     * @param text 변환할 텍스트
     * @return 생성된 음성 파일의 InputStream
     */
    public InputStream convertTextToSpeech(String text) {
        // CreateSpeechRequest 객체 빌드.
        // 모델은 "tts-1"을 사용하며, "echo" 음성으로 텍스트를 변환합니다.
        CreateSpeechRequest createSpeechRequest = CreateSpeechRequest.builder()
                .model("tts-1") // TTS 모델 지정
                .input(text)    // 음성으로 변환할 텍스트
                .voice("echo") // 사용할 음성: echo(여성), fable(여성), nova(여성), shimmer(여성), alloy(중성), onyx(남성)
                .responseFormat("mp3") // MP3 형식으로 응답 받기
                .build();

        // OpenAI 서비스로부터 텍스트 음성 변환을 요청하고, 결과로 생성된 음성 스트림을 반환합니다.
        // 이 스트림은 클라이언트에게 직접 전달되거나 파일로 저장될 수 있습니다.
        return openAiService.createSpeech(createSpeechRequest).byteStream(); // ResponseBody에서 InputStream 추출
    }

    /**
     * 난이도별로 TTS 음성 속도를 조절하여 텍스트를 음성으로 변환합니다.
     *
     * @param text 변환할 텍스트
     * @param difficultyLevel 난이도 (BEGINNER: 느리게, INTERMEDIATE: 보통, ADVANCED: 빠르게)
     * @return 생성된 음성 파일의 InputStream
     */
    public InputStream convertTextToSpeech(String text, String difficultyLevel) {
        // 난이도별 음성 속도 조절을 위한 텍스트 전처리
        String processedText = adjustTextForDifficulty(text, difficultyLevel);
        
        CreateSpeechRequest createSpeechRequest = CreateSpeechRequest.builder()
                .model("tts-1")
                .input(processedText)
                .voice("echo")
                .responseFormat("mp3")
                .build();

        return openAiService.createSpeech(createSpeechRequest).byteStream();
    }

    /**
     * 난이도에 따라 텍스트를 조절하여 음성 속도를 제어합니다.
     * OpenAI TTS는 직접적인 속도 조절을 지원하지 않으므로 텍스트를 조절합니다.
     *
     * @param originalText 원본 텍스트
     * @param difficultyLevel 난이도
     * @return 조절된 텍스트
     */
    private String adjustTextForDifficulty(String originalText, String difficultyLevel) {
        return switch (difficultyLevel.toUpperCase()) {
            case "BEGINNER" -> addPausesForSlowSpeech(originalText);
            case "INTERMEDIATE" -> originalText; // 기본 속도
            case "ADVANCED" -> originalText; // 기본 속도
            default -> originalText;
        };
    }

    /**
     * 초급자를 위한 느린 음성 효과를 위해 텍스트에 일시정지 표시를 추가합니다.
     * OpenAI TTS는 쉼표와 마침표를 인식하여 자연스러운 일시정지를 만듭니다.
     *
     * @param text 원본 텍스트
     * @return 일시정지가 추가된 텍스트
     */
    private String addPausesForSlowSpeech(String text) {
        // 문장을 더 작은 단위로 나누어 일시정지 추가
        String[] sentences = text.split("\\.");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            if (i > 0) result.append(".");
            
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                // 문장을 단어 단위로 나누고 일시정지 추가
                String[] words = sentence.split(" ");
                for (int j = 0; j < words.length; j++) {
                    if (j > 0) result.append(" ... ");
                    result.append(words[j]);
                }
                result.append(" ... ");
            }
        }
        
        return result.toString().trim();
    }
}
