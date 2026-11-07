package com.ems.events.model;

public class Vendor {

    private long vendorId;
    private long eventId;
    private String vendorName;
    private String serviceType;
    private String contactPerson;
    private String phone;
    private String email;

    public Vendor() {
    }

    public Vendor(long vendorId, long eventId, String vendorName,
                  String serviceType, String contactPerson,
                  String phone, String email) {
        this.vendorId = vendorId;
        this.eventId = eventId;
        this.vendorName = vendorName;
        this.serviceType = serviceType;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
    }

    public long getVendorId() {
        return vendorId;
    }

    public void setVendorId(long vendorId) {
        this.vendorId = vendorId;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}