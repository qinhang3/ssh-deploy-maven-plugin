package util;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hang.qin on 2016/12/16.
 */
public class SSHUtil {

    private Session session = null;

    public SSHUtil(String user,String host,Integer port,String psw) throws JSchException {
        JSch jsch=new JSch();
        session = jsch.getSession(user, host, port);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(psw);
        session.connect();
    }

    public List<String> exec(String command) throws JSchException, IOException {
            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);
//        int exitStatus = channelExec.getExitStatus();
//        System.out.println(exitStatus);
            channelExec.connect();

        try(InputStream in = channelExec.getInputStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String buf;
            List<String> result = new ArrayList<>();
            while ((buf = reader.readLine()) != null) {
                result.add(new String(buf.getBytes("gbk"), "UTF-8"));
            }
            return result;
        }
    }

    public void upload(InputStream is,String target) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp)session.openChannel("sftp");
        channelSftp.connect();
        channelSftp.put(is,target);
    }
}
