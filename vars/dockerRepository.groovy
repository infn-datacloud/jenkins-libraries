/* groovylint-disable-next-line BuilderMethodWithSideEffects, FactoryMethodName */
Object buildImage(String imageName, String dockerfile, String pythonVersion = '', List<String> customTags = []) {
    buildArgs = ''
    name = imageName
    if ("${pythonVersion}" != '') {
        buildArgs += "--build-arg PYTHON_VERSION=${pythonVersion}"
        customTags.add("python-${pythonVersion}")
    }
    if (customTags.size() > 0) {
        tags = customTags.join('-')
        name += ":${tags}"
    }
    return docker.build("${name}", "-f ${dockerfile} ${buildArgs} .")
}

void pushImage(
    Object srcImage,
    String registryUrl,
    String registryCredentialsName,
    String latestMatch = 'python-3.11'
    ) {
    /* groovylint-disable-next-line NoDef, UnusedVariable, VariableTypeRequired */
    def (imageName, imageTag) = srcImage.tag().tokenize(':')
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
            srcImage.push("${env.BRANCH_NAME}-${imageTag}")
        }
        srcImage.push("${env.GIT_COMMIT}-${imageTag}")
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
    String pythonVersion = '',
    List<String> customTags = []
    ) {
    Object img
    stage('Build image') {
        img = buildImage(imageName, dockerfile, pythonVersion, customTags)
    }
    stage('Push image to registry') {
        pushImage(img, registryUrl, registryCredentialsName)
    }
    stage('Update repository description') {
        updateReadMe(img, registryUser, registryPassword, registryHost, registryType)
    }
}
