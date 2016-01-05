package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public class PreauthorizationRequest extends BaseRequest {

    /**
     * ID of the sub account
     */
    private String aid;

    private String clearingtype;

    private String reference;

    private int amount;

    private String currency;

    private String lastname;

    private String country;

    private String param;

    private String narrative_text;

    private String customerid;

    private String userid;

    private String salutation;

    private String title;

    private String firstname;

    private String company;

    private String street;

    private String zip;

    private String city;

    private String addressaddition;

    private String state;

    private String email;

    private String telephonenumber;

    private String birthday;

    private String language;

    private String vatid;

    private String gender;

    private String personalid;

    private String ip;

    public PreauthorizationRequest(final PayoneConfig config, final String clearingtype) {
        super(config, RequestType.PREAUTHORIZATION.getType());

        this.aid = config.getSubAccountId();
        this.clearingtype = clearingtype;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getAid() {
        return aid;
    }

    public String getClearingtype() {
        return clearingtype;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(final String lastname) {
        this.lastname = lastname;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getParam() {
        return param;
    }

    public void setParam(final String param) {
        this.param = param;
    }

    public String getNarrative_text() {
        return narrative_text;
    }

    public void setNarrative_text(final String narrative_text) {
        this.narrative_text = narrative_text;
    }

    public String getCustomerid() {
        return customerid;
    }

    public void setCustomerid(final String customerid) {
        this.customerid = customerid;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(final String userid) {
        this.userid = userid;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(final String salutation) {
        this.salutation = salutation;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(final String firstname) {
        this.firstname = firstname;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(final String company) {
        this.company = company;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(final String street) {
        this.street = street;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(final String zip) {
        this.zip = zip;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getAddressaddition() {
        return addressaddition;
    }

    public void setAddressaddition(final String addressaddition) {
        this.addressaddition = addressaddition;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getTelephonenumber() {
        return telephonenumber;
    }

    public void setTelephonenumber(final String telephonenumber) {
        this.telephonenumber = telephonenumber;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(final String birthday) {
        this.birthday = birthday;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getVatid() {
        return vatid;
    }

    public void setVatid(final String vatid) {
        this.vatid = vatid;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getPersonalid() {
        return personalid;
    }

    public void setPersonalid(final String personalid) {
        this.personalid = personalid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }
}
