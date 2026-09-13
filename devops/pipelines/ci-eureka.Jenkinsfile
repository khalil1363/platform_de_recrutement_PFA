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
        // Maven/Sonar Docker may leave root-owned files; fix so git clean works
        sh '''
          docker run --rm -v jenkins_home:/var/jenkins_home alpine \
            sh -c "chown -R 1000:1000 \"$PWD\" 2>/dev/null || true"
        '''
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
        // uid 1000 = jenkins user — avoids root-owned target/ next checkout
        sh """
          docker run --rm \
            -u 1000:1000 \
            -e HOME=/tmp \
            -v jenkins_home:/var/jenkins_home \
            -v maven-repo:/tmp/.m2 \
            -w "\$PWD" \
            ${MAVEN_IMAGE} \
            mvn -B -f backend/eureka/pom.xml clean package -DskipTests
        """
      }
    }

    stage('SonarQube') {
      steps {
        withSonarQubeEnv('SonarQube') {
          // Keep .scannerwork in workspace so Jenkins finds report-task.txt for Quality Gate
          sh """
            docker run --rm --add-host=host.docker.internal:host-gateway \
              -v jenkins_home:/var/jenkins_home \
              -w "\$PWD" \
              -e SONAR_HOST_URL="\$SONAR_HOST_URL" \
              -e SONAR_TOKEN="\$SONAR_AUTH_TOKEN" \
              -e SONAR_USER_HOME="\$PWD/.sonar" \
              sonarsource/sonar-scanner-cli:11 \
              -Dsonar.projectKey=daam-eureka \
              -Dsonar.projectName=DAAM-eureka \
              -Dsonar.sources=backend/eureka/src/main/java \
              -Dsonar.java.binaries=backend/eureka/target/classes \
              -Dsonar.sourceEncoding=UTF-8 \
              -Dsonar.working.directory="\$PWD/.scannerwork"
          """
          sh '''
            docker run --rm -v jenkins_home:/var/jenkins_home alpine \
              sh -c "chown -R 1000:1000 \"$PWD/.scannerwork\" \"$PWD/.sonar\" 2>/dev/null || true"
          '''
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
