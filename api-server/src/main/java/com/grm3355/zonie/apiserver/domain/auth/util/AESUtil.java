package com.grm3355.zonie.apiserver.domain.auth.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AESUtil {

	private static final String ALGORITHM = "AES";
	@Value("${aes.key}")
	private String KEY;

	//AES (대칭키, 양방향)
	public String encrypt(String value) throws Exception {
		SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		byte[] encrypted = cipher.doFinal(value.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	public String decrypt(String encrypted) throws Exception {
		SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		byte[] decoded = Base64.getDecoder().decode(encrypted);
		return new String(cipher.doFinal(decoded));
	}
}