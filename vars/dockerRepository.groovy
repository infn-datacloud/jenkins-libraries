/* groovylint-disable-next-line BuilderMethodWithSideEffects, FactoryMethodName */
Object buildImage(String imageName, String dockerfile, String pythonVersion = '') {
    buildArgs = ''
    tags = ''
    if ("${pythonVersion}" != '') {
        buildArgs += "--build-arg PYTHON_VERSION=${pythonVersion}"
        tags += "python-${pythonVersion}"
    }
    if ("${tags}".length() > 0) {
        imageName = "${imageName}:${tags}"
    }
    return docker.build("${imageName}", "-f ${dockerfile} ${buildArgs} .")
}

void pushImage(
    Object srcImage,
    String registryUrl,
    String registryCredentialsName,
    String latestMatch = 'python-3.12'
    ) {
    (imageName, imageTag) = srcImage.tag().tokenize(':')
    docker.withRegistry("${registryUrl}", "${registryCredentialsName}") {
        if ("${env.BRANCH_NAME}" == 'main') {
            gitTag = sh(returnStdout: true, script: 'git tag --sort version:refname | tail -1').trim()
            srcImage.push()
            if (imageTag.contains("${latestMatch}")) {
                srcImage.push('latest')
                srcImage.push("${gitTag}")
            }
            srcImage.push("${gitTag}-${imageTag}")
        } else {
            srcImage.push("${imageTag}-${env.BRANCH_NAME}")
        }
        srcImage.push("${imageTag}-${env.GIT_COMMIT}")
    }
}

void updateReadMe(
    Object srcImage,
    String registryUser,
    String registryPassword,
    String registryHost,
    String registryType = 'dockerhub'
    ) {
    imageName = srcImage.imageName()
    sh """docker run --rm \
        -v ${WORKSPACE}:/myvol \
        -e DOCKER_USER=${registryUser} \
        -e DOCKER_PASS=${registryPassword} \
        chko/docker-pushrm:1 \
        --provider ${registryType} \
        --file /myvol/README.md \
        --debug \
        ${registryHost}/${imageName}
        """
}

/* groovylint-disable-next-line BuilderMethodWithSideEffects, FactoryMethodName */
void buildAndPushImage(
    String imageName,
    String dockerfile,
    String registryUrl,
    String registryCredentialsName,
    String registryUser,
    String registryPassword,
    String registryHost,
    String registryType = 'dockerhub',
    String pythonVersion = ''
    ) {
    Object img
    stage('Build image') {
        img = buildImage(imageName, dockerfile, pythonVersion)
    }
    post {
        success {
            echo "Docker image ${imageName} build succeeded!"
        }
        failure {
            echo "Docker image ${imageName} build failed!"
        }
    }
    stage('Push image to registry') {
        pushImage(img, registryUrl, registryCredentialsName)
    }
    post {
        success {
            echo "Docker image ${imageName} push to ${registryUrl} succeeded!"
        }
        failure {
            echo "Docker image ${imageName} push to ${registryUrl} failed!"
        }
    }
    stage('Update repository description') {
        updateReadMe(img, registryUser, registryPassword, registryHost, registryType)
    }
    post {
        success {
            echo "Docker image ${imageName} description update succeeded!"
        }
        failure {
            echo "Docker image ${imageName} description update failed!"
        }
    }
}
