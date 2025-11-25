package com.grm3355.zonie.apiserver.global.jwt;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String userId) throws
		UsernameNotFoundException {
		try {
			User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
				.orElseThrow(() -> new UsernameNotFoundException("아이디를 찾을 수 없습니다: " + userId));
			return UserDetailsImpl.build(user);
		} catch (UsernameNotFoundException e) {
			// 사용자 없음
			throw e;

		} catch (Exception e) {
			// DB 연결 오류 등 기타 예외
			throw new UsernameNotFoundException("사용자 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	public UserDetailsImpl getUserDetailsByEmail(String email) {
		return (UserDetailsImpl)loadUserByUsername(email);
	}
}
