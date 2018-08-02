/**
 * This method allows to set the build result of the current job in an arbitrary way.
 * The default setter only allows results to get worse.
 *
 * @see <a href="https://github.com/jenkinsci/jenkins/blob/578d6ba/core/src/main/java/hudson/model/Run.java">setResult()</a>
 * @param state: Result to be set, one of ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]
 */
def call(String state) {
	currentBuild.rawBuild.@result = hudson.model.Result.metaClass.getAttribute(hudson.model.Result, state)
}
