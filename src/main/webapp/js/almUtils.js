/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
if (window.NodeList && !NodeList.prototype.forEach) {
	NodeList.prototype.forEach = Array.prototype.forEach;
}
if (typeof CredScope == "undefined") {
	CredScope = {JOB : "JOB", SYSTEM : "SYSTEM"};
}
if (typeof RUN_FROM_ALM_BUILDER_SELECTOR == "undefined") {
	RUN_FROM_ALM_BUILDER_SELECTOR = 'div[name="builder"][descriptorid="com.microfocus.application.automation.tools.run.RunFromAlmBuilder"]';
}
function setupAlmCredentials() {
	let divMain = null;
	if (document.location.href.indexOf("pipeline-syntax")>0) { // we are on pipeline-syntax page, where runFromAlmBuilder step can be selected only once, so it's ok to use document
		divMain = document;
	} else if (document.currentScript) { // this block is used for non-IE browsers, for the first ALM build step only, it finds very fast the parent DIV (containing all ALM controls)
		divMain = document.currentScript.parentElement.closest(RUN_FROM_ALM_BUILDER_SELECTOR);
	}
	setTimeout( function() {
		prepareTask(divMain)}, 200);
}
function prepareTask(divMain) {
	if (divMain == null) { // this block is needed for IE, but also for non-IE browsers when adding more than one ALM build step
		let divs = document.querySelectorAll(RUN_FROM_ALM_BUILDER_SELECTOR);
		divMain = divs[divs.length-1];
	}

	const lstServerName = divMain.querySelector('select.alm-server-name');
	const lstCredentialsScope = divMain.querySelector('select[name="almCredentialsScope"]');
	const chkSsoEnabled = divMain.querySelector('input[type="checkbox"][name="runfromalm.isSSOEnabled"]');
	const divSysAlmUsername = divMain.querySelector('div.sys-alm-username');
	const divSysAlmClientId = divMain.querySelector('div.sys-alm-client-id');
	const divJobAlmUsername = divMain.querySelector('div.job-alm-username');
	const divJobAlmPwd = divMain.querySelector('div.job-alm-password');
	const divJobAlmClientId = divMain.querySelector('div.job-alm-client-id');
	const divJobAlmSecret = divMain.querySelector('div.job-alm-secret');

	selectCredentialsType();
	if (typeof chkSsoEnabled.onclick !== "function") {
		chkSsoEnabled.onclick = selectCredentialsType;
	}
	if (typeof lstCredentialsScope.onchange !== "function") {
		lstCredentialsScope.onchange = selectCredentialsType;
	}
	if (typeof lstServerName.onchange !== "function") {
		lstServerName.onchange = resetCredentials;
	}
	function selectCredentialsType() {
		const isJobScope = lstCredentialsScope.value === CredScope.JOB;
		const isSSO = chkSsoEnabled.checked;

		if (isJobScope) {
			[divSysAlmUsername, divSysAlmClientId].forEach(function(div) { div.querySelector("select").name += "_x"; div.style.display = "none"; });
			divJobAlmUsername.querySelector("input").name = "almUserName";
			divJobAlmPwd.querySelector("input").name = "almPassword";
			divJobAlmClientId.querySelector("input").name = "almClientID";
			divJobAlmSecret.querySelector("input").name = "almApiKey";
			divJobAlmUsername.style.display = divJobAlmPwd.style.display = isSSO ? "none" : "block";
			divJobAlmClientId.style.display = divJobAlmSecret.style.display = isSSO ? "block" : "none";
		} else {
			divSysAlmUsername.querySelector("select").name = "almUserName";
			divSysAlmClientId.querySelector("select").name = "almClientID";
			[divJobAlmUsername, divJobAlmPwd, divJobAlmClientId, divJobAlmSecret].forEach(function(div) { div.querySelector("input").name += "_x"; div.style.display = "none"; });
			divSysAlmUsername.style.display = isSSO ? "none" : "block";
			divSysAlmClientId.style.display = isSSO ? "block" : "none";
		}
	}
	function resetCredentials() {
		let evt;
		try {
			evt = new UIEvent("change", { "view": window, "bubbles": false, "cancelable": true });
		} catch(e) { // IE does not support UIEvent constructor
			evt = document.createEvent("UIEvent");
			evt.initUIEvent("change", false, true, window, 1);
		}
		[divJobAlmSecret, divJobAlmPwd].forEach(function(div) {
			const btnUpdate = div.querySelector(".hidden-password-update-btn");
			btnUpdate && btnUpdate.click();
		});
		[divJobAlmClientId, divJobAlmUsername, divJobAlmSecret, divJobAlmPwd].forEach(function(div) {
			const input = div.querySelector("input");
			input.value = "";
			input.dispatchEvent(evt);
		});
	}
}
