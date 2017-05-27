package cnet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krispeirelinck on 26/05/17.
 */
public class AuctionResult {

    private List<Auction> wonAuctions;
    private List<Auction> lostAuctions;

    AuctionResult() {
        this.wonAuctions = new ArrayList<>();
        this.lostAuctions = new ArrayList<>();
    }

    public void addWonAuction(Auction auction) {
        this.wonAuctions.add(auction);
    }

    public void addLostAuction(Auction auction) {
        this.lostAuctions.add(auction);
    }

    public List<Auction> getWonAuctions() {
        return this.wonAuctions;
    }

    public List<Auction> getLostAuctions() {
        return lostAuctions;
    }
}
