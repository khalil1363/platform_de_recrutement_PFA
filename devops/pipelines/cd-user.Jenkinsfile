// CD — User
// Jenkins Script Path: devops/pipelines/cd-user.Jenkinsfile
// Requires: Eureka (8761) + MySQL on host (3306, db us)

pipeline {
  agent any

  parameters {
    string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Tag to pull')
  }

  environment {
    DOCKER_HUB_USER = 'lfray'
    DOCKER_CREDENTIALS_ID = 'dockerhub-daam'
    IMAGE_NAME = "${DOCKER_HUB_USER}/daam-user"
    IMAGE_TAG = "${params.IMAGE_TAG}"
  }

  options {
    skipDefaultCheckout(true)
    timestamps()
    disableConcurrentBuilds()
  }

  stages {
    stage('Docker login') {
      steps {
        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
          sh 'echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin'
        }
      }
    }

    stage('Docker pull') {
      steps {
        sh "docker pull ${IMAGE_NAME}:${IMAGE_TAG}"
      }
    }

    stage('Run User') {
      steps {
        sh """
          docker rm -f daam-user || true
          docker run -d --name daam-user --restart unless-stopped \
            -p 8089:8089 \
            -e SPRING_PROFILES_ACTIVE=docker \
            --add-host=host.docker.internal:host-gateway \
            ${IMAGE_NAME}:${IMAGE_TAG}
        """
      }
    }

    stage('Docker logout') {
      steps {
        sh 'docker logout'
      }
    }
  }

  post {
    success { echo 'CD OK — user on http://localhost:8089' }
    failure { echo 'CD FAILED — user' }
  }
}
