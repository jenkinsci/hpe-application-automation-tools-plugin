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

import hudson.Util;
import hudson.util.VariableResolver;

/**
 * Created by barush on 09/11/2014.
 */
public class AUTEnvironmentModelResolver {
    
    public static AUTEnvironmentResolvedModel resolveModel(
            AutEnvironmentModel autEnvironmentModel,
            VariableResolver<String> buildResolver) {
        
        String resolvedUserName =
                Util.replaceMacro(autEnvironmentModel.getAlmUserName(), buildResolver);
        String resolvedAlmDomain =
                Util.replaceMacro(autEnvironmentModel.getAlmDomain(), buildResolver);
        String resolvedAlmProject =
                Util.replaceMacro(autEnvironmentModel.getAlmProject(), buildResolver);
        String resolvedAutEnvironmentId =
                Util.replaceMacro(autEnvironmentModel.getAutEnvironmentId(), buildResolver);
        String resolvedAutEnvConfId =
                Util.replaceMacro(autEnvironmentModel.getExistingAutEnvConfId(), buildResolver);
        String resolvedAutEnvConfName =
                Util.replaceMacro(autEnvironmentModel.getNewAutEnvConfName(), buildResolver);
        String resolvedJsonPath =
                Util.replaceMacro(autEnvironmentModel.getPathToJsonFile(), buildResolver);
        
        return new AUTEnvironmentResolvedModel(
                autEnvironmentModel.getAlmServerName(),
                autEnvironmentModel.getAlmServerUrl(),
                resolvedUserName,
                autEnvironmentModel.getAlmPassword(),
                resolvedAlmDomain,
                resolvedAlmProject,
                resolvedAutEnvironmentId,
                autEnvironmentModel.isUseExistingAutEnvConf(),
                resolvedAutEnvConfId,
                autEnvironmentModel.isCreateNewAutEnvConf(),
                resolvedAutEnvConfName,
                autEnvironmentModel.getAutEnvironmentParameters(),
                resolvedJsonPath,
                autEnvironmentModel.getOutputParameter());
        
    }
}
