package com.grm3355.zonie.commonlib.domain.chatroom.dto;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;

public record ChatRoomInfoDto(
	ChatRoom chatRoom,
	String festivalTitle,
	Long participantCount
) {}