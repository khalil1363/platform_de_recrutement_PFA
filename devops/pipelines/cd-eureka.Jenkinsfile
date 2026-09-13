// CD — Eureka only
// Jenkins Script Path: devops/pipelines/cd-eureka.Jenkinsfile

pipeline {
  agent any

  parameters {
    string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Tag to pull')
  }

  environment {
    DOCKER_HUB_USER = 'lfray'
    DOCKER_CREDENTIALS_ID = 'dockerhub-daam'
    IMAGE_NAME = "${DOCKER_HUB_USER}/daam-eureka"
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

    stage('Run Eureka') {
      steps {
        sh """
          docker rm -f daam-eureka || true
          docker run -d --name daam-eureka --restart unless-stopped \
            -p 8761:8761 \
            -e SPRING_PROFILES_ACTIVE=docker \
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
    success { echo 'CD OK — eureka on http://localhost:8761' }
    failure { echo 'CD FAILED — eureka' }
  }
}
