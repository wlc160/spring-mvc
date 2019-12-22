# spring-mvc

了解spring-mvc基本设计思路，简单实现spring-mvc框架；

设计思路：

（1）配置阶段
   
   ①配置web.xml，配置DispatcherServlet类路径
   
   ②设定init-param初始化配置文件路径
   
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>application.properties</param-value>
        </init-param>
   
   ③设定url-pattern
       
        <servlet-mapping>
           <servlet-name>mymvc</servlet-name>
           <url-pattern>/*</url-pattern>
        </servlet-mapping>
    
   ④配置Annotation，如@Controller、@Autowried、@RequestMapping等

（2）初始化阶段

   ①调用init方法（加载配置文件）
   
   ②IOC容器初始化（数据类型：Map<String,Object>）
   
   ③扫描相关的类
   
   ④创建实例化并保存至容器（通过反射机制将类实例化放入IOC容器中）
   
   ⑤进行DI操作（扫描IOC容器中的实例，给没有赋值的属性自动赋值）
   
   ⑥初始化HandlerMapping（将一个URL和一个Method进行一对一的关联映射Map<String,Method>）
   
（3）运行阶段

   ①调用doGet()/doPost()（Web容器调用doGet/doPost方法，获得Request/Response对象）
   
   ②匹配HandlerMapping（从request对象中获得用户输入的url，找到其对应的method）
   
   ③反射调用method.invoke()（利用反射调用方法返回结果）
   
   ④response.getWrite().write()（将返回结果返回给浏览器）