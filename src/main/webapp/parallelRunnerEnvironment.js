/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */
if (typeof RUN_FROM_FS_BUILDER_SELECTOR == "undefined") {
	RUN_FROM_FS_BUILDER_SELECTOR = 'div[name="builder"][descriptorid="com.microfocus.application.automation.tools.run.RunFromFileBuilder"]';
}

/**
 * Prototype that represents the modal dialog.
 * @constructor
 */
function ModalDialog() {}

/**
 * Add the style sheet to the provided modal node.
 * @param modalCSS the modal style sheet link
 */
ModalDialog.addStyleSheet = function(modalCSS) {
	const ss = document.styleSheets;
	for (let i = 0, max = ss.length; i < max; i++) {
		if (ss[i].href == modalCSS)
			return;
	}

	var link = document.createElement("link");
	Utils.addStyleSheet(link,modalCSS);
	document.head.appendChild(link);
};

/**
 * Generate the browser item for the browsers dialog.
 * @param iconSrc the icons source
 * @param browserId the id of the browser
 * @param labelText the label text
 * @param checked true if the radio should be checked, false otherwise
 * @returns {*}
 */
ModalDialog.generateBrowserItem = function(iconSrc,browserId,labelText,checked) {
	var browserRadio = document.createElement("input");
	browserRadio.setAttribute("class","browser-item-child customRadio");
	browserRadio.setAttribute("type","radio");
	browserRadio.setAttribute("name","browser-radio");

	if(checked) {
		browserRadio.setAttribute("checked", "true");
	}

	browserRadio.setAttribute("id",browserId);

	var browserImage = document.createElement("img");
	browserImage.setAttribute("class","browser-item-child");
	browserImage.setAttribute("src",iconSrc);

	var browserItem = document.createElement("div");
	browserItem.setAttribute("class","browser-item");

	var browserLabel = document.createElement("div");
	browserLabel.setAttribute("class","browser-item-child browser-name");
	browserLabel.innerHTML = labelText;

	browserItem.appendChild(browserRadio);
	browserItem.appendChild(browserImage);
	browserItem.appendChild(browserLabel);

	return browserItem;
};

ModalDialog.saveBrowserSettings = function(modalNode) {
	// retrieve the button that was used to open the env wizard
	const parent = ModalDialog.currentEnv;

	let radioButtons = modalNode.querySelectorAll('input[type="radio"]');

	for(let i =0; i < radioButtons.length;i++) {
		if(radioButtons[i].checked) {
			ParallelRunnerEnv.setUpBrowserEnvironment(parent,radioButtons[i],modalNode);
		}
	}
};

ModalDialog.setSelectedBrowser = function(modal,browserId){
	if(ModalDialog.browsers == null) return false;

	// check if the provided browser id matches any of the available browsers
	var selectedBrowser = null;
	for(var i = 0; i < ModalDialog.browsers.length; i++) {
		if(ModalDialog.browsers[i].toLowerCase() === browserId.toLowerCase()) {
			selectedBrowser = ModalDialog.browsers[i];
		}
	}

	// no match, select the default one
	if(selectedBrowser == null) return false;

	var radioButton = modal.querySelector("input[id=" + selectedBrowser + "]");

	if(radioButton == null) return false;

	// set the corresponding radio button to be checked
	radioButton.checked = true;

	return true;
};

/**
 * Hide the modal with the given modalId.
 * @param modalId the modal id
 * @returns {boolean} true if the modal was hidden, false otherwise
 */
ModalDialog.hide = function(modalId) {
	var modal = document.getElementById(modalId);

	if(modal == null) return false;

	modal.style.display = "none";
};

/**
 * Generate the modal dialog
 * @constructor
 */
