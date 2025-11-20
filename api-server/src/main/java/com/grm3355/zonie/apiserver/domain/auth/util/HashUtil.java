package com.grm3355.zonie.apiserver.domain.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

	/**
	 * 입력 문자열을 SHA-256 해시로 변환
	 *
	 * @param input 평문 문자열
	 * @return SHA-256 해시 값 (16진수)
	 */
	public static String sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

			// 바이트 배열 → 16진수 문자열 변환
			StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 해시 생성 실패", e);
		}
	}
}