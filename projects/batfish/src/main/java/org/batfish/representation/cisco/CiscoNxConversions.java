package org.batfish.representation.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Warnings;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfNeighborAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfNeighborConfiguration;

/**
 * A utility class for converting between Cisco NX-OS configurations and the Batfish
 * vendor-independent {@link org.batfish.datamodel}.
 */
final class CiscoNxConversions {
  /** Computes the router ID on Cisco NX-OS. */
  // See CiscoNxosTest#testRouterId for a test that is verifiable using GNS3.
  @Nonnull
  static Ip getNxBgpRouterId(
      @Nonnull CiscoNxBgpVrfConfiguration vrfConfig,
      @Nonnull org.batfish.datamodel.Vrf vrf,
      @Nonnull Warnings w) {
    // If Router ID is configured in the VRF-Specific BGP config, it always wins.
    if (vrfConfig.getRouterId() != null) {
      return vrfConfig.getRouterId();
    }

    String messageBase =
        String.format(
            "Router-id is not manually configured for BGP process in VRF %s", vrf.getName());

    // Otherwise, Router ID is defined based on the interfaces in the VRF that have IP addresses.
    // NX-OS does use shutdown interfaces to configure router-id.
    Map<String, Interface> interfaceMap =
        vrf.getInterfaces()
            .entrySet()
            .stream()
            .filter(e -> e.getValue().getAddress() != null)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    if (interfaceMap.isEmpty()) {
      w.redFlag(
          String.format(
              "%s. Unable to infer default router-id as no interfaces have IP addresses",
              messageBase));
      // With no interfaces in the VRF that have IP addresses, show ip bgp vrf all reports 0.0.0.0
      // as the router ID. Of course, this is not really relevant as no routes will be exchanged.
      return Ip.ZERO;
    }

    // Next, NX-OS prefers the IP of Loopback0 if one exists.
    Interface loopback0 = interfaceMap.get("Loopback0");
    if (loopback0 != null) {
      w.redFlag(String.format("%s. Using the IP address of Loopback0", messageBase));
      return loopback0.getAddress().getIp();
    }

    // Next, NX-OS prefers "first" loopback interface. NX-OS is non-deterministic, but we will
    // enforce determinism by always choosing the smallest loopback IP.
    Collection<Interface> interfaces = interfaceMap.values();
    Optional<Ip> lowestLoopback =
        interfaces
            .stream()
            .filter(i -> i.getName().startsWith("Loopback"))
            .map(Interface::getAddress)
            .map(InterfaceAddress::getIp)
            .min(Comparator.naturalOrder());
    if (lowestLoopback.isPresent()) {
      w.redFlag(
          String.format(
              "%s. Making a non-deterministic choice from associated loopbacks", messageBase));
      return lowestLoopback.get();
    }

    // Finally, NX uses the first non-loopback interface defined in the vrf, assuming no loopback
    // addresses with IP address are present in the vrf. NX-OS is non-deterministic, by we will
    // enforce determinism by always choosing the smallest interface IP.
    Optional<Ip> lowestIp =
        interfaces
            .stream()
            .map(Interface::getAddress)
            .map(InterfaceAddress::getIp)
            .min(Comparator.naturalOrder());
    w.redFlag(
        String.format(
            "%s. Making a non-deterministic choice from associated interfaces", messageBase));
    assert lowestIp.isPresent(); // This cannot happen if interfaces is non-empty.
    return lowestIp.get();
  }

  @Nonnull
  static BgpNeighbor toBgpNeighbor(
      @Nonnull Configuration c,
      @Nonnull org.batfish.datamodel.Vrf vrf,
      @Nonnull Prefix prefix,
      @Nonnull CiscoNxBgpVrfConfiguration vrfConfig,
      @Nonnull CiscoNxBgpVrfNeighborConfiguration neighbor,
      @Nonnull Warnings w) {
    BgpNeighbor newNeighbor =
        new BgpNeighbor(prefix, c, prefix.getPrefixLength() < Prefix.MAX_PREFIX_LENGTH);

    newNeighbor.setDescription(neighbor.getDescription());
    newNeighbor.setVrf(vrf.getName());
    if (neighbor.getRemoteAs() != null) {
      long asn = neighbor.getRemoteAs();
      if (asn >= (1 << 16)) {
        w.redFlag(
            String.format(
                "4-byte AS numbers are not fully supported: vrf %s neighbor %s remote-as %d",
                vrf.getName(), prefix, asn));
      }
      newNeighbor.setRemoteAs((int) asn);
    }

    if (neighbor.getLocalAs() != null) {
      long asn = neighbor.getLocalAs();
      if (asn >= (1 << 16)) {
        w.redFlag(
            String.format(
                "4-byte AS numbers are not fully supported: vrf %s neighbor %s local-as %d",
                vrf.getName(), prefix, asn));
      }
      newNeighbor.setLocalAs((int) asn);
    } else if (vrfConfig.getLocalAs() != null) {
      long asn = vrfConfig.getLocalAs();
      if (asn >= (1 << 16)) {
        w.redFlag(
            String.format(
                "4-byte AS numbers are not fully supported: vrf %s neighbor %s local-as %d",
                vrf.getName(), prefix, asn));
      }
      newNeighbor.setLocalAs((int) asn);
    }

    if (vrfConfig.getClusterId() != null) {
      newNeighbor.setClusterId(vrfConfig.getClusterId().asLong());
    }

    newNeighbor.setEbgpMultihop(firstNonNull(neighbor.getEbgpMultihopTtl(), 0) > 1);

    @Nullable
    CiscoNxBgpVrfNeighborAddressFamilyConfiguration naf4 = neighbor.getIpv4UnicastAddressFamily();
    if (naf4 != null) {
      newNeighbor.setAllowLocalAsIn(firstNonNull(naf4.getAllowAsIn(), Boolean.FALSE));
      newNeighbor.setSendCommunity(firstNonNull(naf4.getSendCommunityStandard(), Boolean.FALSE));
    }
    @Nullable CiscoNxBgpVrfAddressFamilyConfiguration af4 = vrfConfig.getIpv4UnicastAddressFamily();
    if (af4 != null) {
      newNeighbor.setRouteReflectorClient(
          vrfConfig.getIpv4UnicastAddressFamily().getClientToClientReflection());
    }

    return newNeighbor;
  }

  private CiscoNxConversions() {} // prevent instantiation of utility class.
}
