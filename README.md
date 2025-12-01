# 실시간 위치 기반 축제 채팅 플랫폼 `Zony` (Backend)

> 공간이 대화를 만들고, 정보는 오피셜보다 더 빠른 실시간 위치 기반 페스티벌 채팅 서비스

Zony는 지역 축제 참여자들이 현장(Zone)에서만 활성화되는 **위치 기반 채팅방**을 통해 익명으로 소통하고 실시간 정보를 교류할 수 있는 Full-Stack 웹앱 서비스의 백엔드 서버입니다. 본 레포지토리는 확장성과 가용성을 고려하여 MSA 기반의 멀티모듈 모노레포 구조로 설계되었으며, Spring Boot 3.5, WebSocket, PostGIS 등 최신 기술을 활용하여 안정적인 실시간 소통 환경을 제공합니다.

|  | | 
|---|---|
| 프론트엔드 레포지토리 | [Go To Frontend Repository](https://github.com/GRM3355/3355-frontend) |
| 시연 영상 | [Go To Zony Youtube](https://www.youtube.com/shorts/5aF3qbJC3hQ) |

</br>
<img width="1920" height="1080" alt="04" src="https://github.com/user-attachments/assets/b8a71a7b-6c7a-4c9f-8056-f59f827f238d" />
<img width="1920" height="1080" alt="15" src="https://github.com/user-attachments/assets/de536991-3cdd-423f-96e1-b3f83cb986b4" />
<img width="1920" height="1080" alt="17" src="https://github.com/user-attachments/assets/ffcb49c6-dc44-4d97-a2e7-e9156ff337bf" />
<img width="1920" height="1080" alt="18" src="https://github.com/user-attachments/assets/5f4419ee-bc93-487b-abc9-f9ebd7326581" />
<img width="1920" height="1080" alt="19" src="https://github.com/user-attachments/assets/a83d3c20-b2ce-41a8-95aa-cc56b71eac08" />

</br>

## 주요 기능/기술 목표

| 카테고리 | 핵심 기능 | 구현 기술 및 목표 |
|---|---|---|
| 위치/Zone | GPS 기반 위치 인증 | 축제 중심 반경 2km(서울 1km) Zone 내 사용자만 메시지 작성 가능 (정보 신뢰성 확보) |
| 실시간 채팅 | 현장 채팅 및 정보 공유 | WebSocket (STOMP) 및 Redis Pub/Sub을 활용한 실시간 채팅 중계 (Scale-Out 구조 확보) |
| 커뮤니티 | 익명성 및 사용자 주도 | 자동 익명 닉네임 부여(#3355부터), 사용자가 주제 별 채팅방 직접 생성 |
| 성능 최적화 | 고성능 데이터 처리 | PostGIS를 활용한 위치 기반 검색, Redis ZSET을 이용한 채팅방 실시간 정렬 |
| DB & 영속성 | 다중 DB 전략 | PostgreSQL (메타 데이터), MongoDB (채팅 메시지 영속성 저장) |
| 인프라/배포 | CI/CD 및 MSA | GitHub Actions 기반 빌드/테스트 자동화, AWS ECS/Fargate 배포 및 운영 |

</br>

## 기술 스택 (Technical Stack)

| 구분 | 기술 스택 | 설명 |
|---|---|---|
| Core | Java 21, Spring Boot 3.5.x, Spring WebFlux/Tomcat, Spring JPA, Spring Security, Spring Batch, SpringDoc (Swagger) | 고성능/안정성 확보 |
| Database | PostgreSQL (+PostGIS), MongoDB, Redis (Caching, Pub/Sub) | 위치 기반 검색, 대용량 채팅 메시지, 실시간 데이터 처리를 위한 분리 전략 |
| DevOps & Infra | Docker, Docker Compose, Github Actions (CI/CD), AWS (ALB, ECS, ECR, RDS, ElastiCache, S3) | 컨테이너 기반 환경 구축 및 자동화된 배포 시스템 | 

</br>

<img width="1920" height="1080" alt="24" src="https://github.com/user-attachments/assets/5be1d42c-251e-4e88-af48-66207ec5073c" />

</br></br>

## 아키텍처 및 시스템 개요

> Zony 백엔드는 확장성(Scalability)과 유지보수성(Maintainability)을 확보하기 위해 MSA(마이크로서비스 아키텍처) 기반의 멀티모듈 모노레포 구조를 채택했습니다. 시스템은 책임과 특성이 분리된 3개의 독립적인 서버로 구성됩니다.

</br>

### 주요 컴포넌트 구성 (Multi-Module)

| 컴포넌트 | 역할 | 주요 특징 |
|---|---|---|
| API Server | 사용자 인증(Kakao), 회원/축제/채팅방 정보 조회/관리 등 대부분의 동기식 RESTful API 담당. | Spring Security/JWT 기반 인증/인가 처리, 시스템의 관문 역할 수행. |
| Chat Server | WebSocket 기반 실시간 채팅 기능을 전담. | STOMP 프로토콜 사용, Redis Pub/Sub을 통한 메시지 브로드캐스팅 및 수평 확장. |
| Batch Server |  대용량 데이터 처리, 주기적인 동기화 작업 담당. | Spring Batch와 Spring Scheduler 하이브리드 구성, 공공데이터 API 동기화 및 캐시 정리와 집계. |

</br>

### 주요 API 명세

| Resource | Method | URI (엔드포인트) | 설명 | 인증/위치 |
|---|---|---|---|---|
| Auth | POST | `/api/auth/refresh` | 리프레시 토큰 재발급 |  X  |
| Festivals | GET | `/api/v1/festivals` |  축제 목록 조회 | X | 
| ChatRooms | POST | `/api/v1/festivals/{fId}/chat-rooms` |  채팅방 생성 | O |
| Locations | POST | `/api/v1/locations/verification/...` |  축제 Zone 위치 인증 및 토큰 발급 | O |
| Messages |  POST | `/api/v1/messages/{msgId}/like` | 메시지 '좋아요' 토글 | O |
| WebSocket | SUBSCRIBE | `/sub/chat-rooms/{roomId}` | 채팅 메시지 구독 | O (Token Header) |
| WebSocket | SEND | `/app/chat-rooms/{roomId}/send` | 채팅 메시지 전송 |  O (Zone 인증 필수) |

</br>

<img width="1920" height="1080" alt="25" src="https://github.com/user-attachments/assets/93092d05-bc0e-46b8-b5cd-3e7646e994bb" />

</br>

### 프로젝트 구조

```
src
 ├─ main/java/com/zony
 │   ├─ api-server/      # Auth, Festival, User 등 RESTful API
 │   ├─ chat-server/     # WebSocket 기반 실시간 채팅 전담
 │   ├─ batch-server/    # 공공데이터 동기화, 캐시 정리 등 비동기 작업
 │   └─ common-lib/      # 공통 엔티티, DTO, 유틸리티
 │
 ├─ main/resources
 │   └─ application.yml  # 환경 설정 파일
 │
 └─ test/java/com/zony
     ├─ api-server/      # API 서버 단위/통합 테스트
     └─ chat-server/     # 채팅 서버 테스트
      ...
```

</br>

## 팀원 소개 (Backend Roles)

| 이름 | 담당 역할 | 
|---|---|
| 박다희 | REST API 서버, RDBMS (Postgres) 설계 및 구현, QA 및 전체 모듈 디버깅 |
| 은지우 | Chat(WebSocket) 서버, Batch 서버, 인프라(AWS/CI/CD) 구축 및 운영, QA 및 전체 모듈 디버깅, 성능 분석 |
