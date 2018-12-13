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
    实例编号默认值：
    ${spring.cloud.client.ipAddress}:${spring.application.name}:${spring.application.instance_id:${server.port}}
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


------------------------------------------------------------------------------------------------------------------------
# Eureka服务注册方式：
默认采用IP Address方式注册
    点击服务注册列表中的服务名称，页面跳转到：http://10.200.78.75:8087/actuator/info
## 如何使用主机方式注册？
    # 配置Eureka Server 信息
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:10000/eureka/
      # 自定义实例编号
      instance:
        instance-id: ${spring.application.name}:${server.port}:@project.version@
        # 配置使用主机名注册服务
        hostname: node1

    点击服务注册列表中的服务名称，页面跳转到：http://node1:8087/actuator/info

## 配置优先使用IP
    instance:
        ...
        prefer-ip-address: true

## 配置使用指定IP注册
    instance:
        ...
        ip-address: 127.0.0.1


------------------------------------------------------------------------------------------------------------------------
# Eureka服务注册方式流程分析：
## 第一步：实例化EurekaInstanceConfigBean配置实体：

    在项目启动时由于依赖spring-cloud-starter-netflix-eureka-client内通过配置spring.factories文件来让项目启动时自动加载并
    实例化org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration配置类，EurekaClientAutoConfiguration内
    会自动实例化EurekaInstanceConfigBean并且自动绑定eureka.instance开头的配置信息（具体为什么会自动映射可以去了解下
    @ConfigurationProperties注解作用），部分源码如下所示：
        public class EurekaClientAutoConfiguration {
            //省略部分源码
            @Bean
            @ConditionalOnMissingBean(value = EurekaInstanceConfig.class, search        = SearchStrategy.CURRENT)
            public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils,
                ManagementMetadataProvider managementMetadataProvider) {
              //省略部分源码
              // 传递
              EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);
              // 省略部分源码
            }
            //省略部分源码
        }

    EurekaClientAutoConfiguration#eurekaInstanceConfigBean方法只有满足
    @ConditionalOnMissingBean(value = EurekaInstanceConfig.class, search = SearchStrategy.CURRENT)表达式后才会去实例化，
    并且把实例化对象放入到IOC容器内容，BeanId为eurekaInstanceConfigBean，也就是方法的名称。
    在EurekaClientAutoConfiguration#eurekaInstanceConfigBean方法中有这么一行代码我们可以进行下一步的分析
        // 通过有参构造函数实例化EurekaInstanceConfigBean配置实体
        EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);

    通过调用EurekaInstanceConfigBean(InetUtils inetUtils)构造函数来进行实例化EurekaInstanceConfigBean对象，在这个构造函
    数内也有一些实例化的工作，源码如下：
        public EurekaInstanceConfigBean(InetUtils inetUtils) {
            this.inetUtils = inetUtils;
            this.hostInfo = this.inetUtils.findFirstNonLoopbackHostInfo();
            this.ipAddress = this.hostInfo.getIpAddress();
            this.hostname = this.hostInfo.getHostname();
        }

