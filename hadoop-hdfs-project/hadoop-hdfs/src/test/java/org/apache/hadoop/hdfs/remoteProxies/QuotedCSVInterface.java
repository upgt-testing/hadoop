package org.apache.hadoop.hdfs.remoteProxies;

public interface QuotedCSVInterface {
    void parsedValueAndParams(java.lang.StringBuffer arg0);
    java.lang.String unquote(java.lang.String arg0);
//    java.util.Spliterator<T> spliterator();
//    java.util.Iterator<T> iterator();
    java.lang.String toString();
    void parsedParam(java.lang.StringBuffer arg0, int arg1, int arg2, int arg3);
    java.util.Iterator<java.lang.String> iterator();
    void addValue(java.lang.String arg0);
    java.util.List<java.lang.String> getValues();
    int size();
    void parsedValue(java.lang.StringBuffer arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    boolean isEmpty();
}