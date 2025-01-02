package org.apache.hadoop.hdfs.protocol;

import java.util.List;

public interface LocatedBlocksJVMInterface {
    LocatedBlockJVMInterface get(int index);
    List<? extends LocatedBlockJVMInterface> getLocatedBlocks();
}
