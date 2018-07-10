/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.hpe.application.automation.tools.octane.model.processors.builders;

import com.hpe.application.automation.tools.octane.model.ModelFactory;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.tasks.BuildTrigger;
import hudson.tasks.Publisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implementation for discovery/provisioning of an internal phases/steps of the specific Job in context of BuildTrigger
 */
public class BuildTriggerProcessor extends AbstractBuilderProcessor {
	private static final Logger logger = LogManager.getLogger(BuildTriggerProcessor.class);

	public BuildTriggerProcessor(Publisher publisher, AbstractProject project, Set<Job> processedJobs) {
		BuildTrigger t = (BuildTrigger) publisher;
		super.phases = new ArrayList<>();
		List<AbstractProject> items = t.getChildProjects(project.getParent());
		for (Iterator<AbstractProject> iterator = items.iterator(); iterator.hasNext(); ) {
			AbstractProject next = iterator.next();
			if (next == null || processedJobs.contains(next)) {
				iterator.remove();
				logger.warn("encountered null project reference; considering it as corrupted configuration and skipping");
			}
		}
		super.phases.add(ModelFactory.createStructurePhase("downstream", false, items));
	}
}
