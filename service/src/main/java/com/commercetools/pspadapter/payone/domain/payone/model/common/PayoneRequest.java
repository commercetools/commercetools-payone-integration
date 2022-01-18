package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.util.ClearSecuredValuesSerializer;

/**
 * @author fhaertig
 * @since 11.12.15
 */
public class PayoneRequest extends BaseRequest {

    /**
     * ID of the sub account
     */
    private String aid;

    private String clearingtype;

    private String reference;

    private int amount;

    private String currency;

    @ClearSecuredValuesSerializer.Apply
    private String lastname;

    @ClearSecuredValuesSerializer.Apply
    private String country;

    @ClearSecuredValuesSerializer.Apply
    private String param;

    @ClearSecuredValuesSerializer.Apply
    private String narrative_text;

    @ClearSecuredValuesSerializer.Apply
    private String customerid;

    @ClearSecuredValuesSerializer.Apply
    private String userid;

    @ClearSecuredValuesSerializer.Apply
    private String salutation;

    @ClearSecuredValuesSerializer.Apply
    private String title;

    @ClearSecuredValuesSerializer.Apply
    private String firstname;

    @ClearSecuredValuesSerializer.Apply
    private String company;

    @ClearSecuredValuesSerializer.Apply
    private String street;

    @ClearSecuredValuesSerializer.Apply
    private String zip;

    @ClearSecuredValuesSerializer.Apply
    private String city;

    @ClearSecuredValuesSerializer.Apply
    private String addressaddition;

    @ClearSecuredValuesSerializer.Apply
    private String state;

    @ClearSecuredValuesSerializer.Apply
    private String email;

    @ClearSecuredValuesSerializer.Apply
    private String telephonenumber;

    @ClearSecuredValuesSerializer.Apply
    private String birthday;

    @ClearSecuredValuesSerializer.Apply
    private String language;

    private String vatid;

    @ClearSecuredValuesSerializer.Apply
    private String gender;

    @ClearSecuredValuesSerializer.Apply
    private String personalid;

    @ClearSecuredValuesSerializer.Apply
    private String ip;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_firstname;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_lastname;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_email;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_company;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_street;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_zip;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_city;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_state;

    @ClearSecuredValuesSerializer.Apply
    private String shipping_country;

    private String successurl;

    private String errorurl;

    private String backurl;

    protected PayoneRequest(final PayoneConfig config, final String requestType, final String clearingtype) {
        super(config, requestType);

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

    public String getShipping_firstname() {
        return shipping_firstname;
    }

    public void setShipping_firstname(final String shipping_firstname) {
        this.shipping_firstname = shipping_firstname;
    }

    public String getShipping_lastname() {
        return shipping_lastname;
    }

    public void setShipping_lastname(final String shipping_lastname) {
        this.shipping_lastname = shipping_lastname;
    }

    public String getShipping_company() {
        return shipping_company;
    }

    public void setShipping_company(final String shipping_company) {
        this.shipping_company = shipping_company;
    }

    public String getShipping_street() {
        return shipping_street;
    }

    public void setShipping_street(final String shipping_street) {
        this.shipping_street = shipping_street;
    }

    public String getShipping_zip() {
        return shipping_zip;
    }

    public void setShipping_zip(final String shipping_zip) {
        this.shipping_zip = shipping_zip;
    }

    public String getShipping_city() {
        return shipping_city;
    }

    public void setShipping_city(final String shipping_city) {
        this.shipping_city = shipping_city;
    }

    public String getShipping_state() {
        return shipping_state;
    }

    public void setShipping_state(final String shipping_state) {
        this.shipping_state = shipping_state;
    }

    public String getShipping_country() {
        return shipping_country;
    }

    public void setShipping_country(final String shipping_country) {
        this.shipping_country = shipping_country;
    }

    public String getSuccessurl() {
        return successurl;
    }

    public void setSuccessurl(final String successurl) {
        this.successurl = successurl;
    }

    public String getErrorurl() {
        return errorurl;
    }

    public void setErrorurl(final String errorurl) {
        this.errorurl = errorurl;
    }

    public String getBackurl() {
        return backurl;
    }

    public void setBackurl(final String backurl) {
        this.backurl = backurl;
    }

    public String getShipping_email() {
        return shipping_email;
    }

    public void setShipping_email(String shipping_email) {
        this.shipping_email = shipping_email;
    }


}
