package com.grm3355.zonie.apiserver.domain.batch.service;

import org.springframework.batch.core.SkipListener;

import com.grm3355.zonie.commonlib.domain.batch.dto.ApiFestivalDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomSkipListener implements SkipListener<ApiFestivalDto, Festival> {

	@Override
	public void onSkipInRead(Throwable t) {
		log.warn("Reading json 중 스킵 발생. Error: {}", t.getMessage());
	}

	@Override
	public void onSkipInProcess(ApiFestivalDto item, Throwable t) {
		log.warn("Processing json 중 스킵 발생. Item: {}, Error: {}", item.toString(), t.getMessage());
	}

	// @Override
	// public void onSkipInWrite(PlaceDocument item, Throwable t) {
	// 	log.warn("Writing to ES 중 스킵 발생. Item ID: {}, Error: {}", item.getPlaceId(), t.getMessage(), t);
	// }
}
