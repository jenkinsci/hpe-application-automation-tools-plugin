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

/**
 * Prototype that represents the modal dialog.
 * @constructor
 */
function ModalDialog() {
}

/**
 * Add the style sheet to the provided modal node.
 * @param modalCSS the modal style sheet link
 */
ModalDialog.addStyleSheet = function (modalCSS) {
    var link = document.createElement("link");
    Utils.addStyleSheet(link, modalCSS);
    document.head.appendChild(link);
};

/**
 * Sets the environment wizard button.
 * @param envWizardButton the environment wizard button
 */
ModalDialog.setEnvironmentWizardButton = function (envWizardButton) {
    ModalDialog.envWizardButton = envWizardButton;
};

/**
 * Generate the browser item for the browsers dialog.
 * @param iconSrc the icons source
 * @param browserId the id of the browser
 * @param labelText the label text
 * @param checked true if the radio should be checked, false otherwise
 * @returns {*}
 */
ModalDialog.generateBrowserItem = function (iconSrc, browserId, labelText, checked) {
    var browserRadio = document.createElement("input");
    browserRadio.setAttribute("class", "browser-item-child customRadio");
    browserRadio.setAttribute("type", "radio");
    browserRadio.setAttribute("name", "browser-radio");

    if (checked) {
        browserRadio.setAttribute("checked", "true");
    }

    browserRadio.setAttribute("id", browserId);

    var browserImage = document.createElement("img");
    browserImage.setAttribute("class", "browser-item-child");
    browserImage.setAttribute("src", iconSrc);

    var browserItem = document.createElement("div");
    browserItem.setAttribute("class", "browser-item");

    var browserLabel = document.createElement("div");
    browserLabel.setAttribute("class", "browser-item-child browser-name");
    browserLabel.innerHTML = labelText;

    browserItem.appendChild(browserRadio);
    browserItem.appendChild(browserImage);
    browserItem.appendChild(browserLabel);

    return browserItem;
};

ModalDialog.saveBrowserSettings = function (modalNode) {
    // retrieve the button that was used to open the env wizard
    var button = ModalDialog.envWizardButton;

    var radioButtons = modalNode.querySelectorAll('input[type="radio"]');

    for (var i = 0; i < radioButtons.length; i++) {
        if (radioButtons[i].checked) {
            ParallelRunnerEnvironment.setUpBrowserEnvironment(button, radioButtons[i], modalNode);
        }
    }
};

ModalDialog.setSelectedBrowser = function (modal, browserId) {
    if (ModalDialog.browsers == null) return false;

    // check if the provided browser id matches any of the available browsers
    var selectedBrowser = null;
    for (var i = 0; i < ModalDialog.browsers.length; i++) {
        if (ModalDialog.browsers[i].toLowerCase() === browserId.toLowerCase()) {
            selectedBrowser = ModalDialog.browsers[i];
        }
    }

    // no match, select the default one
    if (selectedBrowser == null) return false;

    var radioButton = modal.querySelector("input[id=" + selectedBrowser + "]");

    if (radioButton == null) return false;

    // set the corresponding radio button to be checked
    radioButton.checked = true;

    return true;
};

/**
 * Hide the modal with the given modalId.
 * @param modalId the modal id
 * @returns {boolean} true if the modal was hidden, false otherwise
 */
ModalDialog.hide = function (modalId) {
    var modal = document.getElementById(modalId);

    if (modal == null) return false;

    modal.style.display = "none";
};

/**
 * Generate the modal dialog
 * @constructor
 */
