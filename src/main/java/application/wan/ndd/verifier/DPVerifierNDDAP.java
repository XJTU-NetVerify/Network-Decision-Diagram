package application.wan.ndd.verifier;

import application.wan.ndd.verifier.apkeep.checker.CheckerNDDAP;
import application.wan.ndd.verifier.apkeep.core.NetworkNDDAP;
import application.wan.ndd.verifier.apkeep.core.NetworkNDDPred;

import org.ants.jndd.diagram.AtomizedNDD;
import org.ants.jndd.diagram.NDD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DPVerifierNDDAP {
	public static int check_method = 1; // 0 use set 1 no cache 2 use limited cache
	public static boolean update_per_acl = false;
	public ArrayList<String> policies;

	public NetworkNDDAP apkeepNetworkModel;
	public CheckerNDDAP apkeepVerifier;

	public DPVerifierNDDAP() {
		AtomizedNDD.initAtomizedNDD(1000000, 1000000, 1000000, 100000);
	}

	public DPVerifierNDDAP(String network_name, ArrayList<String> topo, ArrayList<String> edge_ports,
			Map<String, Map<String, List<Map<String, Map<String, List<Map<String, String>>>>>>> dpDevices)
			throws IOException {
		this();
		apkeepNetworkModel = new NetworkNDDAP(network_name);
		apkeepNetworkModel.initializeNetwork(topo, edge_ports, dpDevices);
	}

	public void run(ArrayList<String> forwarding_rules, ArrayList<String> acl_rules) throws IOException {
		long t0 = System.nanoTime();
		HashMap<String, HashSet<Integer>> moved_aps = apkeepNetworkModel.UpdateBatchRules(forwarding_rules, acl_rules);
		long t1 = System.nanoTime();

		AtomizedNDD.enableCaches();

		apkeepVerifier = new CheckerNDDAP(apkeepNetworkModel, false);
		apkeepVerifier.PropertyCheck();
		System.out.println("The number of reachable pairs: " + apkeepVerifier.ans.size());

		AtomizedNDD.disableCaches();

		long t2 = System.nanoTime();
		System.out.println("Property Check Time: " + (t2 - t1) / 1000000000.0);
		System.out.println("Total atoms:" + AtomizedNDD.getAtomsCount());
	}
}