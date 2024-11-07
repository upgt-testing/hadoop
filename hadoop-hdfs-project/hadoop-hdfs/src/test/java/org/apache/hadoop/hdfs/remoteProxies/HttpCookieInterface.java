package org.apache.hadoop.hdfs.remoteProxies;

public interface HttpCookieInterface {
    boolean isHttpOnly();
    long getMaxAge();
    java.lang.String getRFC6265SetCookie();
    java.lang.String getRFC2965SetCookie();
    java.lang.String getCommentWithAttributes(java.lang.String arg0, boolean arg1, org.eclipse.jetty.http.HttpCookie.SameSite arg2);
    int getVersion();
    java.lang.String getCommentWithoutAttributes(java.lang.String arg0);
    org.eclipse.jetty.http.HttpCookie.SameSite getSameSiteFromComment(java.lang.String arg0);
    java.lang.String getComment();
    java.lang.String getDomain();
    boolean isSecure();
    org.eclipse.jetty.http.HttpCookie.SameSite getSameSiteDefault(javax.servlet.ServletContext arg0);
    boolean isHttpOnlyInComment(java.lang.String arg0);
    java.lang.String getValue();
    java.lang.String getSetCookie(org.eclipse.jetty.http.CookieCompliance arg0);
    org.eclipse.jetty.http.HttpCookie.SameSite getSameSite();
    boolean isQuoteNeededForCookie(java.lang.String arg0);
    java.lang.String getPath();
    boolean isExpired(long arg0);
    java.lang.String getName();
    java.lang.String asString();
    void quoteOnlyOrAppend(java.lang.StringBuilder arg0, java.lang.String arg1, boolean arg2);
}