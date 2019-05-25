import org.electronicvisions.jenlib.SharedWorkspace

/**
 * Actions that may be done in all jenkins jobs on an irregular basis.
 */
void call() {
	if (env.JENLIB_RECURRENT_ACTIONS_EXECUTED) {
		return
	}

	// Don't move this to the end, within the locked sections might be new
	// calls to jenlibRecurrentActions
	env.JENLIB_RECURRENT_ACTIONS_EXECUTED = true

	lock("JENLIB_RECURRENT_ACTIONS") {
		// Cleanup unused jenlib workspaces
		SharedWorkspace.cleanup(this)
	}
}
