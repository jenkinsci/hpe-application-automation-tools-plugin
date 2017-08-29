![HPE LOGO](https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Hewlett_Packard_Enterprise_logo.svg/200px-Hewlett_Packard_Enterprise_logo.svg.png)

# HPE automation plugin for Jenkins CI                        

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8ec4415bffe94fda8ae40415388c063e)](https://www.codacy.com/app/HPEbot/hp-application-automation-tools-plugin?utm_source=github.com&utm_medium=referral&utm_content=hpsa/hp-application-automation-tools-plugin&utm_campaign=badger)

Project status:
[![Build status](https://ci.appveyor.com/api/projects/status/gqd0x8ov1ebqjjcu?svg=true)](https://ci.appveyor.com/project/HPEbot/hp-application-automation-tools-plugin)

Latest release branch status:
[![Build status](https://ci.appveyor.com/api/projects/status/gqd0x8ov1ebqjjcu/branch/latest?svg=true)](https://ci.appveyor.com/project/HPEbot/hp-application-automation-tools-plugin/branch/latest)


##### The plugin provides the ability to run HPE products with Jenkins during builds.

## Relevent links
-	**Download the most recent LTS version of the plugin** at [offical plugin Wiki page](https://wiki.jenkins-ci.org/display/JENKINS/HPE+Application+Automation+Tools)
-	**Check the open issues (and add new issues)** at [Jenkins plugin jira](https://issues.jenkins-ci.org/issues/?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20hp-application-automation-tools-plugin)
-	**View the plugin’s usage statistics** at [Jenkins.io plugin stats](http://stats.jenkins.io/plugin-installation-trend/hp-application-automation-tools-plugin.stats.json).

## Development & release timeline 
#### LTS release branch
- Once in 3 to 4 months, we will release an LTS version.
- After a rigid QA cycle, the version will be released to the main Jenkins update center.
- Each release will have feature freeze and code freeze dates that will be published at our Jira. After this dates, We will accept only fixes to issues discovered during the QA to the current release.

#### Current release branches
-	Each pull request merge that will pass module owner QA cycle will trigger a stable release to Jenkins exprimental update center.
- Additional releases handled by the standard pull request process followed by a basic QA cycle of the related module.
-	Release to the Jenkins experimental update center (More information below).
  
## Experimental Jenkins update center
- The experimental center lets us deliver regular version releases per fix and feature.
- The workflow on the experimental update center has the following stages:
	1. On the main page, click Manage Jenkins
	2. Click Manage plugins
	3. Click the Advanced tab
	4. In the “update site” field, enter the following address: http://updates.jenkins-ci.org/experimental/update-center.json
  5. Click Submit.
  6. Click Check now. You will see the new build.
	
- For more information on the experimental update center, see https://jenkins.io/blog/2013/09/23/experimental-plugins-update-center.

## Contribute to the Jenkins plugin
- Contributions of code are always welcome!
- Follow the standard GIT workflow: Fork, Code, Commit, Push and start a Pull request
- Each pull request will be tested, pass static code analysis and code review results.
- All efforts will be made to expedite this process.

#### Guidelines
- Document your code – it enables others to continue the great work you did on the code and update it.
- SonarLint your code – we use sonarQube with its basic built-in rule set. In the future, we will provide direct online access to test with a custom rule set.

### Feel free to contact us on any question related to contributions - hpsauth-[at]-gmail-dot-com



