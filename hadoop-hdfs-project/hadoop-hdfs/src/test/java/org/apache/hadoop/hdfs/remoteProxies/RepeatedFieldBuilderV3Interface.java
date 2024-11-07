package org.apache.hadoop.hdfs.remoteProxies;

public interface RepeatedFieldBuilderV3Interface<MType, BType, IType> {
    MType getMessage(int arg0);
    void markDirty();
    RepeatedFieldBuilderV3Interface<MType, BType, IType> addMessage(int arg0, MType arg1);
    java.util.List<MType> getMessageList();
    java.util.List<BType> getBuilderList();
    BType addBuilder(MType arg0);
    IType getMessageOrBuilder(int arg0);
    boolean isEmpty();
    java.util.List<MType> build();
    void ensureMutableMessageList();
    int getCount();
    RepeatedFieldBuilderV3Interface<MType, BType, IType> setMessage(int arg0, MType arg1);
    void ensureBuilders();
    MType getMessage(int arg0, boolean arg1);
    RepeatedFieldBuilderV3Interface<MType, BType, IType> addAllMessages(java.lang.Iterable<? extends MType> arg0);
    BType getBuilder(int arg0);
    void incrementModCounts();
    java.util.List<IType> getMessageOrBuilderList();
    void dispose();
    void onChanged();
    RepeatedFieldBuilderV3Interface<MType, BType, IType> addMessage(MType arg0);
    BType addBuilder(int arg0, MType arg1);
    void clear();
    void remove(int arg0);
}