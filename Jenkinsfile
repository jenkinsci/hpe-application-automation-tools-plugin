if (env.JENKINS_URL == 'https://ci.jenkins.io/') {
    // Builds the plugin using https://github.com/jenkins-infra/pipeline-library
    buildPlugin(
      useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
      configurations: [
        [platform: 'linux', jdk: 11],
    ])
}
else {
    // Do internal build
}
