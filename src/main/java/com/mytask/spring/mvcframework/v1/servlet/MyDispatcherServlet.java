package com.mytask.spring.mvcframework.v1.servlet;

import com.mytask.spring.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {

    //存储aplication.properties的配置内容
    private Properties properties = new Properties();

    //存放已扫描到类
    private List<String> classNames = new ArrayList<String>();

    //ioc容器
    private Map<String,Object> ioc = new HashMap<String,Object>();

    //handerMapping
    private Map<String, Method> handlerMapping = new HashMap<String,Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //执行
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Excetion Detail:" +Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        //找不到路径
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //第一个参数：方法所在的实例
        //第二个参数：调用时所需要的实参
        Map<String,String[]> params = req.getParameterMap();
        //设置参数对号入座
        setParamValue(req,resp,method);
        //通过反射拿到method所在class，拿到class之后还是拿到class的名称
        //再调用toLowerFirstCase获得beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});
    }

    private void setParamValue(HttpServletRequest req, HttpServletResponse resp, Method method) {
        //获取方法的形参列表
        Class<?> [] parameterTypes = method.getParameterTypes();
        //保存请求的url参数列表
        Map<String,String[]> parameterMap = req.getParameterMap();
        //保存赋值参数的位置
        Object [] paramValues = new Object[parameterTypes.length];
        //按根据参数位置动态赋值
        for (int i = 0; i < parameterTypes.length; i ++){
            Class parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            }else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            }else if (parameterType == String.class) {
                //提取方法中加了注解的参数
                Annotation[][] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length ; j++) {
                    for (Annotation a : pa[j]) {
                        if (a instanceof MyRequestParam) {
                            String paramName = ((MyRequestParam) a).value();
                            if (!"".equals(paramName.trim())) {
                                String value = Arrays.toString(parameterMap.get(paramName))
                                        .replaceAll("\\[|\\]","")
                                        .replaceAll("\\s",",");
                                paramValues[i] = value;
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类
        doScanner(properties.getProperty("scanPackage"));

        //3.初始化所有相关类的实例，并放入IOC容器中
        doInstance();

        //4.依赖注入
        doAutowired();

        //5.初始化handerMapping
        initHanderMapping();

        System.out.println("初始化完毕...");
    }

    //初始化handerMapping
    private void initHanderMapping() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry: ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            //判断是否控制器
            if (!clazz.isAnnotationPresent(MyController.class)){continue;}

            String baseUrl = "";//存放主访问路径

            //获取Controller的url配置
            if (clazz.isAnnotationPresent(MyRequestMapping.class)){
                baseUrl =  clazz.getAnnotation(MyRequestMapping.class).value();
            }

            //获取Method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method:methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)){continue;}
                String methodUrl = method.getAnnotation(MyRequestMapping.class).value();
                methodUrl = (baseUrl + methodUrl).replaceAll("/+","/");
                handlerMapping.put(methodUrl,method);
                System.out.println("RequestMapped " + methodUrl + "," + method);
            }
        }
    }

    //依赖注入
    private void doAutowired() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry: ioc.entrySet()) {
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields) {
                if(!field.isAnnotationPresent(MyAutowired.class)){ continue;}
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);//设置私有属性的访问权限
                try {
                    field.set(entry.getValue(),ioc.get(beanName));//执行注入动作
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //实例化加载到的类
    private void doInstance() {
        if(classNames.size() == 0){
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                //只识别有注解的类
                if (clazz.isAnnotationPresent(MyController.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object obj = clazz.newInstance();
                    ioc.put(beanName,obj);
                }else if(clazz.isAnnotationPresent(MyService.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    MyService service = clazz.getAnnotation(MyService.class);
                    if(!"".equals(service.value())){
                       beanName = service.value();
                    }
                    Object obj = clazz.newInstance();
                    ioc.put(beanName,obj);
                    //注入实现类
                    for (Class<?> c : clazz.getInterfaces()) {
                        if(ioc.containsKey(c.getName())){
                            throw new Exception("The beanName is exists!!");
                        }
                        ioc.put(c.getName(),obj);
                    }
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    //加载需要扫描的类
    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file:classPath.listFiles()) {
            String fn = file.getName();
            if (file.isDirectory()){
                doScanner(scanPackage + "." + fn);
            }else {
                if (!fn.endsWith(".class")){continue;}
                String className = scanPackage + "." + fn.replace(".class","");
                classNames.add(className);
            }
        }
    }

    //加载配置文件
    private void doLoadConfig(String contextConfig) {
        InputStream fis = null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(contextConfig);
            properties.load(fis);
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            if (fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return  String.valueOf(chars);
    }
}
