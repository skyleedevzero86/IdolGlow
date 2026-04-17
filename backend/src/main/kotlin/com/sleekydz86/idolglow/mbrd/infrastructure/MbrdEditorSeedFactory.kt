package com.sleekydz86.idolglow.mbrd.infrastructure

import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentId
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentPublicationStatus
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorDocument
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class MbrdEditorSeedFactory {
    fun create(clock: Clock): MbrdEditorDocument =
        MbrdEditorDocument.create(
            id = MbrdDocumentId.newId(),
            title = "OIDC 로그인 이후의 회원 생명주기와 상태 모델링",
            author = "sleek",
            markdown = seedMarkdown(),
            tags = listOf(
                "DDD",
                "OIDC",
                "oauth2",
                "도메인주도설계",
                "백엔드아키텍처",
                "상태머신",
                "스프링부트",
                "클린코드",
            ),
            urlSlug = "oidc-member-lifecycle-state-modeling",
            introduction = "OIDC 로그인 이후 실제 운영에서 마주치는 가입, 승인, 탈퇴 상태를 어떻게 안정적으로 모델링했는지 정리한 글입니다.",
            thumbnailImageUrl = "/reference-editor-flow.svg",
            publicationStatus = MbrdDocumentPublicationStatus.PUBLISHED,
            clock = clock,
        )

    private fun seedMarkdown(): String =
        """
        ![회원 상태 모델](/reference-editor-flow.svg)

        # 회원 생명주기와 상태 모델링
        ## 튜토리얼이 알려주지 않는 실제 문제

        소셜 로그인 연동 튜토리얼은 보통 `인증 완료 -> 사용자 저장 -> 로그인 처리` 흐름으로 끝납니다.
        하지만 실제 서비스에서는 인증 성공과 가입 완료가 같은 의미가 아니고, 상태에 따라 허용되는 행동도 달라집니다.

        - OIDC 인증이 끝났다고 가입까지 끝난 것으로 볼 수 있을까?
        - 추가 동의가 필요한 정보는 어느 시점에 받아야 할까?
        - 관리자 승인 대기 상태를 도메인 모델에서 어떻게 표현할까?
        - 탈퇴 이후 데이터 정리와 접근 권한은 어디에서 막아야 할까?

        > 좋은 회원 모델은 로그인 성공만 저장하는 것이 아니라 상태 차이를 분명하게 표현하는 것에서 시작합니다.

        ### 초안 방향

        1. 인증 성공과 가입 완료를 분리한다.
        2. 회원 상태 전이를 도메인 규칙으로 관리한다.
        3. 프론트엔드와 백엔드가 같은 상태 언어를 사용한다.
        4. 승인, 반려, 탈퇴를 이벤트처럼 기록한다.

        ```java
        public enum MemberStatus {
            REGISTRATION_NEEDED,
            PENDING_APPROVAL,
            ACTIVE,
            REJECTED,
            WITHDRAWN
        }
        ```

        참고 링크: [Spring Security](https://spring.io/projects/spring-security)
        """.trimIndent()
}
