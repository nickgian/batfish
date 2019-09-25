package org.batfish.compiler;

import org.batfish.symbolic.IDeepCopy;

public class Environment implements IDeepCopy<Environment> {

  // NV expression for ad
  private String _lp;

  private String _ad;

  private String _cost;

  private String _med;

  private String _communities;

  private String _protocol;

  private String _prefixLength;

  private String _prefixValue;

  private boolean _valid;

  public Environment() {
    this._lp = "b.lp";
    this._ad = "b.bgpAd";
    this._cost = "b.aslen";
    this._med = "b.med";
    this._communities = "b.comms";
    this._protocol = "x.selected";
    this._prefixLength = "prefixLen";
    this._prefixValue = "prefix";
    this._valid = false;
  }

  public Environment(
      String lp,
      String ad,
      String cost,
      String med,
      String communities,
      String protocol,
      String prefixLength,
      String prefixValue,
      boolean valid) {
    this._lp = lp;
    this._ad = ad;
    this._cost = cost;
    this._med = med;
    this._communities = communities;
    this._protocol = protocol;
    this._prefixLength = prefixLength;
    this._prefixValue = prefixValue;
    this._valid = valid;
  }

  public String get_prefixValue() {
    return _prefixValue;
  }

  public void set_prefixValue(String prefixValue) {
    this._prefixValue = prefixValue;
  }

  public String get_prefixLength() {
    return _prefixLength;
  }

  public void set_prefixLength(String prefixLength) {
    this._prefixLength = prefixLength;
  }

  public String get_lp() {
    return _lp;
  }

  public void set_lp(String lp) {
    this._lp = lp;
  }

  public String get_ad() {
    return _ad;
  }

  public void set_ad(String ad) {
    this._ad = ad;
  }

  public String get_cost() {
    return _cost;
  }

  public void set_cost(String cost) {
    this._cost = cost;
  }

  public String get_med() {
    return _med;
  }

  public void set_med(String med) {
    this._med = med;
  }

  public String get_communities() {
    return _communities;
  }

  public void set_communities(String communities) {
    this._communities = communities;
  }

  public String get_protocol() {
    return _protocol;
  }

  public void set_protocol(String protocol) {
    this._protocol = protocol;
  }

  public boolean get_valid() {return _valid; }

  public void set_valid(boolean v) { this._valid = v; }

  @Override
  public Environment deepCopy() {
    Environment env = new Environment();
    env.set_ad(this.get_ad());
    env.set_lp(this.get_lp());
    env.set_cost(this.get_cost());
    env.set_communities(this.get_communities());
    env.set_med(this.get_med());
    env.set_protocol(this.get_protocol());
    env.set_prefixLength(this.get_prefixLength());
    env.set_prefixValue(this.get_prefixValue());
    env.set_valid(this.get_valid());
    return env;
  }
}