package com.grm3355.zonie.apiserver.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String nickname
) {
}
