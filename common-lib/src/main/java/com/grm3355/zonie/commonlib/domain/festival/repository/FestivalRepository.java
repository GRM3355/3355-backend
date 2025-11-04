package com.grm3355.zonie.commonlib.domain.festival.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {

	Optional<Festival> findAllByContentIdAndTargetType(int contentId, String targetType);

	Festival findByContentId(int contentId);

	Optional<Festival> findByFestivalId(long festivalId);

}
