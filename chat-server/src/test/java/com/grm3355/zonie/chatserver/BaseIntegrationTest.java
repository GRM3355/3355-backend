package com.grm3355.zonie.chatserver;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.sql.SQLException;

@SpringBootTest
@ActiveProfiles("test") // application-test.yml (빈 파일)을 로드
@Testcontainers         // 이 클래스가 Testcontainers를 사용함을 알림
public abstract class BaseIntegrationTest {

	// === 1. PostgreSQL (PostGIS) 컨테이너 ===
	@Container
	static GenericContainer<?> postgresContainer =
		new GenericContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine"))
			.withExposedPorts(5432)
			.withEnv("POSTGRES_DB", "testdb")
			.withEnv("POSTGRES_USER", "testuser")
			.withEnv("POSTGRES_PASSWORD", "testpass")
			.withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/amd64")); // M1/M2 호환

	// === 2. Redis 컨테이너 ===
	@Container
	static GenericContainer<?> redisContainer =
		new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	// === 3. MongoDB 컨테이너 ===
	@Container
	static MongoDBContainer mongoContainer =
		new MongoDBContainer(DockerImageName.parse("mongo:7.0"));


	// === 4. PostGIS 확장 설치 (테스트 시작 전 1회) ===
	@BeforeAll
	static void setupPostGIS() throws SQLException {
		String jdbcUrl = String.format(
			"jdbc:postgresql://%s:%d/testdb",
			postgresContainer.getHost(),
			postgresContainer.getMappedPort(5432)
		);
		try (var connection = DriverManager.getConnection(jdbcUrl, "testuser", "testpass")) {
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE EXTENSION IF NOT EXISTS postgis;");
			}
		}
	}

	// === 5. Spring Boot에 컨테이너 정보 동적 주입 ===
	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {

		// PostgreSQL
		registry.add("spring.datasource.url",
			() -> String.format(
				"jdbc:postgresql://%s:%d/testdb",
				postgresContainer.getHost(),
				postgresContainer.getMappedPort(5432)
			)
		);
		registry.add("spring.datasource.username", () -> "testuser");
		registry.add("spring.datasource.password", () -> "testpass");

		// Redis
		registry.add("spring.data.redis.host", redisContainer::getHost);
		registry.add("spring.data.redis.port", () -> redisContainer.getFirstMappedPort());

		// MongoDB
		registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);

		// JWT
		String jwtSecret = System.getenv("JWT_SECRET_KEY");
		registry.add("jwt.secret", () -> jwtSecret);

	}
}
