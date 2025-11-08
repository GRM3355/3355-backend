package com.grm3355.zonie.commonlib.domain.chatroom.dto;

public record ChatRoomInfoDto(
	String chatRoomId,          // SELECT c.chat_room_id
	Long festivalId,            //
	String title,               // SELECT c.title // 채팅방 이름
	Long participantCount,      // SELECT c.participant_count
	Long lastMessageAt,// SELECT c.last_message_at
	String festivalTitle        // SELECT f.title AS festival_title // 축제 이름
) {}