node {
    def server = Artifactory.server 'demo-server'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo = Artifactory.newBuildInfo()

    stage ('Clone') {
        git url: 'https://gitee.com/mumu79/app-maven.git'
    }

    stage('env capture') {
        echo "收集系统变量"
        buildInfo.env.capture = true
    }

    // 添加元数据方式一（如jira issue ID）
    stage('Add jiraResult') {
        def requirements = getRequirementsIds();
        echo "requirements : ${requirements}"
        def revisionIds = getRevisionIds();
        echo "revisionIds : ${revisionIds}"
        rtMaven.deployer.addProperty("project.issues", requirements).addProperty("project.revisionIds", revisionIds)
        rtMaven.deployer.addProperty("JiraUrl", "https://jfrogchina.atlassian.net/browse/" + requirements)
    }

    stage ('Artifactory configuration') {
        rtMaven.tool = 'maven' // Tool name from Jenkins configuration
        rtMaven.deployer releaseRepo: 'slash-maven-dev-local', snapshotRepo: 'slash-maven-dev-local', server: server
        rtMaven.resolver releaseRepo: 'slash-guide-maven-virtual', snapshotRepo: 'slash-guide-maven-virtual', server: server
    }

    stage ('Maven build') {
        rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage ('Deploy to Artifactory') {
        server.publishBuildInfo buildInfo
    }

/*
    // 添加元数据方式二（如jira issue ID）
    stage('Add jiraResult') {
        def requirements = getRequirementsIds();
        echo "requirements : ${requirements}"
        def revisionIds = getRevisionIds();
        echo "revisionIds : ${revisionIds}"
        sh 'curl -uliujy:Helloljy -X PUT "http://182.92.214.141:8081/artifactory/api/storage/slash-maven-dev-local/org/jfrog/test?properties=project.issues=' + requirements+ ';project.revisionIds=' + revisionIds + ';JiraUrl=https://jfrogchina.atlassian.net/browse/' + requirements +'"'
    }
*/

}

//@NonCPS
def getRequirementsIds() {
    def reqIds = "";
    final changeSets = currentBuild.changeSets
    echo 'changeset count:' + changeSets.size().toString()
    echo 'changeSets: ' + changeSets.toString()

    final changeSetIterator = changeSets.iterator()
    while (changeSetIterator.hasNext()) {
        final changeSet = changeSetIterator.next();
        echo 'changeSet: ' + changeSet.toString()

        def logEntryIterator = changeSet.iterator();
        while (logEntryIterator.hasNext()) {
            final logEntry = logEntryIterator.next()
            echo 'logEntry: ' + logEntry.getMsg()

            def patten = ~/#[\w\-_\d]+/;
            def matcher = (logEntry.getMsg() =~ patten);
            echo 'matcher:  ' + matcher

            def count = matcher.getCount();
            echo 'count:  ' + count

            for (int i = 0; i < count; i++) {
                reqIds += matcher[i].replace('#', '') + ","
            }
        }
    }

    echo 'reqIds: ' + reqIds.toString()
    return reqIds;
}
//@NonCPS
def getRevisionIds() {
    def reqIds = "";
    final changeSets = currentBuild.changeSets
    final changeSetIterator = changeSets.iterator()
    while (changeSetIterator.hasNext()) {
        final changeSet = changeSetIterator.next();
        def logEntryIterator = changeSet.iterator();
        while (logEntryIterator.hasNext()) {
            final logEntry = logEntryIterator.next()
            reqIds += logEntry.getRevision() + ","
        }
    }
    return reqIds
}
