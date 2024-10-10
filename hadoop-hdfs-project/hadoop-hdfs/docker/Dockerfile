FROM shuaiwang516/hadoop-base:latest

ARG HADOOP_BUILD_VERSION=3.3.5

# print the version for debugging
RUN echo ${HADOOP_BUILD_VERSION}

# Set environment variables
ENV HADOOP_VERSION=$HADOOP_BUILD_VERSION
ENV HADOOP_HOME=/opt/hadoop
ENV HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
ENV PATH=$PATH:$HADOOP_HOME/bin

# Download and install Hadoop
RUN wget -q https://dlcdn.apache.org/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz && \
    tar -xzf hadoop-$HADOOP_VERSION.tar.gz -C /opt && \
    mv /opt/hadoop-$HADOOP_VERSION /opt/hadoop && \
    rm hadoop-$HADOOP_VERSION.tar.gz

# Add Hadoop configuration files
COPY core-site.xml $HADOOP_CONF_DIR/core-site.xml
COPY hdfs-site.xml $HADOOP_CONF_DIR/raw-hdfs-site.xml
COPY hdfs-site.xml $HADOOP_CONF_DIR/hdfs-site.xml


# Expose necessary ports
EXPOSE 50070 8020 50010 50075 50020 9000 9870 9090


COPY modify_hdfs_site.sh /modify_hdfs_site.sh
COPY modify_hdfs_env.sh /modify_hdfs_env.sh
COPY start_datanode.sh /start_datanode.sh
COPY start_namenode.sh /start_namenode.sh
COPY loop.sh /loop.sh


#ENV TINI_VERSION v0.19.0
#ADD https://github.com/krallin/tini/releases/download/v0.19.0/tini /tini
#RUN chmod +x /tini
#ENTRYPOINT ["/tini", "--"]

# run the loop script with bash
#CMD ["bash", "-c", "/loop.sh"]

# Start the Hadoop services
# CMD service ssh start && $HADOOP_HOME/sbin/start-dfs.sh && tail -f /dev/null

# Start HDFS
#CMD ["bash", "-c", "/opt/hadoop/bin/hdfs namenode -format && /opt/hadoop/bin/hdfs namenode"]
# CMD ["bash", "-c", "/opt/hadoop/bin/hdfs datanode"]
