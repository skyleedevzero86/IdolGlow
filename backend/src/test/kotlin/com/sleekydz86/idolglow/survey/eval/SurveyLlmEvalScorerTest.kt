package com.sleekydz86.idolglow.survey.eval

import com.sleekydz86.idolglow.survey.infrastructure.SurveyRecommendationValidator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("설문 추천 LLM 채점")
class SurveyLlmEvalScorerTest {
    private val scorer = SurveyLlmEvalScorer(SurveyRecommendationValidator(ObjectMapper()))
    private val case =
        SurveyEvalCase(
            listOf("전통 문화"),
            listOf(
                SurveyEvalAttraction("A1", "경복궁", "종로구", "역사관광지"),
                SurveyEvalAttraction("A2", "창덕궁", "종로구", "역사관광지"),
            ),
        )

    @Test
    @DisplayName("필수 값이 모두 있으면 형식 성공으로 판정한다")
    fun scoresValidFormat() {
        val score = scorer.score(response(), case)

        assertTrue(score.formatSuccess)
        assertFalse(score.outsideRecommendation)
        assertFalse(score.forbiddenExpression)
    }

    @Test
    @DisplayName("목록에 없는 관광지 코드와 근거 없는 영문을 판정한다")
    fun scoresOutsideRecommendation() {
        val reasons = "{\"A1\":\"전통 문화\",\"A2\":\"고궁 산책\",\"X9\":\"Starbucks 방문\"}"
        val score = scorer.score(response(reasons = reasons), case)

        assertTrue(score.outsideRecommendation)
    }

    @Test
    @DisplayName("외국 문자와 숫자 및 가격 표현을 판정한다")
    fun scoresForbiddenExpression() {
        listOf("한자 漢字", "일본어 かな", "키릴 문자 Привет", "가격 1000원", "입장료 3,000 원", "요금 $5").forEach { narrative ->
            assertTrue(scorer.score(response(narrative = narrative), case).forbiddenExpression)
        }
    }

    @Test
    @DisplayName("공원이나 시장 이름에 들어간 글자는 가격으로 보지 않는다")
    fun allowsWordsContainingPriceUnit() {
        val narrative = "한강공원과 망원시장을 함께 둘러보세요."

        assertFalse(scorer.score(response(narrative = narrative), case).forbiddenExpression)
    }

    @Test
    @DisplayName("입력에 있는 숫자는 허용하고 없는 숫자는 금지로 본다")
    fun allowsNumbersFromSource() {
        val tower = SurveyEvalAttraction("A3", "63빌딩", "영등포구", "전망대")
        val numbered = case.copy(attractions = case.attractions + tower)
        val reasons = "{\"A1\":\"고궁 산책\",\"A2\":\"고궁 산책\",\"A3\":\"전망을 즐겨보세요.\"}"

        val allowed = scorer.score(response(narrative = "63빌딩에서 야경을 보세요.", reasons = reasons), numbered)
        val invented = scorer.score(response(narrative = "2시간 코스로 즐겨보세요.", reasons = reasons), numbered)

        assertFalse(allowed.forbiddenExpression)
        assertTrue(invented.forbiddenExpression)
    }

    @Test
    @DisplayName("subtitle이 비어 있으면 형식 실패로 본다")
    fun failsWhenSubtitleMissing() {
        val text = response().replace("\"subtitle\":\"고궁 산책\"", "\"subtitle\":\"\"")

        assertFalse(scorer.score(text, case).formatSuccess)
    }

    private fun response(
        narrative: String = "경복궁과 창덕궁에서 전통 문화를 만나보세요.",
        reasons: String = "{\"A1\":\"전통 문화를 느껴보세요.\",\"A2\":\"고궁을 산책해 보세요.\"}",
    ): String = """{"title":"전통 문화 여행","subtitle":"고궁 산책","narrative":"$narrative","attractionReasons":$reasons}"""
}
