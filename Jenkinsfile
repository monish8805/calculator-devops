pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        // Poll SCM every 2 minutes. If you configure a GitHub webhook instead,
        // replace this with githubPush() (requires the GitHub plugin).
        pollSCM('H/2 * * * *')
    }

    environment {
        // Jenkins runs as a background service with a minimal PATH
        PATH           = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
        DOCKERHUB_USER = 'monish8805'
        IMAGE_NAME     = "${DOCKERHUB_USER}/calculator-api"
        NOTIFY_EMAIL   = 'monureddig@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    // Publish JUnit results whether or not the tests passed
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE_NAME:$BUILD_NUMBER -t $IMAGE_NAME:latest .'
            }
        }

        stage('Push') {
            environment {
                // Private Docker config: avoids the macOS keychain credential helper under a service
                DOCKER_CONFIG = "${WORKSPACE}/.docker"
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh 'echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin'
                    sh 'docker push $IMAGE_NAME:$BUILD_NUMBER'
                    sh 'docker push $IMAGE_NAME:latest'
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                // Idempotent: re-running updates the stack in place
                sh 'IMAGE_TAG=$BUILD_NUMBER docker compose up -d --remove-orphans'
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    for i in $(seq 1 20); do
                        if curl -sf http://localhost:8081/health | grep -q '"UP"'; then
                            echo "Service is healthy"
                            exit 0
                        fi
                        sleep 3
                    done
                    echo "Health check failed"
                    docker compose ps
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            mail to: "${NOTIFY_EMAIL}",
                 subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Pipeline succeeded. Deployed ${IMAGE_NAME}:${env.BUILD_NUMBER}.\n${env.BUILD_URL}"
        }
        failure {
            mail to: "${NOTIFY_EMAIL}",
                 subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Pipeline failed. Check the console output.\n${env.BUILD_URL}"
        }
    }
}
