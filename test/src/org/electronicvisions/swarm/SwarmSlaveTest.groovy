package org.electronicvisions.swarm


abstract class SwarmSlaveTest extends GroovyTestCase {

	final static Map DEFAULT_PARAMETERS = [javaHome          : "/test",
	                                       jenkinsHostname   : "something",
	                                       jenkinsJnlpPort   : 8079,
	                                       jenkinsKeyfile    : "/something/else",
	                                       jenkinsUsername   : "user",
	                                       jenkinsWebPort    : 80,
	                                       jenkinsWebProtocol: SwarmSlaveConfig.WebProtocol.HTTP,
	                                       mode              : SwarmSlaveConfig.SlaveMode.NORMAL,
	                                       slaveName         : "some_name",
	                                       numExecutors      : 10,
	                                       slaveJar          : "/some/path",
	                                       workspace         : "/some/other/path"]

	abstract List<String> getMandatorySlaveParameters()

	abstract List<String> getProhibitedSlaveParameters()

	abstract SwarmSlave generateSwarmSlave(List<String> configuredParameters, SwarmSlaveConfig config)

	void testSlaveStartStop() {
		List<List<String>> configCombinations = buildPowerset(DEFAULT_PARAMETERS.keySet())

		for (List<String> comb in configCombinations) {
			SwarmSlaveConfig config = new SwarmSlaveConfig()
			for (String key in comb) {
				config."$key" = DEFAULT_PARAMETERS.get(key)
			}

			SwarmSlave slave = generateSwarmSlave(comb, config)

			if (!mandatorySlaveParameters.any { comb.contains(it) }
					| prohibitedSlaveParameters.any { comb.contains(it) }) {
				shouldFail {
					slave.startSlave()
				}
				shouldFail {
					slave.stopSlave()
				}
			} else {
				slave.startSlave()
				slave.stopSlave()
			}
		}
	}

	/**
	 * Construct a power set from a given Collection.
	 *
	 * Implementation copied as-is from <a href="https://rosettacode.org/">rosettacode.org</a> on 2018-08-14.
	 * <br>
	 * License: <a href="http://www.gnu.org/licenses/fdl-1.2.html">GNU Free Documentation License</a>
	 *
	 * @see <a href="https://rosettacode.org/wiki/Power_set#Java">[Rosettacode]</a>
	 * @see <a href="http://www.gnu.org/licenses/fdl-1.2.html">GNU Free Documentation License</a>
	 */
	static <T> List<List<T>> buildPowerset(Collection<T> list) {
		List<List<T>> ps = new ArrayList<List<T>>()
		ps.add(new ArrayList<T>())   // add the empty set

		// for every item in the original list
		for (T item : list) {
			List<List<T>> newPs = new ArrayList<List<T>>()

			for (List<T> subset : ps) {
				// copy all of the current powerset's subsets
				newPs.add(subset)

				// plus the subsets appended with the current item
				List<T> newSubset = new ArrayList<T>(subset)
				newSubset.add(item)
				newPs.add(newSubset)
			}

			// powerset is now powerset of list.subList(0, list.indexOf(item)+1)
			ps = newPs
		}
		return ps
	}
}
