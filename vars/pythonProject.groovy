String getDockerImage(Map args) {
    Map kwargs = [
        pythonVersion: '3.12',
        poetryVersion: '1.8.3',
        isSlim: true
    ]
    kwargs << args
    String dockerImage = "ghcr.io/withlogicco/poetry:${kwargs.poetryVersion}-python-${kwargs.pythonVersion}"
    if (kwargs.isSlim) {
        dockerImage += '-slim'
    }
    return dockerImage
}

void formatCode(Map args) {
    Map kwargs = [
        pythonVersion: '3.12',
        poetryVersion: '1.8.3',
        imageIsSlim: true,
        srcDir: 'src'
    ]
    kwargs << args
    String dockerImage = getDockerImage(
        poetryVersion: kwargs.poetryVersion,
        pythonVersion: kwargs.pythonVersion,
        isSlim: kwargs.imageIsSlim
        )
    docker
    .image("${dockerImage}")
    .inside('-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root') {
        sh 'poetry install'
        sh "ruff check ${kwargs.srcDir}"
        sh 'ruff format --check .'
    }
}

void testCode(Map args) {
    Map kwargs = [
        pythonVersion: '3.12',
        poetryVersion: '1.8.3',
        dockerArgs: '',
        imageIsSlim: true,
        pytestArgs: '',
        coverageDir: 'coverage-reports',
        coveragercId: '.coveragerc'
    ]
    kwargs << args
    String dockerImage = getDockerImage(
        poetryVersion: kwargs.poetryVersion,
        pythonVersion: kwargs.pythonVersion,
        isSlim: kwargs.imageIsSlim
        )
    docker
    .image("${dockerImage}")
    .inside("""-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root ${kwargs.dockerArgs}""") {
        sh 'poetry install'
        configFileProvider([configFile(fileId: "${kwargs.coveragercId}", variable: 'COVERAGERC')]) {
                sh """poetry run pytest \
                ${kwargs.pytestArgs} \
                --cov \
                --cov-config=${COVERAGERC} \
                --cov-report=xml:${kwargs.coverageDir}/coverage-${kwargs.pythonVersion}.xml \
                --cov-report=html:${kwargs.coverageDir}/htmlcov-${kwargs.pythonVersion}"""
        }
    }
}
