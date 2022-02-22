/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

function loadMobileInfo(a) {
    var buttonStatus = false;
    if (buttonStatus) return;
    buttonStatus = true;
    var recreatJob = document.getElementsByName("runfromfs.recreateJob")[0].checked;
    var mcUserName = document.getElementsByName("runfromfs.mcUserName")[0].value;
    var mcPassword = document.getElementsByName("runfromfs.mcPassword")[0].value;
    var mcTenantId = document.getElementsByName("runfromfs.mcTenantId")[0].value;
    var mcExecToken = document.getElementsByName("runfromfs.mcExecToken")[0].value;
    var authType = document.querySelector('input[name$="authModel"]:checked').value;
    var mcUrl = document.getElementsByName("runfromfs.mcServerName")[0].value;
    var useProxy = document.getElementsByName("proxySettings")[0].checked;
    var proxyAddress = document.getElementsByName("runfromfs.fsProxyAddress")[0].value;
    var useAuthentication = document.getElementsByName("runfromfs.fsUseAuthentication")[0].checked;
    var proxyUserName = document.getElementsByName("runfromfs.fsProxyUserName")[0].value;
    var proxyPassword = document.getElementsByName("runfromfs.fsProxyPassword")[0].value;
    var baseUrl = "";
    var isMcCredentialMissing;
    if ('base' == authType) {
        isMcCredentialMissing = (mcUserName.trim() == "" || mcPassword.trim() == "");
    } else {
        isMcCredentialMissing = mcExecToken.trim() == "";
    }

    const isProxyAddressRequiredButMissing = useProxy && proxyAddress.trim() == "";
    const isProxyCredentialRequiredButMissing = useAuthentication && (proxyUserName.trim() == "" || proxyPassword.trim() == "");
    if (isMcCredentialMissing || isProxyAddressRequiredButMissing || isProxyCredentialRequiredButMissing) {
        document.getElementById("errorMessage").style.display = "block";
        buttonStatus = false;
        return;
    }
    var previousJobId = document.getElementsByName("runfromfs.fsJobId")[0].value;
    //recreate job if checked
    if (recreatJob) {
        previousJobId = "";
    }

    if (!useProxy) {
        proxyAddress = proxyUserName = proxyPassword = "";
    }

    a.getMcServerUrl(mcUrl, function (r) {
        baseUrl = r.responseObject();
        if (baseUrl) {
            baseUrl = baseUrl.trim().replace(/[\/]+$/, "");
        } else {
            ParallelRunnerEnvironment.setEnvironmentError(button, true);
            buttonStatus = false;
            return;
        }
        a.getJobId(baseUrl, mcUserName, mcPassword, mcTenantId, mcExecToken, authType,
            useAuthentication, proxyAddress, proxyUserName, proxyPassword, previousJobId, function (response) {
                var jobId = response.responseObject();
                if (jobId == null) {
                    document.getElementById("errorMessage").style.display = "block";
                    buttonStatus = false;
                    return;
                }
                //hide the error message after success login
                document.getElementById("errorMessage").style.display = "none";
                var openedWindow = window.open('/', 'test parameters', 'height=820,width=1130');
                openedWindow.location.href = 'about:blank';
                openedWindow.location.href = baseUrl + "/integration/#/login?jobId=" + jobId + "&displayUFTMode=true";
                var messageCallBack = function (event) {
                    if (event && event.data && event.data == "mcCloseWizard") {
                        a.populateAppAndDevice(baseUrl, mcUserName, mcPassword, mcTenantId, mcExecToken, authType, useAuthentication, proxyAddress, proxyUserName, proxyPassword, jobId, function (app) {
                            var jobInfo = app.responseObject();
                            let deviceId = "", OS = "", manufacturerAndModel = "", targetLab = "";
                            if (jobInfo['deviceJSON']) {
                                if (jobInfo['deviceJSON']['deviceId']) {
                                    deviceId = jobInfo['deviceJSON']['deviceId'];
                                }
                                if (jobInfo['deviceJSON']['OS']) {
                                    OS = jobInfo['deviceJSON']['OS'];
                                }
                                if (jobInfo['deviceJSON']['manufacturerAndModel']) {
                                    manufacturerAndModel = jobInfo['deviceJSON']['manufacturerAndModel'];
                                }
                            }
                            if (jobInfo['deviceCapability']) {
                                if (jobInfo['deviceCapability']['OS']) {
                                    OS = jobInfo['deviceCapability']['OS'];
                                }
                                if (jobInfo['deviceCapability']['manufacturerAndModel']) {
                                    manufacturerAndModel = jobInfo['deviceCapability']['manufacturerAndModel'];
                                }
                                if (jobInfo['deviceCapability']['targetLab']) {
                                    targetLab = jobInfo['deviceCapability']['targetLab'];
                                }
                            }
                            document.getElementsByName("runfromfs.fsDeviceId")[0].value = deviceId;
                            document.getElementsByName("runfromfs.fsOs")[0].value = OS;
                            document.getElementsByName("runfromfs.fsManufacturerAndModel")[0].value = manufacturerAndModel;
                            document.getElementsByName("runfromfs.fsTargetLab")[0].value = targetLab;
                            document.getElementsByName("runfromfs.fsLaunchAppName")[0].value = jobInfo['definitions']['launchApplicationName'];
                            document.getElementsByName("runfromfs.fsInstrumented")[0].value = jobInfo['definitions']['instrumented'];
                            document.getElementsByName("runfromfs.fsAutActions")[0].value = jobInfo['definitions']['autActions'];
                            document.getElementsByName("runfromfs.fsDevicesMetrics")[0].value = jobInfo['definitions']['deviceMetrics'];
                            document.getElementsByName("runfromfs.fsExtraApps")[0].value = jobInfo['extraApps'];
                            document.getElementsByName("runfromfs.fsJobId")[0].value = jobInfo['jobUUID'];
                            buttonStatus = false;
                            document.getElementById("errorMessage").style.display = "none";
                            window.removeEventListener("message", messageCallBack, false);
                            openedWindow.close();
                        });
                    }
                };
                window.addEventListener("message", messageCallBack, false);

                function checkChild() {
                    if (openedWindow && openedWindow.closed) {
                        clearInterval(timer);
                        buttonStatus = false;
                    }
                }

                var timer = setInterval(checkChild, 500);
            });
    });

}

function hideAndMoveAdvancedBody(_id) {
    const tBody = document.querySelector("#" + _id).parentNode; // initial advanced block content
    const initialAdvancedBlock = tBody.previousSibling; // advanced link button block and here was hidden the initial advanced block content
    initialAdvancedBlock.querySelector(".advancedBody").appendChild(tBody); // moves the initial advanced block content back to the hidden block
    initialAdvancedBlock.querySelector(".advancedLink").style = ""; // enables once again the advanced link
}