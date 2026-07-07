def extractSectionByName(String changelog, String sectionName) {
    def heading = "## [${sectionName}]"
    def start = changelog.indexOf(heading)

    if (start < 0) {
        error("CHANGELOG.md에 '${heading}' 섹션이 없습니다.")
    }

    def next = changelog.indexOf("\n## [", start + heading.length())
    if (next < 0) {
        next = changelog.length()
    }

    return changelog.substring(start, next).trim()
}

def extractLatestReleaseSection(String changelog) {
    def semverMatcher = (changelog =~ /(?m)^## \[(\d+\.\d+\.\d+)\]/)
    if (semverMatcher.find()) {
        return extractSectionByName(changelog, semverMatcher.group(1))
    }

    def matcher = (changelog =~ /(?m)^## \[([^\]]+)\]/)
    while (matcher.find()) {
        def sectionName = matcher.group(1)
        if (sectionName != 'Unreleased') {
            return extractSectionByName(changelog, sectionName)
        }
    }

    return extractSectionByName(changelog, 'Unreleased')
}

def resolveReleaseNotes(String changelog, String sectionName) {
    if (!sectionName?.trim() || sectionName == 'latest-release') {
        return extractLatestReleaseSection(changelog)
    }

    return extractSectionByName(changelog, sectionName)
}

def resolveFrontendDir() {
    if (fileExists('frontend/package.json')) {
        return 'frontend'
    }

    return ''
}

def installFrontendDependencies(String frontendDir = 'frontend') {
    if (fileExists("${frontendDir}/pnpm-lock.yaml")) {
        if (isUnix()) {
            sh 'pnpm install --frozen-lockfile'
        } else {
            bat 'pnpm install --frozen-lockfile'
        }
        return
    }

    echo 'pnpm-lock.yaml이 없습니다. frozen-lockfile 없이 install을 실행합니다. (lockfile 커밋 권장)'
    if (isUnix()) {
        sh 'pnpm install'
    } else {
        bat 'pnpm install'
    }
}

def resolveGitTag() {
    if (env.TAG_NAME?.trim()) {
        return env.TAG_NAME.trim()
    }

    if (isUnix()) {
        return sh(
            script: 'git describe --tags --exact-match 2>/dev/null || true',
            returnStdout: true
        ).trim()
    }

    return bat(
        script: '@git describe --tags --exact-match 2>nul',
        returnStdout: true
    ).trim()
}

def resolveLatestGitTag() {
    if (isUnix()) {
        return sh(
            script: 'git describe --tags --abbrev=0 2>/dev/null || true',
            returnStdout: true
        ).trim()
    }

    return bat(
        script: '@git describe --tags --abbrev=0 2>nul',
        returnStdout: true
    ).trim()
}

def resolveReleasePleaseManifestVersion() {
    if (!fileExists('.github/.release-please-manifest.json')) {
        return ''
    }

    def manifestText = readFile(file: '.github/.release-please-manifest.json', encoding: 'UTF-8')
    def matcher = (manifestText =~ /"\.":\s*"([^"]+)"/)
    return matcher.find() ? matcher.group(1).trim() : ''
}

def normalizeReleaseVersionLabel(String version) {
    if (!version?.trim()) {
        return ''
    }

    def normalized = version.trim()
    return normalized.startsWith('v') ? normalized : "v${normalized}"
}

def resolveReleaseVersionForDisplay() {
    def exactTag = env.RELEASE_TAG?.trim()
    if (exactTag) {
        return normalizeReleaseVersionLabel(exactTag)
    }

    def manifestVersion = resolveReleasePleaseManifestVersion()
    if (manifestVersion) {
        return normalizeReleaseVersionLabel(manifestVersion)
    }

    if (fileExists('version.txt')) {
        def versionFile = readFile(file: 'version.txt', encoding: 'UTF-8').trim()
        if (versionFile) {
            return normalizeReleaseVersionLabel(versionFile)
        }
    }

    return normalizeReleaseVersionLabel(resolveLatestGitTag())
}

def readPipelineContextProperty(String key) {
    if (!fileExists('pipeline-context.properties')) {
        return ''
    }

    def lines = readFile(file: 'pipeline-context.properties', encoding: 'UTF-8').split('\n')
    for (line in lines) {
        if (line.startsWith("${key}=")) {
            return line.substring(key.length() + 1).trim()
        }
    }

    return ''
}

def resolveFrontendDirForDisplay() {
    def frontendDir = env.FRONTEND_DIR?.trim()
    if (frontendDir) {
        return frontendDir
    }

    frontendDir = readPipelineContextProperty('FRONTEND_DIR')
    if (frontendDir) {
        return frontendDir
    }

    return resolveFrontendDir()
}

def refreshPipelineContextFile() {
    def frontendDir = resolveFrontendDir()
    if (frontendDir) {
        env.FRONTEND_DIR = frontendDir
    }

    def exactTag = resolveGitTag()
    if (exactTag) {
        env.RELEASE_TAG = exactTag
    }

    env.RELEASE_VERSION = resolveReleaseVersionForDisplay()

    writeFile(
        file: 'pipeline-context.properties',
        text: """FRONTEND_DIR=${frontendDir}
RELEASE_TAG=${env.RELEASE_TAG ?: ''}
RELEASE_VERSION=${env.RELEASE_VERSION ?: ''}
EFFECTIVE_CHANGELOG_SECTION=${env.EFFECTIVE_CHANGELOG_SECTION ?: resolveChangelogSectionParam()}
""",
        encoding: 'UTF-8'
    )
}

def resolveChangelogSectionFromTag(String tag) {
    if (!tag?.trim()) {
        return ''
    }

    return tag.startsWith('v') ? tag.substring(1) : tag
}

def defaultHealthCheckUrl(String deployEnv) {
    if (deployEnv == 'staging') {
        return 'https://staging.example.com/actuator/health'
    }
    return ''
}

def escapeTelegramHtml(String text) {
    if (text == null) {
        return ''
    }

    return text
        .replace('&', '&amp;')
        .replace('<', '&lt;')
        .replace('>', '&gt;')
}

def truncateTelegramText(String text, int maxLength = 3200) {
    if (text == null || text.length() <= maxLength) {
        return text ?: ''
    }

    return text.substring(0, maxLength - 3) + '...'
}

def escapeTelegramHtmlAttr(String text) {
    if (text == null) {
        return ''
    }

    return escapeTelegramHtml(text).replace('"', '&quot;')
}

def stripTelegramHtml(String html) {
    if (html == null) {
        return ''
    }

    return html
        .replaceAll(/<br\s*\/?>/, '\n')
        .replaceAll(/<[^>]+>/, '')
        .replace('&amp;', '&')
        .replace('&lt;', '<')
        .replace('&gt;', '>')
        .replace('&quot;', '"')
}

def resolveChangelogSectionParam() {
    def section = params.CHANGELOG_SECTION?.trim()
    return section ? section : 'latest-release'
}

def resolveDeployEnvParam() {
    def deployEnv = params.DEPLOY_ENV?.trim()
    return deployEnv ? deployEnv : 'dev'
}

def normalizeBranchName(String branch) {
    if (!branch?.trim()) {
        return ''
    }

    return branch.trim()
        .replaceFirst(/^origin\//, '')
        .replaceFirst(/^refs\/heads\//, '')
        .replaceFirst(/^refs\/tags\//, '')
        .replaceFirst(/~.*$/, '')
}

def resolveGitBranchFromGit() {
    if (isUnix()) {
        return sh(
            script: '''
                set +e
                branch="$(git symbolic-ref -q --short HEAD 2>/dev/null)"
                if [ -n "$branch" ]; then
                  echo "$branch"
                  exit 0
                fi
                branch="$(git branch -r --contains HEAD 2>/dev/null | sed 's/^[* ]*//' | head -1)"
                branch="${branch#origin/}"
                if [ -n "$branch" ]; then
                  echo "$branch"
                  exit 0
                fi
                git name-rev --name-only HEAD 2>/dev/null | sed 's/^remotes\\/origin\\///;s/~.*$//'
            ''',
            returnStdout: true
        ).trim()
    }

    return bat(
        script: '''
            @for /f "delims=" %%b in ('git symbolic-ref -q --short HEAD 2^>nul') do @echo %%b&goto :done
            @for /f "delims=" %%b in ('git branch -r --contains HEAD 2^>nul') do @set REMOTE=%%b&goto :remote
            @goto :done
            :remote
            @echo %REMOTE:origin/=%
            :done
        ''',
        returnStdout: true
    ).trim()
}

def resolveGitBranch() {
    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: env.RESOLVED_GIT_BRANCH
    branch = normalizeBranchName(branch)
    if (branch) {
        return branch
    }

    branch = normalizeBranchName(resolveGitBranchFromGit())
    if (branch && branch != 'HEAD') {
        return branch
    }

    return 'unknown'
}

def shouldRequireReleaseTag() {
    def deployEnv = resolveDeployEnvParam()
    if (deployEnv == 'prod') {
        return true
    }

    if (deployEnv == 'staging' && params.REQUIRE_RELEASE_TAG) {
        return true
    }

    return false
}

def fetchGitTagsForReleaseDetection() {
    if (isUnix()) {
        sh 'git fetch origin --tags --force 2>/dev/null || true'
    } else {
        bat '@git fetch origin --tags --force 2>nul'
    }
}

def resolveGitCommitShort() {
    if (isUnix()) {
        return sh(
            script: 'git rev-parse --short HEAD 2>/dev/null || echo unknown',
            returnStdout: true
        ).trim()
    }

    return bat(
        script: '@git rev-parse --short HEAD 2>nul || echo unknown',
        returnStdout: true
    ).trim()
}

def withTelegramCredentials(Closure body) {
    try {
        withCredentials([
            string(credentialsId: 'telegram-bot-token', variable: 'TG_TOKEN'),
            string(credentialsId: 'telegram-chat-id', variable: 'TG_CHAT'),
        ]) {
            body(env.TG_TOKEN, env.TG_CHAT)
        }
    } catch (Exception credentialError) {
        echo "Telegram Credentials 미사용: ${credentialError.message}"
        def token = ''
        def chat = ''
        if (isUnix()) {
            token = sh(script: 'printf %s "${TELEGRAM_BOT_TOKEN}"', returnStdout: true).trim()
            chat = sh(script: 'printf %s "${TELEGRAM_CHAT_ID}"', returnStdout: true).trim()
        } else {
            token = bat(script: '@echo %TELEGRAM_BOT_TOKEN%', returnStdout: true).trim()
            chat = bat(script: '@echo %TELEGRAM_CHAT_ID%', returnStdout: true).trim()
        }
        if (!token || !chat) {
            error('Telegram 설정이 없습니다. TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID 를 확인하세요.')
        }
        body(token, chat)
    }
}

def sendTelegramMessage(String message, boolean finalSummary = false) {
    if (!params.SEND_TELEGRAM) {
        return
    }

    if (finalSummary && env.TELEGRAM_FINAL_SUMMARY_SENT == 'true') {
        echo 'Telegram 최종 요약은 이미 전송됨 — 중복 생략'
        return
    }

    def payload = truncateTelegramText(message)
    writeFile file: 'telegram-message-plain.txt', text: stripTelegramHtml(payload), encoding: 'UTF-8'

    withTelegramCredentials { String token, String chatId ->
        def htmlJson = groovy.json.JsonOutput.toJson([
            chat_id: chatId,
            text: payload,
            parse_mode: 'HTML',
            disable_web_page_preview: true,
        ])
        writeFile file: 'telegram-payload.json', text: htmlJson, encoding: 'UTF-8'

        def htmlExit = 1
        if (isUnix()) {
            htmlExit = sh(
                script: """
                    curl -fsS -X POST "https://api.telegram.org/bot${token}/sendMessage" \
                      -H 'Content-Type: application/json; charset=utf-8' \
                      --data-binary @telegram-payload.json
                """,
                returnStatus: true
            )
        } else {
            htmlExit = bat(
                script: """
                    curl -fsS -X POST "https://api.telegram.org/bot${token}/sendMessage" ^
                      -H "Content-Type: application/json; charset=utf-8" ^
                      --data-binary @telegram-payload.json
                """,
                returnStatus: true
            )
        }

        if (htmlExit != 0) {
            echo "Telegram HTML 전송 실패(exit=${htmlExit}), plain text로 재시도합니다."
            def plainJson = groovy.json.JsonOutput.toJson([
                chat_id: chatId,
                text: stripTelegramHtml(payload),
                disable_web_page_preview: true,
            ])
            writeFile file: 'telegram-payload-plain.json', text: plainJson, encoding: 'UTF-8'
            if (isUnix()) {
                sh """
                    curl -fsS -X POST "https://api.telegram.org/bot${token}/sendMessage" \
                      -H 'Content-Type: application/json; charset=utf-8' \
                      --data-binary @telegram-payload-plain.json
                """
            } else {
                bat """
                    curl -fsS -X POST "https://api.telegram.org/bot${token}/sendMessage" ^
                      -H "Content-Type: application/json; charset=utf-8" ^
                      --data-binary @telegram-payload-plain.json
                """
            }
        }
    }

    if (finalSummary) {
        env.TELEGRAM_FINAL_SUMMARY_SENT = 'true'
    }
}

def formatTelegramActionLines(String action) {
    return action
        .split('\n')
        .collect { line -> line?.trim() }
        .findAll { it }
        .collect { line -> "• ${escapeTelegramHtml(line)}" }
        .join('\n')
}

def resolveFailedStageName() {
    if (env.PIPELINE_FAILED_STAGE?.trim()) {
        return env.PIPELINE_FAILED_STAGE.trim()
    }

    def stageName = env.STAGE_NAME?.trim()
    if (stageName && stageName != 'Declarative: Post Actions') {
        return stageName
    }

    if (fileExists('.pipeline-failed-stage')) {
        return readFile(file: '.pipeline-failed-stage', encoding: 'UTF-8').trim()
    }

    return '알 수 없음'
}

def captureFailureStageIfMissing() {
    if (env.PIPELINE_FAILED_STAGE?.trim()) {
        return
    }

    def stageName = env.STAGE_NAME?.trim()
    if (stageName && stageName != 'Declarative: Post Actions') {
        env.PIPELINE_FAILED_STAGE = stageName
        writeFile file: '.pipeline-failed-stage', text: stageName, encoding: 'UTF-8'
    }
}

def markPipelineFailed(String stageName) {
    env.PIPELINE_FAILED_STAGE = stageName
    writeFile file: '.pipeline-failed-stage', text: stageName, encoding: 'UTF-8'
}

def backendGradleFlags() {
    return '--build-cache --console=plain --stacktrace --info --profile --no-parallel'
}

def resolveBackendBootJarGoals() {
    if (resolveDeployEnvParam() == 'prod' || env.RELEASE_TAG?.trim()) {
        return 'clean bootJar'
    }
    if (resolveDeployEnvParam() == 'staging') {
        return 'clean bootJar'
    }
    return 'bootJar'
}

def shouldRunBackendBootJar() {
    if (env.RELEASE_TAG?.trim()) {
        return true
    }
    return resolveDeployEnvParam() in ['staging', 'prod']
}

def verifyBackendGradleWrapper() {
    if (!fileExists('backend/gradlew') && !fileExists('backend/gradlew.bat')) {
        markPipelineFailed('백엔드 JAR 빌드')
        error('backend/gradlew가 없습니다. Gradle wrapper 위치를 확인하세요.')
    }
    if (!fileExists('backend/gradle/wrapper/gradle-wrapper.jar')) {
        markPipelineFailed('백엔드 JAR 빌드')
        error(
            'backend/gradle/wrapper/gradle-wrapper.jar 가 없습니다. ' +
            'Gradle wrapper 파일을 저장소에 커밋했는지 확인하세요.'
        )
    }
}

def runBackendGradle(String stageLabel, String goals, int timeoutMinutes = 20) {
    try {
        dir('backend') {
            timeout(time: timeoutMinutes, unit: 'MINUTES') {
                if (isUnix()) {
                    sh 'chmod +x gradlew || true'
                    sh "./gradlew ${goals} ${backendGradleFlags()}"
                } else {
                    bat "gradlew.bat ${goals} ${backendGradleFlags()}"
                }
            }
        }
    } catch (Exception buildError) {
        markPipelineFailed(stageLabel)
        throw buildError
    }
}

def resolveFailureGuide(String failedStage) {
    switch (failedStage) {
        case '릴리즈 검증':
            return [
                headline: '배포 전 검사에서 중단',
                reason: resolveDeployEnvParam() == 'prod'
                    ? '운영(prod) 배포인데 이 커밋에 release-please 태그(v*)가 없습니다.'
                    : 'staging 배포인데 이 커밋에 release-please 태그(v*)가 없습니다.',
                impact: '서버에 새 버전은 반영되지 않았습니다. (빌드·배포 미실행)',
                action: 'GitHub Release PR 머지 후 생성된 v* 태그 커밋을 빌드하세요.\n개발 테스트: DEPLOY_ENV=dev 로 실행 (태그 불필요)',
            ]
        case '변경 로그 읽기':
            return [
                headline: '변경 이력(CHANGELOG) 확인 실패',
                reason: "CHANGELOG.md에서 '${env.EFFECTIVE_CHANGELOG_SECTION ?: params.CHANGELOG_SECTION}' 섹션을 찾지 못했습니다.",
                impact: '서버에 새 버전은 반영되지 않았습니다. (빌드·배포 미실행)',
                action: 'CHANGELOG.md 섹션명을 확인하거나, Jenkins 파라미터 CHANGELOG_SECTION을 수정하세요.',
            ]
        case '백엔드 JAR 빌드':
        case '백엔드 테스트':
        case '백엔드 Detekt':
        case '백엔드 Ktlint':
        case '백엔드 bootJar':
        case '백엔드 test':
        case '백엔드 detekt':
        case '백엔드 ktlint':
        case '백엔드 빌드':
            return [
                headline: '백엔드 빌드 실패',
                reason: '백엔드 컴파일·테스트·코드 검사 중 오류가 발생했습니다. (gradle-wrapper.jar 누락 시 저장소에 wrapper 파일이 커밋됐는지 확인)',
                impact: '서버에 새 버전은 반영되지 않았습니다. (배포 미실행)',
                action: 'Jenkins 빌드 로그에서 Gradle 오류를 확인하고 backend/gradle/wrapper 를 커밋했는지 확인하세요.',
            ]
        case '프론트엔드 빌드':
            return [
                headline: '프론트엔드 빌드 실패',
                reason: '프론트엔드 설치(pnpm) 또는 빌드 중 오류가 발생했습니다.',
                impact: '서버에 새 버전은 반영되지 않았습니다. (배포 미실행)',
                action: "Jenkins 빌드 로그를 확인하세요. (프론트 경로: ${env.FRONTEND_DIR ?: '미감지'})",
            ]
        case '배포':
            return [
                headline: '배포 스크립트 실패',
                reason: '배포 스크립트 실행 중 오류가 발생했습니다.',
                impact: '새 버전 배포에 실패했습니다. 이전 버전이 유지될 수 있습니다.',
                action: 'Jenkins 빌드 로그와 deploy-local 스크립트 출력을 확인하세요.',
            ]
        case '배포 후 헬스체크':
            return [
                headline: '배포 후 검증 실패',
                reason: env.HEALTH_CHECK_TARGET?.trim()
                    ? "HTTP health check(${env.HEALTH_CHECK_TARGET})에 실패했습니다."
                    : '배포 산출물 검증에 실패했습니다. backend JAR 또는 deploy-info.txt 가 없습니다.',
                impact: 'HTTP 검증 실패 시 이전 배포본으로 롤백을 시도했습니다.',
                action: '배포 로그와 /deployments/{env}/latest 를 확인하세요. HTTP 검증은 HEALTH_CHECK_URL 에 실제 서비스 URL을 설정하세요.',
            ]
        default:
            return [
                headline: '파이프라인 실패',
                reason: failedStage ? "'${failedStage}' 단계에서 오류가 발생했습니다." : '알 수 없는 단계에서 오류가 발생했습니다.',
                impact: '서버 반영 여부는 실패 단계에 따라 다릅니다. 아래 로그를 확인하세요.',
                action: 'Jenkins 빌드 로그에서 ERROR 메시지를 확인하세요.',
            ]
    }
}

def buildTelegramContextBlock(boolean includeReleaseNotes = false) {
    def branch = escapeTelegramHtml(resolveGitBranch())
    def tag = escapeTelegramHtml(resolveReleaseVersionForDisplay() ?: '(없음)')
    def section = escapeTelegramHtml(env.EFFECTIVE_CHANGELOG_SECTION ?: resolveChangelogSectionParam())
    def frontendDir = resolveFrontendDirForDisplay()
    def frontend = escapeTelegramHtml(frontendDir ?: '(없음)')
    def commit = resolveGitCommitShort()
    def notesBlock = ''

    if (includeReleaseNotes && env.RELEASE_NOTES?.trim()) {
        notesBlock = "\n<b>📝 변경 내용</b>\n<pre>${escapeTelegramHtml(truncateTelegramText(env.RELEASE_NOTES, 1200))}</pre>"
    }

    return """<b>📎 참고</b>
환경: <code>${escapeTelegramHtml(resolveDeployEnvParam())}</code> | 브랜치: <code>${branch}</code>
커밋: <code>${commit}</code> | 릴리즈: <code>${tag}</code>
CHANGELOG: <code>${section}</code> | 프론트: <code>${frontend}</code>
<a href="${escapeTelegramHtmlAttr(env.BUILD_URL)}">상세 로그 (#${env.BUILD_NUMBER})</a>${notesBlock}"""
}

def notifyTelegramSummary(String outcome) {
    if (!params.SEND_TELEGRAM) {
        return
    }

    try {
        def message = ''

        if (outcome == 'success') {
            def healthStatus = env.HEALTH_CHECK_TARGET?.trim()
                ? "헬스체크: <code>${escapeTelegramHtml(env.HEALTH_CHECK_TARGET)}</code> 통과"
                : '헬스체크: 배포 산출물 검증 통과'
            def versionLine = resolveReleaseVersionForDisplay()?.trim()
                ? "릴리즈: <code>${escapeTelegramHtml(resolveReleaseVersionForDisplay())}</code>"
                : "CHANGELOG: <code>${escapeTelegramHtml(env.EFFECTIVE_CHANGELOG_SECTION ?: resolveChangelogSectionParam())}</code>"

            def frontendDir = resolveFrontendDirForDisplay()
            message = """✅ <b>IdolGlow 배포 완료</b>

<b>📋 요약</b>
<code>${escapeTelegramHtml(resolveDeployEnvParam())}</code> 환경에 새 버전이 반영되었습니다.

${versionLine}
백엔드: ${params.RUN_BACKEND_BUILD ? '빌드 완료' : '건너뜀'} | 프론트: ${params.RUN_FRONTEND_BUILD && frontendDir ? "빌드 완료 (${frontendDir})" : '건너뜀'}
${healthStatus}

${buildTelegramContextBlock(true)}
<i>알림 summary-v5 · 빌드 #${env.BUILD_NUMBER}</i>"""
        } else if (outcome == 'failure') {
            def failedStage = resolveFailedStageName()
            def guide = resolveFailureGuide(failedStage)

            message = """❌ <b>IdolGlow 배포 중단</b>

<b>📋 요약</b>
자동 배포가 시작됐으나 <code>${escapeTelegramHtml(failedStage)}</code> 단계에서 중단되었습니다.

<b>🔍 원인</b>
${escapeTelegramHtml(guide.reason)}

<b>⚠️ 영향</b>
${escapeTelegramHtml(guide.impact)}

<b>✅ 다음 조치 (개발팀)</b>
${formatTelegramActionLines(guide.action)}

${buildTelegramContextBlock(false)}
<i>알림 summary-v5 · 빌드 #${env.BUILD_NUMBER}</i>"""
        } else if (outcome == 'aborted') {
            message = """⏹️ <b>IdolGlow 배포 중단됨</b>

<b>📋 요약</b>
사용자 또는 시스템에 의해 파이프라인이 중단되었습니다.

<b>⚠️ 영향</b>
중단 시점에 따라 서버 반영이 없거나 일부만 진행됐을 수 있습니다.

<b>✅ 다음 조치</b>
Jenkins에서 중단 사유를 확인하고 필요 시 다시 실행하세요.

${buildTelegramContextBlock(false)}"""
        } else if (outcome == 'approval_wait') {
            message = """⏸️ <b>IdolGlow 운영 배포 승인 대기</b>

<b>📋 요약</b>
운영(prod) 환경 배포 전 <b>관리자 승인</b>이 필요합니다.

<b>✅ 다음 조치</b>
Jenkins UI에서 이 빌드를 열고 「배포 승인」을 눌러 주세요.

${buildTelegramContextBlock(false)}"""
        }

        if (message) {
            def isFinalSummary = outcome in ['success', 'failure', 'aborted']
            sendTelegramMessage(message, isFinalSummary)
        }
    } catch (Exception telegramError) {
        echo "Telegram 알림 전송 실패 (빌드는 계속): ${telegramError.message}"
    }
}

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'staging', 'prod'],
            description: '배포 대상 환경입니다.'
        )
        string(
            name: 'CHANGELOG_SECTION',
            defaultValue: 'latest-release',
            description: '게시할 CHANGELOG 섹션 (예: latest-release, Unreleased, 0.3.0). Git 태그(v*) 빌드 시 태그 버전으로 자동 대체됩니다.'
        )
        booleanParam(
            name: 'REQUIRE_RELEASE_TAG',
            defaultValue: false,
            description: 'true이면 staging/prod에서 v* 태그를 요구합니다. dev(main/chor/dev)는 태그 없이 빌드·배포 가능. prod는 항상 태그 강제.'
        )
        booleanParam(
            name: 'RUN_BACKEND_BUILD',
            defaultValue: true,
            description: '배포 전 백엔드 빌드를 실행합니다.'
        )
        booleanParam(
            name: 'RUN_FRONTEND_BUILD',
            defaultValue: true,
            description: '배포 전 프론트엔드 빌드를 실행합니다.'
        )
        booleanParam(
            name: 'RUN_HEALTH_CHECK',
            defaultValue: false,
            description: '배포 후 애플리케이션 health endpoint를 검증합니다. prod는 파이프라인에서 자동으로 true로 전환됩니다.'
        )
        string(
            name: 'HEALTH_CHECK_URL',
            defaultValue: '',
            description: '배포 후 검증할 health endpoint (예: http://host.docker.internal:9090/health/check). 비우면 산출물 검증만 수행합니다. prod에서 HTTP 검증을 하려면 URL을 설정하세요.'
        )
        string(
            name: 'DEPLOY_ROOT',
            defaultValue: '/deployments',
            description: '로컬 배포 산출물을 둘 디렉터리입니다.'
        )
        booleanParam(
            name: 'SEND_TELEGRAM',
            defaultValue: true,
            description: 'true이면 파이프라인 완료·실패·승인 대기 시 Telegram으로 요약 알림 1통을 전송합니다.'
        )
    }

    environment {
        GRADLE_USER_HOME = "${env.JENKINS_HOME}/.gradle"
        RELEASE_NOTES = ''
        FRONTEND_DIR = ''
        REPO_ROOT = ''
        RELEASE_TAG = ''
        RELEASE_VERSION = ''
        EFFECTIVE_CHANGELOG_SECTION = ''
        HEALTH_CHECK_TARGET = ''
        RESOLVED_GIT_BRANCH = ''
        PIPELINE_FAILED_STAGE = ''
        TELEGRAM_FINAL_SUMMARY_SENT = 'false'
    }

    stages {
        stage('체크아웃') {
            steps {
                checkout scm
                script {
                    fetchGitTagsForReleaseDetection()
                    env.REPO_ROOT = pwd()
                    env.FRONTEND_DIR = resolveFrontendDir()
                    env.RESOLVED_GIT_BRANCH = normalizeBranchName(resolveGitBranchFromGit())
                    env.RELEASE_TAG = resolveGitTag()
                    refreshPipelineContextFile()

                    echo "Git 브랜치: ${resolveGitBranch()} (BRANCH_NAME=${env.BRANCH_NAME ?: '-'}, GIT_BRANCH=${env.GIT_BRANCH ?: '-'}, git=${env.RESOLVED_GIT_BRANCH ?: '-'})"
                    echo "릴리즈 버전: ${env.RELEASE_VERSION ?: '(없음)'} | 프론트: ${env.FRONTEND_DIR ?: '(없음)'}"
                    if (env.RELEASE_TAG) {
                        echo "Git 태그 감지: ${env.RELEASE_TAG}"
                    } else {
                        echo "Git 태그 없음 (브랜치: ${resolveGitBranch()}, 환경: ${resolveDeployEnvParam()}, 태그 검증: ${shouldRequireReleaseTag()})"
                    }
                }
            }
        }

        stage('릴리즈 검증') {
            steps {
                script {
                    def requireTag = shouldRequireReleaseTag()
                    if (requireTag && !env.RELEASE_TAG) {
                        markPipelineFailed('릴리즈 검증')
                        error(
                            'release-please 태그(v*)가 없습니다. ' +
                            '운영/스테이징 배포는 Release PR 머지 후 v* 태그 커밋이 필요합니다. ' +
                            '개발 테스트는 DEPLOY_ENV=dev 로 실행하세요.'
                        )
                    }

                    if (env.RELEASE_TAG) {
                        def changelogSection = resolveChangelogSectionFromTag(env.RELEASE_TAG)
                        env.EFFECTIVE_CHANGELOG_SECTION = changelogSection
                        echo "CHANGELOG 섹션을 태그 기준으로 사용합니다: ${changelogSection}"
                    } else {
                        def changelogSection = resolveChangelogSectionParam()
                        env.EFFECTIVE_CHANGELOG_SECTION = changelogSection
                        echo "CHANGELOG 섹션을 파라미터 기준으로 사용합니다: ${changelogSection}"
                    }

                    refreshPipelineContextFile()
                }
            }
        }

        stage('변경 로그 읽기') {
            steps {
                script {
                    if (!fileExists('CHANGELOG.md')) {
                        markPipelineFailed('변경 로그 읽기')
                        error('저장소 루트에 CHANGELOG.md 파일이 없습니다.')
                    }

                    def changelog = readFile(file: 'CHANGELOG.md', encoding: 'UTF-8')
                    def releaseNotes = resolveReleaseNotes(changelog, env.EFFECTIVE_CHANGELOG_SECTION)

                    env.RELEASE_NOTES = releaseNotes
                    writeFile file: 'changelog-release-notes.txt', text: releaseNotes, encoding: 'UTF-8'

                    echo "=== 변경 로그 (${env.EFFECTIVE_CHANGELOG_SECTION}) ===\n${releaseNotes}"
                    currentBuild.description = "${resolveDeployEnvParam()} / ${env.EFFECTIVE_CHANGELOG_SECTION}"
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'changelog-release-notes.txt', allowEmptyArchive: false
                }
            }
        }

        stage('백엔드 JAR 빌드') {
            when {
                expression {
                    params.RUN_BACKEND_BUILD &&
                        fileExists('backend/build.gradle.kts') &&
                        shouldRunBackendBootJar()
                }
            }
            steps {
                script {
                    verifyBackendGradleWrapper()
                    runBackendGradle('백엔드 JAR 빌드', resolveBackendBootJarGoals(), 30)
                }
            }
        }

        stage('백엔드 테스트') {
            when {
                expression { params.RUN_BACKEND_BUILD && fileExists('backend/build.gradle.kts') }
            }
            steps {
                script {
                    runBackendGradle('백엔드 테스트', 'test', 20)
                }
            }
        }

        stage('백엔드 Detekt') {
            when {
                expression { params.RUN_BACKEND_BUILD && fileExists('backend/build.gradle.kts') }
            }
            steps {
                script {
                    runBackendGradle('백엔드 Detekt', 'detekt', 15)
                }
            }
        }

        stage('백엔드 Ktlint') {
            when {
                expression { params.RUN_BACKEND_BUILD && fileExists('backend/build.gradle.kts') }
            }
            steps {
                script {
                    runBackendGradle('백엔드 Ktlint', 'runKtlintCheckOverMainSourceSet', 15)
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'backend/build/reports/profile/*.html', allowEmptyArchive: true
                }
            }
        }

        stage('프론트엔드 빌드') {
            when {
                expression { params.RUN_FRONTEND_BUILD }
            }
            steps {
                script {
                    if (!env.FRONTEND_DIR?.trim()) {
                        markPipelineFailed('프론트엔드 빌드')
                        error(
                            'frontend/package.json을 찾을 수 없습니다. ' +
                            'frontend 프로젝트와 build 스크립트가 있는지 확인하세요.'
                        )
                    }

                    dir("${env.FRONTEND_DIR}") {
                        try {
                            if (isUnix()) {
                                sh 'node -v'
                                sh 'corepack enable'
                                sh 'corepack prepare pnpm@9.15.4 --activate || true'
                                sh 'pnpm -v'
                                installFrontendDependencies(env.FRONTEND_DIR)
                                sh 'pnpm build'
                            } else {
                                bat 'node -v'
                                bat 'corepack enable'
                                bat 'corepack prepare pnpm@9.15.4 --activate || true'
                                bat 'pnpm -v'
                                installFrontendDependencies(env.FRONTEND_DIR)
                                bat 'pnpm build'
                            }
                        } catch (Exception frontendBuildError) {
                            markPipelineFailed('프론트엔드 빌드')
                            throw frontendBuildError
                        }
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'frontend/.next/**', allowEmptyArchive: true
                }
            }
        }

        stage('운영 배포 승인') {
            when {
                expression { params.DEPLOY_ENV == 'prod' }
            }
            steps {
                script {
                    notifyTelegramSummary('approval_wait')
                    input message: '운영 환경에 배포하시겠습니까?', ok: '배포 승인'
                }
            }
        }

        stage('배포') {
            steps {
                script {
                    def deployEnv = resolveDeployEnvParam()
                    echo "배포 대상 환경: ${deployEnv}"
                    echo "릴리즈 태그: ${env.RELEASE_TAG ?: '(없음)'}"
                    echo "릴리즈 노트:\n${env.RELEASE_NOTES}"

                    if (isUnix()) {
                        sh """
                            export DEPLOY_ROOT='${params.DEPLOY_ROOT}'
                            export BUILD_ID='${env.BUILD_TAG}'
                            bash infra/jenkins-local/deploy-local.sh '${deployEnv}' '${env.REPO_ROOT}' '${env.REPO_ROOT}/changelog-release-notes.txt'
                        """
                    } else {
                        bat """
                            set DEPLOY_ROOT=${params.DEPLOY_ROOT}
                            set BUILD_ID=${env.BUILD_TAG}
                            powershell -ExecutionPolicy Bypass -File infra\\jenkins-local\\deploy-local.ps1 -DeployEnv ${deployEnv} -WorkspaceDir ${env.REPO_ROOT} -ReleaseNotesFile ${env.REPO_ROOT}\\changelog-release-notes.txt
                        """
                    }
                }
            }
        }

        stage('배포 후 헬스체크') {
            steps {
                script {
                    def deployEnv = resolveDeployEnvParam()
                    def deployRoot = params.DEPLOY_ROOT

                    if (isUnix()) {
                        sh """
                            export DEPLOY_ROOT='${deployRoot}'
                            bash infra/jenkins-local/verify-deploy.sh '${deployEnv}'
                        """
                    } else {
                        markPipelineFailed('배포 후 헬스체크')
                        error('배포 산출물 검증은 Linux Jenkins 에이전트에서 실행하세요.')
                    }

                    def httpUrl = params.HEALTH_CHECK_URL?.trim()
                    if (!httpUrl && (params.RUN_HEALTH_CHECK || deployEnv == 'prod')) {
                        httpUrl = defaultHealthCheckUrl(deployEnv)
                    }

                    if (!httpUrl) {
                        echo '배포 산출물 검증 완료 (HTTP health check URL 없음)'
                        return
                    }

                    env.HEALTH_CHECK_TARGET = httpUrl
                    echo "HTTP health check URL: ${httpUrl}"

                    try {
                        sh "curl -fsS --retry 5 --retry-delay 3 --retry-connrefused '${httpUrl}'"
                    } catch (Exception healthCheckError) {
                        echo "HTTP health check 실패. 이전 배포본으로 롤백을 시도합니다."

                        sh """
                            export DEPLOY_ROOT='${deployRoot}'
                            bash infra/jenkins-local/rollback-local.sh '${deployEnv}'
                        """

                        markPipelineFailed('배포 후 헬스체크')
                        error("배포 후 HTTP health check 실패: ${httpUrl}")
                    }
                }
            }
        }
    }

    post {
        failure {
            script {
                captureFailureStageIfMissing()
            }
        }
        always {
            script {
                refreshPipelineContextFile()
                def result = currentBuild.currentResult
                if (result == 'SUCCESS') {
                    notifyTelegramSummary('success')
                    echo '배포 파이프라인이 성공적으로 완료되었습니다.'
                } else if (result == 'FAILURE') {
                    notifyTelegramSummary('failure')
                    echo '배포 파이프라인이 실패했습니다. CHANGELOG 섹션, 태그, 빌드 로그를 확인하세요.'
                } else if (result == 'ABORTED') {
                    notifyTelegramSummary('aborted')
                }
            }
            archiveArtifacts artifacts: 'CHANGELOG.md', allowEmptyArchive: false
            archiveArtifacts artifacts: 'changelog-release-notes.txt', allowEmptyArchive: true
            archiveArtifacts artifacts: 'backend/build/reports/profile/*.html', allowEmptyArchive: true
        }
    }
}
