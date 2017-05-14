package communication;

import com.github.rinde.rinsim.core.model.comm.MessageContents;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class TypedMessage implements MessageContents {

    private MessageType type;

    public TypedMessage() {}

    public MessageType getType() {
        return this.type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}