ModalDialog.generate = function(path) {
	var modalCSS = path + "plugin/hp-application-automation-tools-plugin/css/modal_dialog.css";

	// add the css style
	ModalDialog.addStyleSheet(modalCSS);

	var browsersModal = document.createElement("div");
	browsersModal.setAttribute("id","browsersModal");
	browsersModal.setAttribute("class","modal");

	var modalContent = document.createElement("div");
	modalContent.setAttribute("class","modal-content");

	var modalHeader = document.createElement("div");
	modalHeader.setAttribute("class","modal-header");

	var span = document.createElement("div");
	span.innerHTML = "x";
	span.setAttribute("class","close");
	span.setAttribute("onclick","ModalDialog.hide('browsersModal')");

	var title = document.createElement("div");
	title.innerHTML = "Choose a browser";
	title.setAttribute("class","modal-title");

	var modalBody = document.createElement("div");
	modalBody.setAttribute("class","modal-body");

	var iconsSRC = path + "plugin/hp-application-automation-tools-plugin/ParallelRunner/icons/";

	var chromeBrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/chrome.svg','Chrome','Chrome',true);
	var firefoxBrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/firefox.svg','Firefox','Firefox',false);
	var firefox64BrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/firefox.svg','Firefox64','Firefox 64',false);
	var ieBrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/explorer.svg','IE','IE',false);
	var ie64BrowserItem = ModalDialog.generateBrowserItem(iconsSRC + 'svg/explorer.svg','IE64','IE 64',false);

	// retain the available browsers
	ModalDialog.browsers = ["Chrome","Firefox","Firefox64","IE","IE64"];

	// add the browser items to the modal body
	modalBody.appendChild(chromeBrowserItem);
	modalBody.appendChild(firefoxBrowserItem);
	modalBody.appendChild(firefox64BrowserItem);
	modalBody.appendChild(ieBrowserItem);
	modalBody.appendChild(ie64BrowserItem);

	var saveText = document.createElement('div');
	saveText.innerHTML = "SAVE";
	saveText.setAttribute("class","save-text");
	saveText.setAttribute("onclick",'ModalDialog.saveBrowserSettings(browsersModal)');

	var modalFooter = document.createElement("div");
	modalFooter.setAttribute("class","modal-footer");

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
function Utils() {}

/**
 * Check if a string is empty.
 * @param str the string to check
 * @returns {boolean}
 */
Utils.isEmptyString = function(str) {
	return (!str || str.length === 0);
};

Utils.addStyleSheet = function(elem,sheetHref) {
	elem.setAttribute("rel","stylesheet");
	elem.setAttribute("type","text/css");
	elem.setAttribute("href",sheetHref);
};

/**
 * Parse the information received for MobileCenter.
 * @param deviceId - the deviceId string
 * @param os - the os string
 * @param manufacturerAndModel - the manufacturer and model string
 * @returns {string} the parsed information.
 */
Utils.parseMCInformation = function(deviceId, os, manufacturerAndModel) {
	var mc_params = [];
	var os_types = {android : 'Android', ios: 'ios', wp : 'Windows Phone'};

	if(!Utils.isEmptyString(deviceId))
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
			}
			else { // version was given to us
				mc_params.push({name: 'osType', value: os_types[match[1]]});
				mc_params.push({name: 'osVersion', value: match[2] == null ? match[3] : match[2] + match[3]});
			}
		}
	}

	if(!Utils.isEmptyString(manufacturerAndModel))
		mc_params.push({name: 'manufacturerAndModel', value: manufacturerAndModel});

	var result_str = "";
	for(var i = 0; i < mc_params.length; i++) {
		result_str = result_str.concat(mc_params[i].name, " : ", mc_params[i].value, (i===mc_params.length - 1) ? "" : ",");
	}
	return result_str;
};

/**
 * Set the provided jenkins element visibility.
 * @param element - the jenkins element to be hidden
 * @param isVisible - the visible boolean state to be aquired
 */
Utils.setJenkinsElementVisibility = function(element,isVisible) {
	let parent = element.parentElement.closest(".jenkins-form-item");
	parent.style.display = isVisible ? "" : "none";
};

/**
 * Load the mobile center wizard.
 * @param a descriptor
 * @param b the environment wizard button
 */
