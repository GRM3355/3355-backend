package com.grm3355.zonie.commonlib.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.grm3355.zonie.commonlib.global.entity.BaseTimeEntity;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import com.grm3355.zonie.commonlib.global.enums.Role;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 100)
	@Column(name = "user_id", length = 100)
	@NotBlank(message = "사용자아이디는 정보는 필수 입력 값입니다.")
	private String userId;

	@NotBlank
	@Size(max = 100)
	@Column(name = "password", length = 100)
	@NotBlank(message = "비밀번호는 정보는 필수 입력 값입니다.")
	private String password;

	// GUEST, USER, ADMIN - 카카오회원은 USER
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private Role role;

	// 닉네임
	@Column(name = "profile_nickname", length = 50)
	private String profileNickName;

	// 이메일
	@Column(name = "account_email")
	private String accountEmail;

	// 프로필 이미지
	@Column(name = "profile_image", length = 100)
	private String profileImage;

	// 로그인 제공자 (KAKAO, GOOGLE, NAVER, APPLE 등)
	@Enumerated(EnumType.STRING)
	private ProviderType provider;

	// SNS가 제공하는 고유 ID (sub, id 등)
	private String socialId;
}
