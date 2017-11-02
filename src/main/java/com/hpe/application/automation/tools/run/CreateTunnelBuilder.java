package com.hpe.application.automation.tools.run;
import groovy.transform.Synchronized;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;

public class CreateTunnelBuilder extends Builder  {
    private PrintStream logger;
    private  String srfTunnelName;
    private AbstractBuild<?, ?> build;
    protected static final ArrayList<Process> Tunnels = new ArrayList<Process>();
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
        logger = listener.getLogger();
        JSONObject connectionData = RunFromSrfBuilder.GetSrfConnectionData(build, logger);
        JSONObject configData;
        String client = "\"" +"-client="  + connectionData.getString("app") +"\"";


        String path =connectionData.getString("tunnel");
        String config = String.format("-\"config=%s\"", srfTunnelName);

        ProcessBuilder pb = new ProcessBuilder(path,  config, "-reconnect-attempts=3", "-log-level=info", "-log=stdout");
        pb.redirectOutput();
        logger.println("Launching "+path + " " + config );

        Process p = pb.start();
        TunnelTracker tracker = new TunnelTracker(logger, p);
        java.lang.Thread th = new Thread(tracker, "trackeer");
        Tunnels.add(p);

        InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        while(true){

            while ((line = br.readLine()) != null) {
                logger.println(line);
                if(line.indexOf("established at") >=0){
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
                        logger.println("Failed to launch tunnel client : Max connection attempts acceded ");
                        break;
                    case 4:
                        logger.println("Failed to launch tunnel client : Allocation of tunnel filed E.g. Tunnel name is not unique.");
                        break;
                    default:
                        logger.println(String.format("Failed to launch tunnel client : Unknown reason(Exit code =%d", p.exitValue()));
                        break;

                }

                p.destroy();
                return false;

            }

        }


    }
    private JSONObject GetSrfConnectionData(){
        return new JSONObject();
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
            return "Create Tunnel";
        }
    }
    static class TestRunData implements Serializable {
        static final long serialVersionUID=11;
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


        public void merge(TestRunData newData)
        {
            if (newData.name != null )  this.name = newData.name;
            if (newData.Start != null )  this.Start = newData.Start;
            if (newData.duration != null )  this.duration = newData.duration;
            if (newData.status != null )  this.status = newData.status;
            if (newData.TunnelName != null )  this.TunnelName = newData.TunnelName;
            if (newData.duration != null )  this.duration = newData.duration;
        }

        String id;                   // "932c6c3e-939e-4b17-a04f-1a2951481758",
        String name;                 // "Test-Test-Run",
        String Start;                // "2016-07-25T08:27:59.318Z",
        String duration;
        String status;               // "status" : "success",
        String TunnelName;              // "246fa1a7-7ed2-4203-a4e9-7ce5fbf4f800",
        int         execCount;
        String [] tags;
        String user;
        JSONObject _context;
    }


    private class TunnelTracker implements Runnable{
        static final long serialVersionUID=456;
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
                    logger.println("\n\nExit Value is " + exitValue);
                } catch (final InterruptedException e) {
                    p.destroy();
                    return;
                }
            }
        }
    }



}