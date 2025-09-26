package lottery.player;

import lottery.infrastructure.LotteryHeadquarters;
import lottery.infrastructure.Retailer;
import lottery.ticket.Slip;
import lottery.exceptions.BadDataException;

import java.util.List;

public class PlayerSameSlip extends PlayerRegular {

    private final int ticketBuyingDelay;
    private int lastDrawNumber;

    public PlayerSameSlip(PersonalInfo personalInfo, long balance, Slip regularSlip,
                          List<Retailer> favoriteRetailers, int ticketBuyingDelay) {
        super(personalInfo, balance, regularSlip, favoriteRetailers);
        if (ticketBuyingDelay < 1)
            throw new BadDataException("The delay can't be a negative number.");
        this.ticketBuyingDelay = ticketBuyingDelay;
        this.lastDrawNumber = 0;
    }

    @Override
    public void buyTicket() {
        LotteryHeadquarters headquarters = LotteryHeadquarters.getInstance();
        if (headquarters.lastDrawNumber() % ticketBuyingDelay !=
                lastDrawNumber % ticketBuyingDelay && lastDrawNumber != 0) {
            return;
        }
        lastDrawNumber = headquarters.lastDrawNumber();
        nextRetailer().buyTicketWithSlip(this, regularSlip);
    }
}