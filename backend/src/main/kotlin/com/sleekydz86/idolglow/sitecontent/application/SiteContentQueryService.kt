package com.sleekydz86.idolglow.sitecontent.application

import com.sleekydz86.idolglow.bnr.domain.BnrRepository
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.mim.domain.MimRepository
import com.sleekydz86.idolglow.pup.domain.PupRepository
import com.sleekydz86.idolglow.sitecontent.application.dto.SiteBannerResponse
import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHeroSlideResponse
import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHomeContentResponse
import com.sleekydz86.idolglow.sitecontent.application.dto.SitePopupResponse
import com.sleekydz86.idolglow.sitecontent.application.port.`in`.SiteContentQueryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class SiteContentQueryService(
    private val bnrRepository: BnrRepository,
    private val pupRepository: PupRepository,
    private val mimRepository: MimRepository,
    private val appPublicUrlProperties: AppPublicUrlProperties,
) : SiteContentQueryUseCase {
    override fun readHomeContent(): SiteHomeContentResponse {
        val resolvedDomainId = "kr"

        val heroSlides = mimRepository.findActiveByDomain(resolvedDomainId)
            .asSequence()
            .filter { !it.imagePath.isNullOrBlank() }
            .map {
                SiteHeroSlideResponse(
                    imageId = it.imageId,
                    title = it.imageName?.takeIf(String::isNotBlank) ?: "Main slide",
                    subtitle = it.description?.takeIf(String::isNotBlank),
                    imageUrl = toAssetUrl(it.imagePath) ?: it.imagePath!!,
                    linkUrl = "/articles",
                    categoryLabel = "main",
                )
            }
            .toList()

        val banners = bnrRepository.findActiveByDomain(resolvedDomainId)
            .asSequence()
            .filter { !it.imagePath.isNullOrBlank() }
            .map {
                SiteBannerResponse(
                    bannerId = it.bannerId,
                    title = it.bannerName?.takeIf(String::isNotBlank) ?: "Banner",
                    description = it.description?.takeIf(String::isNotBlank),
                    imageUrl = toAssetUrl(it.imagePath) ?: it.imagePath!!,
                    linkUrl = it.linkUrl?.takeIf(String::isNotBlank) ?: "/articles",
                )
            }
            .toList()

        val now = LocalDateTime.now()
        val popups = pupRepository.findPublicByDomain(resolvedDomainId)
            .asSequence()
            .filter { isVisiblePopup(it.noticeStartDate, it.noticeEndDate, now) }
            .map {
                SitePopupResponse(
                    popupId = it.popupId,
                    title = it.title?.takeIf(String::isNotBlank) ?: "Popup",
                    imageUrl = toAssetUrl(it.imagePath?.takeIf(String::isNotBlank)),
                    linkUrl = it.fileUrl?.takeIf(String::isNotBlank),
                    linkTarget = it.linkTarget?.takeIf(String::isNotBlank) ?: "_blank",
                    noticeStartDate = it.noticeStartDate,
                    noticeEndDate = it.noticeEndDate,
                    stopViewYn = it.stopViewYn,
                )
            }
            .take(5)
            .toList()

        return SiteHomeContentResponse(
            heroSlides = heroSlides,
            banners = banners,
            popups = popups,
        )
    }

    private fun toAssetUrl(imagePath: String?): String? {
        val raw = imagePath?.trim().orEmpty()
        if (raw.isBlank()) {
            return null
        }

        val objectKey = when {
            raw.contains("/webzine/") -> "webzine/${raw.substringAfter("/webzine/").trimStart('/')}"
            raw.contains("/uploads/webzine/") -> "webzine/${raw.substringAfter("/uploads/webzine/").trimStart('/')}"
            else -> return raw
        }

        val encodedObjectKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8)
        val baseUrl = appPublicUrlProperties.publicBaseUrl.trimEnd('/')
        return "$baseUrl/site-content/assets?objectKey=$encodedObjectKey"
    }

    private fun isVisiblePopup(
        noticeStartDate: String?,
        noticeEndDate: String?,
        now: LocalDateTime,
    ): Boolean {
        val start = parseDateTime(noticeStartDate)
        val end = parseDateTime(noticeEndDate)

        if (start != null && now.isBefore(start)) {
            return false
        }

        if (end != null && now.isAfter(end)) {
            return false
        }

        return true
    }

    private fun parseDateTime(raw: String?): LocalDateTime? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            return null
        }

        return DATE_TIME_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
        }
    }

    companion object {
        private val DATE_TIME_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyyMMddHHmm"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        )
    }
}
