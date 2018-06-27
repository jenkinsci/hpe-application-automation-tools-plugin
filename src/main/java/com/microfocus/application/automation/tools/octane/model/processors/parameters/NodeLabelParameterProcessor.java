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

package com.microfocus.application.automation.tools.octane.model.processors.parameters;

import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterDefinition;
import org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterValue;
import org.jvnet.jenkins.plugins.nodelabelparameter.NodeParameterDefinition;
import org.jvnet.jenkins.plugins.nodelabelparameter.NodeParameterValue;

import java.util.ArrayList;

/**
 * Created by gullery on 19/02/2015.
 */

public class NodeLabelParameterProcessor extends AbstractParametersProcessor {
    NodeLabelParameterProcessor() {
    }

    @Override
    public CIParameter createParameterConfig(ParameterDefinition pd) {
        if (pd instanceof NodeParameterDefinition) {
            NodeParameterDefinition nodePd = (NodeParameterDefinition) pd;
            return ModelFactory.createParameterConfig(pd, CIParameterType.STRING, new ArrayList<Object>(nodePd.allowedSlaves));
        } else if (pd instanceof LabelParameterDefinition) {
            LabelParameterDefinition labelPd = (LabelParameterDefinition) pd;
            return ModelFactory.createParameterConfig(pd, CIParameterType.STRING);
        } else {
            return ModelFactory.createParameterConfig(pd);
        }
    }

    @Override
    public CIParameter createParameterInstance(ParameterDefinition pd, ParameterValue pv) {
        Object value = null;
        if (pv instanceof NodeParameterValue) {
            value = ((NodeParameterValue) pv).getLabel();
        } else if (pv instanceof LabelParameterValue) {
            value = ((LabelParameterValue) pv).getLabel();
        }
        return ModelFactory.createParameterInstance(createParameterConfig(pd), value);
    }
}
