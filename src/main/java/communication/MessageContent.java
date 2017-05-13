package communication;

import com.github.rinde.rinsim.core.model.comm.MessageContents;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class MessageContent implements MessageContents {

    private MessageType type;

    public MessageContent() {}

    public MessageType getType() {
        return this.type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}
