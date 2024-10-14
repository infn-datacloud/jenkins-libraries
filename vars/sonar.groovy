void analysis(
    String sonarToken,
    String sonarProject,
    String sonarOrganization,
    String sonarHost = 'https://sonar.cloud.io/',
    String coverageDir = 'coverage-reports',
    String srcDir = 'src',
    String testsDir = 'tests',
    String pythonVersions = '3.12'
) {
    archiveArtifacts artifacts: "${coverageDir}/**/*", fingerprint: true
    sh """docker run --rm \
        -e SONAR_HOST_URL=${sonarHost} \
        -e SONAR_TOKEN=${sonarToken} \
        -v ${WORKSPACE}:/usr/src \
        sonarsource/sonar-scanner-cli \
        -D sonar.projectKey=${sonarOrganization}_${sonarProject} \
        -D sonar.organization=${sonarOrganization} \
        -D sonar.sources=${srcDir} \
        -D sonar.tests=${testsDir} \
        -D sonar.python.version='${pythonVersions}'
        """
}
