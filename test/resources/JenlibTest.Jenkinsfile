@Library('jenlib')
import org.electronicvisions.SingularityInstance

pipeline{
	agent {label "frontend"}

	stages {

		// Classes to test
		stage("SingularityInstance"){
			steps{
				script{
					container = new SingularityInstance(this, "/containers/jenkins/softies_darling", "visionary-defaults")
					container.exec("env")
					container.stop()
				}
			}
		}
	}
}
