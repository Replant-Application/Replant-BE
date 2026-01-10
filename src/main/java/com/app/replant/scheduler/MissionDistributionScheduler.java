package com.app.replant.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 미션 자동 배분 스케줄러 (비활성화됨)
 * - 일간: 매일 오전 7시 (KST), 3개 미션
 * - 주간: 매주 월요일 오전 7시 (KST), 3개 미션
 * - 월간: 매월 1일 오전 7시 (KST), 3개 미션
 *
 * TODO: MissionType enum 재설계 후 재활성화 필요
 * - 현재 MissionType은 OFFICIAL/CUSTOM (미션 출처)
 * - 기간 타입 (DAILY/WEEKLY/MONTHLY)은 별도 enum 필요
 */
// @Component  // 비활성화 - MissionType enum 변경으로 인한 호환성 문제
@RequiredArgsConstructor
@Slf4j
public class MissionDistributionScheduler {

    // 스케줄러 비활성화됨
    // 재활성화 시 MissionType enum 재설계 및 MissionRepository.findActiveByType 메서드 추가 필요
}
