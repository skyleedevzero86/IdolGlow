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

pipeline {
    agent any

    options {
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
                input message: '운영 환경에 배포하시겠습니까?', ok: '배포 승인'
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
            echo '배포 파이프라인이 성공적으로 완료되었습니다.'
        }
        failure {
            echo '배포 파이프라인이 실패했습니다. CHANGELOG 섹션, 태그, 빌드 로그를 확인하세요.'
        }
        always {
            archiveArtifacts artifacts: 'CHANGELOG.md', allowEmptyArchive: false
            archiveArtifacts artifacts: 'changelog-release-notes.txt', allowEmptyArchive: true
        }
    }
}
