FROM postgres:16.16-bookworm

RUN apt-get update \
    && apt-get install --no-install-recommends -y postgresql-16-pgtap \
    && rm -rf /var/lib/apt/lists/*
