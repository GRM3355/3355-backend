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
	private Long uuid;

	@NotBlank
	@Size(max = 100)
	@Column(name = "user_id", nullable = false, unique = true, length = 100)
	@NotBlank(message = "사용자아이디는 정보는 필수 입력 값입니다.")
	private String userId;

	@NotBlank
	@Size(max = 100)
	@Column(name = "password", nullable = false, length = 100)
	@NotBlank(message = "비밀번호는 정보는 필수 입력 값입니다.")
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private Role role;

}

