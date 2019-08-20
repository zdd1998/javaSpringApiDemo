package com.example.demo.controller;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class MySShSessionFactory extends JschConfigSessionFactory {

    private String sshKeyFilePath;
    private String knowHostFilePath;
    @Override
    protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
        JSch jsch = super.getJSch(hc, fs);
        jsch.removeAllIdentity();
        try {
            jsch.addIdentity(sshKeyFilePath);
            jsch.setKnownHosts(knowHostFilePath);
        }catch (Exception e){
            e.printStackTrace();
            //System.out.println("key err");
        }
        return jsch;
    }

    @Override
    protected void configure(Host hc, Session session) {
        session.setConfig("StrictHostKeyChecking", "yes");
    }

    public String getSshKeyFilePath() {
        return sshKeyFilePath;
    }

    public void setSshKeyFilePath(String sshKeyFilePath) {
        this.sshKeyFilePath = sshKeyFilePath;
    }

    public void setKnowHostFilePath(String knowHostFilePath){
        this.knowHostFilePath = knowHostFilePath;
    }
}
