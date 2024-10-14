void formatCode(String pythonVersion, String srcDir) {
    docker.image("ghcr.io/withlogicco/poetry:1.8.3-python-$pythonVersion-slim")
        .inside('-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root') {
            sh 'poetry install'
            sh "ruff check ./$srcDir"
            sh 'ruff format --check .'
    }
}

void testCode(String dockerArgs = '', String coverageDir = 'coverage-reports') {
    docker
    .image("ghcr.io/withlogicco/poetry:1.8.3-python-${pythonVersion}-slim")
    .inside("""-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root ${dockerArgs}""") {
        sh 'poetry install'
        configFileProvider([configFile(fileId:  '.coveragerc', variable: 'COVERAGERC')]) {
                sh """poetry run pytest \
                --resetdb \
                --cov \
                --cov-config=${COVERAGERC} \
                --cov-report=xml:${coverageDir}/coverage-${pythonVersion}.xml \
                --cov-report=html:${coverageDir}/htmlcov-${pythonVersion}"""
        }
    }
}
