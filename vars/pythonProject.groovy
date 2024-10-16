String getDockerImage(String poetryVersion = '1.8.3', String pythonVersion = '3.12', Bool isSlim = true) {
    String dockerImage = "ghcr.io/withlogicco/poetry:${poetryVersion}-python-${pythonVersion}"
    if (isSlim) {
        dockerImage += '-slim'
    }
    return dockerImage
}

void formatCode(String pythonVersion = '3.12', String srcDir = 'src', Bool isSlim = true) {
    String dockerImage = getDockerImage(poetryVersion, pythonVersion, isSlim)
    docker
    .image("${dockerImage}")
    .inside('-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root') {
        sh 'poetry install'
        sh "ruff check ${srcDir}"
        sh 'ruff format --check .'
    }
}

void testCode(
    String pythonVersion = '3.12',
    String dockerArgs = '',
    String coveragercId = '.coveragerc',
    String coverageDir = 'coverage-reports',
    Bool isSlim = true,
    String poetryVersion = '1.8.3'
    ) {
    String dockerImage = getDockerImage(poetryVersion, pythonVersion, isSlim)
    docker
    .image("${dockerImage}")
    .inside("""-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root ${dockerArgs}""") {
        sh 'poetry install'
        configFileProvider([configFile(fileId: "${coveragercId}", variable: 'COVERAGERC')]) {
                sh """poetry run pytest \
                --resetdb \
                --cov \
                --cov-config=${COVERAGERC} \
                --cov-report=xml:${coverageDir}/coverage-${pythonVersion}.xml \
                --cov-report=html:${coverageDir}/htmlcov-${pythonVersion}"""
        }
    }
}
