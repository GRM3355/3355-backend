package com.grm3355.zonie.apiserver.domain.auth.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.grm3355.zonie.apiserver.global.exception.BadRequestException;
import com.grm3355.zonie.apiserver.global.exception.InternalServerException;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

public class OAuth2Clients {
	private final Map<ProviderType, Supplier<OAuth2Client>> oAuth2ClientMap;

	private OAuth2Clients(Map<ProviderType, Supplier<OAuth2Client>> oAuth2ClientMap) {
		this.oAuth2ClientMap = oAuth2ClientMap;
	}

	public static OAuth2ClientsBuilder builder() {
		return new OAuth2ClientsBuilder();
	}

	public OAuth2Client getClient(ProviderType socialType) {
		return oAuth2ClientMap.getOrDefault(socialType, () -> {
			throw new BadRequestException(ErrorCode.OAUTH2_NOT_SUPPORTED_PROVIDER_TYPE);
		}).get();
	}

	public static class OAuth2ClientsBuilder {

		private final Map<ProviderType, Supplier<OAuth2Client>> oAuth2ClientMap = new EnumMap<>(ProviderType.class);

		private OAuth2ClientsBuilder() {
		}

		public OAuth2ClientsBuilder addAll(List<OAuth2Client> oAuth2Clients) {
			for (OAuth2Client oAuth2Client : oAuth2Clients) {
				add(oAuth2Client);
			}
			return this;
		}

		public OAuth2ClientsBuilder add(OAuth2Client oAuth2Client) {
			ProviderType providerType = oAuth2Client.getProviderType();
			if (oAuth2ClientMap.containsKey(providerType)) {
				throw new InternalServerException(ErrorCode.DUPLICATE_SOCIAL_TYPE);
			}
			oAuth2ClientMap.put(providerType, () -> oAuth2Client);
			return this;
		}

		public OAuth2Clients build() {
			return new OAuth2Clients(oAuth2ClientMap);
		}
	}
}
