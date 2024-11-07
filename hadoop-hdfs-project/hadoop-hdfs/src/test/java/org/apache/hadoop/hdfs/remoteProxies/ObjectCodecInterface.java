package org.apache.hadoop.hdfs.remoteProxies;

public interface ObjectCodecInterface {
    <T> java.util.Iterator<T> readValues(JsonParserInterface arg0, TypeReferenceInterface<T> arg1) throws java.io.IOException;
    JsonFactoryInterface getFactory();
    JsonParserInterface treeAsTokens(com.fasterxml.jackson.core.TreeNode arg0);
    <T> java.util.Iterator<T> readValues(JsonParserInterface arg0, ResolvedTypeInterface arg1) throws java.io.IOException;
    void writeTree(JsonGeneratorInterface arg0, com.fasterxml.jackson.core.TreeNode arg1) throws java.io.IOException;
    VersionInterface version();
    com.fasterxml.jackson.core.TreeNode createObjectNode();
    <T> T treeToValue(com.fasterxml.jackson.core.TreeNode arg0, java.lang.Class<T> arg1) throws com.fasterxml.jackson.core.JsonProcessingException;
    <T> T readValue(JsonParserInterface arg0, ResolvedTypeInterface arg1) throws java.io.IOException;
    <T> T readValue(JsonParserInterface arg0, java.lang.Class<T> arg1) throws java.io.IOException;
    com.fasterxml.jackson.core.TreeNode createArrayNode();
    void writeValue(JsonGeneratorInterface arg0, java.lang.Object arg1) throws java.io.IOException;
    <T> java.util.Iterator<T> readValues(JsonParserInterface arg0, java.lang.Class<T> arg1) throws java.io.IOException;
    JsonFactoryInterface getJsonFactory();
    <T> T readTree(JsonParserInterface arg0) throws java.io.IOException;
    com.fasterxml.jackson.core.TreeNode nullNode();
    <T> T readValue(JsonParserInterface arg0, TypeReferenceInterface<T> arg1) throws java.io.IOException;
    com.fasterxml.jackson.core.TreeNode missingNode();
}