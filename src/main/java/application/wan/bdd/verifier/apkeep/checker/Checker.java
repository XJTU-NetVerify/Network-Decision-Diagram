package application.wan.bdd.verifier.apkeep.checker;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import org.ants.jndd.diagram.AtomizedNDD;
import org.ants.jndd.diagram.NDD;

import application.wan.bdd.exp.EvalDataplaneVerifier;
import application.wan.bdd.verifier.apkeep.core.Network;

import application.wan.bdd.verifier.apkeep.element.ACLElement;
import application.wan.bdd.verifier.apkeep.element.ForwardElement;
import application.wan.bdd.verifier.common.PositionTuple;
import jdd.bdd.BDDIO;

public class Checker {
    Network net;
    Stack<TranverseNode> queue;
    public HashSet<String> ans;
    HashMap<PositionTuple, HashMap<PositionTuple, Integer>> reach;

    public Checker(Network net) {
        this.net = net;
        queue = new Stack<TranverseNode>();
        ans = new HashSet<>();
        reach = new HashMap<PositionTuple, HashMap<PositionTuple, Integer>>();
        for (String device : net.edge_ports.keySet()) {
            for (String port : net.edge_ports.get(device)) {
                HashMap<PositionTuple, Integer> subMap = new HashMap<PositionTuple, Integer>();
                subMap.put(new PositionTuple(device, port), net.bdd_engine.BDDTrue);
                reach.put(new PositionTuple(device, port), subMap);
                HashSet<Integer> all_fw = new HashSet<Integer>(net.apk.AP);
                HashSet<Integer> all_acl = new HashSet<Integer>(net.ACL_apk.AP);
                queue.add(new TranverseNode(new PositionTuple(device, port), all_fw, all_acl));
            }
        }
    }

