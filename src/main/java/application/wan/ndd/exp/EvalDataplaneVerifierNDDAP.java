package application.wan.ndd.exp;

import application.wan.ndd.verifier.DPVerifierNDDAP;
import application.wan.ndd.verifier.DPVerifierNDDAPIncre;
import application.wan.ndd.verifier.apkeep.utils.UtilityTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvalDataplaneVerifierNDDAP {
    private static final String currentPath = System.getProperty("user.dir");
    private static boolean incrementACL = false;
    public static boolean CHECK_CORRECTNESS = false;
    public static boolean DEBUG_MODEL = false;

    public static void runFattreeUpdate(String configPath, String ACL_Usage_Path, String ACL_Rule_Path)
            throws IOException {
        // long t0 = System.nanoTime();
        configPath = Paths.get(configPath).toRealPath().toString();
        String testcase = Paths.get(configPath).toRealPath().getFileName().toString();
        String inPath = Paths.get(configPath, "dpv").toString();
        String outPath = Paths.get(currentPath, "results", testcase).toString();
        File dir = Paths.get(outPath).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String topoFile = Paths.get(configPath, "layer1Topology").toString();
        String edgePortFile = Paths.get(configPath, "edgePorts").toString();

        ArrayList<String> topo = UtilityTools.readFile(topoFile);
        ArrayList<String> edge_ports = UtilityTools.readFile(edgePortFile);
        File[] inFiles = Paths.get(inPath).toFile().listFiles();
        int count = 0;
        for (File folder : inFiles) {
            count++;
            if (!folder.isDirectory())
                continue;
            String updateFolder = folder.getAbsolutePath();
            Map<String, Map<String, List<Map<String, Map<String, List<Map<String, String>>>>>>> ACL_json = new HashMap<String, Map<String, List<Map<String, Map<String, List<Map<String, String>>>>>>>();
            Map<String, Map<String, List<Map<String, Map<String, List<String>>>>>> ACL_json1 = new HashMap<String, Map<String, List<Map<String, Map<String, List<String>>>>>>();
            UtilityTools.get_ACL(ACL_Usage_Path, ACL_json);

            if (incrementACL) {
                DPVerifierNDDAPIncre dpv = new DPVerifierNDDAPIncre(testcase, topo, edge_ports, ACL_json);
                // update base rules
                String baseFile = Paths.get(updateFolder, "change_base").toString();
                ArrayList<String> forwarding_rules = UtilityTools.readFile(baseFile);
                ArrayList<String> acl_rules = new ArrayList<String>();
                acl_rules = UtilityTools.readFile(ACL_Rule_Path);
                dpv.run(forwarding_rules, acl_rules);
            } else {
                DPVerifierNDDAP dpv = new DPVerifierNDDAP(testcase, topo, edge_ports, ACL_json);
                String baseFile = Paths.get(updateFolder, "change_base").toString();
                ArrayList<String> forwarding_rules = UtilityTools.readFile(baseFile);
                ArrayList<String> acl_rules = new ArrayList<String>();
                acl_rules = UtilityTools.readFile(ACL_Rule_Path);

                Runtime r = Runtime.getRuntime();
                r.gc();
                r.gc();
                long m1 = r.totalMemory() - r.freeMemory();

                dpv.run(forwarding_rules, acl_rules);

                Runtime r1 = Runtime.getRuntime();
                r1.gc();
                r1.gc();
                long m2 = r1.totalMemory() - r1.freeMemory();

                System.out.println("memory:" + (m2 - m1) / (1024 * 1024) + "MB");
                System.out.flush();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        UtilityTools.split_str = "_";
        String configPath = "/data/zcli-data/network-decision-diagram/datasets/wan/purdue/pd";
        String ACL_Usage_Path = "/data/zcli-data/network-decision-diagram/datasets/wan/purdue/purdue/acls/usage";
        String ACL_Rule_Path = "/data/zcli-data/network-decision-diagram/datasets/wan/purdue/purdue/acl_rule";

        runFattreeUpdate(configPath, ACL_Usage_Path, ACL_Rule_Path);
    }
}