package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ContainerInterface {
    /**

    Container id(String id);

    String getId();

    void setId(String id);

    Container launchTime(Date launchTime);

    Date getLaunchTime();

    void setLaunchTime(Date launchTime);

    Container ip(String ip);

    String getIp();

    void setIp(String ip);

    Container hostname(String hostname);

    String getHostname();

    void setHostname(String hostname);

    Container bareHost(String bareHost);

    String getBareHost();

    void setBareHost(String bareHost);

    Container state(ContainerState state);

    ContainerState getState();

    void setState(ContainerState state);

    Container componentInstanceName(String componentInstanceName);

    String getComponentInstanceName();

    void setComponentInstanceName(String componentInstanceName);

    Container resource(Resource resource);

    Resource getResource();

    void setResource(Resource resource);

    Container artifact(ArtifactInterface artifact);

    Artifact getArtifact();

    void setArtifact(Artifact artifact);

    Container privilegedContainer(Boolean privilegedContainer);

    Boolean getPrivilegedContainer();

    void setPrivilegedContainer(Boolean privilegedContainer);

    Map<String, List<Map<String, String>>> getExposedPorts();

    void setExposedPorts(Map<String, List<Map<String, String>>> ports);

    List<LocalizationStatus> getLocalizationStatuses();

    void setLocalizationStatuses(List<LocalizationStatus> statuses);

    Container localizationStatuses(List<LocalizationStatus> statuses);

    boolean equals(java.lang.Object o);

    int hashCode();

    String toString();
     **/
}
