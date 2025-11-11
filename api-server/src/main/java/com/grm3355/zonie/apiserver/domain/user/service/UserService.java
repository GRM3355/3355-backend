package com.grm3355.zonie.apiserver.domain.user.service;

import com.grm3355.zonie.apiserver.domain.user.dto.EmailUpdateRequest;
import com.grm3355.zonie.apiserver.domain.user.dto.PhoneNumberUpdateRequest;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void updateEmail(String userId, EmailUpdateRequest request) {
        User user = userRepository.getOrThrow(userId);
        user.updateEmail(request.email());
    }

    //todo 휴대전화 컬럼 필요
//    @Transactional
//    public void updatePhoneNumber(String userId, PhoneNumberUpdateRequest request) {
//        User user = userRepository.getOrThrow(userId);
//        user.updatePhoneNumber(request.phoneNumber());
//    }
}
