@Library('jenlib') _

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
					sish "exit 1"
				}
			}
		}

		stage("checkPythonPackage") {
			steps {
				// Create python package
				sish "mkdir -p lib/my_python_lib"
				sish "echo 'class NiceClass(object):\n    pass' > lib/my_python_lib/__init__.py"

				// Check it
				checkPythonPackage(pkg: "lib/my_python_lib")
			}
		}

		stage("wafSetup") {
			steps {
				wafSetup(projects: ["frickel-dls@v3testing"])
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
					sish "hostname"
				}
			}
		}

		stage("runOnSlave") {
			steps {
				runOnSlave(label: "frontend") {
					sish "hostname"
				}
			}
		}

		stage("inSingularity") {
			steps {
				inSingularity {
					sish "hostname"
				}
			}
		}

		stage("withWaf") {
			steps {
				withWaf() {
					sish "waf --help"
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
