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

import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotPhase;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.AbstractProjectProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
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
		return createStructureItem(job, new HashSet<>());
	}

	public static PipelinePhase createStructurePhase(String name, boolean blocking, List<AbstractProject> items, Set<Job> processedJobs) {
		PipelinePhase pipelinePhase = dtoFactory.newDTO(PipelinePhase.class);
		pipelinePhase.setName(name);
		pipelinePhase.setBlocking(blocking);

		PipelineNode[] tmp = new PipelineNode[items.size()];
		for (int i = 0; i < tmp.length; i++) {
			if (items.get(i) != null) {
				tmp[i] = ModelFactory.createStructureItem(items.get(i), processedJobs);

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

		CIBuildResult result = BuildHandlerUtils.translateRunResult(build);

		if (!metaOnly) {
			AbstractProjectProcessor flowProcessor = JobProcessorFactory.getFlowProcessor(build.getParent());
			List<PipelinePhase> tmpPipelinePhasesInternals = flowProcessor.getInternals();
			List<PipelinePhase> tmpPipelinePhasesPostBuilds = flowProcessor.getPostBuilds();
			List<String> invokeesNames = new ArrayList<>();
			appendInvokeesNames(invokeesNames, tmpPipelinePhasesInternals);
			appendInvokeesNames(invokeesNames, tmpPipelinePhasesPostBuilds);
			Map<String, List<Run>> invokedBuilds = getInvokedBuilds(build, invokeesNames);
			snapshotNode.setPhasesInternal((inflatePhases(tmpPipelinePhasesInternals, invokedBuilds)));
			snapshotNode.setPhasesPostBuild(inflatePhases(tmpPipelinePhasesPostBuilds, invokedBuilds));
		}

		snapshotNode.setJobCiId(build.getParent().getName());
		snapshotNode.setName(build.getParent().getName());
		snapshotNode.setBuildCiId(String.valueOf(build.getNumber()));
		snapshotNode.setNumber(String.valueOf(build.getNumber()));
		snapshotNode.setCauses(CIEventCausesFactory.processCauses(build));
		snapshotNode.setDuration(build.getDuration());
		snapshotNode.setEstimatedDuration(build.getEstimatedDuration());
		if (build instanceof AbstractBuild) {
			snapshotNode.setScmData(scmProcessor == null ? null : scmProcessor.getSCMData((AbstractBuild) build, ((AbstractBuild) build).getProject().getScm()));
		}
		snapshotNode.setStartTime(build.getStartTimeInMillis());
		snapshotNode.setParameters(ParameterProcessors.getInstances(build));
		snapshotNode.setResult(result);
		snapshotNode.setStatus(status);

		return snapshotNode;
	}

	private static PipelineNode createStructureItem(Job job, Set<Job> processedJobs) {
		AbstractProjectProcessor projectProcessor = JobProcessorFactory.getFlowProcessor(job, processedJobs);
		PipelineNode pipelineNode = dtoFactory.newDTO(PipelineNode.class);
		pipelineNode.setJobCiId(projectProcessor.getTranslateJobName());
		pipelineNode.setName(job.getName());
		pipelineNode.setParameters(ParameterProcessors.getConfigs(job));
		pipelineNode.setPhasesInternal(projectProcessor.getInternals());
		pipelineNode.setPhasesPostBuild(projectProcessor.getPostBuilds());

		return pipelineNode;
	}

	private static SnapshotNode createSnapshotItem(Job project, boolean metaOnly) {
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

	private static void appendInvokeesNames(List<String> list, List<PipelinePhase> phases) {
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

	private static Map<String, List<Run>> getInvokedBuilds(Run self, List<String> invokeesNames) {
		Map<String, List<Run>> result = new HashMap<>();
		Job run;
		for (String invokeeName : invokeesNames) {
			run = (Job) Jenkins.getInstance().getItem(invokeeName);
			result.put(invokeeName, getInvokees(self, run));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static List<Run> getInvokees(Run invoker, Job job) {
		List<Run> result = new ArrayList<>();
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

	private static List<SnapshotPhase> inflatePhases(List<PipelinePhase> structures, Map<String, List<Run>> invokedBuilds) {
		List<SnapshotPhase> phases = new ArrayList<>();
		for (int i = 0; i < structures.size(); i++) {
			phases.add(i, createSnapshotPhase(structures.get(i), invokedBuilds));
		}
		return phases;
	}

	private static SnapshotPhase createSnapshotPhase(PipelinePhase pipelinePhase, Map<String, List<Run>> invokedBuilds) {
		SnapshotPhase snapshotPhase = dtoFactory.newDTO(SnapshotPhase.class);
		snapshotPhase.setName(pipelinePhase.getName());
		snapshotPhase.setBlocking(pipelinePhase.isBlocking());

		List<Run> tmpBuilds;
		List<PipelineNode> structures = pipelinePhase.getJobs();
		List<SnapshotNode> tmp = new ArrayList<>();

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

		sortedList.sort(Comparator.comparing(CIParameter::getName));

		StringBuilder subBuildName = new StringBuilder();
		if (sortedList.size() > 0) {
			int i = 0;
			for (; i < sortedList.size() - 1; i++) {
				subBuildName
						.append(sortedList.get(i).getName())
						.append("=")
						.append(sortedList.get(i).getValue().toString())
						.append(",");
			}
			subBuildName
					.append(sortedList.get(i).getName())
					.append("=")
					.append(sortedList.get(i).getValue().toString());
		}
		return subBuildName.toString();
	}
}
