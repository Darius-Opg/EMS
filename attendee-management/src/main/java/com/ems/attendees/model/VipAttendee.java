package com.ems.attendees.model;

public class VipAttendee extends Attendee {

    private String vipLevel;
    private String specialRequirements;

    public VipAttendee() {
        super();
    }

    public VipAttendee(
            Long eventId,
            String fullName,
            String email,
            String phone,
            String vipLevel,
            String specialRequirements) {

        super(eventId, fullName, email, phone);
        this.vipLevel = vipLevel;
        this.specialRequirements = specialRequirements;
    }

    public String getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(String vipLevel) {
        this.vipLevel = vipLevel;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    @Override
    public boolean isVip() {
        return true;
    }
}
