import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

def botToken = System.getenv('TELEGRAM_BOT_TOKEN')?.trim()
def chatId = System.getenv('TELEGRAM_CHAT_ID')?.trim()

if (!botToken || !chatId) {
    println '[IdolGlow] TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID 미설정 → Telegram Credentials 건너뜀'
    return
}

def tokenCredentialId = System.getenv('TELEGRAM_CREDENTIALS_TOKEN_ID')?.trim() ?: 'telegram-bot-token'
def chatCredentialId = System.getenv('TELEGRAM_CREDENTIALS_CHAT_ID')?.trim() ?: 'telegram-chat-id'
def provider = SystemCredentialsProvider.getInstance()
def store = provider.getStore()

def upsertSecret = { String id, String description, String secret ->
    def existing = provider.credentials.find { it.id == id }
    if (existing != null) {
        store.removeCredentials(Domain.global(), existing)
    }

    def credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        id,
        description,
        hudson.util.Secret.fromString(secret)
    )

    store.addCredentials(Domain.global(), credential)
}

upsertSecret(tokenCredentialId, 'IdolGlow Jenkins Telegram Bot Token', botToken)
upsertSecret(chatCredentialId, 'IdolGlow Jenkins Telegram Chat ID', chatId)

println "[IdolGlow] Telegram Credentials 등록 (${tokenCredentialId}, ${chatCredentialId})"
