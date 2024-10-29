package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.service.api.records.Resource;

public interface ResourceUsageInterface {

    ResourceInterface getUsed();

    ResourceInterface getUsed(String label);

    //void incUsed(String label, Resource res);

    //void incUsed(Resource res);

    //void decUsed(Resource res);

    //void decUsed(String label, Resource res);

    //void setUsed(Resource res);

    void copyAllUsed(AbstractResourceUsageInterface other);

    //void setUsed(String label, Resource res);

    ResourceInterface getPending();

    ResourceInterface getPending(String label);

    //void incPending(String label, Resource res);

    //void incPending(Resource res);

    //void decPending(Resource res);

    //void decPending(String label, Resource res);

    //void setPending(Resource res);

    //void setPending(String label, Resource res);

    ResourceInterface getReserved();

    ResourceInterface getReserved(String label);

    //void incReserved(String label, Resource res);

    //void incReserved(Resource res);

    //void decReserved(Resource res);

    //void decReserved(String label, Resource res);

    //void setReserved(Resource res);

    //void setReserved(String label, Resource res);

    ResourceInterface getAMUsed();

    ResourceInterface getAMUsed(String label);

    //void incAMUsed(String label, Resource res);

    //void incAMUsed(Resource res);

    //void decAMUsed(Resource res);

    //void decAMUsed(String label, Resource res);

    //void setAMUsed(Resource res);

/**
    //void setAMUsed(String label, Resource res);

    Resource getAllPending();

    Resource getAllUsed();

    Resource getAllReserved();

    Resource getCachedUsed();

    Resource getCachedUsed(String label);

    Resource getCachedPending();

    Resource getCachedPending(String label);

    void setCachedUsed(String label, Resource res);

    void setCachedUsed(Resource res);

    void setCachedPending(String label, Resource res);

    void setCachedPending(Resource res);

    Resource getAMLimit();

    Resource getAMLimit(String label);

    void incAMLimit(String label, Resource res);

    void incAMLimit(Resource res);

    void decAMLimit(Resource res);

    void decAMLimit(String label, Resource res);

    void setAMLimit(Resource res);

    void setAMLimit(String label, Resource res);

    Resource getUserAMLimit();

    Resource getUserAMLimit(String label);

    void setUserAMLimit(Resource res);

    void setUserAMLimit(String label, Resource res);

    Resource getCachedDemand(String label);

 **/
}
