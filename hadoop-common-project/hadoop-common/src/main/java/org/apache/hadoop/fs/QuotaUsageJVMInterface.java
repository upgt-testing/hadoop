package org.apache.hadoop.fs;

public interface QuotaUsageJVMInterface {

    int hashCode();

    boolean isTypeConsumedAvailable();

    boolean equals(java.lang.Object arg0);

    boolean isTypeQuotaSet();

    java.lang.String toString();

    long getSpaceConsumed();

    java.lang.String toString(boolean arg0, boolean arg1, java.util.List<org.apache.hadoop.fs.StorageType> arg2);

    long getTypeQuota_bridge(java.lang.Object arg0);

    long getQuota();

    long getSpaceQuota();

    long getFileAndDirectoryCount();

    java.lang.String toString(boolean arg0);

    long getTypeConsumed_bridge(java.lang.Object arg0);
}
