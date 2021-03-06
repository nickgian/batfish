package org.batfish.minesweeper.nv;

public class Attributes {

  private CompilerOptions _flags;

  public Attributes (CompilerOptions flags) {
    _flags = flags;
  }

  public String buildBgpAttribute(
        String _lp,
        String _ad,
        String _aspath,
        String _med,
        String _communities,
        String _nexthop,
        String _origin) {
      String bgp =
          "Some {bgpAd="
              + _ad
              + "; lp="
              + _lp
              + "; aslen="
              + _aspath
            + "; med="
            + _med
            + "; comms="
            + _communities
            + (_flags.doOrigin() ? "; bgpOrigin=" + _origin : "")
            + (_flags.doNextHop() ? "; bgpNextHop=" + _nexthop : "")
            + "}";
    return bgp;
  }

  public String buildOspfAttribute(
      String _ad,
      String _weight,
      String _areaType,
      String _areaId,
      String _nexthop,
      String _origin) {
    String ospf =
        "Some {ospfAd="
            + _ad
            + "; weight="
            + _weight
            + "; areaType="
            + _areaType
            + "; areaId="
            + _areaId
            + (_flags.doOrigin() ? "; ospfOrigin=" + _origin : "")
            + (_flags.doNextHop() ? "; ospfNextHop=" + _nexthop : "")
            + "}";
    return ospf;
  }

  public String buildBgpType() {
    String bgp = "{bgpAd: int8; lp: int; aslen: int; med:int; comms:set[int];" +
        (_flags.doOrigin() ? " bgpOrigin: tnode;" : "") +
        (_flags.doNextHop() ? " bgpNextHop: option[tedge];" : "") +
        "}";
    return bgp;
  }

  public String buildOspfType() {
    String ospf = "{ospfAd: int8; weight: int16; areaType:int2; areaId: int;" +
        (_flags.doOrigin() ? " ospfOrigin: tnode;" : "") +
            (_flags.doNextHop() ? " ospfNextHop: option[tedge];" : "") +
    "}";
    return ospf;
  }


}
