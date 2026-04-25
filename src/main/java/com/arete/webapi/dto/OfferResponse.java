package com.arete.webapi.dto;

import java.util.List;

public class OfferResponse {

    private double monthlyPremium;
    private double annualPremium;
    private String currency;
    private List<String> coverageDetails;

    public OfferResponse(double monthlyPremium, double annualPremium, String currency, List<String> coverageDetails) {
        this.monthlyPremium = monthlyPremium;
        this.annualPremium = annualPremium;
        this.currency = currency;
        this.coverageDetails = coverageDetails;
    }

    public double getMonthlyPremium() { return monthlyPremium; }
    public double getAnnualPremium() { return annualPremium; }
    public String getCurrency() { return currency; }
    public List<String> getCoverageDetails() { return coverageDetails; }
}