Utils.loadMC = function(a, b, prEnv){
	b.disabled = true;
	const divMain = prEnv.parentElement.closest(RUN_FROM_FS_BUILDER_SELECTOR);
	const dl = divMain.querySelector("#mobileSpecificSection");
    var mcUserName = dl.querySelector('input[name="mcUserName"]')?.value;
    var mcPassword = dl.querySelector('input[name="mcPassword"]')?.value;
	var mcTenantId = dl.querySelector('input[name="mcTenantId"]')?.value;
    var mcExecToken = dl.querySelector('input[name="mcExecToken"]')?.value;
    var mcAuthType = dl.querySelector('input[name$="authModel"]:checked')?.value;
	var mcUrl = dl.querySelector('select[name="mcServerName"]')?.value;
	var useProxy = dl.querySelector('input[name="proxySettings"]')?.checked;
	var proxyAddress = dl.querySelector('input[name="fsProxyAddress"]')?.value;
	var useProxyAuth = dl.querySelector('input[name="fsUseAuthentication"]')?.checked;
	var proxyUserName = dl.querySelector('input[name="fsProxyUserName"]')?.value;
	var proxyPassword = dl.querySelector('input[name="fsProxyPassword"]')?.value;
    var isMcCredentialMissing;
    if ('base' == mcAuthType) {
        isMcCredentialMissing = !mcUserName || !mcPassword || mcUserName.trim() == "" || mcPassword.trim() == "";
    } else {
        isMcCredentialMissing = !mcExecToken || mcExecToken.trim() == "";
    }

	const isProxyAddressRequiredButMissing = useProxy && (proxyAddress == null || proxyAddress.trim() == "");
	const isProxyCredentialRequiredButMissing = useProxyAuth && (!proxyUserName || !proxyPassword || proxyUserName.trim() == ""  || proxyPassword.trim() == "");
	if(isMcCredentialMissing || isProxyAddressRequiredButMissing || isProxyCredentialRequiredButMissing){
		ParallelRunnerEnv.setEnvironmentError(prEnv,true);
		b.disabled = false;
		return;
	}
	var previousJobId = dl.querySelector('[name="fsJobId"]')?.value;
	a.getMcServerUrl(mcUrl, function(r){
		let baseUrl = r.responseObject();
		if(baseUrl){
			baseUrl = baseUrl.trim().replace(/[\/]+$/, "");
		} else {
			ParallelRunnerEnv.setEnvironmentError(prEnv,true);
			b.disabled = false;
			return;
		}
        a.getJobId(baseUrl, mcUserName, mcPassword, mcTenantId, mcExecToken, mcAuthType, useProxyAuth, proxyAddress, proxyUserName, proxyPassword, previousJobId, function (response) {
			let map = response.responseObject();
			let jobId = '';
			let tenantId = '';
			let isSaaS = false;
			//First need to check the return type(not sure if java map can automatically covert to js map)
			if(map.hasOwnProperty("jobId")) {
				jobId = map["jobId"];
			}
			if(map.hasOwnProperty("TENANT_ID_COOKIE")) {
				tenantId = map["TENANT_ID_COOKIE"];
			}
			if(map.hasOwnProperty("isSaaS")) {
				isSaaS = map["isSaaS"];
			}

			// var jobId = response.responseObject();
			if(jobId == null) {
				ParallelRunnerEnv.setEnvironmentError(prEnv,true);
				b.disabled = false;
				return;
			}
			var openedWindow = window.open('/','test parameters','height=820,width=1130');
			openedWindow.location.href = 'about:blank';
			if (isSaaS) {
				openedWindow.location.href = baseUrl + "/integration8/en/#/main/wizard?TENANTID=" + tenantId + "&jobId=" +  jobId + "&displayUFTMode=true&deviceOnly=true";
			} else {
				openedWindow.location.href = baseUrl+"/integration8/en/#/login?jobId="+jobId+"&displayUFTMode=true&deviceOnly=true";
			}
			var messageCallBack = function (event) {
				if (event?.data=="mcCloseWizard") {
                    a.populateAppAndDevice(baseUrl, mcUserName, mcPassword, mcTenantId, mcExecToken, mcAuthType, useProxyAuth, proxyAddress, proxyUserName, proxyPassword, jobId, function (app) {
						var jobInfo = app.responseObject();
						let deviceId = "", OS = "", manufacturerAndModel = "";
						if(jobInfo['deviceJSON']){
							if(jobInfo['deviceJSON']['deviceId']){
								deviceId = jobInfo['deviceJSON']['deviceId'];
							}
							if(jobInfo['deviceJSON']['OS']){
								OS = jobInfo['deviceJSON']['OS'];
							}
							if(jobInfo['deviceJSON']['manufacturerAndModel']){
								manufacturerAndModel = jobInfo['deviceJSON']['manufacturerAndModel'];
							}
						}
						if(jobInfo['deviceCapability']){
							if(jobInfo['deviceCapability']['OS']){
								OS = jobInfo['deviceCapability']['OS'];
							}
							if(jobInfo['deviceCapability']['manufacturerAndModel']){
								manufacturerAndModel = jobInfo['deviceCapability']['manufacturerAndModel'];
							}
						}
						console.log(deviceId);
						ParallelRunnerEnv.setEnvironmentSettingsInput(prEnv,Utils.parseMCInformation(deviceId,OS,manufacturerAndModel));

						b.disabled = false;
						ParallelRunnerEnv.setEnvironmentError(prEnv,false);
						window.removeEventListener("message",messageCallBack, false);
						openedWindow.close();
					});
				}
			};
			window.addEventListener("message", messageCallBack ,false);
			function checkChild() {
				if (openedWindow && openedWindow.closed) {
					clearInterval(timer);
					b.disabled = false;
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
function ParallelRunnerEnv() {}

/**
 *
 * @param parent
 * @returns {*}
 */
ParallelRunnerEnv.getEnvironmentSettingsInputNode = function (parent) {
	return parent?.querySelector(".jenkins-input,.setting-input") ;
};

/**
 * Set the environment text box input to the given input value,
 * the input corresponds to the clicked environment wizard button.
 * @param parent the parent container of the clicked environment wizard button, like "parallelRunnerEnvironments"
 * @param inputValue the input value to be set
 * @returns {boolean} true if it succeeded, false otherwise.
 */
ParallelRunnerEnv.setEnvironmentSettingsInput = function(parent,inputValue) {
	let settingInput = ParallelRunnerEnv.getEnvironmentSettingsInputNode(parent);
	settingInput && (settingInput.value = inputValue);
};

/**
 * Enable the error div that corresponds to the clicked
 * environment wizard button.
 * @param parent the current environment container
 * @param enable the div visibility state(true - visible, false - hidden)
 * @returns {boolean} true if it succeeded, false otherwise.
 */
ParallelRunnerEnv.setEnvironmentError = function(parent, enable) {
	const errorDiv = parent?.querySelector('div[name="mcSettingsError"]');
	errorDiv && (errorDiv.style.display = enable ? "block" : "none");
};

/**
 * Set the browser selection modal visibility.
 * @param parent - the current environment container
 * @param modalId - the browser modal id
 * @param visible - should the modal be visible?(true / false)
 * @param path - the patch to the root of the plugin
 */
 //TODO refactor ModalDialog to be appended to a parent divMain instead of document, with divMain computed from button
ParallelRunnerEnv.setBrowsersModalVisibility = function(parent,modalId,visible,path) {
	var modal = document.getElementById(modalId);
	// it wasn't generated, so we need to generate it
	if(modal == null) {
		// generate it
		modal = ModalDialog.generate(path);

		// add it to the DOM
		document.body.appendChild(modal);
	}

	ModalDialog.currentEnv = parent;

	modal = document.getElementById(modalId);

	let settingInput = ParallelRunnerEnv.getEnvironmentSettingsInputNode(parent);
	const environmentInputValue = settingInput?.value;

	// set the selected browser to match the one in the input
	if(environmentInputValue != null) {
		var browser = environmentInputValue.split(":");

		// should be of the form browser: BrowserName
		if(browser?.length === 2)
		{
			ModalDialog.setSelectedBrowser (modal,browser[1].trim());
		}
	}

	if(visible) {
		modal.style.display = "block";

		// also allow the user to hide it by clicking anywhere on the window
		window.onclick = function(event) {
			if (event.target === modal) {
				modal.style.display = "none";
			}
		};
	}
	else
		modal.style.display = "none";
};

/**
 * Determine the currently selected environment type.
 * @param parent - the current environment container
 * @returns {*}
 */
ParallelRunnerEnv.getCurrentEnvironmentType = function(parent) {
	let input = parent?.querySelector('input[type="radio"][name$="environmentType"]:checked');
	return input?.defaultValue;
};

/**
 * Set the environment text based on the browser selection.
 * @param parent - the current environment container
 * @param radio - the selected radio
 * @param modal - the browser selection modal
 */
ParallelRunnerEnv.setUpBrowserEnvironment = function(parent,radio,modal) {
	// we can close the modal now
	modal.style.display = "none";
	// based on the browser chosen we will prepare the environment
	ParallelRunnerEnv.setEnvironmentSettingsInput(parent,"browser : " + radio['id']);
};

/**
 * Sets the environment and test set visibility based on the parallel runner checkBox state.
 * @param panel - the current build step container
 * @param visible - the visible boolean state to be aquired
 */
ParallelRunnerEnv.setEnvironmentsVisibility = function(panel, visible) {
	var environments = panel.querySelectorAll("div[name='fileSystemTestSet']");
	if (environments == null || environments.length == 0) return;
	[...environments].forEach(env => {
		Utils.setJenkinsElementVisibility(env,visible);
	});
};

/**
 * Click handler for the environment wizard button.
 * @param btn the environment wizard button
 * @param a first mc argument
 * @param modalId the browser modalId to be shown
 * @param visibility of the modal
 * @param pluginPath the ${root} path
 * @returns {boolean}
 */
ParallelRunnerEnv.onEnvironmentWizardClick = function(a,btn,modalId,visibility,pluginPath) {
	// get the environment type for the current env, it could be: 'web' or 'mobile'
	const prEnv = btn.parentElement.closest('div[name="parallelRunnerEnvironments"]');
	let type = ParallelRunnerEnv.getCurrentEnvironmentType(prEnv);
	if(type == null) return false;

	// if the type is web we need to show the browsers modal
	if(type.toLowerCase() === 'web') {
		ParallelRunnerEnv.setBrowsersModalVisibility(prEnv,modalId,visibility,pluginPath);
		return true;
	}

	// open the mobile center wizard
	if(type.toLowerCase() === 'mobile') {
		try {
			Utils.loadMC(a, btn, prEnv);
		} catch(e) {
			console.error(e);
		} finally {
			btn.disabled = false;
		}
		return true;
	}

	return false;
};

/**
 * Utility class for the RunFromFileSystem model.
 * @constructor
 */
function RunFromFileSystemEnv() {}

/**
 * Sets the visibility of a given multi line text box.
 * @param panel - the current build step container
 * @param name the textbox name
 * @param visible - the visible boolean state to be aquired
 */
RunFromFileSystemEnv.setMultiLineTextBoxVisibility = function(panel, name, visible) {
	var textBox = panel.querySelector(`input[name="${name}"], textarea[name="${name}"]`);
	let parent = textBox.parentElement.closest(".jenkins-form-item");
	parent.style.display = visible ? "" : "none";
};

RunFromFileSystemEnv.setInputVisibility = function(panel, name, visible) {
	var textBox = panel.querySelector(`input[name="${name}"]`);
	Utils.setJenkinsElementVisibility(textBox,visible);
};

RunFromFileSystemEnv.setFsTestsVisibility = function(panel, visible) {
	this.setMultiLineTextBoxVisibility(panel, "fsTests", visible);
};

RunFromFileSystemEnv.setFsReportPathVisibility = function(panel, visible) {
	this.setInputVisibility(panel, "fsReportPath", visible);
};

RunFromFileSystemEnv.setTimeoutVisibility = function (panel, visible) {
	this.setInputVisibility(panel, "fsTimeout", visible);
};

RunFromFileSystemEnv.setParamsVisibility = function(panel, visible) {
	this.setInputVisibility(panel, "areParametersEnabled", visible);
};

/**
 * Hide/Show the corresponding controls based on the parallel runner checkBox state.
 */
function setViewVisibility(panel) {
	const chkParallelRunner = panel.querySelector("input[type=checkbox][name=isParallelRunnerEnabled]");
	updateFsView(panel, chkParallelRunner);
	chkParallelRunner.addEventListener('click', () => {
		updateFsView(panel, chkParallelRunner);
	}, false);
}
function updateFsView(panel, chkParallelRunner) {
	const isParallelRun = chkParallelRunner.checked;
	RunFromFileSystemEnv.setFsTestsVisibility(panel, !isParallelRun);
	RunFromFileSystemEnv.setTimeoutVisibility(panel, !isParallelRun);
	RunFromFileSystemEnv.setParamsVisibility(panel, !isParallelRun);
	//this panel should be automatically shown/hidden, so comment-out it for now to see if all works fine
	//ParallelRunnerEnv.setEnvironmentsVisibility(panel, isParallelRun);
}
function setupFsTask(hasConfigPermission) {
	let divMain = null;
	if (document.location.href.indexOf("pipeline-syntax")>0) { // we are on pipeline-syntax page, where runFromFileBuilder step can be selected only once
		divMain = document;
	} else if (document.currentScript) { // this block is used for non-IE browsers, for the first FS build step only, it finds very fast the parent DIV
		divMain = document.currentScript.parentElement.closest(RUN_FROM_FS_BUILDER_SELECTOR);
	}
	setTimeout(function() {
		prepareFsTask(divMain, hasConfigPermission)}, 100);
}
function prepareFsTask(divMain, hasConfigPermission) {
	if (divMain == null) { // this block is needed for IE, but also for non-IE browsers when adding more than one FS build step
		let divs = document.querySelectorAll(RUN_FROM_FS_BUILDER_SELECTOR);
		divMain = divs[divs.length - 1];
	}
	if (hasConfigPermission) {
		setViewVisibility(divMain);
	} else {
		const chkParallelRunner = divMain.querySelector("input[type=checkbox][name=isParallelRunnerEnabled]");
		if (chkParallelRunner.checked) {
			const buttons = chkParallelRunner.closest("div.optionalBlock-container").querySelectorAll("input[type=button], button");
			buttons.forEach(function (btn) {
				if (!btn.disabled) {
					btn.disabled = true;
					btn.style.cursor = "not-allowed";
					btn.style.pointerEvents = "auto";
				}
			});
		}
	}
}