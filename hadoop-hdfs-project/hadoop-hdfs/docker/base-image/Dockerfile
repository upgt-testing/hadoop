# Use a base image with Java installed
FROM ubuntu:bionic

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN echo APT::Install-Recommends "0"\; > /etc/apt/apt.conf.d/10disableextras
RUN echo APT::Install-Suggests "0"\; >>  /etc/apt/apt.conf.d/10disableextras

ENV DEBIAN_FRONTEND noninteractive
ENV DEBCONF_TERSE true

RUN apt-get -q update \
    && apt-get -q install -y --no-install-recommends \
        apt-utils \
        emacs \
        wget \
        unzip \
	tar \
	git \
	iputils-ping \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

#######
# OpenJDK 11 and Maven
#######
# hadolint ignore=DL3008
RUN apt-get -q update \
    && apt-get -q install -y --no-install-recommends openjdk-11-jdk libbcprov-java maven \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-arm64/

# Configure SSH
#RUN ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa \
#    && cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys \
#    && chmod 600 ~/.ssh/authorized_keys
