package com.arete.webapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "form_record")
public class FormRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_number", unique = true, nullable = false)
    private String formNumber;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "profile")
    private String profile;

    @Column(name = "cover_partner")
    private boolean coverPartner;

    @Column(name = "cover_children")
    private boolean coverChildren;

    @Column(name = "number_of_children")
    private int numberOfChildren;

    @Column(name = "age")
    private String age;

    @Column(name = "postcode")
    private String postcode;

    @Column(name = "optical_needs")
    private String opticalNeeds;

    @Column(name = "dental_needs")
    private String dentalNeeds;

    @Column(name = "alternative_medicine")
    private String alternativeMedicine;

    @Column(name = "hospitalisation_preference")
    private String hospitalisationPreference;

    @Column(name = "doctor_choice")
    private String doctorChoice;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "monthly_premium")
    private BigDecimal monthlyPremium;

    @Column(name = "annual_premium")
    private BigDecimal annualPremium;

    @Column(name = "currency")
    private String currency;

    @Column(name = "coverage_details", columnDefinition = "CLOB")
    private String coverageDetails;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFormNumber() { return formNumber; }
    public void setFormNumber(String formNumber) { this.formNumber = formNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public boolean isCoverPartner() { return coverPartner; }
    public void setCoverPartner(boolean coverPartner) { this.coverPartner = coverPartner; }

    public boolean isCoverChildren() { return coverChildren; }
    public void setCoverChildren(boolean coverChildren) { this.coverChildren = coverChildren; }

    public int getNumberOfChildren() { return numberOfChildren; }
    public void setNumberOfChildren(int numberOfChildren) { this.numberOfChildren = numberOfChildren; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public String getOpticalNeeds() { return opticalNeeds; }
    public void setOpticalNeeds(String opticalNeeds) { this.opticalNeeds = opticalNeeds; }

    public String getDentalNeeds() { return dentalNeeds; }
    public void setDentalNeeds(String dentalNeeds) { this.dentalNeeds = dentalNeeds; }

    public String getAlternativeMedicine() { return alternativeMedicine; }
    public void setAlternativeMedicine(String alternativeMedicine) { this.alternativeMedicine = alternativeMedicine; }

    public String getHospitalisationPreference() { return hospitalisationPreference; }
    public void setHospitalisationPreference(String hospitalisationPreference) {
        this.hospitalisationPreference = hospitalisationPreference;
    }

    public String getDoctorChoice() { return doctorChoice; }
    public void setDoctorChoice(String doctorChoice) { this.doctorChoice = doctorChoice; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public BigDecimal getMonthlyPremium() { return monthlyPremium; }
    public void setMonthlyPremium(BigDecimal monthlyPremium) { this.monthlyPremium = monthlyPremium; }

    public BigDecimal getAnnualPremium() { return annualPremium; }
    public void setAnnualPremium(BigDecimal annualPremium) { this.annualPremium = annualPremium; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCoverageDetails() { return coverageDetails; }
    public void setCoverageDetails(String coverageDetails) { this.coverageDetails = coverageDetails; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
