/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.model;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hpe.application.automation.tools.octane.tests.build.BuildHandlerUtils;
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
