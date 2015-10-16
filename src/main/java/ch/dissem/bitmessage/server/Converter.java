package ch.dissem.bitmessage.server;

import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.server.entities.Broadcasts;
import ch.dissem.bitmessage.server.entities.Message;
import ch.dissem.bitmessage.server.entities.Sender;
import ch.dissem.bitmessage.utils.UnixTime;

import java.util.Collection;

public class Converter {
    public static Broadcasts broadcasts(BitmessageAddress sender, Collection<Plaintext> messages) {
        Broadcasts result = new Broadcasts();
        result.sender = sender(sender);
        result.messages = new Message[messages.size()];
        int i = 0;
        for (Plaintext msg : messages) {
            result.messages[i] = message(msg);
            i++;
        }
        return result;
    }

    public static Broadcasts broadcasts(BitmessageAddress sender, Message... messages) {
        Broadcasts result = new Broadcasts();
        result.sender = sender(sender);
        result.messages = messages;
        return result;
    }

    public static Sender sender(BitmessageAddress sender) {
        Sender result = new Sender();
        result.address = sender.getAddress();
        result.alias = sender.toString();
        return result;
    }

    public static Message message(String subject, String body) {
        Message result = new Message();
        result.id = 0;
        result.received = UnixTime.now();
        result.subject = subject;
        result.body = body;
        return result;
    }

    public static Message message(Plaintext plaintext) {
        Message result = new Message();
        result.id = plaintext.getId();
        result.received = plaintext.getReceived();
        result.subject = plaintext.getSubject();
        result.body = plaintext.getText();
        return result;
    }
}
