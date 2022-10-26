node{
//	def server = Artifactory.server 'demo-server'
	def server = Artifactory.server 'JFrogChina-Server'

	def rtMaven = Artifactory.newMavenBuild()
	def buildInfo

	stage ('Clone') {
		git url: 'https://gitee.com/mumu79/app-maven.git'
	}

	stage ('Artifactory configuration') {
	    rtMaven.tool = 'maven' // Tool name from Jenkins configuration
	    rtMaven.deployer releaseRepo: 'slash-maven-dev-local', snapshotRepo: 'slash-maven-dev-local', server: server
	    rtMaven.resolver releaseRepo: 'slash-maven-virtual', snapshotRepo: 'slash-maven-virtual', server: server
	    buildInfo = Artifactory.newBuildInfo()
	 }

	// 测试 rtdownload 是同步还是异步，测试结果同步
	// 下载完文件后才会执行后续 stage
//	stage ("download a large file"){
//		def downloadSpec = """{
//		 "files": [
//		  	{
//			  "pattern": "slash-maven-dev-local/jfrog-artifactory-pro-7.19.10-compose.tar.gz",
//			  "target": "/Users/jingyil/work/"
//			}
//		 ]
//		}"""
//		server.download spec: downloadSpec
//		echo "download has done."
//	}

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
				'buildNumber': buildInfo.number, //构建号
                'failBuild': true
		]
		def scanResult = server.xrayScan scanConfig
		echo "scanResult:" + scanResult;
	}
}