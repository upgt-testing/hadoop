package org.apache.hadoop.hdfs.remoteProxies;

import java.net.ContentHandler;
import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.util.XMLUtils;
//import org.apache.hadoop.mapred.nativetask.buffer.DataOutputStream;

public interface FSEditLogOpInterface {

    long getTransactionId();

    String getTransactionIdStr();

    boolean hasTransactionId();

    void setTransactionId(long txid);

    boolean hasRpcIds();

    byte[] getClientId();

    void setRpcClientId(byte[] clientId);

    int getCallId();

    void setRpcCallId(int callId);

    void writeFields(DataOutputStreamInterface out);

    //void writeFields(DataOutputStream out, int logVersion);

    void outputToXml(ContentHandler contentHandler);

    void decodeXml(XMLUtils.Stanza st);
}
