package lottery.player;

import lottery.infrastructure.Retailer;

public class PlayerMinimalist extends Player {

    private final Retailer favoriteRetailer;

    public PlayerMinimalist(PersonalInfo personalInfo, long balance, Retailer favoriteRetailer) {
        super(personalInfo, balance);
        this.favoriteRetailer = favoriteRetailer;
    }

    @Override
    public void buyTicket() {
        favoriteRetailer.buyRandomTicket(this, 1, 1);
    }
}