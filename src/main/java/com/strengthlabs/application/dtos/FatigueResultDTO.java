package com.strengthlabs.application.dtos;

public class FatigueResultDTO {

    private double atl;
    private double ctl;
    private double acwr;
    private double tsb;

    public FatigueResultDTO() {}

    public FatigueResultDTO(double atl, double ctl, double acwr, double tsb) {
        this.atl = atl;
        this.ctl = ctl;
        this.acwr = acwr;
        this.tsb = tsb;
    }

    public double getAtl() { return atl; }
    public void setAtl(double atl) { this.atl = atl; }
    public double getCtl() { return ctl; }
    public void setCtl(double ctl) { this.ctl = ctl; }
    public double getAcwr() { return acwr; }
    public void setAcwr(double acwr) { this.acwr = acwr; }
    public double getTsb() { return tsb; }
    public void setTsb(double tsb) { this.tsb = tsb; }
}
