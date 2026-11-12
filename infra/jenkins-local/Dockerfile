ARG JENKINS_BASE_IMAGE=jenkins/jenkins:jdk21
FROM ${JENKINS_BASE_IMAGE}

ENV COREPACK_HOME=/opt/corepack

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl git gnupg openssh-client gosu \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && npm install -g corepack \
    && corepack enable \
    && corepack install --global pnpm@9.15.4 \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && chmod a+r /etc/apt/keyrings/docker.gpg \
    && . /etc/os-release \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list \
    && curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" > /etc/apt/sources.list.d/adoptium.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin temurin-25-jdk \
    && apt-get purge -y --auto-remove gnupg \
    && npm cache clean --force \
    && rm -rf /root/.cache /tmp/* \
    && rm -rf /var/lib/apt/lists/*

ARG KUBECTL_VERSION=v1.31.4
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && arch="$(uname -m)" \
    && curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${arch}.zip" -o /tmp/awscliv2.zip \
    && unzip -q /tmp/awscliv2.zip -d /tmp \
    && /tmp/aws/install \
    && curl -fsSL "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/$(dpkg --print-architecture)/kubectl" -o /usr/local/bin/kubectl \
    && chmod 755 /usr/local/bin/kubectl \
    && rm -rf /tmp/aws /tmp/awscliv2.zip /var/lib/apt/lists/*

RUN mkdir -p /var/jenkins_home/.gradle \
        /var/jenkins_home/deployments/dev /var/jenkins_home/deployments/staging /var/jenkins_home/deployments/prod \
        /deployments/dev /deployments/staging /deployments/prod \
    && chown -R jenkins:jenkins /var/jenkins_home/.gradle /var/jenkins_home/deployments /deployments \
    && chmod -R 775 /var/jenkins_home/deployments /deployments

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/
COPY docker-entrypoint.sh /usr/local/bin/idolglow-jenkins-entrypoint

RUN chmod 755 /usr/local/bin/idolglow-jenkins-entrypoint

ENTRYPOINT ["/usr/local/bin/idolglow-jenkins-entrypoint"]
