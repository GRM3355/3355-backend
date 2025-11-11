package com.grm3355.zonie.apiserver.domain.user.controller;

import com.grm3355.zonie.apiserver.domain.user.dto.EmailUpdateRequest;
import com.grm3355.zonie.apiserver.domain.user.dto.PhoneNumberUpdateRequest;
import com.grm3355.zonie.apiserver.domain.user.service.UserService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/update/email")
    public void updateEmail(@AuthenticationPrincipal UserDetailsImpl userDetails,
                            @RequestBody EmailUpdateRequest request) {
        userService.updateEmail(userDetails.getId(), request);
    }

    //todo 휴대전화 컬럼필요
//    @PatchMapping("/update/phoneNumber")
//    public void updateEmail(@AuthenticationPrincipal UserDetailsImpl userDetails,
//                            @RequestBody PhoneNumberUpdateRequest request) {
//        userService.updatePhoneNumber(userDetails.getId(), request);
//    }
}
