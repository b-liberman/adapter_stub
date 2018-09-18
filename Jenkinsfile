node  {

	def scmVars

	stage('start') {
		echo "Starting pipeline triggered by git"
	}
	
	stage('clone repository') {
		scmVars = checkout scm
		scmVars.each { k, v -> echo "*** ${k}:${v}"}
	}
	
	jarFileName = 'adapter_stub'
	jarFileVersion = currentBuild.number
	
	stage('build project') {
		echo "./gradlew -PjarFileName=${jarFileName} clean build test"
    	sh "./gradlew -PjarFileName=${jarFileName} -PjarFileVersion=${jarFileVersion} clean build test"
	}
	
	def image
	
	stage('build docker image') {
    		image = docker.build("boris/avrotest:${currentBuild.number}", "--build-arg jarFileName=${jarFileName} --build-arg jarFileVersion=${jarFileVersion} .")
    		echo "---------Image name is ${image.imageName()}"
    		echo "---------Image id is ${image.id}"
	}
	
	stage('test in docker') {
    	docker.image("boris/avrotest:${currentBuild.number}").inside() {
    		sh "pwd"
    		sh "ls -la"
    	}
    }
	
	stage('set tag in GIT') {	
	
		if(env.BRANCH_NAME == 'master') {
			
			def tag = "${jarFileName}/${jarFileVersion}"
			sh "git tag -a ${tag} -m 'automatic jenkins tag 1231 Development MB AA'"

			def repositoryUrl = scm.getUserRemoteConfigs()[0].getUrl()
						
			withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "b-liberman", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
     		   	def authenticatedUrl = authenticatedUrl(repositoryUrl, env.USERNAME, env.PASSWORD)
        		sh("git remote set-url origin ${authenticatedUrl}")
        		sh("git push origin tag ${tag}")
    		
    		}
		} else {
			echo "do nothing in any branch but master"
		}
    	
	}
}

def authenticatedUrl(url, username, password){
    encodedUsername = URLEncoder.encode(username as String, "UTF-8")
    encodedPassword = URLEncoder.encode(password as String, "UTF-8")
    def bits = (url as String).split("://")
    if(bits.length == 2){
        return bits[0] + "://${encodedUsername}:${encodedPassword}@" + bits[1] as String
    }
    return "${encodedUsername}:${encodedPassword}@" + bits[0] as String
}