@Library('jenlib')
import org.electronicvisions.SingularityInstance

pipeline {
	agent { label "frontend" }

	stages {

		/**
		 * Usage Examples
		 */
		stage("setBuildState") {
			steps {
				setBuildState "SUCCESS"
			}
		}

		stage("assertBuildResult") {
			steps {
				assertBuildResult("FAILURE") {
					sh "exit 1"
				}
			}
		}

		stage("checkPythonPackage") {
			steps {
				// Create python package
				sh "mkdir -p lib/my_python_lib"
				sh "echo 'class NiceClass(object):\n    pass' > lib/my_python_lib/__init__.py"

				// Check it
				checkPythonPackage(package: "lib/my_python_lib")
			}
		}

		stage("SingularityInstance") {
			steps {
				script {
					container = new SingularityInstance(this, "/containers/jenkins/softies_darling", "visionary-defaults")
					container.exec("env")
					container.stop()
				}
			}
		}
	}

	post {
		regression {
			mattermostSend(channel: "#softies",
					text: "@channel Regression in Jenkins build `${env.JOB_NAME}`!",
					message: "${env.BUILD_URL}",
					endpoint: "https://brainscales-r.kip.uni-heidelberg.de:6443/hooks/qrn4j3tx8jfe3dio6esut65tpr")
		}
	}
}
