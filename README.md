# ssh-deploy-maven-plugin
- 一个maven plugin
- 可以通过ssh将war包通过ssh部署到机器tomcat上
- ssh使用jsch

## 使用方法

1. 引入plugin
````
<build>
    <plugins>
        <plugin>
            <groupId>com.watsons</groupId>
            <artifactId>ssh-deploy-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <configuration>
                <server>127.0.0.1</server>
                <tomcatHome>/root/download/tomcat_item</tomcatHome>
                <userName>root</userName>
                <password>root</password>
                <watchPort>8081,20882</watchPort>
            </configuration>
        </plugin>
    </plugins>
</build>
````
2. 执行构建并部署
````
mvn clean package ssh-deploy:run
````

目前支持2种指令
- status
  - 用来判断机器当前状态
  - 以及端口被监听的状态

- run 正式部署
  - 部署动作主要分为以下几步：
    1. 通过 tomcat_home 寻找pid
    2. kill -9 $PID
    3. 删除 webapps 下的所有文件
    4. 寻找 target 下的war包 并上传
    5. 调用tomcat_home/bin/startup.sh

