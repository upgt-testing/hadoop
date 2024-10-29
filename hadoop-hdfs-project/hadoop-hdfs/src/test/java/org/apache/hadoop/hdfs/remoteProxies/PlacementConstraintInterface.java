package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface PlacementConstraintInterface {

    PlacementConstraintInterface name(String name);

    String getName();

    void setName(String name);

    //PlacementConstraintInterface type(PlacementType type);

    //PlacementType getType();

    //void setType(PlacementType type);

    //PlacementConstraintInterface scope(PlacementScope scope);

    //PlacementScope getScope();

    //void setScope(PlacementScope scope);

    PlacementConstraintInterface targetTags(List<String> targetTags);

    List<String> getTargetTags();

    void setTargetTags(List<String> targetTags);

    PlacementConstraintInterface nodeAttributes(Map<String, List<String>> nodeAttributes);

    Map<String, List<String>> getNodeAttributes();

    void setNodeAttributes(Map<String, List<String>> nodeAttributes);

    PlacementConstraintInterface nodePartitions(List<String> nodePartitions);

    List<String> getNodePartitions();

    void setNodePartitions(List<String> nodePartitions);

    PlacementConstraintInterface minCardinality(Long minCardinality);

    Long getMinCardinality();

    void setMinCardinality(Long minCardinality);

    PlacementConstraintInterface maxCardinality(Long maxCardinality);

    Long getMaxCardinality();

    void setMaxCardinality(Long maxCardinality);

    boolean equals(java.lang.Object o);

    int hashCode();

    String toString();
}
