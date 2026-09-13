// CI — Eureka only
// Jenkins Script Path: devops/pipelines/ci-eureka.Jenkinsfile

pipeline {
  agent any

  parameters {
    string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
  }

  environment {
    DOCKER_HUB_USER = 'lfray'
    DOCKER_CREDENTIALS_ID = 'dockerhub-daam'
    IMAGE_NAME = "${DOCKER_HUB_USER}/daam-eureka"
    IMAGE_TAG = "${params.IMAGE_TAG}"
    MAVEN_IMAGE = 'maven:3.9.9-eclipse-temurin-17'
  }

  options {
    skipDefaultCheckout(true)
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '10'))
  }

  stages {
    stage('Checkout') {
      steps {
        checkout([
          $class: 'GitSCM',
          branches: scm.branches,
          userRemoteConfigs: scm.userRemoteConfigs,
          extensions: [
            [$class: 'CloneOption', shallow: true, depth: 1, noTags: true, timeout: 60],
            [$class: 'CleanBeforeCheckout']
          ]
        ])
      }
    }

    stage('Maven clean package') {
      steps {
        sh """
          docker run --rm \
            -v "\$PWD":/app \
            -v maven-repo:/root/.m2 \
            -w /app \
            ${MAVEN_IMAGE} \
            mvn -B -f backend/eureka/pom.xml clean package -DskipTests
        """
      }
    }

    stage('SonarQube') {
      steps {
        withSonarQubeEnv('SonarQube') {
          sh """
            docker run --rm --add-host=host.docker.internal:host-gateway \
              -v "\$PWD":/usr/src \
              -w /usr/src \
              -e SONAR_HOST_URL="\$SONAR_HOST_URL" \
              -e SONAR_TOKEN="\$SONAR_AUTH_TOKEN" \
              sonarsource/sonar-scanner-cli:11 \
              -Dsonar.projectKey=daam-eureka \
              -Dsonar.projectName=DAAM-eureka \
              -Dsonar.sources=backend/eureka/src/main/java \
              -Dsonar.java.binaries=backend/eureka/target/classes \
              -Dsonar.sourceEncoding=UTF-8
          """
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 15, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Docker build') {
      steps {
        sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} backend/eureka"
      }
    }

    stage('Docker push') {
      steps {
        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
          sh 'echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin'
          sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
          sh 'docker logout'
        }
      }
    }
  }

  post {
    success { echo 'CI OK — eureka' }
    failure { echo 'CI FAILED — eureka' }
  }
}
