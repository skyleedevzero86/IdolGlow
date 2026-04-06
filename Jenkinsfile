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

    return ''
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
            description: '게시할 CHANGELOG 섹션 (예: latest-release, Unreleased, v0.3.0)'
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
    }

    stages {
        stage('체크아웃') {
            steps {
                checkout scm
                script {
                    env.REPO_ROOT = pwd()
                    env.FRONTEND_DIR = resolveFrontendDir()
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
                    def releaseNotes = resolveReleaseNotes(changelog, params.CHANGELOG_SECTION)

                    env.RELEASE_NOTES = releaseNotes
                    writeFile file: 'changelog-release-notes.txt', text: releaseNotes, encoding: 'UTF-8'

                    echo "=== 변경 로그 (${params.CHANGELOG_SECTION}) ===\n${releaseNotes}"
                    currentBuild.description = "${params.DEPLOY_ENV} / ${params.CHANGELOG_SECTION}"
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
                        if (isUnix()) {
                            sh 'chmod +x gradlew || true'
                            sh './gradlew clean test bootJar --no-daemon'
                        } else {
                            bat 'gradlew.bat clean test bootJar --no-daemon'
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

        stage('배포') {
            steps {
                script {
                    echo "배포 대상 환경: ${params.DEPLOY_ENV}"
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
    }

    post {
        success {
            echo '배포 파이프라인이 성공적으로 완료되었습니다.'
        }
        failure {
            echo '배포 파이프라인이 실패했습니다. CHANGELOG 섹션과 빌드 로그를 확인하세요.'
        }
        always {
            archiveArtifacts artifacts: 'CHANGELOG.md', allowEmptyArchive: false
            archiveArtifacts artifacts: 'changelog-release-notes.txt', allowEmptyArchive: true
        }
    }
}
