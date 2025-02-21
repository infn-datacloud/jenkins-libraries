void analysis(Map args) {
    Map kwargs = [
        sonarHost: 'https://sonar.cloud.io/',
        coverageDir: 'coverage-reports',
        srcDir: 'src',
        testsDir: 'tests',
        pythonVersions: '3.12'
    ] + args
    archiveArtifacts artifacts: "${kwargs.coverageDir}/**/*", fingerprint: true
    sh """docker run --rm \
        -e SONAR_HOST_URL=${kwargs.sonarHost} \
        -e SONAR_TOKEN=${kwargs.sonarToken} \
        -v ${WORKSPACE}:/usr/src \
        sonarsource/sonar-scanner-cli \
        -D sonar.projectKey=${kwargs.sonarOrganization}_${kwargs.sonarProject} \
        -D sonar.organization=${kwargs.sonarOrganization} \
        -D sonar.sources=${kwargs.srcDir} \
        -D sonar.tests=${kwargs.testsDir} \
        -D sonar.python.version='${kwargs.pythonVersions}'
        """
}
