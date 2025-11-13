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
	public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String id) throws
		UsernameNotFoundException {
		User user = userRepository.findByUserIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new UsernameNotFoundException("아이디를 찾을 수 없습니다: " + id));

		return UserDetailsImpl.build(user);
	}

	public UserDetailsImpl getUserDetailsByEmail(String email) {
		return (UserDetailsImpl)loadUserByUsername(email);
	}
}
