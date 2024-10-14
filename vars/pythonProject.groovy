void formatCode(String pythonVersion, String srcDir) {
    docker.image("ghcr.io/withlogicco/poetry:1.8.3-python-$pythonVersion-slim")
        .inside('-e POETRY_VIRTUALENVS_IN_PROJECT=true -u root:root') {
            sh 'poetry install'
            sh "ruff check ./$srcDir"
            sh 'ruff format --check .'
    }
}
