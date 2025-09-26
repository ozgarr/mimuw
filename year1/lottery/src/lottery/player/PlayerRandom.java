package lottery.player;

import lottery.infrastructure.LotteryHeadquarters;
import lottery.infrastructure.Retailer;

import java.util.Random;

public class PlayerRandom extends Player {

    private static final Random RAND = new Random();

    public PlayerRandom(PersonalInfo personalInfo) {
        super(personalInfo, RAND.nextInt(1_000_000_00));
    }

    @Override
    public void buyTicket() {
        Retailer retailer = chooseRandomRetailer();
        int ticketCount = RAND.nextInt(100) + 1;
        for (int i = 0; i < ticketCount; i++) {
            int betCount = RAND.nextInt(8) + 1;
            int drawCount = RAND.nextInt(10) + 1;
            retailer.buyRandomTicket(this, betCount, drawCount);
        }
    }

    private Retailer chooseRandomRetailer() {
        LotteryHeadquarters headquarters = LotteryHeadquarters.getInstance();
        int retailerIndex = RAND.nextInt(headquarters.lastRetailerNumber());
        return headquarters.getRetailer(retailerIndex);
    }
}