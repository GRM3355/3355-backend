package com.grm3355.zonie.apiserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

/**
 * OpenAPI 3.0 (Swagger) 문서 생성을 위한 설정 클래스.
 * API 서버 정보, 보안 스키마(JWT Access Token, Refresh Token) 등을 정의하여
 * API 문서의 정확성과 사용 편의성을 높인다.
 */
@OpenAPIDefinition(
	servers = {
		@Server(url = "https://www.zony.kro.kr", description = "운영 서버"),
		@Server(url = "http://localhost:8080", description = "로컬 서버")
	})
@Configuration
@SecurityScheme(
	name = "Authorization",
	type = SecuritySchemeType.HTTP,
	scheme = "Bearer",
	bearerFormat = "JWT",
	description = "JWT Access Token"
)
@SecurityScheme(
	name = "Refresh-Token",
	type = SecuritySchemeType.APIKEY,
	in = SecuritySchemeIn.HEADER,
	paramName = "Refresh-Token",
	description = "액세스 토큰 재발급을 위한 Refresh Token"
)
public class SwaggerConfig {
	@Bean
	public OpenAPI openApi() {
		// 1. 공통 응답(ApiResponse) 스키마 정의
		Schema<?> apiResponseSchema = new Schema<>()
			.name("ApiResponse")
			.type("object")
			.addProperty("success", new Schema<>().type("boolean"))
			.addProperty("data", new Schema<>().type("object").nullable(true))
			.addProperty("error", new Schema<>()
				.$ref("#/components/schemas/ErrorResponse")
				.nullable(true))
			.addProperty("timestamp", new Schema<>().type("string").format("date-time"));

		// 2. 공통 에러(ErrorResponse) 스키마 정의
		Schema<?> errorResponseSchema = new Schema<>()
			.name("ErrorResponse")
			.type("object")
			.addProperty("code", new Schema<>().type("string"))
			.addProperty("message", new Schema<>().type("string"));

		// 3. Components 객체 생성, 위에서 정의한 스키마들 추가
		Components components = new Components()
			.addSchemas("ApiResponse", apiResponseSchema)
			.addSchemas("ErrorResponse", errorResponseSchema);

		// 4. 공통 에러 응답들 Components에 추가
		addErrorResponse(components, "400", "BadRequest", "입력값 유효성 검증 실패");
		addErrorResponse(components, "401", "Unauthorized", "인증 실패 (토큰 누락 또는 만료)");
		addErrorResponse(components, "403", "Forbidden", "권한 없음");
		addErrorResponse(components, "404", "NotFound", "요청한 리소스 없음");
		addErrorResponse(components, "405", "MethodNotAllowed", "허용되지 않은 메소드");
		addErrorResponse(components, "415", "UnsupportedMediaType", "지원되지 않는 미디어 타입");
		addErrorResponse(components, "429", "TooManyRequests", "요청 횟수 초과");
		addErrorResponse(components, "500", "InternalServerError", "서버 내부 로직 오류");

		// 5. 204 No Content 공통 응답 등록
		components.addResponses("NoContent",
			new io.swagger.v3.oas.models.responses.ApiResponse()
				.description("정상 처리 (콘텐츠 없음)")
				.content(null) // 바디가 없음을 명시
		);

		return new OpenAPI()
			.components(components) // 수정된 components 주입
			.info(apiInfo());
	}

	private Info apiInfo() {
		return new Info()
			.title("Swagger")
			.description("Zonie REST API")
			.version("1.0.1");
	}

	/**
	 * 공통 에러 응답을 Components에 추가하는 헬퍼 메서드
	 *
	 * @param components  수정할 Components 객체
	 * @param httpCode    HTTP 상태 코드 (예: "400")
	 * @param name        컴포넌트 참조 이름 (예: "BadRequest")
	 * @param description 에러 설명
	 */
	private void addErrorResponse(Components components, String httpCode, String name, String description) {
		// 공통 ApiResponse 스키마를 참조하도록 설정
		Schema<?> schema = new Schema<>().$ref("#/components/schemas/ApiResponse");

		// JSON 예시 생성
		String exampleJson = String.format(
			"{\"success\":false,\"data\":null,\"error\":{\"code\":\"%s\",\"message\":\"%s\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}",
			name.toUpperCase(), // ex) BAD_REQUEST
			description
		);
		Content content = new Content()
			.addMediaType("application/json", new MediaType()
				.schema(schema)
				.example(exampleJson));

		// Components에 Response 등록
		components.addResponses(name, new io.swagger.v3.oas.models.responses.ApiResponse()
			.description(description)
			.content(content));
	}
}
