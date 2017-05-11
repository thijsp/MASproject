package communication;

import com.github.rinde.rinsim.core.model.comm.MessageContents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

public class NewParcelMessage implements MessageContents {

    private MessageType type;

    NewParcelMessage(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return this.type;
    }





}
