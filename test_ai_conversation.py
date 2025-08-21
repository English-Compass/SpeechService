#!/usr/bin/env python3
"""
AI 대화 테스트 스크립트
실제 음성 파일 없이도 API 테스트가 가능하도록 도와줍니다.
"""

import requests
import json
import time
import base64
import os

# API 기본 URL
BASE_URL = "http://localhost:8082"

def test_health_check():
    """헬스 체크 테스트"""
    try:
        response = requests.get(f"{BASE_URL}/health")
        print(f"✅ 헬스 체크 성공: {response.status_code}")
        print(f"응답: {response.text}")
        return True
    except Exception as e:
        print(f"❌ 헬스 체크 실패: {e}")
        return False

def test_start_session():
    """세션 시작 테스트"""
    try:
        url = f"{BASE_URL}/api/v1/sessions/role-playing"
        data = {
            "kakaoId": 12345,  # 테스트용 카카오 ID
            "topic": "travel",
            "difficultyLevel": "BEGINNER"
        }
        
        response = requests.post(url, json=data)
        print(f"✅ 세션 시작 성공: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"세션 ID: {result.get('sessionId')}")
            print(f"AI 첫 인사: {result.get('aiFirstGreeting')}")
            return result.get('sessionId')
        else:
            print(f"응답: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ 세션 시작 실패: {e}")
        return None

def test_talk_with_text(session_id, user_text):
    """텍스트 기반 대화 테스트 (음성 파일 대신 텍스트 사용)"""
    try:
        url = f"{BASE_URL}/api/v1/sessions/role-playing/{session_id}/talk"
        
        # 실제로는 음성 파일을 보내야 하지만, 테스트를 위해 텍스트를 직접 전송
        # 이는 실제 구현에서는 작동하지 않을 수 있음
        data = {
            "text": user_text  # 실제로는 audio 파일이어야 함
        }
        
        response = requests.post(url, data=data)
        print(f"✅ 대화 요청 성공: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"AI 응답: {result.get('aiText')}")
            print(f"평가 상태: {result.get('evaluationStatus')}")
            return result
        else:
            print(f"응답: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ 대화 요청 실패: {e}")
        return None

def test_get_evaluations(session_id):
    """평가 결과 조회 테스트"""
    try:
        url = f"{BASE_URL}/api/v1/sessions/role-playing/{session_id}/evaluations"
        
        response = requests.get(url)
        print(f"✅ 평가 결과 조회 성공: {response.status_code}")
        
        if response.status_code == 200:
            results = response.json()
            print(f"평가 결과 수: {len(results)}")
            
            for i, result in enumerate(results):
                print(f"\n--- 평가 {i+1} ---")
                print(f"발음 점수: {result.get('pronunciationScore')}")
                print(f"문법 점수: {result.get('grammarScore')}")
                print(f"유창성 점수: {result.get('fluencyScore')}")
                print(f"피드백: {result.get('feedback')}")
                print(f"AI 분석: {result.get('aiAnalysis')}")
            
            return results
        else:
            print(f"응답: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ 평가 결과 조회 실패: {e}")
        return None

def test_end_session(session_id):
    """세션 종료 테스트"""
    try:
        url = f"{BASE_URL}/api/v1/sessions/role-playing/{session_id}/end"
        
        response = requests.post(url)
        print(f"✅ 세션 종료 성공: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"종료 사유: {result.get('endReason')}")
            print(f"총 소요 시간: {result.get('totalDuration')}")
            return result
        else:
            print(f"응답: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ 세션 종료 실패: {e}")
        return None

def main():
    """메인 테스트 실행"""
    print("🚀 AI 대화 테스트 시작")
    print("=" * 50)
    
    # 1. 헬스 체크
    if not test_health_check():
        print("❌ 애플리케이션이 실행되지 않고 있습니다.")
        return
    
    print("\n" + "=" * 50)
    
    # 2. 세션 시작
    session_id = test_start_session()
    if not session_id:
        print("❌ 세션을 시작할 수 없습니다.")
        return
    
    print("\n" + "=" * 50)
    
    # 3. 대화 테스트 (여러 번)
    conversation_topics = [
        "I like to travel to different countries.",
        "What's your favorite place to visit?",
        "I want to learn more about English culture.",
        "Can you help me improve my pronunciation?",
        "Thank you for the conversation today."
    ]
    
    for i, topic in enumerate(conversation_topics):
        print(f"\n--- 대화 {i+1} ---")
        print(f"사용자: {topic}")
        
        result = test_talk_with_text(session_id, topic)
        if result:
            print(f"AI: {result.get('aiText')}")
        
        time.sleep(1)  # 잠시 대기
    
    print("\n" + "=" * 50)
    
    # 4. 평가 결과 조회
    test_get_evaluations(session_id)
    
    print("\n" + "=" * 50)
    
    # 5. 세션 종료
    test_end_session(session_id)
    
    print("\n" + "=" * 50)
    print("🎉 AI 대화 테스트 완료!")

if __name__ == "__main__":
    main()

