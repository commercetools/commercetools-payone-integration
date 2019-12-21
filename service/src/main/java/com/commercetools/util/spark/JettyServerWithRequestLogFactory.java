package com.commercetools.util.spark;

import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import spark.embeddedserver.jetty.JettyServerFactory;

public class JettyServerWithRequestLogFactory implements JettyServerFactory {


    private final RequestLog requestLog;

    public JettyServerWithRequestLogFactory(final RequestLog requestLog) {
        this.requestLog = requestLog;
    }

    /**
     * Creates a Jetty server.
     *
     * @param maxThreads          maxThreads
     * @param minThreads          minThreads
     * @param threadTimeoutMillis threadTimeoutMillis
     * @return a new jetty server instance
     */
    @Override
    public Server create(int maxThreads, int minThreads, int threadTimeoutMillis) {
        Server server;

        if (maxThreads > 0) {
            int min = (minThreads > 0) ? minThreads : 8;
            int idleTimeout = (threadTimeoutMillis > 0) ? threadTimeoutMillis : 60000;

            server = new Server(new QueuedThreadPool(maxThreads, min, idleTimeout));
        } else {
            server = new Server();
        }

        server.setRequestLog(requestLog);
        return server;
    }

    @Override
    public Server create(ThreadPool threadPool) {
        final Server server = threadPool != null ? new Server(threadPool) : new Server();
        server.setRequestLog(requestLog);
        return server;
    }
}
