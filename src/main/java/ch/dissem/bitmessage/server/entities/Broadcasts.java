package ch.dissem.bitmessage.server.entities;

import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;

import java.util.Collection;

/**
 * Created by chrigu on 30.09.15.
 */
public class Broadcasts {
    private final Sender sender;
    private final Message[] messages;

    public Broadcasts(BitmessageAddress sender, Collection<Plaintext> messages) {
        this.sender = new Sender(sender);
        this.messages = new Message[messages.size()];
        int i = 0;
        for (Plaintext msg : messages) {
            this.messages[i] = new Message(msg);
        }
    }

    public Sender getSender() {
        return sender;
    }

    public Message[] getMessages() {
        return messages;
    }
}
