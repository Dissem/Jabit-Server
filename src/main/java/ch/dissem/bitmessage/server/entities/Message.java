package ch.dissem.bitmessage.server.entities;

import ch.dissem.bitmessage.entity.Plaintext;

/**
 * Created by chrigu on 30.09.15.
 */
public class Message {
    private Object id;
    private String subject;
    private String body;

    public Message(){}
    public Message(Plaintext plaintext) {
        this.id = plaintext.getId();
        this.subject = plaintext.getSubject();
        this.body = plaintext.getText();
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Object getId() {
        return id;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }
}
