package com.example.speechservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 캐시 및 데이터베이스 연결을 위한 Spring 설정 클래스입니다.
 * RedisTemplate을 구성하여 데이터 직렬화/역직렬화 방식을 정의합니다.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 빈을 설정합니다.
     * 이 템플릿은 Redis와의 상호작용을 간소화하며, 특히 ChatMessage 객체 리스트와 같은 복합 객체를
     * JSON 형식으로 직렬화하여 저장할 수 있도록 구성됩니다.
     *
     * @param connectionFactory Redis 연결 팩토리 (Spring Boot에 의해 자동 구성됨)
     * @return String 키와 Object 값(JSON 직렬화)을 사용하는 RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // Redis 키를 String으로 직렬화 (읽기 쉬운 형식)
        template.setKeySerializer(new StringRedisSerializer());
        // Redis 값을 JSON으로 직렬화 (자바 객체를 JSON 문자열로 변환)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 해시 키를 String으로 직렬화
        template.setHashKeySerializer(new StringRedisSerializer());
        // 해시 값을 JSON으로 직렬화
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
