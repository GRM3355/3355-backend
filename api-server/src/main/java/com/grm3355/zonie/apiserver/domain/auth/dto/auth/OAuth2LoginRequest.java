package com.grm3355.zonie.apiserver.domain.auth.dto.auth;

import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuth2LoginRequest(
        @NotNull ProviderType providerType,
        @NotBlank String code) {
}
