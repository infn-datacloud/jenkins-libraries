void pushImage(String srcImage, String targetImageName, String registryUrl, String registryCredentialsName) {
    sh "docker tag ${srcImage} ${targetImageName}"
    docker.withRegistry("${registryUrl}", "${registryCredentialsName}") {
        if ("${BRANCH_NAME}" == 'main') {
            docker.image("${targetImageName}").push('latest')
        }
        docker.image("${targetImageName}").push("${BRANCH_NAME}")
        docker.image("${targetImageName}").push("${COMMIT_SHA}")
    }
}

void updateReadMe(
    String provider,
    String imageName,
    String registryUser,
    String registryPassword,
    String registryHost
    ) {
    sh """docker run --rm \
        -v ${WORKSPACE}:/myvol \
        -e DOCKER_USER=${registryUser} \
        -e DOCKER_PASS=${registryPassword} \
        chko/docker-pushrm:1 \
        --provider ${provider} \
        --file /myvol/README.md \
        --debug \
        ${registryHost}/${imageName}
        """
}
