package communication;

import com.github.rinde.rinsim.core.model.comm.MessageContents;

/**
 * Tagged union for the different types of message that can be sent.
 */
public abstract class TypedMessage implements MessageContents {

    public final MessageType type;

    TypedMessage(MessageType type) {
        this.type = type;
    }

    abstract String getDescription();
    public String toString() {
        return String.format("<Message.%s [%s]>", this.type, this.getDescription());
    }
//    public MessageType getType() {
//        return this.type;
//    }

//    public void setType(MessageType type) {
//        this.type = type;
//    }
}
