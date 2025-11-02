package com.grm3355.zonie.commonlib.domain.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.batch.entity.BatchJobStatus;

@Repository
public interface BatchJobStatusRepository extends JpaRepository<BatchJobStatus, Long> {
}
