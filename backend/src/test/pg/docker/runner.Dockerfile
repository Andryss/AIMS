FROM postgres:16.16-bookworm

RUN apt-get update \
    && apt-get install --no-install-recommends -y \
        ca-certificates curl openjdk-17-jre-headless perl \
        libtap-parser-sourcehandler-pgtap-perl python3 python3-venv \
    && python3 -m venv /opt/test-tools \
    && /opt/test-tools/bin/pip install --no-cache-dir pytest==8.4.2 allure-pytest==2.15.0 \
    && curl --fail --location --retry 3 \
        https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline/2.46.1/allure-commandline-2.46.1.tgz \
        | tar -xz -C /opt \
    && ln -s /opt/allure-2.46.1/bin/allure /usr/local/bin/allure \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="/opt/test-tools/bin:$PATH"

WORKDIR /work/pg
