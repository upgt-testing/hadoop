package org.apache.hadoop.hdfs.remoteProxies;

public interface CharacterEscapesInterface {
    int[] getEscapeCodesForAscii();
    com.fasterxml.jackson.core.SerializableString getEscapeSequence(int arg0);
    int[] standardAsciiEscapesForJSON();
}