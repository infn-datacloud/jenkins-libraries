/* groovylint-disable-next-line BuilderMethodWithSideEffects, FactoryMethodName */
Object buildImage(Map args) {
    Map kwargs = [pythonVersion: '', customTags: []] + args
    String buildArgs = ''
    String name = kwargs.imageName
    if ("${kwargs.pythonVersion}" != '') {
        buildArgs += "--build-arg PYTHON_VERSION=${kwargs.pythonVersion}"
        kwargs.customTags.add("python${kwargs.pythonVersion}")
    }
    if (kwargs.customTags.size() > 0) {
        String tags = kwargs.customTags.join('-')
        name += ":${tags}"
    }
    return docker.build("${name}", "-f ${kwargs.dockerfile} ${buildArgs} .")
}

void pushImage(Map args) {
    Map kwargs = [latestMatch: 'python3.11'] + args
    /* groovylint-disable-next-line NoDef, UnusedVariable, VariableTypeRequired */
    def (imageName, imageTag) = kwargs.srcImage.tag().tokenize(':')
    docker.withRegistry("${kwargs.registryUrl}", "${kwargs.registryCredentialsName}") {
        String latestGitTag = sh(returnStdout: true, script: 'git tag --sort version:refname | tail -1').trim()
        if (imageTag =~ kwargs.latestMatch) {
            if (env.BRANCH_NAME == latestGitTag) {
                kwargs.srcImage.push('latest')
            }
            kwargs.srcImage.push("${env.BRANCH_NAME}")
        }
        kwargs.srcImage.push("${env.BRANCH_NAME}-${imageTag}")
    }
}

void updateReadMe(Map args) {
    Map kwargs = [registryType: 'dockerhub'] + args
    String imageName = kwargs.srcImage.imageName()
    sh """docker run --rm \
        -v ${WORKSPACE}:/myvol \
        -e DOCKER_USER=${kwargs.registryUser} \
        -e DOCKER_PASS=${kwargs.registryPassword} \
        chko/docker-pushrm:1 \
        --provider ${kwargs.registryType} \
        --file /myvol/README.md \
        --debug \
        ${kwargs.registryHost}/${imageName}
        """
}

/* groovylint-disable-next-line BuilderMethodWithSideEffects, FactoryMethodName */
void buildAndPushImage(Map args) {
    Map kwargs = [registryType: 'dockerhub', pythonVersion: '', customTags: []] + args
    Object img
    stage('Build image') {
        img = buildImage(
            imageName: kwargs.imageName,
            dockerfile: kwargs.dockerfile,
            pythonVersion: kwargs.pythonVersion,
            customTags: kwargs.customTags
            )
    }
    stage('Push image to registry') {
        pushImage(
            srcImage: img,
            registryUrl: kwargs.registryUrl,
            registryCredentialsName: kwargs.registryCredentialsName
            )
    }
    stage('Update repository description') {
        updateReadMe(
            srcImage: img,
            registryUser: kwargs.registryUser,
            registryPassword: kwargs.registryPassword,
            registryHost: kwargs.registryHost,
            registryType: kwargs.registryType
            )
    }
}

void periodicTrigger(String period) {
    cron(env.BRANCH_NAME.find(/^v/) ? period : '')
}
