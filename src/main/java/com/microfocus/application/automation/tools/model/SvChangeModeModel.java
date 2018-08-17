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

package com.microfocus.application.automation.tools.model;

import com.hp.sv.jsvconfigurator.core.impl.jaxb.ServiceRuntimeConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;

public class SvChangeModeModel extends AbstractSvRunModel {

    private static final SvDataModelSelection NONE_DATA_MODEL = new SvDataModelSelection(SvDataModelSelection.SelectionType.NONE, null);
    private static final SvPerformanceModelSelection NONE_PERFORMANCE_MODEL = new SvPerformanceModelSelection(SvPerformanceModelSelection.SelectionType.NONE, null);
    private final ServiceRuntimeConfiguration.RuntimeMode mode;
    private final SvDataModelSelection dataModel;
    private final SvPerformanceModelSelection performanceModel;

    @DataBoundConstructor
    public SvChangeModeModel(String serverName, boolean force, ServiceRuntimeConfiguration.RuntimeMode mode,
                             SvDataModelSelection dataModel, SvPerformanceModelSelection performanceModel, SvServiceSelectionModel serviceSelection) {
        super(serverName, force, serviceSelection);
        this.mode = mode;
        this.dataModel = dataModel;
        this.performanceModel = performanceModel;
    }

    public ServiceRuntimeConfiguration.RuntimeMode getMode() {
        return mode;
    }

    public SvDataModelSelection getDataModel() {
        return (mode == ServiceRuntimeConfiguration.RuntimeMode.STAND_BY) ? NONE_DATA_MODEL : dataModel;
    }

    public SvPerformanceModelSelection getPerformanceModel() {
        return (mode == ServiceRuntimeConfiguration.RuntimeMode.STAND_BY) ? NONE_PERFORMANCE_MODEL : performanceModel;
    }
}