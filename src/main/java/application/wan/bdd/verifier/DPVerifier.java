package application.wan.bdd.verifier;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import application.wan.bdd.verifier.apkeep.checker.Checker;
import application.wan.bdd.verifier.apkeep.core.Network;

public class DPVerifier {
	public static boolean update_per_acl = false;
	private Network apkeepNetworkModel;
	private Checker apkeepVerifier;

	public long dpm_time;
	public long dpv_time;

	public DPVerifier(String network_name, ArrayList<String> topo, ArrayList<String> edge_ports,
			Map<String, Map<String, List<Map<String, Map<String, List<Map<String, String>>>>>>> dpDevices)
			throws IOException {
		apkeepNetworkModel = new Network(network_name);
		apkeepNetworkModel.initializeNetwork(topo, edge_ports, dpDevices);

		dpm_time = 0;
		dpv_time = 0;
	}

	public void run(ArrayList<String> forwarding_rules, ArrayList<String> acl_rules) throws IOException {
		long t0 = System.nanoTime();
		HashMap<String, HashSet<Integer>> moved_aps = apkeepNetworkModel.UpdateBatchRules(forwarding_rules, acl_rules);
		long t1 = System.nanoTime();
		apkeepVerifier = new Checker(apkeepNetworkModel);
		apkeepVerifier.PropertyCheck();
		long t2 = System.nanoTime();
		System.out.println("Property Check Time: " + (t2 - t1) / 1000000000.0);
		System.out.println("The number of reachable pairs:" + apkeepVerifier.ans.size());
	}
}
