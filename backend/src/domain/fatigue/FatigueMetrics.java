package domain.fatigue;

public class FatigueMetrics {

    private final double atl;
    private final double ctl;
    private final double acwr;
    private final double tsb;

    public FatigueMetrics(double atl, double ctl, double acwr, double tsb) {
        this.atl = atl;
        this.ctl = ctl;
        this.acwr = acwr;
        this.tsb = tsb;
    }

    public double getAtl() {
        return atl;
    }

    public double getCtl() {
        return ctl;
    }

    public double getAcwr() {
        return acwr;
    }

    public double getTsb() {
        return tsb;
    }
}
