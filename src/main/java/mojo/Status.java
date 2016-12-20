package mojo;

import com.jcraft.jsch.JSchException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import util.SSHUtil;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by hang.qin on 2016/12/16.
 */
@Mojo(name = "status")
public class Status extends AbstractMojo {

    @Parameter(defaultValue = "127.0.0.1")
    private String server;

    @Parameter(defaultValue = "22")
    private Integer sshPort;

    @Parameter(defaultValue = "root")
    private String userName;

    @Parameter(defaultValue = "pwd")
    private String password;

    @Parameter(defaultValue = "/root/download/tomcat_home")
    private String tomcatHome;

    @Parameter(defaultValue = "8080")
    private String watchPort;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException{

        List<Integer> ports = Arrays.stream(watchPort.split(",")).map(new Function<String, Integer>() {
            @Override
            public Integer apply(String s) {
                return Integer.valueOf(s);
            }
        }).collect(Collectors.toList());

        getLog().info(String.format("ssh %s@%s",userName,server));
        SSHUtil sshUtil;
        try {
            sshUtil = new SSHUtil(userName,server,sshPort,password);
            List<String> exec = sshUtil.exec(String.format("ps -ef | grep '%s' | grep -v grep | awk '{print $2}'", tomcatHome));
            if (exec.isEmpty()){
                getLog().info("GET PID : NONE!");
                getLog().info("TOMCAT MAYBE SHUTDOWN NOW!");
            } else {
                getLog().info("GET PID : " + exec);
            }

            getLog().info("CHECK WATCH PORT : ");
            for (Integer port : ports) {
                List<String> portResult = sshUtil.exec(String.format("lsof -i :%s | grep LISTEN | awk '{print $2}'",port));
                if (portResult.isEmpty()){
                    getLog().info(String.format("PORT[%s] LISTEN BY NONE", port));
                } else {
                    getLog().warn(String.format("PORT[%s] LISTEN BY " + portResult, port));
                }
            }

        } catch (Exception e) {
            getLog().error("init ssh error",e);
            throw new RuntimeException(e);
        }
    }

}
