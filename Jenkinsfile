pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                script {
                    if (env.JENKINS_URL == 'https://ci.jenkins.io/') {
                        // Builds the plugin using https://github.com/jenkins-infra/pipeline-library
                        if (isUnix()) {
                            sh "ls"
                            sh "pwd"
                            sh "cp settings.xml %MAVEN_HOME\\conf\\settings.xml"
                        } else {
                            bat "dir"
                            bat "pwd"
                            bat "mv settings.xml %MAVEN_HOME\\conf\\settings.xml"
                        }
                        buildPlugin(platforms: ['windows'])
                    } else {
                        // Do internal build
                    }
                }
            }
        }
    }
}