package com.winthier.simpleshop;

public enum ShopType {
    BUY,
    SELL,
    ;

    @Override
    public String toString() {
        switch(this) {
        case SELL:
            return "sell";
        case BUY:
        default:
            return "buy";
        }
    }
}
