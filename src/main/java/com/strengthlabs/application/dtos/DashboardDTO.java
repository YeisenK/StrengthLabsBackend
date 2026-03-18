package com.strengthlabs.application.dtos;

import java.util.List;

public class DashboardDTO {

    private double atl;
    private double ctl;
    private double acwr;
    private double tsb;
    private String riskZone;
    private List<SessionSummary> recentSessions;
    private List<HistoryPoint> history;

    public static class SessionSummary {
        private String id;
        private String title;
        private String date;
        private int durationMinutes;
        private double rpe;
        private double load;

        public SessionSummary(String id, String title, String date,
                              int durationMinutes, double rpe, double load) {
            this.id = id;
            this.title = title;
            this.date = date;
            this.durationMinutes = durationMinutes;
            this.rpe = rpe;
            this.load = load;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDate() { return date; }
        public int getDurationMinutes() { return durationMinutes; }
        public double getRpe() { return rpe; }
        public double getLoad() { return load; }
    }

    public static class HistoryPoint {
        private String date;
        private double atl;
        private double ctl;
        private double acwr;
        private double tsb;

        public HistoryPoint(String date, double atl, double ctl, double acwr, double tsb) {
            this.date = date;
            this.atl = atl;
            this.ctl = ctl;
            this.acwr = acwr;
            this.tsb = tsb;
        }

        public String getDate() { return date; }
        public double getAtl() { return atl; }
        public double getCtl() { return ctl; }
        public double getAcwr() { return acwr; }
        public double getTsb() { return tsb; }
    }

    public double getAtl() { return atl; }
    public void setAtl(double atl) { this.atl = atl; }
    public double getCtl() { return ctl; }
    public void setCtl(double ctl) { this.ctl = ctl; }
    public double getAcwr() { return acwr; }
    public void setAcwr(double acwr) { this.acwr = acwr; }
    public double getTsb() { return tsb; }
    public void setTsb(double tsb) { this.tsb = tsb; }
    public String getRiskZone() { return riskZone; }
    public void setRiskZone(String riskZone) { this.riskZone = riskZone; }
    public List<SessionSummary> getRecentSessions() { return recentSessions; }
    public void setRecentSessions(List<SessionSummary> recentSessions) { this.recentSessions = recentSessions; }
    public List<HistoryPoint> getHistory() { return history; }
    public void setHistory(List<HistoryPoint> history) { this.history = history; }
}
