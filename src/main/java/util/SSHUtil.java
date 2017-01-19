package util;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
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

    public void stop(){
        if (session != null) {
            session.disconnect();
        }
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

    public Iterator<String> tail(String file, String endExp) throws JSchException, IOException {
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(String.format("tail -0f %s",file));
        channelExec.connect();

        InputStream in = channelExec.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        return new Iterator<String>() {
            String temp = null;
            boolean isEnd = false;

            @Override
            public boolean hasNext() {
                if (temp != null) {
                    return true;
                }
                if (isEnd) {
                    return false;
                }
                temp = readLine();

                if (temp == null) {
                    return false;
                } else if (temp.matches(endExp)) {
                    isEnd = true;
                    return true;
                } else {
                    isEnd = false;
                    return true;
                }
            }

            @Override
            public String next() {
                if (temp != null) {
                    String now = temp;
                    temp = null;
                    return now;
                } else {
                    return readLine();
                }
            }

            private String readLine() {
                try {
                    String s = reader.readLine();
                    if (s != null){
                        s = new String(s.getBytes("gbk"),"UTF-8");
                    }
                    return s;
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }
}
