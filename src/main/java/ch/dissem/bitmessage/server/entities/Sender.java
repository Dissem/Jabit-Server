package ch.dissem.bitmessage.server.entities;

import ch.dissem.bitmessage.entity.BitmessageAddress;

/**
 * Created by chrigu on 30.09.15.
 */
public class Sender {
    private final String address;
    private final String alias;

    public Sender(BitmessageAddress address) {
        this.address = address.getAddress();
        this.alias = address.toString();
    }

    public String getAddress() {
        return address;
    }

    public String getAlias() {
        return alias;
    }
}
