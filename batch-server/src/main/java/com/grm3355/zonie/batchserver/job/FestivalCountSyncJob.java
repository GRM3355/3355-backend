package com.grm3355.zonie.batchserver.job;

import jakarta.persistence.EntityManager;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalCountSyncJob {

	private final EntityManager entityManager;
	private final FestivalRepository festivalRepository;

	/**
	 * 10분마다 실행
	 * Redis -> DB로 동기화된 채팅방(chat_rooms)의 참여자 수를
	 * 축제(festivals) 단위로 합산하여 total_participant_count 필드에 업데이트합니다.
	 */
	@Deprecated
	@Transactional
	// @Scheduled(cron = "0 */10 * * * *") // 매 10분마다 (0초, 10분 0초, 20분 0초...)
	public void syncFestivalParticipantCountsEM() {
		log.info("FestivalCountSyncJob 시작: 축제별 총 참여자 수 집계를 시작합니다.");
		try {
			// EntityManager를 사용하여 쿼리를 직접 실행
			String nativeQuery = """
				UPDATE festivals f
				SET total_participant_count = COALESCE(sub.total_count, 0)
				FROM (
				    SELECT
				        cr.festival_id,
				        COUNT(DISTINCT cru.user_id) AS total_count
				    FROM chat_rooms cr
				    JOIN chat_room_user cru ON cr.chat_room_id = cru.chat_room_id
				    WHERE cr.festival_id IS NOT NULL
				    GROUP BY cr.festival_id
				) AS sub
				WHERE f.festival_id = sub.festival_id
				""";

			int updateCount = entityManager.createNativeQuery(nativeQuery).executeUpdate(); // 쿼리 실행
			// EntityManager를 사용했으므로 flush가 필요할 수 있지만, @Transactional이 commit 시 자동 처리할 가능성이 높습니다.

			log.info("FestivalCountSyncJob 완료: festivals.total_participant_count 업데이트 성공, {}건 처리", updateCount);
		} catch (Exception e) {
			log.error("축제별 총 참여자 수 집계 중 오류 발생", e);
		}
	}

	@Scheduled(cron = "0 */10 * * * *") // 매 10분마다 (0초, 10분 0초, 20분 0초...)
	@Transactional
	public void syncFestivalParticipantCounts() {
		log.info("FestivalCountSyncJob 시작: 축제별 총 참여자 수 집계를 시작합니다.");
		try {
			int updateCount = festivalRepository.syncTotalParticipantCounts();
			log.info("FestivalCountSyncJob 완료: festivals.total_participant_count 업데이트 성공, {}건 처리", updateCount);
		} catch (Exception e) {
			log.error("축제별 총 참여자 수 집계 중 오류 발생", e);
		}
	}
}
