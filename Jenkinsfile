pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/gagandeep-singh2212/Shortify-API.git'
            }
        }

        stage('Build') {
            agent {
                docker {
                    image 'maven:3.9.9-eclipse-temurin-17'
                }
            }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t my-app .'
            }
        }

        stage('Stop Old Container') {
            steps {
                sh 'docker rm -f my-app-container || true'
            }
        }

        stage('Run Container') {
            steps {
                sh 'docker run -d --name my-app-container -p 8081:8080 my-app'
            }
        }
    }
}