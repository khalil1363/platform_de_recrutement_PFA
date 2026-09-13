// CD — Recruitment
// Jenkins Script Path: devops/pipelines/cd-recruitment.Jenkinsfile
// Requires: Eureka (8761) + MySQL on host (3306, db RhhjETC)

pipeline {
  agent any

  parameters {
    string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Tag to pull')
  }

  environment {
    DOCKER_HUB_USER = 'lfray'
    DOCKER_CREDENTIALS_ID = 'dockerhub-daam'
    IMAGE_NAME = "${DOCKER_HUB_USER}/daam-recruitment"
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

    stage('Run Recruitment') {
      steps {
        sh """
          docker rm -f daam-recruitment || true
          docker run -d --name daam-recruitment --restart unless-stopped \
            -p 8090:8090 \
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
    success { echo 'CD OK — recruitment on http://localhost:8090' }
    failure { echo 'CD FAILED — recruitment' }
  }
}
