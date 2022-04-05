import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def user_apikey

withCredentials([string(credentialsId: 'home-arti-key', variable: 'secret_text')]) {
    user_apikey = "${secret_text}"
}

node{
    def artiServer
    def buildInfo
    def rtMaven
    def warVersion

    stage('Prepare') {
        artiServer = Artifactory.server('home-server')
    }

    stage('Check out') {
        git url: 'https://gitee.com/mumu79/app-maven.git'
    }

    //执行maven构建SNAPSHOT包
//    stage('SNAPSHOT Maven Build'){
//        buildInfo = Artifactory.newBuildInfo()
//        buildInfo.env.capture = true
//        rtMaven = Artifactory.newMavenBuild()
//
//        rtMaven.resolver server: artiServer, snapshotRepo: 'slash-maven-virtual', releaseRepo: 'slash-maven-virtual'
//        rtMaven.deployer server: artiServer, snapshotRepo: 'slash-maven-dev-local', releaseRepo: 'slash-maven-release-local'
//
//        rtMaven.tool = 'maven'
//        rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
//
//        artiServer.publishBuildInfo buildInfo
//    }

    //生成Release版本
    stage('Generate Release Version'){
        warVersion = "1.0.${BUILD_NUMBER}"
        def descriptor = Artifactory.mavenDescriptor()
        descriptor.version = warVersion
        descriptor.failOnSnapshot = true
        descriptor.transform()
    }

    //执行maven构建Release包
    stage('Release Maven Build'){
        buildInfo = Artifactory.newBuildInfo()
        buildInfo.env.capture = true
        rtMaven = Artifactory.newMavenBuild()

        rtMaven.resolver server: artiServer, releaseRepo: 'slash-maven-virtual', snapshotRepo: 'slash-maven-virtual'
        rtMaven.deployer server: artiServer, releaseRepo: 'slash-maven-release-local', snapshotRepo: 'slash-maven-dev-local'

        rtMaven.tool = 'maven'
        rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo

        artiServer.publishBuildInfo buildInfo
    }

}
