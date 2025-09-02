package com.example.speechservice.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.speechservice.dto.RolePlayingScenario;

/**
 * 롤 플레잉 시나리오를 관리하는 서비스 클래스입니다.
 */
@Service
public class RolePlayingScenarioService {
    
    /**
     * 미리 정의된 롤 플레잉 시나리오 목록을 반환합니다.
     */
    public List<RolePlayingScenario> getPredefinedScenarios() {
        return Arrays.asList(
            new RolePlayingScenario(
                "cafe",
                "카페",
                "바리스타",
                "고객",
                "당신은 카페에서 음료를 주문하려고 합니다.",
                "카페에서 음료 주문하기"
            ),
            new RolePlayingScenario(
                "restaurant",
                "식당",
                "웨이터",
                "손님",
                "당신은 식당에서 주문한 음식과 다른 음식을 받았습니다.",
                "식당에서 음식 문제 해결하기"
            ),
            new RolePlayingScenario(
                "hotel",
                "호텔",
                "프론트 데스크",
                "투숙객",
                "당신은 호텔 체크인을 하려고 합니다.",
                "호텔 체크인하기"
            ),
            new RolePlayingScenario(
                "shop",
                "상점",
                "점원",
                "고객",
                "당신은 상점에서 특정 상품을 찾고 있습니다.",
                "상점에서 상품 찾기"
            ),
            new RolePlayingScenario(
                "doctor",
                "의사",
                "의사",
                "환자",
                "당신은 의사에게 증상을 설명하려고 합니다.",
                "의사에게 증상 설명하기"
            ),
            new RolePlayingScenario(
                "airport",
                "공항",
                "항공사 직원",
                "승객",
                "당신은 항공편 변경을 요청하려고 합니다.",
                "공항에서 항공편 변경하기"
            ),
            new RolePlayingScenario(
                "bank",
                "은행",
                "은행원",
                "고객",
                "당신은 계좌 개설을 신청하려고 합니다.",
                "은행에서 계좌 개설하기"
            )
        );
    }
    
    /**
     * ID로 특정 시나리오를 찾습니다.
     */
    public RolePlayingScenario findScenarioById(String id) {
        return getPredefinedScenarios().stream()
            .filter(scenario -> scenario.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 사용자 정의 시나리오를 생성합니다.
     */
    public RolePlayingScenario createCustomScenario(String aiRole, String userRole, String situation) {
        return new RolePlayingScenario(
            "custom",
            "사용자 정의",
            aiRole,
            userRole,
            situation,
            "사용자가 정의한 롤 플레잉 시나리오"
        );
    }

    /**
     * 롤 플레잉 시나리오에 동적으로 상황을 생성합니다.
     * 같은 역할이라도 매번 다른 상황으로 대화가 진행됩니다.
     */
    public String generateDynamicSituation(String aiRole, String userRole, String baseSituation) {
        if ("cafe".equals(aiRole) || "바리스타".equals(aiRole)) {
            String[] cafeSituations = {
                "카페에서 음료를 주문하려고 합니다",
                "카페에서 친구와 만나기로 했습니다",
                "카페에서 공부하려고 합니다",
                "카페에서 음료를 반품하려고 합니다",
                "카페에서 좌석을 예약하려고 합니다",
                "카페에서 메뉴를 추천받고 싶습니다",
                "카페에서 결제 문제가 발생했습니다",
                "카페에서 와이파이 비밀번호를 물어봅니다"
            };
            return cafeSituations[(int)(Math.random() * cafeSituations.length)];
        } else if ("restaurant".equals(aiRole) || "웨이터".equals(aiRole)) {
            String[] restaurantSituations = {
                "식당에서 음식을 주문하려고 합니다",
                "식당에서 예약을 취소하려고 합니다",
                "식당에서 음식이 맛없다고 불평합니다",
                "식당에서 추가 주문을 하려고 합니다",
                "식당에서 계산서를 요청합니다",
                "식당에서 테이블을 옮기고 싶습니다",
                "식당에서 음식 알레르기가 있다고 말합니다",
                "식당에서 생일 축하를 요청합니다"
            };
            return restaurantSituations[(int)(Math.random() * restaurantSituations.length)];
        } else if ("hotel".equals(aiRole) || "호텔 직원".equals(aiRole)) {
            String[] hotelSituations = {
                "호텔에 체크인하려고 합니다",
                "호텔에서 체크아웃하려고 합니다",
                "호텔에서 추가 수건을 요청합니다",
                "호텔에서 룸서비스를 주문합니다",
                "호텔에서 객실을 변경하고 싶습니다",
                "호텔에서 투어를 예약하려고 합니다",
                "호텔에서 수영장 이용 시간을 문의합니다",
                "호텔에서 주차 문제를 해결하려고 합니다"
            };
            return hotelSituations[(int)(Math.random() * hotelSituations.length)];
        }
        
        // 기본 상황에 약간의 변화 추가
        return baseSituation + " (상황이 약간 다를 수 있습니다)";
    }
}
