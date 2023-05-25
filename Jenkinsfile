pipeline  {
	environment {
         NAME = "ibis-simulator"
         VERSION = "${env.BUILD_ID}"
    }
    
    agent any

    tools {
        jdk 'OpenJDK17'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
    	stage('Export simulator') {
            steps {
                echo "I am exporting de.jena.publictransport.simulator"
                sh "./gradlew clean build de.jena.publictransport.simulator:export"
	    	}
        }
        stage('Docker image') {
	    	when{
				branch 'main'
	    	}

            steps {
				step([$class: 'DockerBuilderPublisher',
                                dockerFileDirectory: '.',
                                cloud: 'docker',
                                tagsString: """devel.data-in-motion.biz:6000/gecko.io/${NAME}:latest
                                        devel.data-in-motion.biz:6000/gecko.io/${NAME}:${VERSION}""",
                                pushOnSuccess: true,
                                pushCredentialsId: 'dim-nexus'])
				step([$class: 'DockerBuilderPublisher',
			        			dockerFileDirectory: '.',
								cloud: 'docker',
								tagsString: """registry-git.jena.de/scj/${NAME}:${VERSION}
										registry-git.jena.de/scj/${NAME}:latest""",
								pushOnSuccess: true,
								pushCredentialsId: 'github-jena'])
            }
        }
    }
}
