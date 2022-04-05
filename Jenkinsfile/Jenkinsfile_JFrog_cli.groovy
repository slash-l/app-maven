node{
//	def server = Artifactory.server 'demo-server'
    def server = Artifactory.server 'saas-server'

    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo

    stage ('Clone') {
        git url: 'https://gitee.com/mumu79/app-maven.git'
    }

    stage ('Artifactory configuration') {
        sh "";
    }

    stage ('Exec Maven') {
        rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }

    stage('xray scan'){
        server.publishBuildInfo buildInfo
        def scanConfig = [
                'buildName': buildInfo.name, //构建名称
                'buildNumber': buildInfo.number //构建号
//                'failBuild': true
        ]
        def scanResult = server.xrayScan scanConfig
        echo "scanResult:" + scanResult;
    }
}
