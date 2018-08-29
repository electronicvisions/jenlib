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
				checkPythonPackage(pkg: "lib/my_python_lib")
			}
		}

		stage("notifyFailure") {
			steps {
				notifyFailure(mattermostChannel: "jenkins-trashbin")
			}
		}

		stage("onSlurmResource") {
			steps {
				onSlurmResource(partition: "jenkins") {
					sh "hostname"
				}
			}
		}

		stage("runOnSlave") {
			steps {
				runOnSlave(label: "frontend") {
					sh "hostname"
				}
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

		stage("withWaf") {
			steps {
				withWaf() {
					sh "waf --help"
				}
			}
		}
	}

	post {
		regression {
			notifyFailure(mattermostChannel: "#softies")
		}
	}
}
