package org.servantframework.web.router;

import cn.hutool.core.convert.Convert;
import com.google.gson.Gson;
import org.servantframework.web.HttpRequest;
import org.servantframework.web.HttpResponse;
import org.servantframework.web.annotation.Path;
import org.servantframework.web.annotation.Servant;
import org.servantframework.web.codec.HttpEncoder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpRouter {
    private final Map<String, Method> routes = new HashMap<>();
    private final Map<String, Object> instances = new HashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // 线程池

    public void scanAndRegister(String packageName) throws Exception {
        List<Class<?>> classes = getClasses(packageName);
        for (Class<?> clazz : classes) {
            addRoutesFromClass(clazz);
        }
    }

    private List<Class<?>> getClasses(String packageName) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
        List<File> dirs = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }

        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }

        return classes;
    }

    private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
        }
        return classes;
    }

    private void addRoutesFromClass(Class<?> clazz) throws Exception {
        if (clazz.isAnnotationPresent(Servant.class)) {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            instances.put(clazz.getName(), instance);
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Path.class)) {
                    Path path = method.getAnnotation(Path.class);
                    routes.put(path.value(), method);
                }
            }
        }
    }

    public void handle(HttpRequest request, HttpResponse response, Selector selector, SelectionKey key) {
        Method method = routes.get(request.getPath());
        if (method != null) {
            executorService.submit(() -> {
                try {
                    Object instance = instances.get(method.getDeclaringClass().getName());
                    Object[] args = resolveParameters(method, request);
                    Object result = method.invoke(instance, args);

                    if (result instanceof String) {
                        response.setStatusCode(200);
                        response.setReasonPhrase("OK");
                        response.addHeader("Content-Type", "text/plain");
                        response.setBody((String) result);
                    } else {
                        String jsonResponse = new Gson().toJson(result);
                        response.setStatusCode(200);
                        response.setReasonPhrase("OK");
                        response.addHeader("Content-Type", "application/json");
                        response.setBody(jsonResponse);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(500);
                    response.setReasonPhrase("Internal Server Error");
                    response.setBody("500 Internal Server Error");
                }
                key.attach(HttpEncoder.encode(response));
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        } else {
            response.setStatusCode(404);
            response.setReasonPhrase("Not Found");
            response.setBody("404 Not Found");
            key.attach(HttpEncoder.encode(response));
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }

    private Object[] resolveParameters(Method method, HttpRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getType() == HttpRequest.class) {
                args[i] = request;
            } else if (parameter.getType() == HttpResponse.class) {
                args[i] = new HttpResponse("HTTP/1.1", 200, "OK");
            } else {
                // 处理请求参数
                String paramName = parameter.getName();
                String paramValue = request.getParameters().get(paramName);
                args[i] = Convert.convert(parameter.getType(), paramValue);
            }
        }
        return args;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
