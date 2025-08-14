package com.example.speechservice.repository;

import com.example.speechservice.entity.SpeechSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpeechSessionRepository extends JpaRepository<SpeechSession, Long> {
    Optional<SpeechSession> findBySessionId(String sessionId);
    boolean existsBySessionId(String sessionId);
    List<SpeechSession> findByUserId(Long userId);
    List<SpeechSession> findByUserIdAndDifficultyLevel(Long userId, SpeechSession.DifficultyLevel difficultyLevel);
}
