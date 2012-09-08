package eu.ttbox.geoping.domain;

public class GeoTrackSmsMsg {

    public String smsNumber;
    public String action;
    public String body;

    public GeoTrackSmsMsg() {
        super();
    }

    public GeoTrackSmsMsg(String smsNumber, String action, String body) {
        super();
        this.smsNumber = smsNumber;
        this.action = action;
        this.body = body;
    }

    @Override
    public String toString() {
        return "GeoTrackSmsMsg [smsNumber=" + smsNumber + ", action=" + action + ", body=" + body + "]";
    }
    
    

}