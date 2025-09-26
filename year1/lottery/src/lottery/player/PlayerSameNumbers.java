package lottery.player;

import lottery.infrastructure.Retailer;
import lottery.ticket.Slip;
import lottery.ticket.Ticket;
import lottery.ticket.SixNumbers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerSameNumbers extends PlayerRegular {

    public PlayerSameNumbers(PersonalInfo personalInfo, long balance, SixNumbers favoriteNumbers,
                             List<Retailer> favoriteRetailers) {
        super(personalInfo, balance,
                new Slip(new ArrayList<>(Collections.singletonList(favoriteNumbers)), 10),
                favoriteRetailers);
    }

    @Override
    public void buyTicket() {
        if (!allTicketsDone()) return;
        nextRetailer().buyTicketWithSlip(this, regularSlip);
    }

    private boolean allTicketsDone() {
        for (Ticket ticket : ownedTickets.values()) {
            if (!ticket.allDrawsDone()) return false;
        }
        return true;
    }
}