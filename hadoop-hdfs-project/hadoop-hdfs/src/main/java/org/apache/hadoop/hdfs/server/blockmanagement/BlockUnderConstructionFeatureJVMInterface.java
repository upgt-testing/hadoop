package org.apache.hadoop.hdfs.server.blockmanagement;

public interface BlockUnderConstructionFeatureJVMInterface {
    DatanodeStorageInfoJVMInterface[] getExpectedStorageLocations();
}