    public void PropertyCheck() throws IOException {
        while (!queue.isEmpty()) {
            // System.out.println(net.bdd_engine.getBDD().table_size);
            TranverseNode curr_node = queue.pop();
            if (curr_node.curr.getDeviceName().split("_").length == 1) // forward element
            {
                ForwardElement curr_device = net.FWelements.get(curr_node.curr.getDeviceName());
                for (String out_port : curr_device.port_aps_raw.keySet()) {
                    if (out_port.equalsIgnoreCase("default") || out_port.equalsIgnoreCase(curr_node.curr.getPortName()))
                        continue;

                    HashSet<Integer> next_fw_aps = new HashSet<Integer>(curr_node.fw_aps);
                    next_fw_aps.retainAll(curr_device.port_aps_raw.get(out_port));

                    if (next_fw_aps.size() == 0)
                        continue;
                    if (net.edge_ports.containsKey(curr_node.curr.getDeviceName())
                            && net.edge_ports.get(curr_node.curr.getDeviceName()).contains(out_port)) {
                        int reachPackets = mergeSet(next_fw_aps, curr_node.acl_aps);
                        if (reachPackets != 0) {
                            if (!ans.contains(
                                    curr_node.source.getDeviceName() + "->" + curr_node.curr.getDeviceName())) {
                                ans.add(curr_node.source.getDeviceName() + "->" + curr_node.curr.getDeviceName());
                            }
                        }
                        if (EvalDataplaneVerifier.CHECK_CORRECTNESS) {
                            if (!reach.containsKey(curr_node.source)) {
                                reach.put(curr_node.source, new HashMap<PositionTuple, Integer>());
                            }
                            int origin;
                            if (reach.get(curr_node.source)
                                    .containsKey(new PositionTuple(curr_node.curr.getDeviceName(), out_port))) {
                                origin = reach.get(curr_node.source)
                                        .get(new PositionTuple(curr_node.curr.getDeviceName(), out_port));
                            } else {
                                origin = net.bdd_engine.BDDFalse;
                            }
                            int reach_bdd = net.bdd_engine.getBDD()
                                    .ref(net.bdd_engine.getBDD().or(origin, reachPackets));
                            net.bdd_engine.getBDD().deref(origin);
                            if (reach_bdd != net.bdd_engine.BDDFalse) {
                                reach.get(curr_node.source).put(
                                        new PositionTuple(curr_node.curr.getDeviceName(), out_port),
                                        reach_bdd);
                            }
                        }
                        net.bdd_engine.getBDD().deref(reachPackets);
                        continue;
                    }
                    for (PositionTuple next_pt : net.topology.get(new PositionTuple(curr_device.name, out_port))) {
                        if (curr_node.visited.contains(next_pt.getDeviceName())) {
                            if (mergeSet(next_fw_aps, curr_node.acl_aps) != net.bdd_engine.BDDFalse) {
                            }
                            continue;
                        }
                        queue.push(new TranverseNode(curr_node.source, next_pt, next_fw_aps, curr_node.acl_aps,
                                curr_node.visited));
                    }
                }
            } else // acl element
            {
                ACLElement curr_device = net.ACLelements_application.get(curr_node.curr.getDeviceName());
                for (String out_port : curr_device.port_aps_raw.keySet()) {
                    if (out_port.equalsIgnoreCase("deny") || out_port.equalsIgnoreCase(curr_node.curr.getPortName()))
                        continue;

                    HashSet<Integer> next_acl_aps = new HashSet<Integer>(curr_node.acl_aps);
                    next_acl_aps.retainAll(curr_device.port_aps_raw.get(out_port));

                    if (next_acl_aps.size() == 0)
                        continue;
                    if (net.edge_ports.containsKey(curr_node.curr.getDeviceName())
                            && net.edge_ports.get(curr_node.curr.getDeviceName()).contains(out_port)) {
                        int reachPackets = mergeSet(curr_node.fw_aps, next_acl_aps);
                        if (reachPackets != 0) {
                            if (!ans.contains(
                                    curr_node.source.getDeviceName() + "->" + curr_node.curr.getDeviceName())) {
                                ans.add(curr_node.source.getDeviceName() + "->" + curr_node.curr.getDeviceName());
                            }
                        }
                        if (EvalDataplaneVerifier.CHECK_CORRECTNESS) {
                            if (!reach.containsKey(curr_node.source)) {
                                reach.put(curr_node.source, new HashMap<PositionTuple, Integer>());
                            }
                            int origin;
                            if (reach.get(curr_node.source)
                                    .containsKey(new PositionTuple(curr_node.curr.getDeviceName(), out_port))) {
                                origin = reach.get(curr_node.source)
                                        .get(new PositionTuple(curr_node.curr.getDeviceName(), out_port));
                            } else {
                                origin = net.bdd_engine.BDDFalse;
                            }
                            int reach_bdd = net.bdd_engine.getBDD()
                                    .ref(net.bdd_engine.getBDD().or(origin, reachPackets));
                            net.bdd_engine.getBDD().deref(origin);
                            if (reach_bdd != net.bdd_engine.BDDFalse) {
                                reach.get(curr_node.source).put(
                                        new PositionTuple(curr_node.curr.getDeviceName(), out_port),
                                        reach_bdd);
                            }
                        }
                        net.bdd_engine.getBDD().deref(reachPackets);
                        continue;
                    }
                    for (PositionTuple next_pt : net.topology.get(new PositionTuple(curr_device.name, out_port))) {
                        if (curr_node.visited.contains(next_pt.getDeviceName())) {
                            if (mergeSet(curr_node.fw_aps, next_acl_aps) != net.bdd_engine.BDDFalse) {
                            }
                            continue;
                        }
                        queue.push(new TranverseNode(curr_node.source, next_pt, curr_node.fw_aps, next_acl_aps,
                                curr_node.visited));
                    }
                }
            }
        }
        if (EvalDataplaneVerifier.CHECK_CORRECTNESS) {
            printReach();
        }
    }

