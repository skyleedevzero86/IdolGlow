import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy
import hudson.security.HudsonPrivateSecurityRealm
import jenkins.install.InstallState
import jenkins.model.Jenkins

def instance = Jenkins.get()
def adminId = System.getenv('JENKINS_ADMIN_ID')?.trim()
def adminPassword = System.getenv('JENKINS_ADMIN_PASSWORD')

if (!adminId || !adminPassword) {
    throw new IllegalStateException('JENKINS_ADMIN_ID and JENKINS_ADMIN_PASSWORD are required')
}

def securityRealm = new HudsonPrivateSecurityRealm(false)
def adminUser = securityRealm.getUser(adminId)
if (adminUser == null) {
    securityRealm.createAccount(adminId, adminPassword)
} else {
    adminUser.addProperty(HudsonPrivateSecurityRealm.Details.fromPlainPassword(adminPassword))
    adminUser.save()
}

instance.setSecurityRealm(securityRealm)

def authorizationStrategy = new FullControlOnceLoggedInAuthorizationStrategy()
authorizationStrategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(authorizationStrategy)

instance.setNumExecutors(1)
instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)

def githubToken = System.getenv('GITHUB_TOKEN')
if (githubToken != null && !githubToken.trim().isEmpty()) {
    def githubUsername = System.getenv('GITHUB_USERNAME') ?: 'git'
    def githubCredentialsId = System.getenv('GITHUB_CREDENTIALS_ID') ?: 'github-http-token'
    def provider = SystemCredentialsProvider.getInstance()
    def store = provider.getStore()
    def existingCredential = provider.credentials.find { it.id == githubCredentialsId }

    if (existingCredential != null) {
        store.removeCredentials(Domain.global(), existingCredential)
    }

    def credential = new UsernamePasswordCredentialsImpl(
        CredentialsScope.GLOBAL,
        githubCredentialsId,
        'IdolGlow Jenkins 파이프라인용 GitHub HTTP 토큰',
        githubUsername,
        githubToken
    )

    store.addCredentials(Domain.global(), credential)
}

instance.save()
