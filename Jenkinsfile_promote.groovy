node {
    def server
    def uploadSpec
    def buildInfo
    def downloadSpec
    def promotionConfig

    stage ('Clone') {
        git url: 'https://gitee.com/mumu79/app-maven.git'
    }

    stage ('Build') {
        server = Artifactory.server 'demo-server'

        // Create the upload spec.
        uploadSpec = readFile 'resources/props-upload.json'

        // Upload to Artifactory.
        buildInfo = server.upload spec: uploadSpec

        // Create the download spec.
        // downloadSpec = readFile 'resources/props-download.json'

        // Download from Artifactory.
        // server.download spec: downloadSpec, buildInfo: buildInfo

        // Publish the build to Artifactory
        server.publishBuildInfo buildInfo
    }

    // sync: 拷贝符合质量关卡的包到指定仓库
    // 定义质量关卡
//    file_contents = '''
//    {
//      "files": [
//        {
//          "aql": {
//            "items.find": {
//             "repo": PROMOTION_SOURCE_REPO,
//             "@test" : {"$eq" : "ok"}
//            }
//          },
//
//          "target": PROMOTION_TARGET_REPO
//        }
//      ]
//    }
//    '''
//
//    stage('sync') {
//        write_file_path = "./sync.spec"
//        writeFile file: write_file_path, text: file_contents, encoding: "UTF-8"
//        // read file and print it out
//        fileContents = readFile file: write_file_path, encoding: "UTF-8"
//        println fileContents
//
//        sh 'jfrog rt cp --spec=sync.spec'
//    }

//    stage('Quality Gate') {
//        //通过aql设置质量关卡
//        def aql = '''items.find({
//            "@build.name": {"$eq" : "''' + buildInfo.name + '''"},
//            "@build.number": {"$eq" : "''' + buildInfo.number + '''"},
//            "@qa.code.quality.coverage": {"$gte" : "''' + '0.8' + '''"}
//        })
//        '''
//
//        def response =
//        httpRequest httpMode: 'POST',
//                consoleLogResponseBody: true,
//                customHeaders: [[name: 'X-JFrog-Art-Api', value: user_apikey]],
//                contentType: 'TEXT_PLAIN',ignoreSslErrors: true,
//                requestBody: aql,url: "${ARTIFACTORY_URL}/api/search/aql"
//
//        echo "Status: " + response.status
//        echo "Content: " + response.content
//        echo aql
//        def props = readJSON text: response.content
//
//        //如果质量关卡没有通过，退出这次构建
//        if(props.range.total <= 0){
//            error 'Did not pass the quality gate!!!'
//        }
//    }

    stage ('Promotion') {
        promotionConfig = [
            //Mandatory parameters
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : 'slash-maven-release-local',

            //Optional parameters
            'comment'            : 'this is the promotion comment',
            'sourceRepo'         : 'slash-maven-snapshot-local',
            'status'             : 'Released',
            'includeDependencies': true,
            'failFast'           : true,
            'copy'               : true
        ]

        // Promote build
        server.promote promotionConfig
    }
}