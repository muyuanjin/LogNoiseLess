# 使用布隆/布谷鸟过滤器对 logback 日志一定周期内重复异常堆栈打印进行压缩过滤
### [掘金: 使用布谷鸟过滤器对 logback 日志一定周期内重复异常堆栈打印进行压缩过滤](https://juejin.cn/post/7155371979128700964/)
## 用法:

### 1.配置logback xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!--  导入spring基础配置  -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <!--    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>-->

    <!--  替换defaults.xml中定义的属性， 使用噪音抑制异常转换器 -->
    <conversionRule conversionWord="wEx"
                    converterClass="com.muyuanjin.lognoiseless.NoiseLessThrowableProxyConverter"/>

    <!-- 控制台日志，因为覆盖了属性，所以这里不能直接借用 console-appender.xml ，copy一遍-->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>${CONSOLE_LOG_CHARSET}</charset>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### 2.设置spring配置:

白名单模式

```yaml
logback:
  stackTrace:
    skipLine: com.example,java,org # 多个包名以逗号分隔
    skipLineMode: whitelist #白名单模式
    maxNumPerCycle: 1 # 每个周期内最大打印全栈的次数，=0时每次均跳过， =1时使用 布隆过滤器
    cycleDuration: 2h #计数周期
```

或者黑名单模式

```yaml
logback:
  stackTrace:
    skipLine: com.example,java,org # 多个包名以逗号分隔
    skipLineMode: blacklist #黑名单模式
    maxNumPerCycle: 1 # 每个周期内最大打印全栈的次数，=0时每次均跳过， =1时使用 布隆过滤器
    cycleDuration: 2h #计数周期
```

或者 class 或 spring bean (需实现`StackLineSkipPredicate`)

```yaml
logback:
  stackTrace:
    skipLine: com.example.TestStackLineSkipPredicate
    skipLineMode: predicate_class #谓词类
    maxNumPerCycle: 1 # 每个周期内最大打印全栈的次数，=0时每次均跳过， =1时使用 布隆过滤器
    cycleDuration: 2h #计数周期
```

或者 janino表达式,使用 `line` 表示该行的字符串参数, 返回true时表示不打印改行

```yaml
logback:
  stackTrace:
    skipLine: "!line.contains(\"com.example\")" #必须以com.example开头才打印
    skipLineMode: janino_expression #janino表达式
    maxNumPerCycle: 1 # 每个周期内最大打印全栈的次数，=0时每次均跳过， =1时使用 布隆过滤器
    cycleDuration: 2h #计数周期
```