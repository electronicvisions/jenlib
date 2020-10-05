import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.workflow.actions.LabelAction
import org.jenkinsci.plugins.workflow.cps.steps.ParallelStepExecution.ParallelLabelAction
import org.jenkinsci.plugins.workflow.graph.BlockStartNode
import org.jenkinsci.plugins.workflow.graph.FlowNode

/**
 * Get a build-unique ids of the parallel branches this step is executed in, innermost first.
 *
 * @return Ids of the current parallel branches
 */
List<String> call() {
	List<Integer> branchHashes = getParallelBranchHashes()
	return branchHashes.collect { it.toString() }
}

@NonCPS
private static List<Integer> getParallelBranchHashes(FlowNode node) {
	List<Integer> foundBranchNames = []
	for (BlockStartNode blockStart : node.enclosingBlocks) {
		LabelAction parallelLabel = blockStart.getAction(ParallelLabelAction)
		if (parallelLabel != null) {
			foundBranchNames.add(parallelLabel.hashCode())
		}
	}
	return foundBranchNames
}

private List<Integer> getParallelBranchHashes() {
	return getParallelBranchHashes(getContext(FlowNode))
}