ModalDialog.generate = function (path) {
    var modalCSS = path + "plugin/hp-application-automation-tools-plugin/css/PARALLEL_RUNNER_UI.css";

    // add the css style
    ModalDialog.addStyleSheet(modalCSS);

    var browsersModal = document.createElement("div");
    browsersModal.setAttribute("id", "browsersModal");
    browsersModal.setAttribute("class", "modal");

    var modalContent = document.createElement("div");
    modalContent.setAttribute("class", "modal-content");

    var modalHeader = document.createElement("div");
    modalHeader.setAttribute("class", "modal-header");

    var span = document.createElement("div");
    span.innerHTML = "x";
    span.setAttribute("class", "close");
    span.setAttribute("onclick", "ModalDialog.hide('browsersModal')");

    var title = document.createElement("div");
    title.innerHTML = "Choose a browser";
    title.setAttribute("class", "modal-title");

    var modalBody = document.createElement("div");
    modalBody.setAttribute("class", "modal-body");

    var iconsSRC = path + "plugin/hp-application-automation-tools-plugin/ParallelRunner/icons/";

    var chromeBrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/chrome.svg', 'Chrome', 'Chrome', true);
    var firefoxBrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/firefox.svg', 'Firefox', 'Firefox', false);
    var firefox64BrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/firefox.svg', 'Firefox64', 'Firefox 64', false);
    var ieBrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/explorer.svg', 'IE', 'IE', false);
    var ie64BrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/explorer.svg', 'IE64', 'IE 64', false);

    // retain the available browsers
    ModalDialog.browsers = ["Chrome", "Firefox", "Firefox64", "IE", "IE64"];

    // add the browser items to the modal body
    modalBody.appendChild(chromeBrowserItem);
    modalBody.appendChild(firefoxBrowserItem);
    modalBody.appendChild(firefox64BrowserItem);
    modalBody.appendChild(ieBrowserItem);
    modalBody.appendChild(ie64BrowserItem);

    var saveText = document.createElement('div');
    saveText.innerHTML = "SAVE";
    saveText.setAttribute("class", "save-text");
    saveText.setAttribute("id", "save-btn");
    saveText.setAttribute("onclick", 'ModalDialog.saveBrowserSettings(browsersModal)');

    var modalFooter = document.createElement("div");
    modalFooter.setAttribute("class", "modal-footer");

    modalFooter.appendChild(saveText);
    modalHeader.appendChild(span);
    modalHeader.appendChild(title);
    modalContent.appendChild(modalHeader);
    modalContent.appendChild(modalBody);
    modalContent.appendChild(modalFooter);
    browsersModal.appendChild(modalContent);

    return browsersModal;
};

/**
 * Multi-purpose utils class.
 * @constructor
 */
function Utils() {
}

/**
 * Find the ancestor of a control by a given tag.
 * @param start - the node from where to start.
 * @param tag - the tag of the ancestor to find.
 * @returns {HTMLElement}
 */
Utils.findAncestorByTag = function (start, tag) {
    tag = tag.toLowerCase();
    while ((start = start.parentElement) && !(start.tagName.toLowerCase() === tag)) {
    }
    return start;
};

/**
 * Find the ancestor of a control by a given name.
 * @param start - the node from where to start.
 * @param tag - the tag of the ancestor to find.
 * @param name - the name attribute value of the ancestor to find.
 * @returns {HTMLElement}
 */
Utils.findAncestorByTagAndName = function (start, tag, name) {
    tag = tag.toLowerCase();
    while ((start = start.parentElement) && start != null && !(start.tagName.toLowerCase() === tag && start.getAttribute("name") === name)) {
    }
    return start;
};
/**
 * Check if a string is empty.
 * @param str the string to check
 * @returns {boolean}
 */
Utils.isEmptyString = function (str) {
    return (!str || str.length === 0);
};

Utils.addStyleSheet = function (elem, sheetHref) {
    elem.setAttribute("rel", "stylesheet");
    elem.setAttribute("type", "text/css");
    elem.setAttribute("href", sheetHref);
};

/**
 * Parse the information received for MobileCenter.
 * @param deviceId - the deviceId string
 * @param os - the os string
 * @param manufacturerAndModel - the manufacturer and model string
 * @returns {string} the parsed information.
 */
Utils.parseMCInformation = function (deviceId, os, manufacturerAndModel) {
    var mc_params = [];
    var os_types = {android: 'Android', ios: 'ios', wp: 'Windows Phone'};

    if (!Utils.isEmptyString(deviceId))
        mc_params.push({name: 'deviceId', value: deviceId});
    else {
        if (!Utils.isEmptyString(os)) {
            // regex for the os string
            // example: ios<=10.2.2 => we need to extract ios,<=,10.2.2
            var regex = /([a-z]+)(>=|>|<|<=)*([0-9].*[0-9])+/gi;
            var match = regex.exec(os);

            if (match == null) { // string does not contain a version
                mc_params.push({name: 'osType', value: os_types[os]});
                mc_params.push({name: 'osVersion', value: 'any'});
            } else { // version was given to us
                mc_params.push({name: 'osType', value: os_types[match[1]]});
                mc_params.push({name: 'osVersion', value: match[2] == null ? match[3] : match[2] + match[3]});
            }
        }
    }

    if (!Utils.isEmptyString(manufacturerAndModel))
        mc_params.push({name: 'manufacturerAndModel', value: manufacturerAndModel});

    var result_str = "";
    for (var i = 0; i < mc_params.length; i++) {
        result_str = result_str.concat(mc_params[i].name, " : ", mc_params[i].value, (i === mc_params.length - 1) ? "" : ",");
    }
    return result_str;
};

