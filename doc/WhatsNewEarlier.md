# What's new in earlier versions


This page shows a history of the enhancements made to the OpenText Application Automation Tools Jenkins plugin.

[What&#39;s New in version CE 24.3](#what&#39;s-new-in-version-CE-24&#46;3)

[What&#39;s New in version CE 24.2](#what&#39;s-new-in-version-CE-24&#46;2)

[What&#39;s New in version CE 24.1.0](#what&#39;s-new-in-version-CE-24&#46;1&#46;0)

[What&#39;s New in version CE 23.4 ](#what&#39;s-new-in-version-CE-23&#46;4)

[What&#39;s New in version 23.3.0 ](#what&#39;s-new-in-version-23&#46;3&#46;0)

[What&#39;s New in version 8.0 ](#what&#39;s-new-in-version-8&#46;0)

[What&#39;s New in versions 7.7 and 7.8 ](#what&#39;s-new-in-versions-7&#46;7-and-7&#39;8)

[What&#39;s New in version 7.6 ](#what&#39;s-new-in-version-7&#46;6)

[What&#39;s New in version 7.5 ](#what&#39;s-new-in-version-7&#46;5)

[What&#39;s New in version 7.4 ](#what&#39;s-new-in-version-7&#46;4)

[What&#39;s New in version 7.3 ](#what&#39;s-new-in-version-7&#46;3)

[What&#39;s New in version 7.2 ](#what&#39;s-new-in-version-7&#46;2)

[What&#39;s New in version 7.1 ](#what&#39;s-new-in-version-7&#46;1)

[What&#39;s New in version 7.0 ](#what&#39;s-new-in-version-7&#46;0)

[What&#39;s New in version 6.9 ](#what&#39;s-new-in-version-6&#46;9)

[What&#39;s New in versions 6.7 and 6.8 ](#what&#39;s-new-in-versions-6&#46;7-and-6&#39;8)

[What&#39;s New in version 6.6 ](#what&#39;s-new-in-version-6.6)

[What&#39;s New in version 6.5 ](#what&#39;s-new-in-version-6.5)

[What&#39;s New in version 6.4 ](#what&#39;s-new-in-version-6.4)

[What&#39;s New in version 6.3 ](#what&#39;s-new-in-version-6.3)

[What&#39;s New in version 6.0 ](#what&#39;s-new-in-version-6.0)

[What&#39;s New in version 5.9 ](#what&#39;s-new-in-version-5.9)


## What&#39;s New in version CE 24&#46;3

**Java 21**
- Starting with version 24.3 version fo the plugin, Java 21 will be required to run

**ALM Octane**
- Bug fixes

## What&#39;s New in version CE 24&#46;2

**UFT One**
- Added permission checks in ALM jobs configurations on all <doFill*Items> methods (CVE-2024-4211).
- Added Job/ExtendedRead permission handing for FS / ALM / ALM Lab job configurations.
- Fixed unexpected Test Suites Rerun Behavior related to reports for FS jobs.

**UFT Digital Lab**
- Admins can configure whether apps can be uploaded to the Shared assets. If it is not allowed, in the "Upload app to Digital Lab Server" build step there is no option to select Shared assets in the workspace selection menu.

**ALM Octane**
- Bug fixes
  - Fixed nga.log that unexpectedly stopped writing data
  - Fixed the 'Test Run Report URL' field population when running tests on different machines in the same job
  - Other fixes

**Service Virtualization**
- Added permission checks in SV jobs for the server enumeration field (CVE-2024-4692)

**Security Improvements**
- fixed XXE vulnerabilities: CVE-2024-4189, CVE-2024-4184, CVE-2024-4690

## What&#39;s New in version CE 24&#46;1&#46;0

**UFT One**
- Added support for Cloud browser.
- Fixed proxy issue for Digital Lab connections.

**UFT Digital Lab**
- Added ability to open the ValueEdge Digital Lab wizard from Jenkins via an access key only.

**ALM Octane**
- Newly discovered units will automatically be allocated to a runner. A new folder will be created for each runner.
- Bug fixes.

## What&#39;s New in version CE 23&#46;4

Version CE 23.4 introduced the following enhancements and fixes:

**UFT One**
 - Support for access key authentication to run parallel mobile tests.
 - Ability to run tests on public devices hosted by ValueEdge Digital Lab.

**UFT Digital Lab**
 - Ability to upload an application to a specific workspace during a Jenkins pipeline cycle.

**ALM Octane**
- Bug fixes.

**OpenText Enterprise Performance Engineering**
 - Parameterization support for the Timeslot duration.

## What&#39;s New in version 23&#46;3&#46;0

Version 23.3.0 introduced the following enhancements and fixes:

**ALM Octane**
- Bug fixes.

**UFT One**
 - Company name rebranded to OpenText.
 - UFT Mobile renamed to Digital Lab.
 - Enhancement of File System jobs to work correctly inside Docker.
 - Bug fixes and optimizations.


## What&#39;s New in version 8&#46;0

Version 8.0 introduced the following enhancements and fixes:

**ALM Octane**

- Support for Active choice parameters with Jenkins.
- Bug fixes.

For information about enhancements introduced in previous versions, see [What's new in earlier versions](WhatsNewEarlier.md). 

## What&#39;s New in versions 7.7 and 7.8

Versions 7.7 and 7.8 introduced the following enhancements and fixes:

**ALM Octane**

- Support for environment variables in auto actions.
- Bug fixes.

**ALM**

- 'Ability to use the Jenkins proxy settings to open a connection when verifying the ALM URL.

**UFT One**

- Support running UFT One as a different user.
- Release UFT One license safely when aborting Jenkins builds.
- Bug fixes and optimizations.

## What&#39;s New in version 7&#46;6

Version 7.6 introduced the following enhancements and fixes:

**UFT One**

- Implementation of the **Password** type for the Table of Parameters (for ALM and File System jobs)
- A job level option to hide the logging of parameters (for File System jobs)
- The ability to transfer parameters from one test to another through a batch command (using MTBX, for File System jobs)
- Layout bug fixes and optimizations

**ALM Octane**

- Abilitiy to enable the **ALM Octane pipeline** action for multi-branch projects
- HTTPS support for the Security tool
- Defect fixes
 
## What&#39;s New in version 7&#46;5

Version 7.5 introduced the following enhancements and fixes:

**ALM**

- For the **Upload test result to ALM using field mapping** build step:
    - You can now search for a test set by its name in a specific folder instead of all folders. 
    - Defect fix: The check for the test map name is suppressed if no new test is being created.

**UFT One**

-	Layout fixes that were broken in Jenkins 2.346.1 LTS latest version. 

## What&#39;s New in version 7&#46;4

Version 7.4 introduced the following enhancements:

**ALM Octane**

- Added the **Default branches** setting. This allows ALM Octane to enable the selection of parameters for multi-branch CI jobs used in the *Run CI Job* auto action. This change allows ALM Octane to validate the branch name used for the multi-branch CI job.
- The log4j version was updated from 2.17.1 to 2.17.2.
- Defect fixes.

**UFT One**

- Enhanced data security in UFT One’s Jenkins tasks.  A public-private key pair is used to encrypt and decrypt sensitive data on each execution node. 
-	Access Key authentication support for UFT Mobile (supported from UFT One 2021 R1 Patch1)
-	Support for the Java 11 JAXB dependency.
-	Ability to specify the UFT Report timestamp format, by defining a date format on the Jenkins configuration page. (Manage Jenkins > Configure System, Run Result (UFT Report) timestamp format string section).
-	Console Output text encoding configuration on the job level.
-	Layout improvements. 

## Release notes

Pipeline scripts are empty in the script editor from within job configurations. You can view scripts in the config.xml file for each job. This issue does not affect the functionality of the plugin. **Note:** This limitation only applies to version 7.4. A fix was provided in version 7.4.1 Beta.

## What&#39;s New in version 7&#46;3

Version 7.3 introduced the following enhancements:

**ALM Octane**

- Code improvements for MBT, model-based testing.
- Support for Codeless text executions.
- Defect fixes.

**Model-Based Testing**

- Code improvements.
- Support for Windows and Linux upper case/lower case strings.
- Defect fixes.

**UFT One**

- Partial test result reporting for aborted Jenkins jobs, for file system executions. When a Jenkins job execution is aborted or cancelled, all the test results until that point, will be available in the build.
- Improved parameter configuration for UFT One tests using a new parameter table, for file system and ALM execution. You can now add different types of parameters, for both GUI and API tests. 
- Improvements in the re-run option for failed scenarios in file system executions, where tests finished with a Warning status.
- Improved reporting for UFT One tests with same name, but different locations. 
- Enhancements to secure log4j issues.

**Service Virtualization**

- Ability to switch virtual services to OFFLINE mode.

## What&#39;s New in version 7&#46;2

Version 7.2 introduced the following enhancements:

**General**

- Updated plugin to handle log4j security issue.

**UFT One**

- Parsing of UFT test names by Jenkins.

## What&#39;s New in version 7&#46;1

Version 7.1 introduced the following enhancements:

**General**

- More user-friendly layout

**OpenText Enterprise Performance Engineering**

- Security fix: Beginning with Jenkins version 2.303.3, the OpenText Enterprise Performance Engineering build step requires the security fix introduced in this release

**ALM Octane**

- Support execution of BDD tests in testing framework
- UFT One test runner can inject Octane details as test parameters. (This requires you to define the global parameter ADD_GLOBAL_PARAMETERS_TO_TESTS.)
- Support for the Fortify SSC plugin version 21.1.36 
- Supports triggering test suites from Jenkins, from the Execute tests build step
 
**UFT One**

- Parsing test parameters enhancement for UFT One Tests through Jenkins, for file system executions. For limitations, see [Limitations in Test Parameters syntax](#test-parameters-syntax).
- The **Warning** test status is now handled for tests executed from ALM
- Final job status improvements for jobs exceeding the specified timeout
- More secure credential handling with the introduction of Job level and Global level credential handling
- Defect fix: Handling of special characters and the NULL value for test parameters
- Defect fix: Non-existent test sets are now handled and taken into consideration in the job execution

## What&#39;s New in version 7&#46;0

Version 7.0 introduced the following enhancements:

**ALM Octane**

- Support for pipeline stop
- Support for BDD scenarios in test runners
- Support for version 21.1.36 of the Fortify SSC plugin  
- Defect fix - Octane builds no longer point to obsolete UFTOne run results

**UFT One**

- Ability to use the Micro Focus plugin on an air-gap network (not connected to the Internet) for ALM executions
- Run Results enhancements, including the use of MTBX files for test execution
- Defect fix - Layout issues were handled

**OpenText Enterprise Performance Engineering**

- Defect fix - It is now possible to browse for the Test ID in the Jenkins plugin.

## What&#39;s New in version 6&#46;9

Version 6.9 introduced the following enhancements:

**ALM Octane**

- Enabled branch publisher
- Support added for Java 11

**Workarounds**

- For running UFTM tests from UFT One: If UFTM tests fail in UFT One after changing the UFTM server version in the Jenkins configuration, manually restart UFT One and run the test again.

## What&#39;s New in versions 6.7 and 6.8

Versions 6.7 and 6.8 introduced the following enhancements:

**General**

- Security fixes: Security issues noted in the *Jenkins Security Advisory 2021-04-07* were addressed. For details, search for SECURITY-2132, SECURITY-2175, and SECURITY-2176 in the [Jenkins Security Advisory 2021-04-07 page](https://www.jenkins.io/security/advisory/2021-04-07/). (Version 6.8)
- Added support for the FOD plugin version 6.1.0.
- SonarQube integration: A new option was added to the Sonar pre-build step to skip automatic webhook configuration. (This allows you to run the integration without admin permissions.)
 - Added XSRF protection for running with Lab, Upload result, and Common result upload for compatibility with new versions of ALM.

**ALM Octane – UFT Integration**

- Reduced the length of the folder name for checkout of auto-generated execution jobs.
- [Bug fix] Ability to execute UFT tests not located in job workspace. To show the test name correctly in ALM Octane, define the "UFT_CHECKOUT_FOLDER" parameter.
- [Bug fix] For the Spanish Windows slave: The duration of a UFT test in Jenkins now displays correctly, in seconds.

**ALM Octane**

- Added the option to inject pull requests to ALM Octane with repository in SSH format.
- Auto-fill: After injection of pull-requests, templates related to SCM repositories are auto-filled in ALM Octane (Settings > Spaces-> DevOps > SCM Repositories).

**OpenText Enterprise Performance Engineering**

- The build step “Execute performance test using OpenText Enterprise Performance Engineering” can authenticate to LoadRunner Enterprise using an access key (available from LoadRunner Enterprise 2021 R1).

## What&#39;s New in version 6.6

Version 6.6 introduced the following enhancements:

**General**

- Compliance with the new Jenkins version 2.264 form layout
- Certified for Jenkins version 2.264.1

## What&#39;s New in version 6.5

Version 6.5 introduced the following enhancements:

**General**

- “github-branch-source” dependency was removed.
- Certified for Jenkins version 2.263.1.

**ALM Octane**

- New option to cache the job list for pipeline creation in ALM Octane
- Caching of Octane pipeline roots, to avoid sending non-relevant events/tasks/logs to ALM Octane
- Added ability to execute UFT tests with parameters from ALM Octane
- Bug fix: UFT test descriptions are properly formatted in ALM Octane

## What&#39;s New in version 6.4

Version 6.4 introduced the following enhancements:

**General**

- Multiple dependencies updated. You can verify your Jenkins plugins compatibility with the [plugin dependencies](https://plugins.jenkins.io/hp-application-automation-tools-plugin/#dependencies).
- Certified for Jenkins version 2.249.1.

**ALM Octane**

- Ability to configure folder for UFT Test Runner creation
- Ability to create auto-generated UFT jobs with SSH credentials
- Reduced name length of UFT auto-generated Test Runner jobs
- Added support for Configuration-as-code plugin to ALM Octane configuration
- collectPullRequestsToAlmOctane pipeline step – now supports environment parameters for all fields (including credentialsId)

**UFT One**

- Migration of ALM credentials from the Task configuration page to global configuration

## What&#39;s New in version 6.3

Version 6.3 introduced the following enhancements:

**ALM Octane**

- Ability to disable configuration

- Ability to configure jenkinsUser per ALM Octane workspace and to restrict job visibility during new pipeline creation

- Ability to configure ALM Octane allowed storage for logs

- Support for base64 encoding for jobIds (relevant for ALM Octane behind an Apache server)

- New icons indicating the connected workspaces

- Bug fix: The UFT Discovery job now populates the description of UFT API tests.

- Bug fix: Connectivity issue where ALM indicated that Jenkins was not connected, was resolved.

- Bug fix: CI pipeline jobs are automatically updated when you rename or move jobs.

**UFT One**

- Support for defining the number of iterations for UFT tests

## What&#39;s New in version 6.0

Version 6.0 introduced the following enhancements:

**ALM Octane**

- You can now SET/GET milestones on ALM Octane Pipeline configurations.


## What&#39;s New in version 5.9

Version 5.9 introduced the following enhancements:

**ALM Octane**

- Support for custom converters to test frameworks

- Improvements in logs

- Support for additional versions of the Sonarqube scanner plugin (2.6.1, 2.8.1, 2.9)

- Bug fixes

**UFT Mobile (Mobile Center)**

- Rebrand from &quot;Mobile Center&quot; to &quot;UFT Mobile&quot;

**ALM**

- Improvements in the logic of parsing test case status.

https://issues.jenkins-ci.org/browse/JENKINS-58134 

**UFT**

- Support for ALM 15.0 with SSO 

- Refactoring of HpToolsLauncher

- Bug fixes

Note: This version of the plugin does not work with UFT Mobile 3.3 and UFT 14.03.