## 第二步：InetUtils#findFirstNonLoopbackHostInfo获取主机基本信息：

    在构造函数EurekaInstanceConfigBean(InetUtils inetUtils)源码实现内hostInfo主机信息通过了
    InetUtils#findFirstNonLoopbackHostInfo方法来进行实例化，我们来看看这个方法的具体实现逻辑，它会自动读取系统网卡列表然
    再进行循环遍历查询正在UP状态的网卡信息，如果没有查询到网卡信息，则使用默认的HostName、IpAddress配置信息，源码如下所
    示：
        public HostInfo findFirstNonLoopbackHostInfo() {
            InetAddress address = findFirstNonLoopbackAddress();
            if (address != null) {
                return convertAddress(address);
            }
            HostInfo hostInfo = new HostInfo();
            hostInfo.setHostname(this.properties.getDefaultHostname());
            hostInfo.setIpAddress(this.properties.getDefaultIpAddress());
            return hostInfo;
        }

        public InetAddress findFirstNonLoopbackAddress() {
            InetAddress result = null;
            try {
                int lowest = Integer.MAX_VALUE;
                for (Enumeration<NetworkInterface> nics = NetworkInterface
                        .getNetworkInterfaces(); nics.hasMoreElements();) {
                    NetworkInterface ifc = nics.nextElement();
                    if (ifc.isUp()) {
                        log.trace("Testing interface: " + ifc.getDisplayName());
                        if (ifc.getIndex() < lowest || result == null) {
                            lowest = ifc.getIndex();
                        }
                        else if (result != null) {
                            continue;
                        }

                        // @formatter:off
                        if (!ignoreInterface(ifc.getDisplayName())) {
                            for (Enumeration<InetAddress> addrs = ifc
                                    .getInetAddresses(); addrs.hasMoreElements();) {
                                InetAddress address = addrs.nextElement();
                                if (address instanceof Inet4Address
                                        && !address.isLoopbackAddress()
                                        && isPreferredAddress(address)) {
                                    log.trace("Found non-loopback interface: "
                                            + ifc.getDisplayName());
                                    result = address;
                                }
                            }
                        }
                        // @formatter:on
                    }
                }
            }
            catch (IOException ex) {
                log.error("Cannot get first non-loopback address", ex);
            }

            if (result != null) {
                return result;
            }

            try {
                return InetAddress.getLocalHost();
            }
            catch (UnknownHostException e) {
                log.warn("Unable to retrieve localhost");
            }

            return null;
        }

    默认的HostName、IpAddress属性配置信息在InetUtilsProperties配置实体类内，如果不进行设置则直接使用默认值，如果你想更换
    默认值，那么你可以在application.yml配置文件内通过设置
    spring.cloud.inetutils.defaultHostname、spring.cloud.inetutils.defaultIpAddress进行修改默认值，源码如下所示：
        public class InetUtilsProperties {
            public static final String PREFIX = "spring.cloud.inetutils";

            /**
             * The default hostname. Used in case of errors.
             */
            private String defaultHostname = "localhost";

            /**
             * The default ipaddress. Used in case of errors.
             */
            private String defaultIpAddress = "127.0.0.1";
        }

## 第三步：EurekaInstanceConfigBean#getHostName方法实现：

    getHostName是一个Override的方法，继承于com.netflix.appinfo.EurekaInstanceConfig接口，该方法有个boolean类型的参数
    refresh来判断是否需要刷新重新获取主机网络基本信息，当传递refresh=false并且在application.yml配置文件内并没有进行手动
    设置eureka.instance.hostname以及eureka.instance.ip-address参数则会根据eureka.instance.prefer-ip-address设置的值进行
    返回信息，源码如下所示：
        @Override
        public String getHostName(boolean refresh) {
            if (refresh && !this.hostInfo.override) {
                this.ipAddress = this.hostInfo.getIpAddress();
                this.hostname = this.hostInfo.getHostname();
            }
            return this.preferIpAddress ? this.ipAddress : this.hostname;
        }

## 默认注册方式源码分析
    由于在实例化EurekaInstanceConfigBean配置实体类时，构造函数进行了获取第一个非回环主机信息，默认的hostName以及
    ipAddress参数则是会直接使用InetUtils#findFirstNonLoopbackHostInfo方法返回的相对应的值。

## IP优先注册方式源码分析
    EurekaInstanceConfigBean#getHostName方法直接调用本类重载方法getHostName(boolean refresh)并且传递参数为false，根据第
    三步源码我们就可以看到：
        return this.preferIpAddress ? this.ipAddress : this.hostname;
    如果eureka.instance.prefer-ip-address参数设置了true就会返回eureka.instance.ip-address的值，这样我们就可以从中明白为
    什么主动设置eureka.instance.ip-address参数后需要同时设置eureka.instance.prefer-ip-address参数才可以生效。

## 指定IP、HostName源码分析
    我们通过application.yml配置文件进行设置eureka.instance.hostname以及eureka.instance.ip-address后会直接替换原默认值，
    在EurekaInstanceConfigBean#getHostName中也是返回的this.hostname、this.ipAddress所以在这里设置后会直接生效作为返回的
    配置值。

## 总结：
    通过源码进行分析服务注册方式执行流程，这样在以后进行配置eureka.instance.hostname、eureka.instance.prefer.ip-address、
    eureka.instance.ip-address三个配置信息时就可以根据优先级顺序达到预期的效果，避免没有必要的错误出现。


