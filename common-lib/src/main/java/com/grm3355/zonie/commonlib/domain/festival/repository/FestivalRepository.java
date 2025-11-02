package com.grm3355.zonie.commonlib.domain.festival.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {

}
