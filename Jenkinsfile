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

    if (fileExists('docs/acc-webzine/package.json')) {
        return 'docs/acc-webzine'
    }

    return ''
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

def resolveChangelogSectionFromTag(String tag) {
    if (!tag?.trim()) {
        return ''
    }

    return tag.startsWith('v') ? tag.substring(1) : tag
}

def defaultHealthCheckUrl(String deployEnv) {
    switch (deployEnv) {
        case 'staging':
            return 'https://staging.example.com/actuator/health'
        case 'dev':
            return 'http://localhost:8080/actuator/health'
        default:
            return ''
    }
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

def resolveGitBranch() {
    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'unknown'
    return branch.replaceFirst(/^origin\//, '')
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

def sendTelegramMessage(String message) {
    if (!params.SEND_TELEGRAM) {
        return
    }

    def payload = truncateTelegramText(message)
    writeFile file: 'telegram-message.txt', text: payload, encoding: 'UTF-8'

    withCredentials([
        string(credentialsId: 'telegram-bot-token', variable: 'TG_TOKEN'),
        string(credentialsId: 'telegram-chat-id', variable: 'TG_CHAT'),
    ]) {
        if (isUnix()) {
            sh '''
                curl -fsS -X POST "https://api.telegram.org/bot${TG_TOKEN}/sendMessage" \
                  -d chat_id="${TG_CHAT}" \
                  -d parse_mode="HTML" \
                  --data-urlencode text@telegram-message.txt
            '''
        } else {
            bat '''
                curl -fsS -X POST "https://api.telegram.org/bot%TG_TOKEN%/sendMessage" ^
                  -d chat_id="%TG_CHAT%" ^
                  -d parse_mode=HTML ^
                  --data-urlencode text@telegram-message.txt
            '''
        }
    }
}

def resolveFailureGuide(String failedStage) {
    switch (failedStage) {
        case '릴리즈 검증':
            return [
                headline: '배포 전 검사에서 중단',
                reason: '정식 버전 태그(v*)가 없습니다. release-please가 main에 생성한 태그가 필요합니다.',
                impact: '서버에 새 버전은 반영되지 않았습니다. (빌드·배포 미실행)',
                action: '개발 테스트: Jenkins에서 REQUIRE_RELEASE_TAG=false 로 재실행\n정식 배포: main 머지 → Release PR → v* 태그 생성 후 재실행',
            ]
        case '변경 로그 읽기':
            return [
                headline: '변경 이력(CHANGELOG) 확인 실패',
                reason: "CHANGELOG.md에서 '${env.EFFECTIVE_CHANGELOG_SECTION ?: params.CHANGELOG_SECTION}' 섹션을 찾지 못했습니다.",
                impact: '서버에 새 버전은 반영되지 않았습니다. (빌드·배포 미실행)',
                action: 'CHANGELOG.md 섹션명을 확인하거나, Jenkins 파라미터 CHANGELOG_SECTION을 수정하세요.',
            ]
        case '백엔드 빌드':
            return [
                headline: '백엔드 빌드 실패',
                reason: '백엔드 컴파일·테스트·코드 검사 중 오류가 발생했습니다.',
                impact: '서버에 새 버전은 반영되지 않았습니다. (배포 미실행)',
                action: 'Jenkins 빌드 로그에서 Gradle 오류를 확인하고 backend 코드를 수정하세요.',
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
                headline: '배포 후 서비스 검증 실패',
                reason: "배포는 시도됐으나 health check(${env.HEALTH_CHECK_TARGET ?: '미설정'})에 실패했습니다.",
                impact: '이전 배포본으로 롤백을 시도했습니다. 서비스 상태를 확인하세요.',
                action: '애플리케이션 기동 로그와 HEALTH_CHECK_URL 설정을 확인하세요.',
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
    def tag = escapeTelegramHtml(env.RELEASE_TAG ?: '(없음)')
    def section = escapeTelegramHtml(env.EFFECTIVE_CHANGELOG_SECTION ?: params.CHANGELOG_SECTION)
    def frontend = escapeTelegramHtml(env.FRONTEND_DIR ?: '(없음)')
    def commit = resolveGitCommitShort()
    def notesBlock = ''

    if (includeReleaseNotes && env.RELEASE_NOTES?.trim()) {
        notesBlock = "\n<b>📝 변경 내용</b>\n<pre>${escapeTelegramHtml(truncateTelegramText(env.RELEASE_NOTES, 1200))}</pre>"
    }

    return """<b>📎 참고</b>
환경: <code>${escapeTelegramHtml(params.DEPLOY_ENV)}</code> | 브랜치: <code>${branch}</code>
커밋: <code>${commit}</code> | 태그: <code>${tag}</code>
CHANGELOG: <code>${section}</code> | 프론트: <code>${frontend}</code>
<a href="${env.BUILD_URL}">🔗 상세 로그 (#${env.BUILD_NUMBER})</a>${notesBlock}"""
}

def notifyTelegramSummary(String outcome) {
    if (!params.SEND_TELEGRAM) {
        return
    }

    try {
        def message = ''

        if (outcome == 'success') {
            def healthStatus = (params.RUN_HEALTH_CHECK || params.DEPLOY_ENV == 'prod') && env.HEALTH_CHECK_TARGET?.trim()
                ? "헬스체크: <code>${escapeTelegramHtml(env.HEALTH_CHECK_TARGET)}</code> 통과"
                : '헬스체크: 건너뜀'
            def versionLine = env.RELEASE_TAG?.trim()
                ? "버전: <code>${escapeTelegramHtml(env.RELEASE_TAG)}</code>"
                : "CHANGELOG: <code>${escapeTelegramHtml(env.EFFECTIVE_CHANGELOG_SECTION ?: params.CHANGELOG_SECTION)}</code>"

            message = """✅ <b>IdolGlow 배포 완료</b>

<b>📋 요약</b>
<code>${escapeTelegramHtml(params.DEPLOY_ENV)}</code> 환경에 새 버전이 반영되었습니다.

${versionLine}
백엔드: ${params.RUN_BACKEND_BUILD ? '빌드 완료' : '건너뜀'} | 프론트: ${params.RUN_FRONTEND_BUILD && env.FRONTEND_DIR?.trim() ? '빌드 완료' : '건너뜀'}
${healthStatus}

${buildTelegramContextBlock(true)}"""
        } else if (outcome == 'failure') {
            def failedStage = env.STAGE_NAME ?: ''
            def guide = resolveFailureGuide(failedStage)

            message = """❌ <b>IdolGlow 배포 중단</b>

<b>📋 요약</b>
자동 배포가 시작됐으나 <b>${escapeTelegramHtml(failedStage ?: '알 수 없음')}</b> 단계에서 중단되었습니다.

<b>🔍 원인</b>
${escapeTelegramHtml(guide.reason)}

<b>⚠️ 영향</b>
${escapeTelegramHtml(guide.impact)}

<b>✅ 다음 조치 (개발팀)</b>
<pre>${escapeTelegramHtml(guide.action)}</pre>

${buildTelegramContextBlock(false)}"""
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
            sendTelegramMessage(message)
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
            defaultValue: true,
            description: 'true이면 release-please 태그(v*)가 있는 커밋에서만 배포를 허용합니다. prod 배포 시 항상 강제됩니다.'
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
            description: '배포 후 검증할 health endpoint. 비우면 dev/staging은 환경 기본값, prod는 HEALTH_CHECK_URL을 Jenkins에 설정해야 검증합니다.'
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
        RELEASE_NOTES = ''
        FRONTEND_DIR = ''
        REPO_ROOT = ''
        RELEASE_TAG = ''
        EFFECTIVE_CHANGELOG_SECTION = ''
        HEALTH_CHECK_TARGET = ''
    }

    stages {
        stage('체크아웃') {
            steps {
                checkout scm
                script {
                    env.REPO_ROOT = pwd()
                    env.FRONTEND_DIR = resolveFrontendDir()
                    env.RELEASE_TAG = resolveGitTag()

                    if (env.RELEASE_TAG) {
                        echo "Git 태그 감지: ${env.RELEASE_TAG}"
                    }
                }
            }
        }

        stage('릴리즈 검증') {
            steps {
                script {
                    def requireTag = params.REQUIRE_RELEASE_TAG || params.DEPLOY_ENV == 'prod'
                    if (requireTag && !env.RELEASE_TAG) {
                        error(
                            'release-please 태그(v*)가 없습니다. ' +
                            'Release PR 머지 후 생성된 태그를 기준으로 배포하거나, ' +
                            '개발 환경 테스트 시 REQUIRE_RELEASE_TAG=false로 실행하세요.'
                        )
                    }

                    if (env.RELEASE_TAG) {
                        env.EFFECTIVE_CHANGELOG_SECTION = resolveChangelogSectionFromTag(env.RELEASE_TAG)
                        echo "CHANGELOG 섹션을 태그 기준으로 사용합니다: ${env.EFFECTIVE_CHANGELOG_SECTION}"
                    } else {
                        env.EFFECTIVE_CHANGELOG_SECTION = params.CHANGELOG_SECTION
                        echo "CHANGELOG 섹션을 파라미터 기준으로 사용합니다: ${env.EFFECTIVE_CHANGELOG_SECTION}"
                    }
                }
            }
        }

        stage('변경 로그 읽기') {
            steps {
                script {
                    if (!fileExists('CHANGELOG.md')) {
                        error('저장소 루트에 CHANGELOG.md 파일이 없습니다.')
                    }

                    def changelog = readFile(file: 'CHANGELOG.md', encoding: 'UTF-8')
                    def releaseNotes = resolveReleaseNotes(changelog, env.EFFECTIVE_CHANGELOG_SECTION)

                    env.RELEASE_NOTES = releaseNotes
                    writeFile file: 'changelog-release-notes.txt', text: releaseNotes, encoding: 'UTF-8'

                    echo "=== 변경 로그 (${env.EFFECTIVE_CHANGELOG_SECTION}) ===\n${releaseNotes}"
                    currentBuild.description = "${params.DEPLOY_ENV} / ${env.EFFECTIVE_CHANGELOG_SECTION}"
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'changelog-release-notes.txt', allowEmptyArchive: false
                }
            }
        }

        stage('백엔드 빌드') {
            when {
                expression { params.RUN_BACKEND_BUILD && fileExists('backend/build.gradle.kts') }
            }
            steps {
                dir('backend') {
                    script {
                        if (!fileExists('gradlew') && !fileExists('gradlew.bat')) {
                            error('backend/gradlew가 없습니다. Gradle wrapper 위치를 확인하세요.')
                        }

                        if (isUnix()) {
                            sh 'chmod +x gradlew || true'
                            sh './gradlew clean kaptKotlin test detekt ktlintCheck bootJar --no-daemon'
                        } else {
                            bat 'gradlew.bat clean kaptKotlin test detekt ktlintCheck bootJar --no-daemon'
                        }
                    }
                }
            }
        }

        stage('프론트엔드 빌드') {
            when {
                expression { params.RUN_FRONTEND_BUILD && env.FRONTEND_DIR?.trim() }
            }
            steps {
                dir("${env.FRONTEND_DIR}") {
                    script {
                        if (isUnix()) {
                            sh 'node -v'
                            sh 'corepack enable'
                            sh 'pnpm -v'
                            sh 'pnpm install --frozen-lockfile'
                            sh 'pnpm build'
                        } else {
                            bat 'node -v'
                            bat 'corepack enable'
                            bat 'pnpm -v'
                            bat 'pnpm install --frozen-lockfile'
                            bat 'pnpm build'
                        }
                    }
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
                    echo "배포 대상 환경: ${params.DEPLOY_ENV}"
                    echo "릴리즈 태그: ${env.RELEASE_TAG ?: '(없음)'}"
                    echo "릴리즈 노트:\n${env.RELEASE_NOTES}"

                    if (isUnix()) {
                        sh """
                            export DEPLOY_ROOT='${params.DEPLOY_ROOT}'
                            export BUILD_ID='${env.BUILD_TAG}'
                            bash infra/jenkins-local/deploy-local.sh '${params.DEPLOY_ENV}' '${env.REPO_ROOT}' '${env.REPO_ROOT}/changelog-release-notes.txt'
                        """
                    } else {
                        bat """
                            set DEPLOY_ROOT=${params.DEPLOY_ROOT}
                            set BUILD_ID=${env.BUILD_TAG}
                            powershell -ExecutionPolicy Bypass -File infra\\jenkins-local\\deploy-local.ps1 -DeployEnv ${params.DEPLOY_ENV} -WorkspaceDir ${env.REPO_ROOT} -ReleaseNotesFile ${env.REPO_ROOT}\\changelog-release-notes.txt
                        """
                    }
                }
            }
        }

        stage('배포 후 헬스체크') {
            when {
                expression { params.RUN_HEALTH_CHECK || params.DEPLOY_ENV == 'prod' }
            }
            steps {
                script {
                    env.HEALTH_CHECK_TARGET = params.HEALTH_CHECK_URL?.trim()
                        ? params.HEALTH_CHECK_URL.trim()
                        : defaultHealthCheckUrl(params.DEPLOY_ENV)

                    if (!env.HEALTH_CHECK_TARGET) {
                        echo 'Health check URL이 없어 검증을 건너뜁니다. prod 운영 시 HEALTH_CHECK_URL을 설정하세요.'
                        return
                    }

                    echo "Health check URL: ${env.HEALTH_CHECK_TARGET}"

                    try {
                        if (isUnix()) {
                            sh "curl -fsS --retry 5 --retry-delay 3 --retry-connrefused '${env.HEALTH_CHECK_TARGET}'"
                        } else {
                            bat "curl -fsS --retry 5 --retry-delay 3 --retry-connrefused \"${env.HEALTH_CHECK_TARGET}\""
                        }
                    } catch (Exception healthCheckError) {
                        echo "Health check 실패. 이전 배포본으로 롤백을 시도합니다."

                        if (isUnix()) {
                            sh """
                                export DEPLOY_ROOT='${params.DEPLOY_ROOT}'
                                bash infra/jenkins-local/rollback-local.sh '${params.DEPLOY_ENV}'
                            """
                        } else {
                            bat """
                                set DEPLOY_ROOT=${params.DEPLOY_ROOT}
                                powershell -ExecutionPolicy Bypass -File infra\\jenkins-local\\rollback-local.ps1 -DeployEnv ${params.DEPLOY_ENV}
                            """
                        }

                        error("배포 후 health check 실패: ${env.HEALTH_CHECK_TARGET}")
                    }
                }
            }
        }
    }

    post {
        success {
            script { notifyTelegramSummary('success') }
            echo '배포 파이프라인이 성공적으로 완료되었습니다.'
        }
        failure {
            script { notifyTelegramSummary('failure') }
            echo '배포 파이프라인이 실패했습니다. CHANGELOG 섹션, 태그, 빌드 로그를 확인하세요.'
        }
        aborted {
            script { notifyTelegramSummary('aborted') }
        }
        always {
            archiveArtifacts artifacts: 'CHANGELOG.md', allowEmptyArchive: false
            archiveArtifacts artifacts: 'changelog-release-notes.txt', allowEmptyArchive: true
        }
    }
}
