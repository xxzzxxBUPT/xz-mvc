package com.xuzhi.xz.servlet;

import com.xuzhi.xz.annotation.*;
import com.xuzhi.xz.aop.annotation.XzAspect;
import com.xuzhi.xz.aop.annotation.XzPointcut;
import com.xuzhi.xz.aop.proxy.AbstractProxyFactory;
import com.xuzhi.xz.controller.OrderController;
import com.xuzhi.xz.service.OrderService;
import com.xuzhi.xz.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    List<String> classNames = new ArrayList<String>();
    Map<String, Object> beans = new HashMap<String, Object>();
    Map<String, Object> methodMapping = new HashMap<String, Object>();
    Map<String, Object> proxys = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.扫描所有的class文件，放入到list中存储起来
        doPackageScan("com.xuzhi.xz");

        //2.根据反射和注解，实例化需要的对象(Controller和Service对象)，放入map中
        doCreateBean();

        //根据反射和注解，扫描aspect bean，并根据pointcut创建代理对象
        doAopProxy();

        //3.处理依赖注入，只有controller里的字段需要注入
        doInjectBeans();



        //4.将请求url和controller的处理方法对应起来，放入map
        doHandleMapping();

    }

    public void doHandleMapping() {
        //遍历controller，提取里面有@XzRequestMapping注解的方法
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String key = entry.getKey();
            //判断是不是controller实例
            if (key.startsWith("/") || key.endsWith("controller")) {
                Object obj = entry.getValue();
                Class clazz = obj.getClass();
                XzRequestMapping classMapping = (XzRequestMapping) clazz.getAnnotation(XzRequestMapping.class);
                String classPath = classMapping.value();
                Method[] methods = clazz.getDeclaredMethods();

                for (Method method : methods) {
                    //如果有@XzRequestMapping注解，处理该方法
                    if (method.isAnnotationPresent(XzRequestMapping.class)) {
                        XzRequestMapping requestMapping = method.getAnnotation(XzRequestMapping.class);
                        String methodPath = requestMapping.value();
                        methodMapping.put(classPath + methodPath, method);
                    }
                }
            }
        }
    }



    public void doInjectBeans() {
        //@XzAutowired注解只在controller里的成员变量里出现
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith("/")) {
                //找到controller，以及所有的字段
                Class clazz = entry.getValue().getClass();
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    //如果是被注入的字段，找到被注入的实例以及set方法，set注入
                    if (field.isAnnotationPresent(XzAutowired.class)) {
                        XzAutowired autowired = field.getAnnotation(XzAutowired.class);
                        String injectName = autowired.value();
                        Object injectInstance = beans.get(injectName);

                        logger.info("注入的bean name 为"+ injectName);

                        if (proxys.containsKey(injectName)){
                            injectInstance = proxys.get(injectName);
                        }

                        try {
                            //通过invoke方法，将service实例注入到controller中
//                            setMethod = clazz.getDeclaredMethod(setMethodName, injectClass);
//                            setMethod.invoke(entry.getValue(), injectInstance);
                            field.setAccessible(true);
                            field.set(entry.getValue(), injectInstance);

                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else {
                        continue;
                    }
                }
            } else {
                continue;
            }
        }
    }

    public void doAopProxy(){
        for (String className : classNames){
            className = className.replace(".class", "");
            try {
                Class<?> clazz = Class.forName(className);
                //判断是否为切面
                if (clazz.isAnnotationPresent(XzAspect.class)){
                    Method[] methods = clazz.getDeclaredMethods();
                    //判断是否为切点
                    for (Method method : methods){
                        if (method.isAnnotationPresent(XzPointcut.class)){
                            XzPointcut xzPointcut = method.getAnnotation(XzPointcut.class);
                            String methodPath = xzPointcut.value();
                            String[] methodPathArr = methodPath.split(":");
                            String targetClassName = methodPathArr[0];
                            String targetMethodName = methodPathArr[1].replace("()","");
                            //创建被代理的对象
                            Object target = ReflectionUtil.newInstance(targetClassName);

                            //创建代理工厂
                            AbstractProxyFactory proxyFactory = (AbstractProxyFactory) ReflectionUtil.newInstance(clazz);

                            proxyFactory.setProxyMethodName(targetMethodName);
                            //创建代理对象
                            Object proxy = proxyFactory.createProxy(target);
                            //将原对象的类名和代理对象放入map中
                            if (proxy != null){
                                proxys.put(target.getClass().getSimpleName(), proxy);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        logger.info("proxys are" + proxys);
    }

    public void doCreateBean() {
        for (String className : classNames) {
            className = className.replace(".class", "");
            try {
                Class<?> clazz = Class.forName(className);
                //如果类上有@XzController注解,创建实例，放入map中
                if (clazz.isAnnotationPresent(XzController.class)) {
                    XzRequestMapping requestMapping = clazz.getAnnotation(XzRequestMapping.class);
                    String path = requestMapping.value();
                    OrderController instance = (OrderController) clazz.newInstance();
                    beans.put(path, instance);
                } else if (clazz.isAnnotationPresent(XzService.class)) {
                    XzService service = clazz.getAnnotation(XzService.class);
                    String key = service.value();
                    OrderService instance = (OrderService) clazz.newInstance();
                    beans.put(key, instance);
                } else {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
        logger.info("beans are" + beans);
    }

    public void doPackageScan(String basePackage) {
        URL url = this.getClass().getClassLoader().getResource("/" +
                basePackage.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        String[] strs = file.list();
        for (String str : strs) {
            File f = new File(url.getFile() + str);
            if (f.isDirectory()) {
                doPackageScan(basePackage + "." + str);
            } else {
                classNames.add(basePackage + "." + f.getName());
            }
        }
        logger.debug("classNames are" + classNames);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doPost(req, resp);
        String context = req.getContextPath();  //返回 /xz-mvc
        String uri = req.getRequestURI();   ///xz-mvc/xzzz/query
        String key = uri.replace(context, "");
        String url = "/" + uri.split("/")[2];   ///xzzz
        Method method = (Method) methodMapping.get(key);

        Object instance = beans.get(url);

        //获取到这个方法的参数列表，参数值数组长度与形参长度一样
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            //如果controller处理方法中有HttpServletRequest或者Response参数
            //就从doPost()方法中赋值过去
            if (ServletRequest.class.isAssignableFrom(parameters[i].getType())) {
                paramValues[i] = req;
            } else if (ServletResponse.class.isAssignableFrom(parameters[i].getType())) {
                paramValues[i] = resp;
            } else {
                //如果是简单值类型，进行赋值；如果有@RequestParam注解，需要用名字赋值
                String paramName = parameters[i].getName();
                if (parameters[i].isAnnotationPresent(XzRequestParam.class)) {
                    paramName = parameters[i].getAnnotation(XzRequestParam.class).value();
                }
                String parameterValue = req.getParameter(paramName);
                if (parameterValue != null) {
                    if (Integer.class.isAssignableFrom(parameters[i].getType())) {
                        paramValues[i] = Integer.parseInt(parameterValue);
                    } else if (Float.class.isAssignableFrom(parameters[i].getType())) {
                        paramValues[i] = Float.parseFloat(parameterValue);
                    } else if (String.class.isAssignableFrom(parameters[i].getType())) {
                        paramValues[i] = parameterValue;
                    }
                }
            }
        }

        try {
            method.invoke(instance, paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
}
