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

package com.microfocus.application.automation.tools.sse;

import com.microfocus.application.automation.tools.model.SseModel;
import com.microfocus.application.automation.tools.sse.sdk.Args;

import hudson.Util;
import hudson.util.VariableResolver;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */

public class ArgsFactory {
    
    public Args create(SseModel model) {
        
        return new Args(
                
                model.getAlmServerUrl(),
                model.getAlmDomain(),
                model.getClientType(),
                model.getAlmProject(),
                model.getAlmUserName(),
                model.getAlmPassword(),
                model.getRunType(),
                model.getAlmEntityId(),
                model.getTimeslotDuration(),
                model.getDescription(),
                model.getPostRunAction(),
                model.getEnvironmentConfigurationId(),
                model.getCdaDetails());
    }
    
    public Args createResolved(SseModel model, VariableResolver<String> buildResolver) {
        
        return new Args(
                
                model.getAlmServerUrl(),
                Util.replaceMacro(model.getAlmDomain(), buildResolver),
                model.getClientType(),
                Util.replaceMacro(model.getAlmProject(), buildResolver),
                Util.replaceMacro(model.getAlmUserName(), buildResolver),
                model.getAlmPassword(),
                model.getRunType(),
                Util.replaceMacro(model.getAlmEntityId(), buildResolver),
                Util.replaceMacro(model.getTimeslotDuration(), buildResolver),
                model.getDescription(),
                model.getPostRunAction(),
                Util.replaceMacro(model.getEnvironmentConfigurationId(), buildResolver),
                model.getCdaDetails());
    }
}
