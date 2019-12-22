package com.mytask.spring.mvcframework.v2.servlet;

import com.mytask.spring.mvcframework.annotation.MyAutowired;
import com.mytask.spring.mvcframework.annotation.MyController;
import com.mytask.spring.mvcframework.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";

    private static final String PACKAGE_NAME = "scanPackage";

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String,Object> ioc = new HashMap<>();

    public MyDispatcherServlet(){
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        doLoadConfig(config.getInitParameter(LOCATION));

        doScanner(properties.getProperty(PACKAGE_NAME));

        doInstance();

    }

    private void doInstance() {
        if (classNames.isEmpty()){return;}
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object obj = clazz.newInstance();
                    ioc.put(beanName,obj);
                }else if(clazz.isAnnotationPresent(MyService.class)){

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            String fn = file.getName();
            if(file.isDirectory()){
                doScanner(scanPackage+"."+fn);
            }else {
                if(!fn.endsWith(".class")){continue;}
                String className = scanPackage + "." + fn.replace(".class","");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String location) {
        InputStream fis = null;
        try{
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            properties.load(fis);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (fis != null){
                try{
                   fis.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch();
    }

    private void doDispatch() {

    }
}
