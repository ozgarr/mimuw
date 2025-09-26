package lottery.infrastructure;

import lottery.player.Player;
import lottery.ticket.Slip;
import lottery.ticket.TicketID;
import lottery.ticket.Ticket;
import lottery.ticket.SixNumbers;
import lottery.exceptions.BadBehaviourException;

import java.util.HashMap;
import java.util.Map;

public class Retailer {

    private static final LotteryHeadquarters HEADQUARTERS = LotteryHeadquarters.getInstance();

    private final int retailerNumber;
    private final Map<TicketID, Ticket> soldTickets;
    private final Map<TicketID, Ticket> claimedTickets;

    public Retailer() {
        HEADQUARTERS.incrementLastRetailerNumber();
        this.retailerNumber = HEADQUARTERS.lastRetailerNumber();
        this.soldTickets = new HashMap<>();
        this.claimedTickets = new HashMap<>();
        HEADQUARTERS.addRetailer(this);
    }

    public void buyTicketWithSlip(Player player, Slip slip) {
        if (!player.tryToPay(slip.price())) return;

        Ticket ticket = new Ticket(this, slip);
        HEADQUARTERS.receiveMoneyForTicketSale(ticket);
        soldTickets.put(ticket.ticketID(), ticket);
        player.addTicket(ticket);

    }

    public void buyRandomTicket(Player player, int betCount, int drawCount) {
        Slip slip = new Slip(SixNumbers.randomList(betCount), drawCount);
        buyTicketWithSlip(player, slip);
    }

    public void calculateDrawResults(Draw draw) {
        for (Ticket ticket : this.soldTickets.values()) {
            ticket.countHits(draw);
        }
    }

    public void givePrize(Player player, Ticket ticket) {
        if (!soldTickets.containsKey(ticket.ticketID()))
            throw new BadBehaviourException("Player can't claim prize for a ticket sold by a different retailer.");
        if (claimedTickets.containsKey(ticket.ticketID()))
            throw new BadBehaviourException("Player can't claim prize for a ticket already claimed.");

        for (int i = 0; i < ticket.drawCount(); i++) {
            int drawNumber = ticket.firstDrawNumber() + i;
            if (drawNumber <= HEADQUARTERS.lastDrawNumber()) {

                for (SixNumbers bet : ticket.bets()) {
                    HEADQUARTERS.givePrize(player, bet, drawNumber);
                }
            }
        }

        player.removeTicket(ticket);
        claimedTickets.put(ticket.ticketID(), ticket);
    }

    public int retailerNumber() {
        return retailerNumber;
    }
}