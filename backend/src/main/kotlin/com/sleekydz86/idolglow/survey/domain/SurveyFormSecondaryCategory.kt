package com.sleekydz86.idolglow.survey.domain

enum class SurveyFormSecondaryCategory(
    val primaryCategory: SurveyFormPrimaryCategory,
    val label: String,
) {
    ACTIVITY(SurveyFormPrimaryCategory.TOUR_EXPERIENCE, "활동"),
    FOOD(SurveyFormPrimaryCategory.TOUR_EXPERIENCE, "미식"),
    K_POP(SurveyFormPrimaryCategory.TOUR_EXPERIENCE, "K-pop"),
    ATTRACTIONS_TICKETS(SurveyFormPrimaryCategory.TOUR_EXPERIENCE, "흥미거리&티켓"),
    PHOTO(SurveyFormPrimaryCategory.TOUR_EXPERIENCE, "포토"),
    TOUR(SurveyFormPrimaryCategory.TOUR_EXPERIENCE, "투어"),

    WIFI_SIM(SurveyFormPrimaryCategory.TRAVEL_ESSENTIALS, "wifi&sim"),
    TRANSPORTATION(SurveyFormPrimaryCategory.TRAVEL_ESSENTIALS, "교통"),
    TRAVEL_SERVICE(SurveyFormPrimaryCategory.TRAVEL_ESSENTIALS, "여행서비스"),
    EXCHANGE(SurveyFormPrimaryCategory.TRAVEL_ESSENTIALS, "환전"),
    INSURANCE(SurveyFormPrimaryCategory.TRAVEL_ESSENTIALS, "보험"),

    HAIR_SALON(SurveyFormPrimaryCategory.BEAUTY, "헤어살롱"),
    K_BEAUTY(SurveyFormPrimaryCategory.BEAUTY, "k-뷰티"),
    SKIN_CARE(SurveyFormPrimaryCategory.BEAUTY, "피부미용과"),

    CLINIC(SurveyFormPrimaryCategory.MEDICAL, "클리닉"),
    PHARMACY(SurveyFormPrimaryCategory.MEDICAL, "약국"),
    VISION_CORRECTION(SurveyFormPrimaryCategory.MEDICAL, "시력교정"),
    HEALTH_CHECKUP(SurveyFormPrimaryCategory.MEDICAL, "건강진단"),
    KOREAN_MEDICINE(SurveyFormPrimaryCategory.MEDICAL, "한의원"),

    SPA_HEALING(SurveyFormPrimaryCategory.ETC, "스파&치유"),
    COUPON(SurveyFormPrimaryCategory.ETC, "쿠폰"),
}
