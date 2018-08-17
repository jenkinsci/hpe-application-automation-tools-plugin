/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.srf.run;
import groovy.transform.Synchronized;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class CreateTunnelBuilder extends Builder  {
    private  String srfTunnelName;
    private static final Logger systemLogger = Logger.getLogger(CreateTunnelBuilder.class.getName());
    static final ArrayList<Process> Tunnels = new ArrayList<Process>();
    @DataBoundConstructor
    public CreateTunnelBuilder( String srfTunnelName ){

        this.srfTunnelName = srfTunnelName;
    }

    public String getSrfTunnelName() {
        return srfTunnelName;
    }

    @Synchronized
    @Override
    public CreateTunnelBuilder.DescriptorImpl getDescriptor() {
        return (CreateTunnelBuilder.DescriptorImpl) super.getDescriptor();
    }


    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        JSONObject connectionData = RunFromSrfBuilder.getSrfConnectionData(build, logger);

        String path =connectionData.getString("tunnel");
        String config = String.format("-config=%s", srfTunnelName);

        ProcessBuilder pb = new ProcessBuilder(path,  config, "-reconnect-attempts=3", "-log-level=info", "-log=stdout");
        pb.redirectOutput();
        logger.println("Launching "+path + " " + config );
        String[] cmdArray = { path, config, "-reconnect-attempts=3", "-log-level=info", "-log=stdout"};
 //     Process p = pb.start();
        Process p = Runtime.getRuntime().exec(cmdArray);
        TunnelTracker tracker = new TunnelTracker(logger, p);
        java.lang.Thread th = new Thread(tracker, "trackeer");
        Tunnels.add(p);

        InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        while(true){

            while ((line = br.readLine()) != null) {
                systemLogger.info(line);
                logger.println(line);
                if(line.contains("established at")){
                    th.start();
                    logger.println("Launched "+path);
                    return true;
                }
                Thread.sleep(100);
                int exitVal = 0;
                try{
                    exitVal = p.exitValue();
                }
                catch (Exception e){
                    continue;
                }

                switch (exitVal) {
                    case 0:
                        logger.println("Tunnel client terminated by the user or the server");
                        return true;
                    case 1:
                        logger.println("Failed to launch tunnel client : unplanned failure");
                        break;
                    case 2:
                        logger.println("Failed to launch tunnel client : Authentication with client/secret failed");
                        break;
                    case 3:
                        logger.println("Failed to launch tunnel client : Max connection attempts acceded");
                        break;
                    case 4:
                        logger.println("Failed to launch tunnel client : Allocation of tunnel failed E.g. Tunnel name is not unique.\nPlease check if tunnel is already running");
                        break;
                    default:
                        logger.println(String.format("Failed to launch tunnel client : Unknown reason(Exit code =%d)", p.exitValue()));
                        break;

                }
                systemLogger.info("Closing tunnel process");
                p.destroy();
                return false;

            }
        }


    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private  String srfTunnelName;
        @DataBoundConstructor
        public DescriptorImpl(String srfTunnelName) {
            this.srfTunnelName = srfTunnelName;
        }
        public DescriptorImpl(){
            load();
        }
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }


        @Override
        public String getDisplayName() {
            return "Create SRF Tunnel";
        }
    }
    static class TestRunData implements Serializable {
        private static final long serialVersionUID=11;
        private String id;                   // "932c6c3e-939e-4b17-a04f-1a2951481758",
        private String name;                 // "Test-Test-Run",
        private String Start;                // "2016-07-25T08:27:59.318Z",
        private String duration;
        private String status;               // "status" : "success",
        private String TunnelName;              // "246fa1a7-7ed2-4203-a4e9-7ce5fbf4f800",

        public TestRunData(JSONObject obj)
        {
            try {
                id = (String) obj.get("id");
                status = (String) obj.get("status");
                if (id == null) {
                    id = (String) obj.get("message");
                    status = "failed";
                }
                name = (String) obj.get("name");
                duration = obj.get("durationMs").toString();
            }
            catch (Exception e)
            {

            }
        }

        public void merge(TestRunData newData) {
            if (newData.name != null )  this.name = newData.name;
            if (newData.Start != null )  this.Start = newData.Start;
            if (newData.duration != null )  this.duration = newData.duration;
            if (newData.status != null )  this.status = newData.status;
            if (newData.TunnelName != null )  this.TunnelName = newData.TunnelName;
            if (newData.duration != null )  this.duration = newData.duration;
        }
    }


    private class TunnelTracker implements Runnable{
        private static final long serialVersionUID=456;
        PrintStream logger;
        Process p;
        public TunnelTracker(PrintStream log, Process p){
            this.logger = log;
            this.p=p;
        }
        @Override
        public  void run() {
            logger.println("In tracker!");
            int exitValue =0;
            while(true) {
                try {
                    //Read out dir output
                    InputStream is = p.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        // logger.println(line);
                    }
                    try {
                        exitValue = p.exitValue();
                        break;
                    } catch (Exception e) {
                        continue;
                    }
                } catch (Exception e) {
                    logger.print(e.getMessage());
                }
                //Wait to get exit value
                try {
                    p.waitFor();
                    logger.println("\n\nTunnel exit value is " + exitValue);
                    return;
                } catch (final InterruptedException e) {
                    p.destroy();
                    return;
                }
            }
        }
    }



}