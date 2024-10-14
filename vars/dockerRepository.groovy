void pushImage(String srcImage, String targetImageName, String registryUrl, String registryCredentialsName) {
    sh "docker tag ${srcImage} ${targetImageName}"
    docker.withRegistry("${registryUrl}", "${registryCredentialsName}") {
        if ("${env.BRANCH_NAME}" == 'main') {
            docker.image("${targetImageName}").push('latest')
        }
        docker.image("${targetImageName}").push("${env.BRANCH_NAME}")
        docker.image("${targetImageName}").push("${env.GIT_COMMIT}")
    }
}

void updateReadMe(
    String imageName,
    String registryUser,
    String registryPassword,
    String registryHost,
    String provider = 'dockerhub'
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
