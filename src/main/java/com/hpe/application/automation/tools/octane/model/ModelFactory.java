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

import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.pipelines.BuildHistory;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotPhase;
import com.hpe.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.hpe.application.automation.tools.octane.model.processors.projects.AbstractProjectProcessor;
import com.hpe.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by lazara on 26/01/2016.
 */
public class ModelFactory {
	private static final Logger logger = LogManager.getLogger(ModelFactory.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	public static PipelineNode createStructureItem(Job job) {
		AbstractProjectProcessor projectProcessor = JobProcessorFactory.getFlowProcessor(job);
		PipelineNode pipelineNode = dtoFactory.newDTO(PipelineNode.class);
		pipelineNode.setJobCiId(projectProcessor.getTranslateJobName());
		pipelineNode.setName(job.getName());
		pipelineNode.setParameters(ParameterProcessors.getConfigs(job));
		pipelineNode.setPhasesInternal(projectProcessor.getInternals());
		pipelineNode.setPhasesPostBuild(projectProcessor.getPostBuilds());

		return pipelineNode;
	}

	public static PipelinePhase createStructurePhase(String name, boolean blocking, List<AbstractProject> items) {
		PipelinePhase pipelinePhase = dtoFactory.newDTO(PipelinePhase.class);
		pipelinePhase.setName(name);
		pipelinePhase.setBlocking(blocking);

		PipelineNode[] tmp = new PipelineNode[items.size()];
		for (int i = 0; i < tmp.length; i++) {
			if (items.get(i) != null) {
				tmp[i] = ModelFactory.createStructureItem(items.get(i));

			} else {
				logger.warn("One of referenced jobs is null, your Jenkins config probably broken, skipping this job...");
			}
		}

		pipelinePhase.setJobs(Arrays.asList(tmp));

		return pipelinePhase;
	}


	public static SnapshotNode createSnapshotItem(Run build, boolean metaOnly) {
		SnapshotNode snapshotNode = dtoFactory.newDTO(SnapshotNode.class);
		SCMProcessor scmProcessor = null;
		if (build.getParent() instanceof AbstractProject) {
			scmProcessor = SCMProcessors.getAppropriate(((AbstractProject) build.getParent()).getScm().getClass().getName());
		}

		CIBuildStatus status = CIBuildStatus.FINISHED;
		if (build.hasntStartedYet()) {
			status = CIBuildStatus.QUEUED;
		} else if (build.isBuilding()) {
			status = CIBuildStatus.RUNNING;
		}

		CIBuildResult result = CIBuildResult.UNAVAILABLE;
		if (build.getResult() == Result.SUCCESS) {
			result = CIBuildResult.SUCCESS;
		} else if (build.getResult() == Result.ABORTED) {
			result = CIBuildResult.ABORTED;
		} else if (build.getResult() == Result.FAILURE) {
			result = CIBuildResult.FAILURE;
		} else if (build.getResult() == Result.UNSTABLE) {
			result = CIBuildResult.UNSTABLE;
		}

		if (!metaOnly) {
			AbstractProjectProcessor flowProcessor = JobProcessorFactory.getFlowProcessor(build.getParent());
			List<PipelinePhase> tmpPipelinePhasesInternals = flowProcessor.getInternals();
			List<PipelinePhase> tmpPipelinePhasesPostBuilds = flowProcessor.getPostBuilds();
			ArrayList<String> invokeesNames = new ArrayList<String>();
			appendInvokeesNames(invokeesNames, tmpPipelinePhasesInternals);
			appendInvokeesNames(invokeesNames, tmpPipelinePhasesPostBuilds);
			HashMap<String, ArrayList<Run>> invokedBuilds = getInvokedBuilds(build, invokeesNames);
			snapshotNode.setPhasesInternal((inflatePhases(tmpPipelinePhasesInternals, invokedBuilds)));
			snapshotNode.setPhasesPostBuild(inflatePhases(tmpPipelinePhasesPostBuilds, invokedBuilds));
		}

		snapshotNode.setJobCiId(build.getParent().getName());
		snapshotNode.setName(build.getParent().getName());
		snapshotNode.setBuildCiId(String.valueOf(build.getNumber()));
		snapshotNode.setNumber(String.valueOf(build.getNumber()));
		snapshotNode.setCauses(CIEventCausesFactory.processCauses(build.getCauses()));
		snapshotNode.setDuration(build.getDuration());
		snapshotNode.setEstimatedDuration(build.getEstimatedDuration());
		if (build instanceof AbstractBuild) {
			snapshotNode.setScmData(scmProcessor == null ? null : scmProcessor.getSCMData((AbstractBuild) build));
		}
		snapshotNode.setStartTime(build.getStartTimeInMillis());
		snapshotNode.setParameters(ParameterProcessors.getInstances(build));
		snapshotNode.setResult(result);
		snapshotNode.setStatus(status);

		return snapshotNode;
	}


	public static SnapshotNode createSnapshotItem(Job project, boolean metaOnly) {
		SnapshotNode snapshotNode = dtoFactory.newDTO(SnapshotNode.class);
		AbstractProjectProcessor flowProcessor = JobProcessorFactory.getFlowProcessor(project);
		snapshotNode.setJobCiId(flowProcessor.getTranslateJobName());
		snapshotNode.setName(project.getName());

		if (!metaOnly) {
			snapshotNode.setPhasesPostBuild(inflatePhases(flowProcessor.getPostBuilds(), null));
			snapshotNode.setPhasesInternal(inflatePhases(flowProcessor.getInternals(), null));
		}
		return snapshotNode;
	}

	private static void appendInvokeesNames(ArrayList<String> list, List<PipelinePhase> phases) {
		for (PipelinePhase phase : phases) {
			for (PipelineNode item : phase.getJobs()) {
				if (item != null) {
					if (!list.contains(item.getJobCiId())) list.add(item.getJobCiId());
				} else {
					logger.error("null referenced project encountered; considering it as corrupted configuration and skipping");
				}
			}
		}
	}

	private static HashMap<String, ArrayList<Run>> getInvokedBuilds(Run self, ArrayList<String> invokeesNames) {
		HashMap<String, ArrayList<Run>> result = new HashMap<String, ArrayList<Run>>();
		Job run;
		for (String invokeeName : invokeesNames) {
			run = (Job) Jenkins.getInstance().getItem(invokeeName);
			result.put(invokeeName, getInvokees(self, run));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<Run> getInvokees(Run invoker, Job job) {
		ArrayList<Run> result = new ArrayList<Run>();
		Cause.UpstreamCause tmpCause;
		for (Object o : job.getBuilds()) {
			Run tmpRun = (Run) o;
			for (Cause cause : (List<Cause>) tmpRun.getCauses()) {
				if (!(cause instanceof Cause.UpstreamCause)) continue;

				tmpCause = (Cause.UpstreamCause) cause;
				if (tmpCause.pointsTo(invoker)) {
					result.add(0, tmpRun);
				} else if (tmpCause.pointsTo(invoker.getParent()) && tmpCause.getUpstreamBuild() < invoker.getNumber()) {
					return result;
				}
			}
		}
		return result;
	}

	private static List<SnapshotPhase> inflatePhases(List<PipelinePhase> structures, HashMap<String, ArrayList<Run>> invokedBuilds) {
		List<SnapshotPhase> phases = new ArrayList<SnapshotPhase>();
		for (int i = 0; i < structures.size(); i++) {
			phases.add(i, createSnapshotPhase(structures.get(i), invokedBuilds));
		}
		return phases;
	}

	public static SnapshotPhase createSnapshotPhase(PipelinePhase pipelinePhase, HashMap<String, ArrayList<Run>> invokedBuilds) {
		SnapshotPhase snapshotPhase = dtoFactory.newDTO(SnapshotPhase.class);
		snapshotPhase.setName(pipelinePhase.getName());
		snapshotPhase.setBlocking(pipelinePhase.isBlocking());

		ArrayList<Run> tmpBuilds;
		List<PipelineNode> structures = pipelinePhase.getJobs();
		List<SnapshotNode> tmp = new ArrayList<SnapshotNode>();

		for (int i = 0; i < structures.size(); i++) {
			if (structures.get(i) != null) {
				tmpBuilds = invokedBuilds == null ? null : invokedBuilds.get(structures.get(i).getJobCiId());
				if (tmpBuilds == null || tmpBuilds.size() == 0) {
					tmp.add(i, createSnapshotItem((Job) Jenkins.getInstance().getItem(structures.get(i).getJobCiId()), false));
				} else {
					tmp.add(i, createSnapshotItem(tmpBuilds.get(0), false));
					tmpBuilds.remove(0);
				}
			} else {
				logger.warn("One of referenced jobs is null, your Jenkins config probably broken, skipping the build info for this job...");
			}
		}
		snapshotPhase.setBuilds(tmp);

		return snapshotPhase;
	}


	public static BuildHistory.SCMUser createScmUser(User user) {
		BuildHistory.SCMUser scmUser = new BuildHistory.SCMUser();
		scmUser.setDisplayName(user.getDisplayName());
		scmUser.setFullName(user.getFullName());
		scmUser.setId(user.getId());

		return scmUser;
	}

	public static Set<BuildHistory.SCMUser> createScmUsersList(Set<User> users) {
		Set<BuildHistory.SCMUser> userList = new HashSet<BuildHistory.SCMUser>();
		if (users != null) {
			for (User user : users) {
				userList.add(ModelFactory.createScmUser(user));
			}
		}
		return userList;
	}


	public static CIParameter createParameterConfig(ParameterDefinition pd) {
		return createParameterConfig(pd, CIParameterType.UNKNOWN, null, null);
	}

	public static CIParameter createParameterConfig(ParameterDefinition pd, CIParameterType type) {
		return createParameterConfig(
				pd,
				type,
				pd.getDefaultParameterValue() == null ? null : pd.getDefaultParameterValue().getValue(),
				null);
	}

	public static CIParameter createParameterConfig(ParameterDefinition pd, CIParameterType type, Object defaultValue) {
		return createParameterConfig(pd, type, defaultValue, null);
	}

	public static CIParameter createParameterConfig(String name, CIParameterType type, List<Object> choices) {
		CIParameter ciParameter = dtoFactory.newDTO(CIParameter.class);
		ciParameter.setName(name);
		ciParameter.setType(type);
		ciParameter.setDescription("");
		ciParameter.setChoices(choices.toArray());
		return ciParameter;
	}

	public static CIParameter createParameterConfig(ParameterDefinition pd, CIParameterType type, Object defaultValue, List<Object> choices) {
		CIParameter ciParameter = dtoFactory.newDTO(CIParameter.class);
		ciParameter.setName(pd.getName());
		ciParameter.setType(type);
		ciParameter.setDescription(pd.getDescription());
		ParameterValue tmp;
		if (type != CIParameterType.UNKNOWN && type != CIParameterType.PASSWORD) {
			if (defaultValue != null) {
				ciParameter.setDefaultValue(defaultValue);
			} else {
				tmp = pd.getDefaultParameterValue();
				ciParameter.setDefaultValue(tmp == null ? "" : tmp.getValue());
			}
			if (choices != null) {
				ciParameter.setChoices(choices.toArray());
			}
		}

		return ciParameter;
	}


	public static CIParameter createParameterInstance(CIParameter pc, Object rawValue) {
	    String value = rawValue == null ? null : rawValue.toString();
		return dtoFactory.newDTO(CIParameter.class)
				.setName(pc.getName())
				.setType(pc.getType())
				.setDescription(pc.getDescription())
				.setChoices(pc.getChoices())
				.setDescription(pc.getDescription())
				.setDefaultValue(pc.getDefaultValue())
				.setValue(value);
	}

	public static String generateSubBuildName(List<CIParameter> parameters) {
		List<CIParameter> sortedList = new ArrayList<>();
		for (CIParameter p : parameters) {
			if (p.getType() == CIParameterType.AXIS) {
				sortedList.add(p);
			}
		}

		Collections.sort(sortedList, new Comparator<CIParameter>() {
			@Override
			public int compare(CIParameter p1, CIParameter p2) {
				return p1.getName().compareTo(p2.getName());
			}
		});

		String subBuildName = "";
		if (sortedList.size() > 0) {
			int i = 0;
			for (; i < sortedList.size() - 1; i++) {
				subBuildName += sortedList.get(i).getName() + "=" + sortedList.get(i).getValue().toString() + ",";
			}
			subBuildName += sortedList.get(i).getName() + "=" + sortedList.get(i).getValue().toString();
		}
		return subBuildName;
	}
}
