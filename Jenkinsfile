def extractSectionByName(String changelog, String sectionName) {
    def heading = "## [${sectionName}]"
    def start = changelog.indexOf(heading)

    if (start < 0) {
        error("CHANGELOG.md에서 '${heading}' 섹션을 찾을 수 없습니다.")
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
            description: '배포할 환경을 선택하세요.'
        )
        string(
            name: 'CHANGELOG_SECTION',
            defaultValue: 'latest-release',
            description: '배포 시 읽을 CHANGELOG 섹션. 예: latest-release, Unreleased, v0.3.0'
        )
        booleanParam(
            name: 'RUN_BACKEND_BUILD',
            defaultValue: true,
            description: '배포 전 backend 빌드/테스트를 실행합니다.'
        )
        booleanParam(
            name: 'RUN_FRONTEND_BUILD',
            defaultValue: true,
            description: '배포 전 frontend 빌드를 실행합니다.'
        )
    }

    environment {
        RELEASE_NOTES = ''
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Read Changelog') {
            steps {
                script {
                    if (!fileExists('CHANGELOG.md')) {
                        error('루트 CHANGELOG.md 파일이 없습니다.')
                    }

                    def changelog = readFile(file: 'CHANGELOG.md', encoding: 'UTF-8')
                    def releaseNotes = resolveReleaseNotes(changelog, params.CHANGELOG_SECTION)

                    env.RELEASE_NOTES = releaseNotes
                    writeFile file: 'changelog-release-notes.txt', text: releaseNotes, encoding: 'UTF-8'

                    echo "=== CHANGELOG (${params.CHANGELOG_SECTION}) ===\n${releaseNotes}"
                    currentBuild.description = "${params.DEPLOY_ENV} / ${params.CHANGELOG_SECTION}"
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'changelog-release-notes.txt', allowEmptyArchive: false
                }
            }
        }

        stage('Backend Build') {
            when {
                expression { params.RUN_BACKEND_BUILD && fileExists('backend/build.gradle.kts') }
            }
            steps {
                dir('backend') {
                    script {
                        if (isUnix()) {
                            sh './gradlew clean test bootJar --no-daemon'
                        } else {
                            bat 'gradlew.bat clean test bootJar --no-daemon'
                        }
                    }
                }
            }
        }

        stage('Frontend Build') {
            when {
                expression { params.RUN_FRONTEND_BUILD && fileExists('frontend/package.json') }
            }
            steps {
                dir('frontend') {
                    script {
                        if (isUnix()) {
                            sh 'corepack enable'
                            sh 'pnpm install --frozen-lockfile'
                            sh 'pnpm build'
                        } else {
                            bat 'corepack enable'
                            bat 'pnpm install --frozen-lockfile'
                            bat 'pnpm build'
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploy target: ${params.DEPLOY_ENV}"
                echo "Release notes:\n${env.RELEASE_NOTES}"
                echo 'TODO: 실제 배포 명령어(서버 반영, Docker 배포, kubectl apply 등)를 여기에 추가하세요.'
            }
        }
    }

    post {
        success {
            echo '배포 파이프라인이 완료되었습니다.'
        }
        failure {
            echo '배포 파이프라인이 실패했습니다. CHANGELOG와 빌드 로그를 확인하세요.'
        }
        always {
            archiveArtifacts artifacts: 'CHANGELOG.md', allowEmptyArchive: false
        }
    }
}
