package org.apache.hadoop.hdfs.remoteProxies;

import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.http.HttpServlet;
import java.net.InetSocketAddress;
import java.util.*;
import java.io.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.webapp.WebAppContext;

public interface HttpServer2Interface {

    void addContext(ServletContextHandler ctxt, boolean isFiltered);

    void setAttribute(String name, Object value);

    void addJerseyResourcePackage(String packageName, String pathSpec);

    void addJerseyResourcePackage(String packageName, String pathSpec, Map<String, String> params);

    void addServlet(String name, String pathSpec, Class<? extends HttpServlet> clazz);

    void addInternalServlet(String name, String pathSpec, Class<? extends HttpServlet> clazz);

    void addInternalServlet(String name, String pathSpec, Class<? extends HttpServlet> clazz, boolean requireAuth);

    void addInternalServlet(String name, String pathSpec, Class<? extends HttpServlet> clazz, Map<String, String> params);

    void addHandlerAtFront(Handler handler);

    void addHandlerAtEnd(Handler handler);

    void addFilter(String name, String classname, Map<String, String> parameters);

    void addGlobalFilter(String name, String classname, Map<String, String> parameters);

    Object getAttribute(String name);

    WebAppContext getWebAppContext();

    int getPort();

    InetSocketAddress getConnectorAddress(int index);

    void setThreads(int min, int max);

    void start();

    void stop();

    void join();

    boolean isAlive();

    String toString();
}
