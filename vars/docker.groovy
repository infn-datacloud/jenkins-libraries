void pushImage(String srcImage, String targetImageName, String registryUrl, String registryCredentialsName) {
    // Login to target registry, retrieve docker image and push it to the registry
    sh "docker tag ${srcImage} ${targetImageName}"
    docker.withRegistry("${registryUrl}", "${registryCredentialsName}") {
        if ("${BRANCH_NAME}" == 'main') {
            docker.image("${targetImageName}").push('latest')
        }
        docker.image("${targetImageName}").push("${BRANCH_NAME}")
        docker.image("${targetImageName}").push("${COMMIT_SHA}")
    }
}
