// 开发环境 Jenkins Pipeline

def currentVersion = 'V1.0.0'
def promoteVersion = 'V1.1.0'

def server
def rtMaven
def buildInfo
def ARTIFACTORY_URL = 'http://182.92.214.141:8081/artifactory/'
def ARTIFACTORY_API_KEY = 'AKCp8jRGcmjskkYxxQK9qz1JM8zhsV9ivYrt7sXT3qycPiBDNkTPDuc8XCumhs5KtZixZQ6XQ'

def RESOLVE_SNAPSHOT_REPO = 'slash-guide-maven-virtual'
def RESOLVE_RELEASE_REPO = 'slash-guide-maven-virtual'
def DEPLOY_SNAPSHOT_REPO = 'slash-maven-dev-local'
def DEPLOY_RELEASE_REPO = 'slash-maven-dev-local'

def PROMOTION_SOURCE_REPO = 'slash-maven-test-local'
def PROMOTION_TARGET_REPO = 'slash-maven-release-local'

node {
//    stage('Artifactory config') {
//        server = Artifactory.server 'demo-server'
//        rtMaven = Artifactory.newMavenBuild()
//        rtMaven.tool = 'maven'
//        rtMaven.deployer releaseRepo: DEPLOY_RELEASE_REPO, snapshotRepo: DEPLOY_SNAPSHOT_REPO, server: server
//        rtMaven.resolver releaseRepo: RESOLVE_RELEASE_REPO, snapshotRepo: RESOLVE_SNAPSHOT_REPO, server: server
//        rtMaven.deployer.deployArtifacts = false
//        buildInfo = Artifactory.newBuildInfo()
//        buildInfo.env.capture = true
//    }

    stage('api test') {
        //调用 newman 执行接口测试,生成json格式报告
        try{
            git url: 'https://gitee.com/mumu79/app-maven.git'
            sh "newman run ./devops/autotest/JFrog.postman_collection.json -r cli,json --reporter-json-export report.json"
        }catch(e){
            echo e.toString()
        }
    }

    stage('collect test result') {
        appPath = "slash-maven-test-local/multi3/multi3/3.7/multi3-3.7.war"

        //解析接口测试报告report.json，作为元数据上传到artifactory
        rest = fileExists'report.json'
        if(rest){
            def props = readJSON file: "report.json"
            testsTotal = props.run.stats.assertions.total.toString()
            testsFailed = props.run.stats.assertions.failed.toString()
            echo "tests.total: ${testsTotal}"
            echo"tests.failed:${testsFailed}"

            //调用artifactory接口，上传元数据
            httpRequest authentication: 'demo-arti',
                    consoleLogResponseBody: true,
                    httpMode: 'PUT',
                    ignoreSslErrors: true,
                    responseHandle: 'NONE',
                    url: "${ARTIFACTORY_URL}/api/storage/${appPath}?properties=qa.test.interface.total=${testsTotal}%7Cqa.test.interface.failed=${testsFailed}&recursive=1"
        }
    }

//    stage('Update Version') {
//        sh "sed -i 's/-BUILD_NUMBER-/${currentVersion}/g' pom.xml **/pom.xml"
//    }
//
//    //生成Release版本
//    stage('Generate Release Version'){
//
//        if( sonar_Total < 4 ) {
//            promoteVersion = "${promoteVersion}.${BUILD_NUMBER}"
//            def descriptor = Artifactory.mavenDescriptor()
//            descriptor.version = promoteVersion
//            descriptor.failOnSnapshot = true
//            descriptor.transform()  // 本地 pom 文件更新版本
//        }else{
//            exit()
//        }
//    }

}