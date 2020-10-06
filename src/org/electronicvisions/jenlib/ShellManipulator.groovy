package org.electronicvisions.jenlib

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

import java.nio.file.Paths

import static java.util.UUID.randomUUID

/**
 * This class manages a queue of shell modifications within the global {@code env}.
 *
 * Supported modifications are: {@code PREFIX}ing and {@code POSTFIX}ing shell commands.
 * All modifications are persisted across a build by serializing to environment variables.
 * Different environment variables are used per parallel branch, therefore allowing for
 * shell manipulations to be used independently per-branch in concurrent sections.
 * The same instance of this class must not be used across multiple parallel branches.
 *
 * To avoid escaping, all commands are written to temporary shell scripts. In case of
 * multiple modifications, these scripts call each other with the respective PREFIX/POSTFIX.
 * The stack of scripts is constructed by calling {@link ShellManipulator#constructScriptStack},
 * which will return the path to the starting script.
 *
 * The modifications are layered queue-like: the latest added modification is applied first.
 *
 * See the {@code jesh} and {@code inSingularity} steps for usage examples.
 */
class ShellManipulator {

	/**
	 * Ordered list of tuples (prefix, postfix) that represent all requested modifications.
	 * The data type has to be list of lists, since Jenkins cannot serialize the {@code Tuple}
	 * data type.
	 */
	private List<List<String>> manipulations

	/**
	 * List of shell scripts that have been constructed.
	 */
	private List<String> scriptStack

	/**
	 * JSON-formatted string of shell modifications that were present before constructing this
	 * class.
	 */
	final private String initialManipulations

	/**
	 * Arbitrary, but unique string used to identify shell manipulations in sequential (no parallel branch)
	 * execution sections.
	 */
	final static private String SEQUENTIAL_BRANCH_ID = "SEQUENTIAL_a3a1ced7-1485-4dfe-8826-05ed58bb1f15"

	/**
	 * Pipeline context
	 */
	private def steps

	/**
	 * Constructor for {@link ShellManipulator}.
	 *
	 * @param steps Pipeline context, {@code steps.env} is used for persisting shell modifications.
	 * @param manipulations JSON-formatted initial pipeline modifications.
	 */
	private ShellManipulator(steps, String manipulations) {
		this.steps = steps
		this.scriptStack = new ArrayList<String>()

		fromString(manipulations)
		initialManipulations = toString()
	}

	/**
	 * Construct an instance of {@link ShellManipulator} from the current environment.
	 *
	 * This method is to be seen as the default entry point for this class.
	 * The functionality can not be integrated into the actual constructor since the latter is implicitly not CPS
	 * converted by Jenkins and we need to call CPS converted methods in here.
	 *
	 * @param steps Pipeline context
	 * @return Instance of {@link ShellManipulator} with initial manipulations as given by the current environment
	 */
	static fromEnvironment(steps) {
		String currentState = steps.env[getEnvironmentVariable(steps)]
		return new ShellManipulator(steps, currentState)
	}

	/**
	 * Serialize all manipulations in a JSON-formatted list of tuples.
	 * @return JSON-formatted list of shell modifications
	 */
	@NonCPS
	String toString() {
		return JsonOutput.toJson(manipulations)
	}

	/**
	 * Fill the internal shell modification list from a given JSON-formatted input string.
	 * {@code null} (as given for undefined environment variables) is a valid argument and will
	 * lead to empty initialization.
	 *
	 * @param input JSON-formatted list of tuples with shell modifications (PREFIX, POSTFIX).
	 */
	@NonCPS
	void fromString(String input) {
		if (input == null) {
			input = JsonOutput.toJson(new ArrayList<ArrayList<String>>())
		}
		manipulations = (ArrayList<ArrayList<String>>) new JsonSlurperClassic().parseText(input)
	}

	/**
	 * Add a shell modification.
	 *
	 * @param prefix String to be prefixed before every shell command.
	 *               Will be whitespace-separated from the actual command.
	 * @param postfix String to be postfixed after every shell command.
	 *                Will be whitespace-separated from the actual command.
	 */
	void add(String prefix, String postfix) {
		manipulations.add(0, new ArrayList<String>([prefix, postfix]))
		steps.env[getEnvironmentVariable(steps)] = toString()
	}

