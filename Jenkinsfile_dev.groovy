// 开发环境 Jenkins Pipeline

def currentVersion = 'V1.0.0'

ToolSonar='sonar-scanner'
SonarQubeServer='sonarqube'
TestResultPath='multi3/target/surefire-reports'
def server
def rtMaven
def buildInfo
def ARTIFACTORY_URL = 'http://ip:8081/artifactory/'
def ARTIFACTORY_API_KEY = '---------------------'

def RESOLVE_SNAPSHOT_REPO = 'slash-guide-maven-virtual'
def RESOLVE_RELEASE_REPO = 'slash-guide-maven-virtual'
def DEPLOY_SNAPSHOT_REPO = 'slash-guide-maven-dev-local'
def DEPLOY_RELEASE_REPO = 'slash-guide-maven-dev-local'

def PROMOTION_SOURCE_REPO = 'slash-guide-maven-dev-local'
def PROMOTION_TARGET_REPO = 'slash-maven-release-local'

node {
    stage('Artifactory config') {
        server = Artifactory.server 'poc-server'
        rtMaven = Artifactory.newMavenBuild()
        rtMaven.tool = 'maven'
        rtMaven.deployer releaseRepo: DEPLOY_RELEASE_REPO, snapshotRepo: DEPLOY_SNAPSHOT_REPO, server: server
        rtMaven.resolver releaseRepo: RESOLVE_RELEASE_REPO, snapshotRepo: RESOLVE_SNAPSHOT_REPO, server: server
        rtMaven.deployer.deployArtifacts = false
        buildInfo = Artifactory.newBuildInfo()
        buildInfo.env.capture = true
    }

    stage('Check out') {
        git url: 'https://gitee.com/mumu79/app-maven.git'
    }

    stage('Update Version') {
        sh "sed -i 's/-BUILD_NUMBER-/${currentVersion}/g' pom.xml **/pom.xml"
    }

}