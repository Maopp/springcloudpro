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
    InetUtils#findFirstNonLoopbackHostInfo方法来进行实例化，我们来看看这个方法的具体实现逻辑，它会自动读取系统网卡列表然再进
    行循环遍历查询正在UP状态的网卡信息，如果没有查询到网卡信息，则使用默认的HostName、IpAddress配置信息，源码如下所示：
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
    由于在实例化EurekaInstanceConfigBean配置实体类时，构造函数进行了获取第一个非回环主机信息，默认的hostName以及ipAddress参
    数则是会直接使用InetUtils#findFirstNonLoopbackHostInfo方法返回的相对应的值。

## IP优先注册方式源码分析
    EurekaInstanceConfigBean#getHostName方法直接调用本类重载方法getHostName(boolean refresh)并且传递参数为false，根据第三步
    源码我们就可以看到：
        return this.preferIpAddress ? this.ipAddress : this.hostname;
    如果eureka.instance.prefer-ip-address参数设置了true就会返回eureka.instance.ip-address的值，这样我们就可以从中明白为什么
    主动设置eureka.instance.ip-address参数后需要同时设置eureka.instance.prefer-ip-address参数才可以生效。

## 指定IP、HostName源码分析
    我们通过application.yml配置文件进行设置eureka.instance.hostname以及eureka.instance.ip-address后会直接替换原默认值，在
    EurekaInstanceConfigBean#getHostName中也是返回的this.hostname、this.ipAddress所以在这里设置后会直接生效作为返回的配置值。

## 总结：
    通过源码进行分析服务注册方式执行流程，这样在以后进行配置eureka.instance.hostname、eureka.instance.prefer.ip-address、
    eureka.instance.ip-address三个配置信息时就可以根据优先级顺序达到预期的效果，避免没有必要的错误出现。