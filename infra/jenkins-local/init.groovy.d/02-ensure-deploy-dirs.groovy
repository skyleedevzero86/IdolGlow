import java.nio.file.Files
import java.nio.file.Paths

def roots = ['/var/jenkins_home/deployments', '/deployments']
def envs = ['dev', 'staging', 'prod']

roots.each { root ->
    envs.each { envName ->
        try {
            Files.createDirectories(Paths.get(root, envName))
        } catch (Exception ignored) {
            // Bind-mounted /deployments may be read-only on some hosts; deploy stage falls back.
        }
    }
}
