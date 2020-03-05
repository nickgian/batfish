package org.batfish.minesweeper.nv;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.minesweeper.Graph;
import org.batfish.minesweeper.GraphEdge;

public class Dataplane {

  private Map<String, Integer> _nodes;

  /* Maps GraphEdge to NV edges */
  private Map<GraphEdge, String> _edgeMap;

  public Dataplane(Map<String, Integer> _nodes, Map<GraphEdge, String> edgeMap) {
    this._nodes = _nodes;
    this._edgeMap = edgeMap;
  }

  /** Type of messages exchanged by the dataplane **/
  private String getAttributeType() {
    String packet = "type packet = {srcIp: int; dstIp: int;}";
    return "type packets = set[packet]";
  }

  private void generateLPM(StringBuilder sb) {

    // Helper function matching a route's forwarding edge and an edge
    sb.append("let matchFwd route edge =\n")
        .append("  match route.selected with\n")
        .append("  | None -> false\n")
        .append("  | Some 0 -> false\n")
        .append("  | Some 1 -> false\n")
        .append("  | Some 2 -> (match route.ospf with\n")
        .append("               | None -> false\n")
        .append("               | Some o -> o.ospfNextHop = edge)\n")
        .append("  | Some 3 -> (match route.bgp with\n")
        .append("               | None -> false\n")
        .append("               | Some b -> b.bgpNextHop = edge)\n");


    sb.append("\nlet lpm rib edge p =\n")
        .append("  let dst = p.dstIp in\n");

    for (int i = 32; i == 0; i--) {
        sb.append("  if matchFwd (rib[(" + NVLang.mkBitAnd("dst", i + "") + "," + i +")]) then true\n")
          .append("  else ");
          }
    sb.append("false\n\n");
  }

  /** Transfer function for the dataplane **/
  private String transferData() {
    StringBuilder sb = new StringBuilder();

    sb.append("\nlet transferData rib edge ps = \n")
        .append("  match edge with\n");

    for (Entry<GraphEdge, String> edge : _edgeMap.entrySet()) {
      Interface i = edge.getKey().getStart();

      IpAccessList outbound = i.getOutgoingFilter();
      Optional<String> outAcl = Optional.empty();
      if (outbound != null) {
        outAcl = computeACL(outbound);
      }

      IpAccessList inbound = i.getIncomingFilter();
      Optional<String> inAcl = Optional.empty();
      if (inbound != null) {
        inAcl = computeACL(inbound);
      }

      // Encoding the ACLs
      String filterAcls = NVLang.mkAnd(outAcl.orElse("true"), inAcl.orElse("true"));

      // Encoding the LPM semantics
      String filterLPM = "(lpm rib edge p)";
      String filter = NVLang.mkAnd(filterLPM, filterAcls);

      sb.append("  | " + edge.getValue() + " ->\n")
          .append("    mapIf (fun p -> " + filter + ") ps\n");
    }

    return sb.toString();
  }

  /*
   * Convert an Access Control List (ACL) to an NV expression.
   * The default action in an ACL is to deny all traffic.
   */
  private Optional<String> computeACL(IpAccessList acl) {

    // Check if there is an ACL first
    if (acl == null) {
      return Optional.empty();
    }

    return Optional.of(new IpAccessListToNvExpr().toNvExpr(acl));
  }

  private String mergeData() {
    return "\nlet mergeData u x y = x union y\n";
  }


  public String generateDataplane() {
    return getAttributeType() + "\n" + transferData() + mergeData();
  }

}
