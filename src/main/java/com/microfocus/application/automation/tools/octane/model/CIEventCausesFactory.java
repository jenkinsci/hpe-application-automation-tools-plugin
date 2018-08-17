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

package com.microfocus.application.automation.tools.octane.model;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.Cause;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 20/10/14
 * Time: 18:19
 * To change this template use File | Settings | File Templates.
 */

public final class CIEventCausesFactory {
	protected CIEventCausesFactory(){}
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	public static List<CIEventCause> processCauses(List<Cause> causes) {
		List<CIEventCause> result = new LinkedList<>();
		CIEventCause tmpResultCause;
		Cause.UserIdCause tmpUserCause;
		Cause.UpstreamCause tmpUpstreamCause;

		if (causes != null) {
			for (Cause cause : causes) {
				tmpResultCause = dtoFactory.newDTO(CIEventCause.class);
				if (cause instanceof SCMTrigger.SCMTriggerCause) {
					tmpResultCause.setType(CIEventCauseType.SCM);
				} else if (cause instanceof TimerTrigger.TimerTriggerCause) {
					tmpResultCause.setType(CIEventCauseType.TIMER);
				} else if (cause instanceof Cause.UserIdCause) {
					tmpUserCause = (Cause.UserIdCause) cause;
					tmpResultCause.setType(CIEventCauseType.USER);
					tmpResultCause.setUser(tmpUserCause.getUserId());
				} else if (cause instanceof Cause.UpstreamCause) {
					tmpUpstreamCause = (Cause.UpstreamCause) cause;
					tmpResultCause.setType(CIEventCauseType.UPSTREAM);
					tmpResultCause.setProject(resolveJobCiId(tmpUpstreamCause.getUpstreamProject()));
					tmpResultCause.setBuildCiId(String.valueOf(tmpUpstreamCause.getUpstreamBuild()));
					tmpResultCause.setCauses(processCauses(tmpUpstreamCause.getUpstreamCauses()));
				} else {
					tmpResultCause.setType(CIEventCauseType.UNDEFINED);
				}
				result.add(tmpResultCause);
			}
		}
		return result;
	}

	private static String resolveJobCiId(String jobPlainName) {
		if(!jobPlainName.contains(",")) {
			return BuildHandlerUtils.translateFolderJobName(jobPlainName);
		}
		return jobPlainName;
	}
}