------------------------------------------------------------------------------------------------------------------------
# Eureka服务注册中心安全配置：
    添加依赖：
        <!--添加安全认证-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
    因为我们是用的是Spring Security作为安全组件，所以在这里需要添加spring-boot-starter-security依赖来完成安全相关组件的
    自动化配置以及实例化。

## 开启注册中心安全配置
### 配置文件的安全配置
    # 服务名称
    spring:
      application:
        name: hengboy-spring-cloud-eureka-security
      # 安全参数配置
      security:
        user:
          name: root
          password: admin
          roles: SERVICE_NODE
    # eureka配置
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:${server.port}/eureka/
        fetch-registry: false
        register-with-eureka: false

    # 端口号
    server:
      port: 8086
    安全相关的内容我们通过spring.security.user开头的参数进行配置，对应自动绑定spring-boot-starter-security依赖内的
    org.springframework.boot.autoconfigure.security.SecurityProperties属性实体类。
    在SecurityProperties的内部类SecurityProperties.User内我们可以看到已经给我们生成了一个默认的name以及password
    spring.security.user.name
    用户名，默认值为user，配置Spring Security内置使用内存方式存储的用户名。
    spring.security.user.password
    用户对应的密码，默认值为UUID随机字符串，配置Spring Security默认对应user用户的密码，该密码在系统启动时会在控制台打印，
    如果使用默认值可以运行查看控制台的输出内容。

### 开启Http Basic 安全认证
    旧版本的Spring Security的依赖是可以在配置文件内容直接通security.basic.enabled参数进行开启basic认证，不过目前版本已经
    被废除，既然这种方式不可行，那我们就使用另外一种方式进行配置，通过继承WebSecurityConfigurerAdapter安全配置类来完成开
    启认证权限，配置类如下所示：
        @Configuration
        public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
            /**
             * 配置安全信息
             * - 禁用csrf攻击功能
             * - 开启所有请求需要验证并且使用http basic进行认证
             *
             * @param http
             * @throws Exception
             */
            @Override
            protected void configure(HttpSecurity http) throws Exception {

                http.csrf()
                        .disable()
                        .authorizeRequests()
                        .anyRequest().authenticated()
                        .and()
                        .httpBasic();
            }
        }
    如果你了解Spring Security那肯定对我们自定义的安全配置类SecurityConfiguration的内容不陌生，在
    SecurityConfiguration#configure方法内，我们禁用了csrf功能并且开启所有请求都需要通过basic方式进行验证。

## 注册服务时的安全配置
    // 修改前
    # 配置Eureka Server 信息
    eureka:
      client:
        service-url:
          defaultZone: http://localhost:8086/eureka/

    // 修改后
    # 配置Eureka Server 信息
    eureka:
      client:
        service-url:
          defaultZone: http://root:admin@localhost:8086/eureka/
    修改后的api:node@这块的内容，前面是spring.security.user.name配置的值，而后面则是spring.security.user.password配置的
    值，@符号后面才是原本之前的Eureka Server的连接字符串信息。

    ## 这样配置之后，登陆注册列表可以使用root/admin，但是登陆服务需要使用默认的用户名：user，密码：控制台输出的uuid

## 总结
    本章为Eureka Server穿上了安全的外套，让它可以更安全，在文章开始的时候我说到了如果使用内网IP或者主机名方式进行服务注
    册时是几乎不存在安全问题的，如果你想你的服务注册中心更新安全，大可不必考虑你的服务注册方式都可以添加安全认证。


------------------------------------------------------------------------------------------------------------------------
# Eureka的服务发现与消费：
只需要创建一个服务节点项目即可，因为服务提供者也是消费者，然后将项目注册到服务注册中心
添加：spring-boot-starter-web、spring-cloud-starter-netflix-ribbon、spring-cloud-starter-netflix-eureka-client三个依赖
## 配置客户端：
    入口类，添加@EnableDiscoveryClient注解

## 通过服务名(spring.application.name)来获取服务实例列表：
    添加依赖spring-cloud-starter-netflix-ribbon就可以直接使用RestTemplate类进行发送http请求，而且RestTemnplate可以直接使
    用服务名进行发送请求！！！

