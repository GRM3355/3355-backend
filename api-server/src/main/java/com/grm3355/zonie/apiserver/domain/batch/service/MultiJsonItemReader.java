package com.grm3355.zonie.apiserver.domain.batch.service;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ItemReader;

public class MultiJsonItemReader<T> implements ItemReader<T> {

	private final Iterator<ItemReader<T>> readerIterator;
	private ItemReader<T> currentReader;

	public MultiJsonItemReader(List<ItemReader<T>> readers) {
		this.readerIterator = readers.iterator();
		this.currentReader = readerIterator.hasNext() ? readerIterator.next() : null;
	}

	@Override
	public T read() throws Exception {
		if (currentReader == null)
			return null;

		T item = currentReader.read();
		if (item == null) {
			if (readerIterator.hasNext()) {
				currentReader = readerIterator.next();
				return read(); // 다음 Reader로 이동
			} else {
				return null;
			}
		}
		return item;
	}
}
