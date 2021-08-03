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
        server = Artifactory.server 'poc-server'

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