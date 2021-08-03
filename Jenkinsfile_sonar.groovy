ToolSonar='sonar-scanner'
SonarQubeServer='sonarqube'
TestResultPath='multi3/target/surefire-reports'
def server
def rtMaven
def buildInfo
def ARTIFACTORY_URL = 'http://182.92.214.141:8081/artifactory/'
def ARTIFACTORY_API_KEY = 'AKCp8jRGcmjskkYxxQK9qz1JM8zhsV9ivYrt7sXT3qycPiBDNkTPDuc8XCumhs5KtZixZQ6XQ'

def RESOLVE_SNAPSHOT_REPO = 'slash-guide-maven-virtual'
def RESOLVE_RELEASE_REPO = 'slash-guide-maven-virtual'
def DEPLOY_SNAPSHOT_REPO = 'slash-guide-maven-dev-local'
def DEPLOY_RELEASE_REPO = 'slash-guide-maven-dev-local'

def PROMOTION_SOURCE_REPO = 'slash-guide-maven-dev-local'
def PROMOTION_TARGET_REPO = 'slash-maven-release-local'

node{
    stage('Artifactory configuration'){
        server = Artifactory.server 'poc-server'
        rtMaven = Artifactory.newMavenBuild()
        rtMaven.tool = 'maven'
        rtMaven.deployer releaseRepo:DEPLOY_RELEASE_REPO, snapshotRepo:DEPLOY_SNAPSHOT_REPO, server: server
        rtMaven.resolver releaseRepo:RESOLVE_RELEASE_REPO, snapshotRepo:RESOLVE_SNAPSHOT_REPO, server: server
        rtMaven.deployer.deployArtifacts = false
        buildInfo = Artifactory.newBuildInfo()
        buildInfo.env.capture = true
    }

    stage('Check out'){
        git url: 'https://gitee.com/mumu79/app-maven.git'
    }

    stage('Maven build'){
        env.JAVA_HOME = tool 'java'
        rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage('Sonar Scan'){
        def scannerHome = tool ToolSonar
        withSonarQubeEnv(SonarQubeServer){
            sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${JOB_NAME} -Dsonar.sources=. -Dsonar.java.binaries=* -Dsonar.junit.reportPaths=${TestResultPath}"
        }
    }

    stage('Collection'){
        timeout(10) {
            waitForQualityGate()
        }

        withSonarQubeEnv(SonarQubeServer){
            // 方式一：httpRequest
            //surl = "${SONAR_HOST_URL}/api/measures/component?component=${JOB_NAME}&metricKeys=alert_status,quality_gate_details,coverage,new_coverage,bugs,new_bugs,reliability_rating,vulnerabilities,new_vulnerabilities,security_rating,sqale_rating,test_success_density,skipped_tests,test_failures,tests,test_errors,sqale_index,sqale_debt_ratio,new_sqale_debt_ratio,duplicated_lines_density&additionalFields=metrics,periods"
            //def response = httpRequest consoleLogResponseBody: true,contentType: 'APPLICATION_JSON',httpMode: 'GET',ignoreSslErrors: true,url: surl

            //echo "Status: "+responses.status
            //echo "Content: "+responses.content
            //def propssonar = readJSON text: responses.content

            //def propssonar = conn.getResponseMessage();

            // 方式二：sh curl
            surl = "${SONAR_HOST_URL}/api/measures/component?component=${JOB_NAME}&metricKeys=alert_status,quality_gate_details,coverage,new_coverage,bugs,new_bugs,reliability_rating,vulnerabilities,new_vulnerabilities,security_rating,sqale_rating,test_success_density,skipped_tests,test_failures,tests,test_errors,sqale_index,sqale_debt_ratio,new_sqale_debt_ratio,duplicated_lines_density&additionalFields=metrics,periods"
            result = sh returnStdout: true ,script: "curl -uadmin:123456 '${surl}'"
            def propssonar = readJSON text: result.trim()
            echo "propssonar: "+propssonar

            if (propssonar != null && propssonar.component.measures) {
                propssonar.component.measures.each{ itm ->
                    if (itm.periods && itm.periods[0].value) {
                        name = "qa.code.quality."+itm.metric
                        value = itm.periods[0].value
                    } else if (itm.value) {
                        name = "qa.code.quality."+itm.metric
                        value = itm.value
                    }
                    rtMaven.deployer.addProperty(name, value)
                }
                //增加sonar扫描结果到artifactory
                rtMaven.deployer.addProperty("qulity.gate.sonarUrl", SONAR_HOST_URL + "/dashboard/index/" + JOB_NAME)
            }
        }
    }

    stage('Deploy to Artifactory'){
        rtMaven.deployer.deployArtifacts buildInfo
        server.publishBuildInfo buildInfo
    }

    stage("JUnit test") {
        //解析测试报告
        // def reportUrl = "/root/.jenkins/workspace/" +buildInfo.name+ "/builds/" +buildInfo.number+ "/performance-reports/JUnit/TEST-artifactory.test.AppTest.xml";
        def reportUrl = "/var/jenkins_home/workspace/" +buildInfo.name+ "/multi3/target/surefire-reports/TEST-artifactory.test.AppTest.xml";
        echo "${reportUrl}"
        sh "cat ${reportUrl}"

        def testSuite = new XmlParser().parse(reportUrl);
        def totalCases = Integer.parseInt( testSuite.attribute("tests"));
        def failures = Integer.parseInt( testSuite.attribute("failures"));
        def errors = Integer.parseInt( testSuite.attribute("errors"));
        def skipped = Integer.parseInt( testSuite.attribute("skipped"));

        echo "=================== testSuite ======================"
        echo "${testSuite}"

        echo "${totalCases}====${failures}====${errors}====${skipped}"

        //计算测试结果
        def passRate = (totalCases - failures - errors - skipped) / totalCases;
        echo "=================== testResult ======================"
        echo "passRate:${passRate}";
        echo "totalCases:${totalCases}";

        //添加元数据
        def pom = readMavenPom file: 'multi3/pom.xml'
        def latestVersionUrl = "${ARTIFACTORY_URL}api/search/latestVersion?g=${pom.parent.groupId.replace(".","/")}&a=${pom.artifactId}&v=${pom.parent.version}&repos=${PROMOTION_SOURCE_REPO}"
        def latestVersionUrlResponse = httpRequest consoleLogResponseBody: true,
                customHeaders: [[name: 'X-JFrog-Art-Api',
                                 value: ARTIFACTORY_API_KEY]],
                ignoreSslErrors: true,
                url: latestVersionUrl
        def warLatestVersion = latestVersionUrlResponse.content
        echo "warLatestVersion:${warLatestVersion}"
        httpRequest httpMode: 'PUT',
                consoleLogResponseBody: true,
                customHeaders: [[name: 'X-JFrog-Art-Api', value: ARTIFACTORY_API_KEY]],
                url: "${ARTIFACTORY_URL}api/storage/${PROMOTION_SOURCE_REPO}/${pom.parent.groupId.replace(".","/")}/${pom.artifactId}/${pom.parent.version}/${pom.artifactId}-${warLatestVersion}.war?properties=passRate=${passRate};totalCases=${totalCases}"

    }

    // sync: 拷贝符合质量关卡的包到指定仓库
    stage('sync') {
        write_file_path = "./sync.spec"
        writeFile file: write_file_path, text: file_contents, encoding: "UTF-8"
        // read file and print it out
        fileContents = readFile file: write_file_path, encoding: "UTF-8"
        println fileContents

        sh 'jfrog rt cp --spec=sync.spec'
    }

    //promotion操作，进行包的升级
    stage('promotion') {
        def promotionConfig = [
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : PROMOTION_TARGET_REPO,
            'comment'            : 'this is the promotion comment',
            'sourceRepo'         : PROMOTION_SOURCE_REPO,
            'status'             : 'Released',
            'includeDependencies': false,
            'failFast'           : true,
            'copy'               : true
        ]
        artiServer.promote promotionConfig
    }

    /*stage('Quality Gate') {
        arti_url = 'http://182.92.214.141:8081/artifactory' //配置artifactory地址

        //通过aql设置质量关卡
        def aql = '''items.find({
            "@build.name": {"$eq" : "''' + buildInfo.name + '''"},
            "@build.number": {"$eq" : "''' + buildInfo.number + '''"},
            "@qa.code.quality.coverage": {"$gte" : "''' + '0.8' + '''"}
        })
        '''

        def response = httpRequest consoleLogResponseBody: true,contentType: 'TEXT_PLAIN',httpMode: 'POST',ignoreSslErrors: true,requestBody: aql,url: "${arti_url}/api/search/aql",authentication: [Username: 'liujy', Password: 'Helloljy']

        echo "Status: " + response.status
        echo "Content: " + response.content
        echo aql
        def props = readJSON text: response.content

        //如果质量关卡没有通过，退出这次构建
        if(props.range.total <= 0){
            error 'Did not pass the quality gate!!!'
        }
    }*/
}