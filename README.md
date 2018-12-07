# springcloudpro
spring cloud pro
add readme
------------------------------------------------------------------------------------------------------------------------
# SpringCloud组件：搭建Eureka服务注册中心：
添加spring-cloud-starter-netflix-eureka-server依赖后，我们就来看看怎么开启Eureka Server服务。开启Eureka的注册中心服务端
比较简单，只需要修改注意两个地方。
第一个地方是在入口类上添加启用Eureka Server的注解@EnableEurekaServer
第二个地方是application.yml/application.properties文件内添加配置基本信息：
    # 服务名称
    spring:
      application:
        name: catpp-spring-cloud-eureka
    # 服务端口号
    server:
      port: 10000

    #Eureka 相关配置
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:${server.port}/eureka/
        # 是否从其他的服务中心同步服务列表
        fetch-registry: false
        # 是否把自己作为服务注册到其他服务注册中心
        register-with-eureka: false

上面的步骤我们已经把Eureka服务端所需要的依赖以及配置进行了集成，接下来我们来运行测试看下效果，Eureka给我们提供了一个漂亮
的管理界面，这样我们就可以通过管理界面来查看注册的服务列表以及服务状态等信息。

测试步骤：
通过Application方式进行启动Eureka Server
在本地浏览器访问http://localhost:8086，8086端口号是我们在application.yml配置文件内设置的server.port的值。
成功访问到Eureka Server管理界面

------------------------------------------------------------------------------------------------------------------------
# SpringCloud组件：将微服务提供者注册到Eureka服务中心：
    <!--Eureka Client 依赖-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    # 服务名称
    spring:
      application:
        name: hengboy-spring-cloud-eureka-provider

    # 服务提供者端口号
    server:
      port: 20000

    # 配置Eureka Server 信息
    eureka:
      client:
        service-url:
          # Eureka Server服务注册中心地址
          defaultZone: http://localhost:10000/eureka/

## 在服务注册的过程中，SpringCloud Eureka为每一个服务节点都提供默认且唯一的实例编号(InstanceId)
    实例编号默认值：${spring.cloud.client.ipAddress}:${spring.application.name}:${spring.application.instance_id:${server.port}}
         10.200.78.75:catpp-spring-cloud-eureka-provider:8087

## 自定义InstanceId：
    # 配置Eureka Server 信息
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:10000/eureka/
      # 自定义实例编号
      instance:
        instance-id: ${spring.application.name}:${server.port}:@project.version@
`@project.version@源码的版本号我们是采用了获取pom.xml配置文件内设置的version来设置的值，通过@xxx@的方式就可以得到maven的
一些相关配置信息来直接使用