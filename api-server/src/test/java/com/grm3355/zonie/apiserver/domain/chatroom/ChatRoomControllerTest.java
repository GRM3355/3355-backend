package com.grm3355.zonie.apiserver.domain.chatroom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("채팅방 생성 통합테스트")
@SpringBootTest
@AutoConfigureMockMvc
class ChatRoomControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ChatRoomService chatRoomService;

	@Test
	@DisplayName("채팅방 생성 성공")
	@WithMockUser(roles = "GUEST")
	void createChatRoomTest() throws Exception {
		ChatRoomRequest request = ChatRoomRequest.builder().title("테스트 채팅방").build();

		ChatRoomResponse response = ChatRoomResponse.builder()
			.chatRoomId(String.valueOf(1L))
			.title(request.getTitle())
			.build();

		when(chatRoomService.setCreateChatRoom(anyLong(), any(ChatRoomRequest.class), any()))
			.thenReturn(response);

		mockMvc.perform(post("/api/v1/festivals/1/chat-rooms")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.chatRoomId").value(1L))
			.andExpect(jsonPath("$.data.title").value("테스트 채팅방"));
	}

	@Test
	@DisplayName("축제별 채팅방 목록 조회")
	void getChatRoomListByFestivalTest() throws Exception {
		ChatRoomResponse chatRoom = ChatRoomResponse.builder()
			.chatRoomId(String.valueOf(1L))
			.title("테스트 채팅방")
			.build();

		when(chatRoomService.getFestivalChatRoomList(anyLong(), any()))
			.thenReturn(new PageImpl<>(List.of(chatRoom), PageRequest.of(0, 10), 1));

		mockMvc.perform(get("/api/v1/festivals/1/chat-rooms")
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].chatRoomId").value(1L))
			.andExpect(jsonPath("$.data.content[0].title").value("테스트 채팅방"));
	}

	@Test
	@DisplayName("내 채팅방 목록 조회")
	@WithMockUser(roles = "GUEST", username = "testuser")
	void getMyChatRoomListTest() throws Exception {

		MyChatRoomResponse chatRoomDto = MyChatRoomResponse.builder()
			.chatRoomId(String.valueOf(1L))
			.title("내 채팅방")
			.participantCount(5L)
			.festivalTitle("테스트 축제")
			.build();

		// Page 객체로 반환값 세팅
		Page<MyChatRoomResponse> page = new PageImpl<>(
			List.of(chatRoomDto),
			PageRequest.of(0, 10),
			1L
		);

		// 서비스 메서드 모킹
		when(chatRoomService.getMyroomChatRoomList(any(), any())).thenReturn(page);

		// 요청
		mockMvc.perform(get("/api/v1/chat-rooms/my-rooms")
				.param("page", "0")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].chatRoomId").value(1L))
			.andExpect(jsonPath("$.data.content[0].title").value("내 채팅방"))
			.andExpect(jsonPath("$.data.content[0].participantCount").value(5))
			.andExpect(jsonPath("$.data.content[0].festivalTitle").value("테스트 축제"));
	}
}
