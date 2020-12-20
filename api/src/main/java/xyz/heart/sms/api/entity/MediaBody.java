package xyz.heart.sms.api.entity;

public class MediaBody {

    public String data;

    public MediaBody(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }

}
