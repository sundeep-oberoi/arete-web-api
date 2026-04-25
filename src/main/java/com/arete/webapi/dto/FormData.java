package com.arete.webapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormData {

    private String profile;
    private boolean coverPartner;
    private boolean coverChildren;
    private int numberOfChildren;
    private String age;
    private String postcode;
    private String opticalNeeds;
    private String dentalNeeds;
    private String alternativeMedicine;
    private String hospitalisationPreference;
    private String doctorChoice;
    private String email;
    private String phoneNumber;

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

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