### 实例化RestTemplate：
    spring-cloud-starter-netflix-ribbon依赖并没有为我们实例化RestTemplate，我们需要手动进行实例化，我采用@Bean方式进行实
    例化，在人口类实例化：
        @Bean
        @LoadBalanced
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    使用@LoadBalanced注解，才能通过服务名直接发送请求

### 请求转发流程：
    执行流程：我们在访问/consumer/index请求地址时，会通过RestTemplate转发请求访问
    http://hengboy-spring-cloud-eureka-consumer/consumer/logic地址并返回信息this is home page。

## 总结：
    通过Ribbon简单的实现了服务节点的消费，通过RestTemplate发送请求来获取响应内容，需要注意的是我们并不是通过IP:Port的形
    式，而是通过服务名的形式发送请求，这都归功于@LoadBalanced这个注解


------------------------------------------------------------------------------------------------------------------------
# Eureka高可用集群部署：

## 服务配置
测试环境都在我们本机，有两种方式可以模拟测试同时运行：
    创建两个不同的项目
    使用一个项目进行根据spring.profiles.active设置运行不同环境
本章采用第二种方式

## Profile多环境配置
创建application-node1.yml、application-node2.yml配置文件：

    # Eureka 客户端配置
    eureka:
      client:
        service-url:
          defaultZone: http://node2:10002/eureka/
      instance:
        # 配置通过主机名方式注册
        hostname: node1
        # 配置实例编号
        instance-id: ${eureka.instance.hostname}:${server.port}:@project.version@
      # 集群节点之间读取超时时间。单位：毫秒
      server:
        peer-node-read-timeout-ms: 1000
    # 服务端口号
    server:
      port: 10001
    -------------------
    # Eureka 客户端配置
    eureka:
      client:
        service-url:
          defaultZone: http://node1:10001/eureka/
      instance:
        # 配置通过主机名方式注册
        hostname: node2
        # 配置实例编号
        instance-id: ${eureka.instance.hostname}:${server.port}:@project.version@
      # 集群节点之间读取超时时间。单位：毫秒
      server:
        peer-node-read-timeout-ms: 1000
    server:
      port: 10002

## 主机名设置
Mac或者Linux配置方式
如果你使用的是osx系统。可以找到/etc/hosts文件并添加如下内容：
127.0.0.1       node1
127.0.0.1       node2
一般情况下配置完成后就会生效，如果没有生效，可以尝试重启。
Windows配置方式
如果你使用的是windows系统，可以修改C:\Windows\System32\drivers\etc\hosts文件，添加内容与Mac方式一致。

## Eureka Server相互注册
application-node1.yml
eureka.client.service-url.defaultZone这个配置参数的值，配置的是http://node2:10002/eureka/，那这里的node2是什么呢？其实一
看应该可以明白，这是们在hosts文件内配置的hostname，而端口号我们配置的则是10002，根据hostname以及port我们可以看出，环境
node1注册到了node2上。

application-node2.yml
在node2环境内配置eureka.client.service-url.defaultZone是指向的http://node1:10001/eureka/，同样node2注册到了node1上。

通过这种相互注册的方式牢靠的把两个服务注册中心绑定在了一块。

## 运行测试
1、clean && package 本项目（idea工具自带maven常用操作命令快捷方式，右侧导航栏Maven Projects -> Lifecycle）
2、打开终端cd项目target目录
3、通过如下命令启动node1环境：
    java -jar hengboy-spring-cloud-eureka-high-0.0.1-SNAPSHOT.jar --spring.profiles.active=node1
4、再打开一个终端，同样是cd项目的target目录下，通过如下命令启动node2环境：
    java -jar hengboy-spring-cloud-eureka-high-0.0.1-SNAPSHOT.jar --spring.profiles.active=node2
5、访问http://node1:10001查看node1环境的Eureka管理中心
6、访问http://node2:10002查看node2环境的Eureka管理中心

## 总结
集群环境让Eureka Server更健壮
在实战环境中建议把Eureka Server节点放在不同的服务器下，并且通过主机名或者内网方式进行相互注册。


------------------------------------------------------------------------------------------------------------------------
# 将服务提供者注册到Eureka集群：

## 启用Eureka Client
    @EnableDiscoveryClient

