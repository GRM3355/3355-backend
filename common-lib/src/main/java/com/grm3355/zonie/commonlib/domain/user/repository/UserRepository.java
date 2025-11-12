package com.grm3355.zonie.commonlib.domain.user.repository;

import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String id);

	Optional<User> findByUserIdAndProviderTypeAndDeletedAtIsNull(String userId, ProviderType providerType);

	Optional<User> findByUserIdAndDeletedAtIsNull(String userId);

    Optional<User> findBySocialIdAndProviderTypeAndDeletedAtIsNull(String socialId, ProviderType providerType);
	Optional<User> findBySocialIdAndDeletedAtIsNull(String socialId);

    default User getOrThrow(String id) {
        return findByUserId(id)
                .orElseThrow(() -> new IllegalArgumentException("등록된 사용자가 없습니다."));
    }
}