    public int updateReach(int origin, HashSet<Integer> fw_aps, HashSet<Integer> acl_aps) {
        int sum = mergeSet(fw_aps, acl_aps);

        int temp = net.bdd_engine.getBDD().ref(net.bdd_engine.getBDD().or(sum, origin));
        net.bdd_engine.getBDD().deref(sum);
        net.bdd_engine.getBDD().deref(origin);

        sum = temp;
        return sum;
    }

    // APKeep maintains 2 sets of atoms for forwarding and ACL rules, respectively.
    // Therefore, when the verifier find a loop or a reachable pair, it has to merge
    // those 2 set of atoms to avoid false positive.
    public int mergeSet(HashSet<Integer> fw_aps, HashSet<Integer> acl_aps) {
        int sum_fw = net.bdd_engine.BDDFalse;
        for (int ap : fw_aps) {
            int temp = net.bdd_engine.getBDD().ref(net.bdd_engine.getBDD().or(sum_fw, ap));
            net.bdd_engine.getBDD().deref(sum_fw);
            sum_fw = temp;
        }

        int sum_acl = net.bdd_engine.BDDFalse;
        for (int ap : acl_aps) {
            int temp = net.bdd_engine.getBDD().ref(net.bdd_engine.getBDD().or(sum_acl, ap));
            net.bdd_engine.getBDD().deref(sum_acl);
            sum_acl = temp;
        }

        int sum = net.bdd_engine.getBDD().ref(net.bdd_engine.getBDD().and(sum_fw, sum_acl));
        net.bdd_engine.getBDD().deref(sum_fw);
        net.bdd_engine.getBDD().deref(sum_acl);

        return sum;
    }

    private void printReach() throws IOException {
        FileWriter fw = new FileWriter(
                "network-decision-diagram/results/WAN/reachableBDD",
                false);
        PrintWriter pw = new PrintWriter(fw);
        for (PositionTuple srcNode : reach.keySet()) {
            String src = srcNode.getDeviceName();
            for (PositionTuple dstNode : reach.get(srcNode).keySet()) {
                String dst = dstNode.getDeviceName();
                pw.println(src + " " + dst + " "
                        + net.bdd_engine.getBDD().satCount(reach.get(srcNode).get(dstNode)));
            }
        }
        pw.flush();
    }

    public void Output_Result(String path) throws IOException {
        FileWriter fw = new FileWriter(path, false);
        PrintWriter pw = new PrintWriter(fw);
        for (PositionTuple source : reach.keySet()) {
            for (PositionTuple dest : reach.get(source).keySet()) {
                pw.println(source + " " + dest + " "
                        + net.bdd_engine.getBDD().satCount(reach.get(source).get(dest)) / 4722366482869645213696.0);
                pw.flush();
            }
        }
    }

    public void Output_Result_ACL(String path) throws IOException {
        FileWriter fw = new FileWriter(path, false);
        PrintWriter pw = new PrintWriter(fw);
        for (PositionTuple source : reach.keySet()) {
            for (PositionTuple dest : reach.get(source).keySet()) {
                pw.println(source + " " + dest + " " + net.bdd_engine.getBDD().satCount(reach.get(source).get(dest)));
                pw.flush();
            }
        }
    }

    public void OutputReachBDD() throws IOException {
        for (PositionTuple src : reach.keySet()) {
            for (PositionTuple dst : reach.get(src).keySet()) {
                BDDIO.save(net.bdd_engine.getBDD(), reach.get(src).get(dst),
                        "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/single/" + net.name
                                + "/correctness/APKeep/" + src.getDeviceName() + "_" + dst.getDeviceName());
            }
        }
    }

    public void printReachSize() {
        int sum = 0;
        for (PositionTuple src : reach.keySet()) {
            sum += reach.get(src).size();
        }
        System.out.println("reach size:" + sum);
    }
}