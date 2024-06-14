package org.servantframework.examples;

import org.servantframework.web.ServantApplication;

public class MyApplication {
    public static void main(String[] args) {
        ServantApplication.run(MyApplication.class, 8080);
    }
}
