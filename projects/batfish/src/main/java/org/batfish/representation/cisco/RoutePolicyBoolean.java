package org.batfish.representation.cisco;

import java.io.Serializable;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;

public abstract class RoutePolicyBoolean implements Serializable {

  public abstract BooleanExpr toBooleanExpr(CiscoConfiguration cc, Configuration c, Warnings w);
}
