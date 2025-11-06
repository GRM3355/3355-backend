package com.grm3355.zonie.commonlib.domain.festival.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {

	Optional<Festival> findAllByContentIdAndTargetType(int contentId, String targetType);

	Festival findByContentId(int contentId);

	Optional<Festival> findByFestivalId(long festivalId);

	//채팅방 생성전에 유효한지체크(축제가 있는지, 해당날짜가 있는지)
	@Query(
		value = """
        SELECT f
        FROM Festival f
        WHERE f.festivalId = :festivalId
          AND CURRENT_TIMESTAMP Between f.eventStartDate And f.eventEndDate
      """)
	Optional<Festival> findByIsValidFestival(long festivalId);

	// 이벤트 종료일이 현재 날짜보다 이전인 축제를 삭제
	void deleteByEventEndDateBefore(LocalDate date);

}
