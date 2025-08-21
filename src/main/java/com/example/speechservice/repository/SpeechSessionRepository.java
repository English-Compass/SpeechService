package com.example.speechservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.speechservice.entity.SpeechSession;

@Repository
public interface SpeechSessionRepository extends JpaRepository<SpeechSession, Long> {
    Optional<SpeechSession> findBySessionId(String sessionId);
    boolean existsBySessionId(String sessionId);

    
    /**
     * 종료되지 않은 활성 세션들을 조회합니다.
     * endTime이 null인 세션들을 반환합니다.
     */
    List<SpeechSession> findByEndTimeIsNull();
}
