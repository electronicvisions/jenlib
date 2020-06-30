import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

/**
 * Declare a stage which conditionally skips execution.
 *
 * @param options Map of options:
 *                <ul>
 *                    <li><b>skip</b> (mandatory): Skip execution of the content.
 *                    <li><i>others</i> (mandatory): See https://www.jenkins.io/doc/pipeline/steps/pipeline-stage-step/
 *                </ul>
 * @param content Code to be run
 */
def call(Map<String, Object> options, Closure content) {
	Map<String, Object> internalOptions = options.clone()

	Boolean skip = internalOptions.get("skip")
	if (skip == null) {
		throw new IllegalArgumentException("'skip' parameter is mandatory.")
	}
	internalOptions.remove("skip")

	stage(internalOptions) {
		if (skip) {
			echo("[Jenlib] Conditional stage '${env.STAGE_NAME}' skipped.")
			Utils.markStageSkippedForConditional(env.STAGE_NAME)
		} else {
			content()
		}
	}
}