## 配置Eureka Client
    # 服务名称
    spring:
      application:
        name: catpp-spring-cloud-eureka-provider

    # 端口号
    server:
      port: 8087

    # Eureka集群配置信息
    eureka:
      client:
        service-url:
          defaultZone: http://node1:10001/eureka/,http://node2:10002/eureka/

## 主动将服务注册到Eureka集群
ureka.clinet.service-url.defaultZone参数，通过“，”隔开配置了两个Eureka Server地址，这两个地址则是Eureka Server集群地址

## 运行测试
1、启动node1环境服务注册中心
2、启动node2环境服务注册中心
3、启动本章项目
4、访问node1管理界面http://node1:10001查看服务列表
5、访问node2管理界面http://node2:10002查看服务列表

## 自动同步到Eureka集群
因为有eureka.client.fetch-registry这个参数，而且还是默认为true，这个参数配置了是否自动同步服务列表，也就是默认就会进行
同步的操作。你就算将Eureka Client注册到http://node1:10001/eureka/注册中心，也会自动同步到http://node2:10002/eureka/。

## 总结
通过主动以及自动同步的方式将Eureka Client注册到服务注册中心集群环境中，为了保证完整性，还是建议手动进行配置，自动同步也
有不成功的情况存在。


------------------------------------------------------------------------------------------------------------------------
# Eureka服务注册中心的失效剔除与自我保护机制：
Eureka作为一个成熟的服务注册中心当然也有合理的内部维护服务节点的机制，比如服务下线、失效剔除、自我保护，也正是因为内部有
这种维护机制才让Eureka更健壮、更稳定。

学习目标：了解Eureka是怎么保证服务相对较短时长内的有效性。

## 服务下线
迭代更新、终止访问某一个或者多个服务节点时，我们在正常关闭服务节点的情况下，Eureka Client会通过PUT请求方式调用
Eureka Server的REST访问节点/eureka/apps/{appID}/{instanceID}/status?value=DOWN请求地址，告知Eureka Server我要下线了，
Eureka Server收到请求后会将该服务实例的运行状态由UP修改为DOWN，这样我们在管理平台服务列表内看到的就是DOWN状态的服务实例。

## 失效剔除
Eureka Server在启动完成后会创建一个定时器每隔60秒检查一次服务健康状况，如果其中一个服务节点超过90秒未检查到心跳，那么
Eureka Server会自动从服务实例列表内将该服务剔除。
由于非正常关闭不会执行主动下线动作，所以才会出现失效剔除机制，该机制主要是应对非正常关闭服务的情况，如：内存溢出、杀死进
程、服务器宕机等非正常流程关闭服务节点时。

## 自我保护
Eureka Server的自我保护机制会检查最近15分钟内所有Eureka Client正常心跳的占比，如果低于85%就会被触发。
我们如果在Eureka Server的管理界面发现如下的红色内容，就说明已经触发了自我保护机制。
    EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT. RENEWALS ARE LESSER THAN THRESHOLD
    AND HENCE THE INSTANCES ARE NOT BEING EXPIRED JUST TO BE SAFE.

当触发自我保护机制后Eureka Server就会锁定服务列表，不让服务列表内的服务过期，不过这样我们在访问服务时，得到的服务很有可
能是已经失效的实例，如果是这样我们就会无法访问到期望的资源，会导致服务调用失败，所以这时我们就需要有对应的“容错机制”、
“熔断机制”

我们的服务如果是采用的公网IP地址，出现自我保护机制的几率就会大大增加，所以这时更要我们部署多个相同InstanId的服务或者建立
一套完整的熔断机制解决方案

## 自我保护开关
如果在本地测试环境，建议关掉自我保护机制，这样方便我们进行测试，也更准确的保证了服务实例的有效性！！！
关闭自我保护只需要修改application.yml配置文件内参数eureka.server.enable-self-preservation将值设置为false即可。

## 总结
了解到了Eureka Server对服务的治理，其中包含“服务下线”、“失效剔除”、“自我保护”等，对自我保护机制一定要谨慎的处理，
防止出现服务失效问题。


------------------------------------------------------------------------------------------------------------------------
# Eureka服务注册中心内置的REST节点列表：
你有没有考虑过Eureka Client与Eureka Server是通过什么方式进行通讯的？
为什么Client启动成功后Server就会被注册到Server的服务列表内？
为什么我们在正常关闭Client后Server会有所感知？

