package net.dovtech.betterfactions.faction;

import java.util.List;

public class TradeDeal {

    private BetterFaction seller;
    private BetterFaction buyer;
    private List<?> payment = null;
    private List<?> items = null;
    private int repeatTimes = 0;
    private int repeatInterval = 0;
    private boolean gift = false;

    public TradeDeal(BetterFaction seller, BetterFaction buyer, List<?> payment, List<?> items) {
        this.seller = seller;
        this.buyer = buyer;
        this.payment = payment;
        this.items = items;
        if(payment == null) {
            gift = true;
        }
        addTradeOpinion(buyer, seller);
        addTradeOpinion(seller, buyer);
    }

    public TradeDeal(BetterFaction seller, BetterFaction buyer, List<?> payment, List<?> items, int repeatTimes, int repeatInterval) {
        this.seller = seller;
        this.buyer = buyer;
        this.payment = payment;
        this.items = items;
        if(payment == null) {
            gift = true;
        }
        this.repeatTimes = repeatTimes;
        this.repeatInterval = repeatInterval;
        addTradeOpinion(buyer, seller);
        addTradeOpinion(seller, buyer);
    }

    public void addTradeOpinion(BetterFaction faction, BetterFaction target) {
        target.addOpinion(faction, "+15 : Trade Deal");
    }
}
