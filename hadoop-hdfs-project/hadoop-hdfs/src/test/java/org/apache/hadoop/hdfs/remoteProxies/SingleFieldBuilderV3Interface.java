package org.apache.hadoop.hdfs.remoteProxies;

public interface SingleFieldBuilderV3Interface<MType, BType, IType> {
    void onChanged();
    BType getBuilder();
    void dispose();
    IType getMessageOrBuilder();
    SingleFieldBuilderV3Interface<MType, BType, IType> clear();
    SingleFieldBuilderV3Interface<MType, BType, IType> mergeFrom(MType arg0);
    SingleFieldBuilderV3Interface<MType, BType, IType> setMessage(MType arg0);
    MType build();
    MType getMessage();
    void markDirty();
}