## 学习目标：熟悉Eureka Server内部提供的REST服务维护请求节点。

## REST节点一览
Eureka Server内部通过JAX-RS(Java API for RESTful Web Services)规范提供了一系列的管理服务节点的请求节点，这样也保证了在非
JVM环境运行的程序可以通过HTTP REST方式进行管理维护指定服务节点，所以只要遵循Eureka协议的服务节点都可以进行注册到
Eureka Server。
Eureka提供的REST请求可以支持XML以及JSON形式通信，默认采用XML方式，REST列表如表所示：
    请求名称	                请求方式	HTTP地址	                                            请求描述
    注册新服务	                POST	    /eureka/apps/{appID}	                                传递JSON或者XML格式参数内容，HTTP code为204时表示成功
    取消注册服务	            DELETE	    /eureka/apps/{appID}/{instanceID}	                    HTTP code为200时表示成功
    发送服务心跳	            PUT	        /eureka/apps/{appID}/{instanceID}	                    HTTP code为200时表示成功
    查询所有服务	            GET	        /eureka/apps	                                        HTTP code为200时表示成功，返回XML/JSON数据内容
    查询指定appID的服务列表	    GET	        /eureka/apps/{appID}	                                HTTP code为200时表示成功，返回XML/JSON数据内容
    查询指定appID&instanceID	GET	        /eureka/apps/{appID}/{instanceID}	                    获取指定appID以及InstanceId的服务信息，HTTP code为200时表示成功，返回XML/JSON数据内容
    查询指定instanceID服务列表	GET	        /eureka/apps/instances/{instanceID}	                    获取指定instanceID的服务列表，HTTP code为200时表示成功，返回XML/JSON数据内容
    变更服务状态	            PUT	        /eureka/apps/{appID}/{instanceID}/status?value=DOWN	    服务上线、服务下线等状态变动，HTTP code为200时表示成功
    变更元数据	                PUT	        /eureka/apps/{appID}/{instanceID}/metadata?key=value	HTTP code为200时表示成功
    查询指定IP下的服务列表	    GET	        /eureka/vips/{vipAddress}	                            HTTP code为200时表示成功
    查询指定安全IP下的服务列表	GET	        /eureka/svips/{svipAddress}	                            HTTP code为200时表示成功

    在上面列表中参数解释
    {appID}：服务名称，对应spring.application.name参数值
    {instanceID}：实例名称，如果已经自定义instanceId则对应eureka.instance.instance-id参数值

