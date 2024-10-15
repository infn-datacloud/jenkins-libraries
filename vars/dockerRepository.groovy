/* groovylint-disable-next-line BuilderMethodWithSideEffects, FactoryMethodName */
Image buildImage(String imageName, String dockerfile, String pythonVersion = '') {
    buildArgs = ''
    tags = ''
    if ("${pythonVersion}" == '') {
        buildArgs += "--build-arg PYTHON_VERSION=${pythonVersion}"
        tags += "python-${item.value}"
    }
    if ("${tags}".length() > 0) {
        imageName = "${imageName}:${tags}"
    }
    return docker.build("${imageName}", "-f ${dockerfile} ${buildArgs} .")
}

void pushImage(
    Image srcImage,
    String registryUrl,
    String registryCredentialsName,
    String latestMatch = 'python-3.12'
    ) {
    imageName = Image.imageName()
    imageTag = Image.tag()
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
    Image srcImage,
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
    stages {
        stage('Build image') {
            script {
                img = buildImage(imageName, dockerfile, pythonVersion)
            }
        }
        stage('Push image to registry') {
            script {
                pushImage(img, registryUrl, registryCredentialsName)
            }
        }
        stage('Update repository description') {
            script {
                updateReadMe(image, registryUser, registryPassword, registryHost, registryType)
            }
        }
    }
}
