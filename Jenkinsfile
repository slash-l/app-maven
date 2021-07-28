node{
	def server = Artifactory.server 'art1'
	def rtMaven = Artifactory.newMavenBuild()
	def buildInfo

	stage ('Clone') {
		git url: 'https://gitee.com/mumu79/app-maven.git'
	}

	stage ('Artifactory configuration') {
	    rtMaven.tool = 'maven' // Tool name from Jenkins configuration
	    rtMaven.deployer releaseRepo: 'guide-maven-dev-local', snapshotRepo: 'slash-guide-maven-dev-local', server: server
	    rtMaven.resolver releaseRepo: 'guide-maven-virtual', snapshotRepo: 'slash-guide-maven-virtual', server: server
	    buildInfo = Artifactory.newBuildInfo()
	 }

	stage ('Exec Maven') {
		rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
	}

	stage ('Publish build info') {
		server.publishBuildInfo buildInfo
	}
}