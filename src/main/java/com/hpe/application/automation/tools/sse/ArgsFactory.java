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

package com.hpe.application.automation.tools.sse;

import com.hpe.application.automation.tools.model.SseModel;
import com.hpe.application.automation.tools.sse.sdk.Args;

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
