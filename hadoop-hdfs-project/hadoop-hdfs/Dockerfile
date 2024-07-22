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
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

#######
# OpenJDK 8 and 11
#######
# hadolint ignore=DL3008
RUN apt-get -q update \
    && apt-get -q install -y --no-install-recommends openjdk-8-jdk libbcprov-java \
    && apt-get -q install -y --no-install-recommends openjdk-11-jdk libbcprov-java \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV HADOOP_VERSION 3.3.5
ENV HADOOP_HOME /opt/hadoop
ENV HADOOP_CONF_DIR $HADOOP_HOME/etc/hadoop
ENV PATH $PATH:$HADOOP_HOME/bin

# Download and install Hadoop
RUN wget -q https://dlcdn.apache.org/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz && \
    tar -xzf hadoop-$HADOOP_VERSION.tar.gz -C /opt && \
    mv /opt/hadoop-$HADOOP_VERSION /opt/hadoop && \
    rm hadoop-$HADOOP_VERSION.tar.gz

# Configure SSH
#RUN ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa \
#    && cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys \
#    && chmod 600 ~/.ssh/authorized_keys

# Add Hadoop configuration files
COPY core-site.xml $HADOOP_CONF_DIR/core-site.xml
COPY hdfs-site.xml $HADOOP_CONF_DIR/hdfs-site.xml

# Format the HDFS namenode (conditionally)
RUN if [ "$NAMENODE_ID" = "0" ]; then $HADOOP_HOME/bin/hdfs namenode -format; fi

# Expose necessary ports
EXPOSE 50070 8020 50010 50075 50020 9000 9870

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-arm64/


# Start the Hadoop services
# CMD service ssh start && $HADOOP_HOME/sbin/start-dfs.sh && tail -f /dev/null

# Start HDFS
#CMD ["bash", "-c", "/opt/hadoop/bin/hdfs namenode -format && /opt/hadoop/bin/hdfs namenode"]
# CMD ["bash", "-c", "/opt/hadoop/bin/hdfs datanode"]
