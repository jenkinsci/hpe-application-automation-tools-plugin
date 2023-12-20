pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                script {
                    if (env.JENKINS_URL == 'https://ci.jenkins.io/') {
                        // Builds the plugin using https://github.com/jenkins-infra/pipeline-library
                        bat "dir"
                        bat "pwd"
                        bat "mv settings.xml %MAVEN_HOME\\conf\\settings.xml"
                        buildPlugin(platforms: ['windows'])
                    } else {
                        // Do internal build
                    }
                }
            }
        }
    }
}