## 服务注册
在Eureka Client启动成功后会发送POST方式的请求到/eureka/apps/{appID}，发送注册请求时的主体内容在官网也有介绍，如果我们根
据指定的主体内容发送请求到Eureka Server时也是可以将服务注册成功的，主体内容要以XML/JSON格式的XSD传递：

    <?xml version="1.0" encoding="UTF-8"?>
    <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" attributeFormDefault="unqualified">
        <xsd:element name="instance">
            <xsd:complexType>
                <xsd:all>
                    <!-- hostName in ec2 should be the public dns name, within ec2 public dns name will
                         always resolve to its private IP -->
                    <xsd:element name="hostName" type="xsd:string" />
                    <xsd:element name="app" type="xsd:string" />
                    <xsd:element name="ipAddr" type="xsd:string" />
                    <xsd:element name="vipAddress" type="xsd:string" />
                    <xsd:element name="secureVipAddress" type="xsd:string" />
                    <xsd:element name="status" type="statusType" />
                    <xsd:element name="port" type="xsd:positiveInteger" minOccurs="0" />
                    <xsd:element name="securePort" type="xsd:positiveInteger" />
                    <xsd:element name="homePageUrl" type="xsd:string" />
                    <xsd:element name="statusPageUrl" type="xsd:string" />
                    <xsd:element name="healthCheckUrl" type="xsd:string" />
                   <xsd:element ref="dataCenterInfo" minOccurs="1" maxOccurs="1" />
                    <!-- optional -->
                    <xsd:element ref="leaseInfo" minOccurs="0"/>
                    <!-- optional app specific metadata -->
                    <xsd:element name="metadata" type="appMetadataType" minOccurs="0" />
                </xsd:all>
            </xsd:complexType>
        </xsd:element>

        <xsd:element name="dataCenterInfo">
            <xsd:complexType>
                 <xsd:all>
                     <xsd:element name="name" type="dcNameType" />
                     <!-- metadata is only required if name is Amazon -->
                     <xsd:element name="metadata" type="amazonMetdataType" minOccurs="0"/>
                 </xsd:all>
            </xsd:complexType>
        </xsd:element>

        <xsd:element name="leaseInfo">
            <xsd:complexType>
                <xsd:all>
                    <!-- (optional) if you want to change the length of lease - default if 90 secs -->
                    <xsd:element name="evictionDurationInSecs" minOccurs="0"  type="xsd:positiveInteger"/>
                </xsd:all>
            </xsd:complexType>
        </xsd:element>

        <xsd:simpleType name="dcNameType">
            <!-- Restricting the values to a set of value using 'enumeration' -->
            <xsd:restriction base = "xsd:string">
                <xsd:enumeration value = "MyOwn"/>
                <xsd:enumeration value = "Amazon"/>
            </xsd:restriction>
        </xsd:simpleType>

        <xsd:simpleType name="statusType">
            <!-- Restricting the values to a set of value using 'enumeration' -->
            <xsd:restriction base = "xsd:string">
                <xsd:enumeration value = "UP"/>
                <xsd:enumeration value = "DOWN"/>
                <xsd:enumeration value = "STARTING"/>
                <xsd:enumeration value = "OUT_OF_SERVICE"/>
                <xsd:enumeration value = "UNKNOWN"/>
            </xsd:restriction>
        </xsd:simpleType>

        <xsd:complexType name="amazonMetdataType">
            <!-- From <a class="jive-link-external-small" href="http://docs.amazonwebservices.com/AWSEC2/latest/DeveloperGuide/index.html?AESDG-chapter-instancedata.html" target="_blank">http://docs.amazonwebservices.com/AWSEC2/latest/DeveloperGuide/index.html?AESDG-chapter-instancedata.html</a> -->
            <xsd:all>
                <xsd:element name="ami-launch-index" type="xsd:string" />
                <xsd:element name="local-hostname" type="xsd:string" />
                <xsd:element name="availability-zone" type="xsd:string" />
                <xsd:element name="instance-id" type="xsd:string" />
                <xsd:element name="public-ipv4" type="xsd:string" />
                <xsd:element name="public-hostname" type="xsd:string" />
                <xsd:element name="ami-manifest-path" type="xsd:string" />
                <xsd:element name="local-ipv4" type="xsd:string" />
                <xsd:element name="hostname" type="xsd:string"/>
                <xsd:element name="ami-id" type="xsd:string" />
                <xsd:element name="instance-type" type="xsd:string" />
            </xsd:all>
        </xsd:complexType>

        <xsd:complexType name="appMetadataType">
            <xsd:sequence>
                <!-- this is optional application specific name, value metadata -->
                <xsd:any minOccurs="0" maxOccurs="unbounded" processContents="skip"/>
            </xsd:sequence>
        </xsd:complexType>
    </xsd:schema>

## 服务状态变更
修改服务实例的运行状态，比如服务关闭，会从UP转换为DOWN，我们通过curl命令来测试服务的状态变更:
    curl -v -X PUT http://localhost:8086/eureka/apps/catpp-spring-cloud-eureka/catpp-spring-cloud-eureka-provider:8087:0.0.1-SNAPSHOT/status?value=DOWN

## 服务基本信息获取
Eureka提供获取指定appID以及instanceID的详细信息，可以详细的返回服务实例的配置内容，获取信息的命令如下：
    curl http://localhost:8086/eureka/apps/catpp-spring-cloud-eureka/catpp-spring-cloud-eureka-provider:8087:0.0.1-SNAPSHOT

## 服务剔除
将服务从Eureka剔除，剔除后会直接从服务实例列表中删除，可执行如下命令：
    curl -v -X DELETE http://localhost:8086/eureka/apps/catpp-spring-cloud-eureka/catpp-spring-cloud-eureka-provider:8087:0.0.1-SNAPSHOT
由于Eureka Client一直在运行，删除后也会自动通过注册服务的REST注册实例。

## 总结
