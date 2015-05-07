package com.hp.application.automation.tools.run;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.io.IOException;
import java.io.PrintStream;

import org.kohsuke.stapler.DataBoundConstructor;

import com.hp.application.automation.tools.model.PostRunAction;
import com.hp.application.automation.tools.run.PcBuilder;

public class PcBuilderOnSlave extends PcBuilder{
	private PrintStream logger;
	private boolean runOnSlave; 
	
	@DataBoundConstructor
	public PcBuilderOnSlave(String pcServerName, String almUserName,
			String almPassword, String almDomain, String almProject,
			String testId, String testInstanceId, String timeslotDurationHours,
			String timeslotDurationMinutes, PostRunAction postRunAction,
			boolean vudsMode, boolean statusBySLA, boolean runOnSlave, String description) {
		super(pcServerName, almUserName, almPassword, almDomain, almProject, testId,
				testInstanceId, timeslotDurationHours, timeslotDurationMinutes,
				postRunAction, vudsMode, statusBySLA, description);
		this.runOnSlave = runOnSlave;
	}
	
	@Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws InterruptedException {
		
        String status = "Status unknown";
    	Callable<String, IOException> task = new Callable<String, IOException>() {
			private static final long serialVersionUID = 1L;
			public String call() throws IOException {
				try {
					return String.valueOf(PcBuilderOnSlave.super.perform(build, launcher, listener));
				} catch (InterruptedException e) {
					logger.println(e.toString());
					return "Error encountered, see log.";
				}
    		}
    	};
    	VirtualChannel channel = launcher.getChannel();
        if ((channel != null) && (runOnSlave == true)) {
        	try {
				status = channel.call(task);
			} catch (IOException e) {
				logger.println(e.toString());
			}
        } else {
        	try {
				status = task.call();
			} catch (IOException e) {
				logger.println(e.toString());
			}
        }
        logger.print("Final status: ");
        logger.println(status);
        return true;
    }
}
