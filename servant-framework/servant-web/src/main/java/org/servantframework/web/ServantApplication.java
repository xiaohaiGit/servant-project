package org.servantframework.web;

import org.servantframework.web.router.HttpRouter;
import org.servantframework.web.server.WebServer;

public class ServantApplication {
    public static void run(Class<?> primarySource, int port) {
        try {
            String packageName = primarySource.getPackage().getName();
            HttpRouter router = new HttpRouter();
            router.scanAndRegister(packageName);

            WebServer webServer = new WebServer(router, port);
            Runtime.getRuntime().addShutdownHook(new Thread(webServer::stop));
            webServer.start();
            webServer.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