	/**
	 * Create a chained set of shell scripts that correctly handle the required prefix/postfix
	 * modifications for a given command.
	 *
	 * Construction can only happen once, {@link ShellManipulator#restore} should be called
	 * after command execution to get rid of temporary files and restore the previous environment.
	 *
	 * @param command Shell command to be augmented by the given modifications
	 * @return Path to a shell script that may be interpreted by a chosen shell.
	 */
	String constructScriptStack(String command) {
		if (scriptStack.size()) {
			throw new IllegalStateException("Script stack is not empty. Cannot construct.")
		}

		String commandFileName = generateRandomFilename()
		steps.writeFile(file: commandFileName, encoding: "UTF-8", text: command)
		scriptStack.add(commandFileName)

		String lastFileName = commandFileName

		for (ArrayList<String> manipulation in manipulations) {
			String wrapFileName = generateRandomFilename()
			steps.writeFile(file: wrapFileName, encoding: "UTF-8",
			                text: "${manipulation[0]} bash ${lastFileName} ${manipulation[1]}",)
			scriptStack.add(wrapFileName)
			lastFileName = wrapFileName
		}

		return lastFileName
	}

	/**
	 * Compute the environment variable's name, shell manipulations are stored at.
	 *
	 * If called in (nested) parallel branches, the resulting environment variable will get initialized with a copy of
	 * pending shell manipulations from hierarchical parents.
	 *
	 * @param steps Pipeline context
	 * @return Name of the environment variable manipulations are stored at.
	 */
	private static String getEnvironmentVariable(steps) {
		String parentBranchId = SEQUENTIAL_BRANCH_ID
		String myBranchId = SEQUENTIAL_BRANCH_ID
		List<String> parallelBranches = steps.getCurrentParallelBranchIds()

		if (parallelBranches.size()) {
			myBranchId = parallelBranches[0]
		}

		// Environment variable shell manipulations are stored at, will be returned eventually
		String targetEnvironmentVariable = convertBranchToEnvironmentVariable(myBranchId)

		if (steps.env[targetEnvironmentVariable] != null) {
			return targetEnvironmentVariable
		}

		// Search for the innermost parent branch with non-null modifications
		for (int i = 1; i < parallelBranches.size(); i++) {
			if (steps.env[convertBranchToEnvironmentVariable(parallelBranches[i])] != null) {
				parentBranchId = parallelBranches[i]
			}
		}

		// Copy parent modifications (if available) as a starting point for the current branch
		String parentModifications = steps.env[convertBranchToEnvironmentVariable(parentBranchId)]
		if (parentModifications != null) {
			steps.env[targetEnvironmentVariable] = parentModifications
		}

		return targetEnvironmentVariable
	}

	/**
	 * Restore the environment's and file system's state as it was before constructing this
	 * object.
	 *
	 * This method should always be called after the commands to-be modified have been issued.
	 */
	void restore() {
		steps.env[getEnvironmentVariable(steps)] = initialManipulations
		fromString(initialManipulations)
		cleanScriptStack()
	}

	/**
	 * Remove temporary script files and clear {@link ShellManipulator#scriptStack}.
	 */
	private void cleanScriptStack() {
		for (String script in scriptStack) {
			steps.jesh("rm $script")
		}
		scriptStack.clear()
	}

	/**
	 * Generate a random filename to be used for temporary shell scripts.
	 * @return Absolute path to a temporary shell script within the workspace.
	 */
	private String generateRandomFilename() {
		String path = Paths.get((String) steps.env.WORKSPACE, ".jenlib_${randomUUID().toString()}.sh").toString()

		if (!path.startsWith("/")) {
			throw new InternalError("Temporary script path is not absolute.")
		}

		return path
	}

	/**
	 * Convert a branch id to an environment variable manipulations are stored at.
	 * @param branchId Branch id to be converted
	 * @return Name of the environment variable manipulations are stored at.
	 */
	private static String convertBranchToEnvironmentVariable(String branchId) {
		return "JENLIB_SHELL_MANIPULATIONS-${branchId}".toString()
	}

	/**
	 * Getter for {@link ShellManipulator#manipulations}.
	 */
	ArrayList<ArrayList<String>> getManipulations() {
		return manipulations
	}
}
