package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
//import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.io.Text;
//import org.apache.hadoop.mapred.nativetask.buffer.DataOutputStream;
import org.apache.hadoop.fs.Path;

public interface CredentialsInterface {

    Token<? extends TokenIdentifier> getToken(Text alias);

    void addToken(Text alias, Token<? extends TokenIdentifier> t);

    Collection<Token<? extends TokenIdentifier>> getAllTokens();

    Map<Text, Token<? extends TokenIdentifier>> getTokenMap();

    int numberOfTokens();

    byte[] getSecretKey(Text alias);

    int numberOfSecretKeys();

    void addSecretKey(Text alias, byte[] key);

    void removeSecretKey(Text alias);

    List<Text> getAllSecretKeys();

    Map<Text, byte[]> getSecretKeyMap();

    void readTokenStorageStream(DataInputStreamInterface in);

    void writeTokenStorageToStream(DataOutputStream os);

    void writeTokenStorageToStream(DataOutputStream os, Credentials.SerializedFormat format);

    void writeTokenStorageFile(PathInterface filename, Configuration conf);

    //void writeTokenStorageFile(Path filename, Configuration conf, SerializedFormat format);

    void write(DataOutput out);

    void readFields(DataInput in);

    void addAll(Credentials other);

    void mergeAll(Credentials other);
}