/**
 * Set the provided jenkins element visibility.
 * @param element the jenkins element to be hidden
 * @param visible the element visibility state
 */
Utils.setJenkinsElementVisibility = function (element, visible) {
    var parent = Utils.findAncestorByTag(element, 'tr');

    if (visible === false) {
        parent.style.display = "none";
    } else {
        parent.style.display = "";
    }
};

/**
 * Load the mobile center wizard.
 * @param a descriptor
 * @param button the environment wizard button
 */
Utils.loadMC = function (a, button) {
    var buttonStatus = false;
    if (buttonStatus) return;
    buttonStatus = true;
    var mcUserName = document.getElementsByName("runfromfs.mcUserName")[0].value;
    var mcPassword = document.getElementsByName("runfromfs.mcPassword")[0].value;
    var mcTenantId = document.getElementsByName("runfromfs.mcTenantId")[0].value;
    var mcExecToken = document.getElementsByName("runfromfs.mcExecToken")[0].value;
    var mcAuthType = document.querySelector('input[name$="authModel"]:checked').value;
    var mcUrl = document.getElementsByName("runfromfs.mcServerName")[0].value;
    var useProxy = document.getElementsByName("proxySettings")[0].checked;
    var proxyAddress = document.getElementsByName("runfromfs.fsProxyAddress")[0].value;
    var useAuthentication = document.getElementsByName("runfromfs.fsUseAuthentication")[0].checked;
    var proxyUserName = document.getElementsByName("runfromfs.fsProxyUserName")[0].value;
    var proxyPassword = document.getElementsByName("runfromfs.fsProxyPassword")[0].value;
    var baseUrl = "";
    var isMcCredentialMissing;
    if ('base' == mcAuthType) {
        isMcCredentialMissing = mcUserName.trim() == "" || mcPassword.trim() == "";
    } else {
        isMcCredentialMissing = mcExecToken.trim() == "";
    }

    const isProxyAddressRequiredButMissing = useProxy && proxyAddress.trim() == "";
    const isProxyCredentialRequiredButMissing = useAuthentication && (proxyUserName.trim() == "" || proxyPassword.trim() == "");
    if (isMcCredentialMissing || isProxyAddressRequiredButMissing || isProxyCredentialRequiredButMissing) {
        ParallelRunnerEnvironment.setEnvironmentError(button, true);
        buttonStatus = false;
        return;
    }
    var previousJobId = document.getElementsByName("runfromfs.fsJobId")[0].value;
    a.getMcServerUrl(mcUrl, function (r) {
        baseUrl = r.responseObject();
        if (baseUrl) {
            baseUrl = baseUrl.trim().replace(/[\/]+$/, "");
        } else {
            ParallelRunnerEnvironment.setEnvironmentError(button, true);
            buttonStatus = false;
            return;
        }
        a.getJobId(baseUrl, mcUserName, mcPassword, mcTenantId, mcExecToken, mcAuthType, useAuthentication, proxyAddress, proxyUserName, proxyPassword, previousJobId, function (response) {
            var jobId = response.responseObject();
            if (jobId == null) {
                ParallelRunnerEnvironment.setEnvironmentError(button, true);
                buttonStatus = false;
                return;
            }
            var openedWindow = window.open('/', 'test parameters', 'height=820,width=1130');
            openedWindow.location.href = 'about:blank';
            openedWindow.location.href = baseUrl + "/integration/#/login?jobId=" + jobId + "&displayUFTMode=true&deviceOnly=true";
            var messageCallBack = function (event) {
                if (event && event.data && event.data == "mcCloseWizard") {
                    a.populateAppAndDevice(baseUrl, mcUserName, mcPassword, mcTenantId, mcExecToken, mcAuthType, useAuthentication, proxyAddress, proxyUserName, proxyPassword, jobId, function (app) {
                        var jobInfo = app.responseObject();
                        let deviceId = "", OS = "", manufacturerAndModel = "";
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
                        }
                        console.log(deviceId);
                        ParallelRunnerEnvironment.setEnvironmentSettingsInput(button, Utils.parseMCInformation(deviceId, OS, manufacturerAndModel));

                        buttonStatus = false;
                        ParallelRunnerEnvironment.setEnvironmentError(button, false);
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
};

/**
 * Prototype that represents the ParallelRunner environment.
 * @constructor
 */
function ParallelRunnerEnvironment() {
}

/**
 *
 * @param button
 * @returns {*}
 */
ParallelRunnerEnvironment.getEnvironmentSettingsInputNode = function (button) {
    // jelly represents each item as a 'div' with data inside
    var parent = Utils.findAncestorByTagAndName(button._button, "div", "parallelRunnerEnvironments");
    if (parent == null) return null;
    return parent.querySelector(".setting-input");
};

/**
 * Set the environment text box input to the given input value,
 * the input corresponds to the clicked environment wizard button.
 * @param button the clicked environment wizard button
 * @param inputValue the input value to be set
 * @returns {boolean} true if it succeeded, false otherwise.
 */
ParallelRunnerEnvironment.setEnvironmentSettingsInput = function (button, inputValue) {
    var settingInput = ParallelRunnerEnvironment.getEnvironmentSettingsInputNode(button);

    if (settingInput == null) return false;

    settingInput.value = inputValue;

    return true;
};
/**
 *
 * @param button
 * @returns {null}
 */
ParallelRunnerEnvironment.getEnvironmentSettingsInputValue = function (button) {
    // jelly represents each item as a 'div' with data inside
    var settingInput = ParallelRunnerEnvironment.getEnvironmentSettingsInputNode(button);
    if (settingInput == null) return null;
    return settingInput.value;
};

/**
 * Enable the error div that corresponds to the clicked
 * environment wizard button.
 * @param button the environment wizard button
 * @param enable the div visibility state(true - visible, false - hidden)
 * @returns {boolean} true if it succeeded, false otherwise.
 */
ParallelRunnerEnvironment.setEnvironmentError = function (button, enable) {
    const parent = Utils.findAncestorByTagAndName(button._button, "div", "parallelRunnerEnvironments");
    if (parent == null) return false;
    const errorDiv = parent.querySelector('div[name="mcSettingsError"]');
    errorDiv.style.display = enable ? "block" : "none";
};

/**
 * Set the browser selection modal visibility.
 * @param button - the environment button
 * @param modalId - the browser modal id
 * @param visible - should the modal be visible?(true / false)
 * @param path - the patch to the root of the plugin
 */
ParallelRunnerEnvironment.setBrowsersModalVisibility = function (button, modalId, visible, path) {
    var modal = document.getElementById(modalId);
    // it wasn't generated, so we need to generate it
    if (modal == null) {
        // generate it
        modal = ModalDialog.generate(path);

        // add it to the DOM
        document.body.appendChild(modal);
    }

    ModalDialog.setEnvironmentWizardButton(button);

    modal = document.getElementById(modalId);

    var environmentInputValue = ParallelRunnerEnvironment.getEnvironmentSettingsInputValue(button);

    // set the selected browser to match the one in the input
    if (environmentInputValue != null) {
        var browser = environmentInputValue.split(":");

        // should be of the form browser: BrowserName
        if (browser != null && browser.length === 2) {
            ModalDialog.setSelectedBrowser(modal, browser[1].trim());
        }
    }

    if (visible) {
        modal.style.display = "block";

        // also allow the user to hide it by clicking anywhere on the window
        window.onclick = function (event) {
            if (event.target === modal) {
                modal.style.display = "none";
            }
        };
    } else
        modal.style.display = "none";
};

/**
 * Determine the currently selected environment type.
 * @param button - the environment wizard button
 * @returns {*}
 */
ParallelRunnerEnvironment.GetCurrentEnvironmentType = function (button) {
    const parent = Utils.findAncestorByTagAndName(button._button, "div", "parallelRunnerEnvironments");
    if (parent == null) return null;
    const input = parent.querySelector('input[type="radio"][name$="environmentType"]:checked');
    return input ? input.defaultValue : null;
};

/**
 * Set the environment text based on the browser selection.
 * @param button - the environment button
 * @param radio - the selected radio
 * @param modal - the browser selection modal
 */
ParallelRunnerEnvironment.setUpBrowserEnvironment = function (button, radio, modal) {
    // we can close the modal now
    modal.style.display = "none";
    // based on the browser chosen we will prepare the environment
    ParallelRunnerEnvironment.setEnvironmentSettingsInput(button, "browser : " + radio['id']);
};

/**
 * Sets the environment and test set visibility based on the parallel runner checkBox state.
 * @param index - the current build index
 */
ParallelRunnerEnvironment.setEnvironmentsVisibility = function (index) {
    var fsTests = document.getElementsByName("runfromfs.fsTests")[index];
    var parent = Utils.findAncestorByTag(fsTests, "tbody");
    var check = document.getElementsByName("isParallelRunnerEnabled")[index];
    var environment = parent.querySelectorAll("div[name='fileSystemTestSet']")[0];

    if (environment == null) return;

    Utils.setJenkinsElementVisibility(environment, check.checked);
};

/**
 * Click handler for the environment wizard button.
 * @param button the environment wizard button
 * @param a first mc argument
 * @param modalId the browser modalId to be shown
 * @param visibility of the modal
 * @param pluginPath the ${root} path
 * @returns {boolean}
 */
ParallelRunnerEnvironment.onEnvironmentWizardClick = function (button, a, modalId, visibility, pluginPath) {
    // get the environment type for the current env, it could be: 'web' or 'mobile'
    var type = ParallelRunnerEnvironment.GetCurrentEnvironmentType(button);
    if (type == null) return false;

    // if the type is web we need to show the browsers modal
    if (type.toLowerCase() === 'web') {
        ParallelRunnerEnvironment.setBrowsersModalVisibility(button, modalId, visibility, pluginPath);
        return true;
    }

    // open the mobile center wizard
    if (type.toLowerCase() === 'mobile') {
        Utils.loadMC(a, button);
        return true;
    }

    return false;
};

/**
 * Utility class for the RunFromFileSystem model.
 * @constructor
 */
function RunFromFileSystemEnvironment() {
}

/**
 * Sets the visibility of a given multi line text box.
 * @param index the textbox build index
 * @param name the textbox name
 */
RunFromFileSystemEnvironment.setMultiLineTextBoxVisibility = function (index, name) {
    var textBox = document.getElementsByName(name)[index];
    var parentElement = textBox.parentElement;
    var parent = Utils.findAncestorByTag(textBox, "tr");

    // when the text box is not expanded
    if (!parentElement.classList.contains("setting-main")) {
        parent = Utils.findAncestorByTag(parent, 'tr');
    }

    var check = document.getElementsByName("isParallelRunnerEnabled")[index];

    parent.style.display = check.checked ? "none" : "";
};

/**
 * Sets the visibility of a given text box.
 * @param index the textbox build index
 * @param name the textbox name
 */
RunFromFileSystemEnvironment.setTextBoxVisibility = function (index, name) {
    var check = document.getElementsByName("isParallelRunnerEnabled")[index];
    var textBox = document.getElementsByName(name)[index];

    Utils.setJenkinsElementVisibility(textBox, !check.checked);
};

/**
 * Sets the fsTests visibility based on the parallel runner checkBox state.
 * @param index - the current build index
 */
RunFromFileSystemEnvironment.setFsTestsVisibility = function (index) {
    this.setMultiLineTextBoxVisibility(index, "runfromfs.fsTests");
};

/**
 * Sets the fsReportPath visibility based on the parallel runner checkBox state.
 * @param index - the current build index
 */
RunFromFileSystemEnvironment.setFsReportPathVisibility = function (index) {
    this.setTextBoxVisibility(index, "runfromfs.fsReportPath");
};

/**
 * Sets the fsTimeout visibility based on the parallel runner checkbox state.
 * @param index the current build index.
 */
RunFromFileSystemEnvironment.setTimeoutVisibility = function (index) {
    this.setTextBoxVisibility(index, "runfromfs.fsTimeout");
};

/**
 * Hide/Show the corresponding controls based on the parallel runner checkBox state.
 */
function setViewVisibility() {
    var parallelRuns = document.getElementsByName("isParallelRunnerEnabled");

    // go over all the available builds and set their corresponding
    // visibilities based on the parallel runner state
    for (var i = 0; i < parallelRuns.length; i++) {
        RunFromFileSystemEnvironment.setFsTestsVisibility(i);
        RunFromFileSystemEnvironment.setTimeoutVisibility(i);
        RunFromFileSystemEnvironment.setFsReportPathVisibility(i);
        ParallelRunnerEnvironment.setEnvironmentsVisibility(i);
    }
}

document.addEventListener('DOMContentLoaded', function () {
    setViewVisibility();
}, false);
