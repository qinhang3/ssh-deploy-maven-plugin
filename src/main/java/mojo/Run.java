package mojo;

import com.jcraft.jsch.JSchException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.omg.SendingContext.RunTime;
import util.SSHUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Created by hang.qin on 2016/12/16.
 */
@Mojo(name = "run")
public class Run extends AbstractMojo {
    @Parameter(defaultValue = "10.66.48.169")
    private String server;

    @Parameter(defaultValue = "22")
    private Integer sshPort;

    @Parameter(defaultValue = "root")
    private String userName;

    @Parameter(defaultValue = "admin123")
    private String password;

    @Parameter(defaultValue = "/root/download/tomcat_item")
    private String tomcatHome;

    @Parameter(defaultValue = "8080")
    private String watchPort;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            SSHUtil sshUtil = new SSHUtil(userName,server,sshPort,password);

            //find pid
            List<String> exec = sshUtil.exec(String.format("ps -ef | grep '%s' | grep -v grep | awk '{print $2}'", tomcatHome));
            getLog().info("FIND PID : " + exec);

            //kill
            for (String pid : exec) {
                sshUtil.exec(String.format("kill -9 %s",pid));
                getLog().info(String.format("kill %s done",pid));
            }

            //delete old war
            getLog().info("DELETE OLD FILE.");
            sshUtil.exec(String.format("rm -rf %s/webapps/*",tomcatHome));

            //upload
            File warFile = findWarFile();
            getLog().info("UPLOAD START");
            long start = System.currentTimeMillis();
            sshUtil.upload(new FileInputStream(warFile),tomcatHome + "/webapps/" + warFile.getName());
            getLog().info("UPLOAD FINISH. COST TIME = " + ((System.currentTimeMillis() - start)) + " MS");

            //start
            List<String> startResult = sshUtil.exec(tomcatHome + "/bin/startup.sh");
            getLog().info("START FINISH.OUTPUT");
            for (String s : startResult) {
                getLog().info(s);
            }

            //tail log
            Iterator<String> tail = sshUtil.tail(String.format("%s/logs/catalina.out", tomcatHome), ".*Server startup in [0-9]+ ms.*");
            while(tail.hasNext()){
                getLog().info(tail.next());
            }
            sshUtil.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public File findWarFile() throws MojoExecutionException {
        //find target/*.war
        File targetDir = new File("target");
        if (!targetDir.exists()){
            getLog().error("FIND WAR FILE FAILED.PATH = " + targetDir.getAbsolutePath() + " NOT EXIST.PLEASE RUN PACKAGE FIRST");
            throw new MojoExecutionException("FIND WAR FILE FAILED");
        }
        File[] wars = targetDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".war");
            }
        });
        if (wars == null || wars.length == 0){
            getLog().error("FIND WAR FILE FAILED.PATH = " + targetDir.getAbsolutePath());
            throw new MojoExecutionException("FIND WAR FILE FAILED");
        }
        if (wars.length > 1){
            getLog().error("FIND MORE THAN 1 WAR FILE FAILED.PATH = " + targetDir.getAbsolutePath());
            for (File war : wars) {
                getLog().error("FIND " + war.getName());
            }
            throw new MojoExecutionException("FIND WAR FILE FAILED");
        }
        getLog().info("FIND WAR FILE SUCCESS.PATH = " + wars[0].getAbsolutePath());
        return wars[0];
    }

    public static void main(String[] args) throws Exception {
//        String output = "20-Dec-2016 10:44:20.316 信息 [main] org.apache.catalina.startup.Catalina.start Server startup in 14155 ms";
//        System.out.println(output.matches(".*Server startup in [0-9]+ ms.*"));
        SSHUtil sshUtil = new SSHUtil("root","10.66.48.169",22,"admin123");
        // Server startup in 14155 ms
        Iterator<String> out = sshUtil.tail("/root/download/tomcat_item/logs/catalina.out", ".*Server startup in [0-9]+ ms.*");
        while(out.hasNext()){
            System.out.println(out.next());
        }
        sshUtil.stop();
    }

}
