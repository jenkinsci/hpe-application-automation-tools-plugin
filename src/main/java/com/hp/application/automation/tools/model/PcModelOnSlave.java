package com.hp.application.automation.tools.model;


public class PcModelOnSlave extends PcModel {

	private final boolean runOnSlave;  
	
	public PcModelOnSlave(String pcServerName, String almUserName,
			String almPassword, String almDomain, String almProject,
			String testId, String testInstanceId, String timeslotDurationHours,
			String timeslotDurationMinutes, PostRunAction postRunAction,
			boolean vudsMode, boolean runOnSlave, String description) {
		super(pcServerName, almUserName, almPassword, almDomain, almProject, testId,
				testInstanceId, timeslotDurationHours, timeslotDurationMinutes,
				postRunAction, vudsMode, description);
		this.runOnSlave = runOnSlave;
	}

	public boolean getRunOnSlave() {
		return runOnSlave;
	}

}
