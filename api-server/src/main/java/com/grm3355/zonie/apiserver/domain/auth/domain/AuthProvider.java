package com.grm3355.zonie.apiserver.domain.auth.domain;

import com.grm3355.zonie.commonlib.domain.user.entity.User;

public interface AuthProvider {
    String provide(User user);
}
