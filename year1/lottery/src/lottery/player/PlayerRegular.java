package lottery.player;

import lottery.infrastructure.Retailer;
import lottery.ticket.Slip;
import lottery.exceptions.BadDataException;

import java.util.List;

public abstract class PlayerRegular extends Player {

    protected final Slip regularSlip;
    protected final List<Retailer> favoriteRetailers;
    protected int lastRetailerIndex;

    public PlayerRegular(PersonalInfo personalInfo, long balance, Slip regularSlip,
                         List<Retailer> favoriteRetailers) {
        super(personalInfo, balance);
        if (favoriteRetailers.isEmpty())
            throw new BadDataException("Player has to have at least one favorite retailer.");

        this.regularSlip = regularSlip;
        this.favoriteRetailers = favoriteRetailers;
        this.lastRetailerIndex = 0;
    }

    protected Retailer nextRetailer() {
        lastRetailerIndex %= favoriteRetailers.size();
        Retailer result = favoriteRetailers.get(lastRetailerIndex);
        lastRetailerIndex++;
        return result;
    }
}