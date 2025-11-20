package com.grm3355.zonie.commonlib.domain.festival.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grm3355.zonie.commonlib.domain.festival.entity.FestivalDetailImage;

public interface FestivalDetailImageRepository extends JpaRepository<FestivalDetailImage, Integer> {
	void deleteByContentId(Integer contentId);

	List<FestivalDetailImage> findByContentId(int contentId);
}
