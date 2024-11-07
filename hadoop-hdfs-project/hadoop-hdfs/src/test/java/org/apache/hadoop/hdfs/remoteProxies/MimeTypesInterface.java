package org.apache.hadoop.hdfs.remoteProxies;

public interface MimeTypesInterface {
    java.lang.String normalizeMimeType(java.lang.String arg0);
    void setMimeMap(java.util.Map<java.lang.String, java.lang.String> arg0);
    java.lang.String getCharsetInferredFromContentType(java.lang.String arg0);
    java.lang.String getMimeByExtension(java.lang.String arg0);
    java.lang.String getDefaultMimeByExtension(java.lang.String arg0);
    java.lang.String getCharsetFromContentType(java.lang.String arg0);
    void addMimeMapping(java.lang.String arg0, java.lang.String arg1);
    java.util.Map<java.lang.String, java.lang.String> getAssumedEncodings();
    java.util.Map<java.lang.String, java.lang.String> getInferredEncodings();
    java.lang.String getCharsetAssumedFromContentType(java.lang.String arg0);
    java.util.Map<java.lang.String, java.lang.String> getMimeMap();
    java.lang.String getContentTypeWithoutCharset(java.lang.String arg0);
    java.util.Set<java.lang.String> getKnownMimeTypes();
    java.lang.String inferCharsetFromContentType(java.lang.String arg0);
}