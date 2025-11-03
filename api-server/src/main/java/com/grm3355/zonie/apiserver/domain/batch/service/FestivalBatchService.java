package com.grm3355.zonie.apiserver.domain.batch.service;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;

public interface FestivalBatchService {
	void runBatch();

	void getJsonDataOpenApi(int page);

	void getJsonDataFile(String fileName);

	int setFestivalSave(JsonNode items);

	String getRegionCode(String areaCode);

	LocalDate parseDate(String date);

}
