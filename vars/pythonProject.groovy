String getDockerImage(String pythonVersion = '3.12', String poetryVersion = '1.8.3', boolean isSlim = true) {
    String dockerImage = "ghcr.io/withlogicco/poetry:${poetryVersion}-python-${pythonVersion}"
    if (isSlim) {
        dockerImage += '-slim'
    }
    return dockerImage
}

void formatCode(
    String pythonVersion = '3.12',
    String poetryVersion = '1.8.3',
    boolean imageIsSlim = true,
    String srcDir = 'src'
    ) {
    String dockerImage = getDockerImage(
        poetryVersion: poetryVersion,
        pythonVersion: pythonVersion,
        isSlim: imageIsSlim
        )
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
    String poetryVersion = '1.8.3',
    String dockerArgs = '',
    boolean imageIsSlim = true,
    String pytestArgs = '',
    String coverageDir = 'coverage-reports',
    String coveragercId = '.coveragerc'
    ) {
    String dockerImage = getDockerImage(
        poetryVersion: poetryVersion,
        pythonVersion: pythonVersion,
        isSlim: imageIsSlim
        )
    docker
    .image("${dockerImage}")
    .inside("""-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root ${dockerArgs}""") {
        sh 'poetry install'
        configFileProvider([configFile(fileId: "${coveragercId}", variable: 'COVERAGERC')]) {
                sh """poetry run pytest \
                ${pytestArgs} \
                --cov \
                --cov-config=${COVERAGERC} \
                --cov-report=xml:${coverageDir}/coverage-${pythonVersion}.xml \
                --cov-report=html:${coverageDir}/htmlcov-${pythonVersion}"""
        }
    }
}
