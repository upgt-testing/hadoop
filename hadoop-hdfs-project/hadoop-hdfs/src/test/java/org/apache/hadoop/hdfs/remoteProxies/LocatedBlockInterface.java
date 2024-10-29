package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.protocol.BlockType;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
//import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.fs.StorageType;

public interface LocatedBlockInterface {

    //Token<BlockTokenIdentifierInterface> getBlockToken();

    //void setBlockToken(Token<BlockTokenIdentifier> token);

    ExtendedBlock getBlock();

    DatanodeInfoWithStorageInterface[] getLocations();

    StorageType[] getStorageTypes();

    String[] getStorageIDs();

    void updateCachedStorageInfo();

    void moveProvidedToEnd(int activeLen);

    long getStartOffset();

    long getBlockSize();

    void setStartOffset(long value);

    void setCorrupt(boolean corrupt);

    boolean isCorrupt();

    void addCachedLoc(DatanodeInfo loc);

    DatanodeInfoInterface[] getCachedLocations();

    String toString();

    boolean isStriped();

    BlockType getBlockType();
}
