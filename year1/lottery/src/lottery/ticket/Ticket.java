package lottery.ticket;

import lottery.utility.Formatter;
import lottery.infrastructure.LotteryHeadquarters;
import lottery.infrastructure.Retailer;
import lottery.infrastructure.Draw;

import java.util.List;

public class Ticket {

    private static final LotteryHeadquarters HEADQUARTERS = LotteryHeadquarters.getInstance();

    private final TicketID ticketID;
    private final int firstDrawNumber;
    private final int drawCount;
    private final long ticketPrice;
    private final List<SixNumbers> bets;

    public Ticket(Retailer retailer, Slip slip) {
        HEADQUARTERS.incrementLastTicketNumber();
        this.ticketID = new TicketID(HEADQUARTERS.lastTicketNumber(), retailer);
        this.firstDrawNumber = HEADQUARTERS.lastDrawNumber() + 1;
        this.bets = List.copyOf(slip.bets());
        this.drawCount = slip.drawCount();
        this.ticketPrice = slip.price();
    }

    public void countHits(Draw draw) {
        int drawNumber = draw.drawNumber();
        if (firstDrawNumber > drawNumber ||
                firstDrawNumber + drawCount <= drawNumber) {
            return;
        }

        for (SixNumbers bet : bets) {
            int hitCount = bet.hitCount(draw.numbers());
            if (hitCount >= 3) {
                draw.registerHit(prizeGrade(hitCount));
            }
        }
    }

    private int prizeGrade(int hitCount) {
        return 7 - hitCount;
    }

    public boolean allDrawsDone() {
        return firstDrawNumber + drawCount - 1 <= HEADQUARTERS.lastDrawNumber();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TICKET NO. ").append(ticketID).append("\n");

        int i = 1;
        for (SixNumbers bet : bets) {
            sb.append(i++).append(": ").append(bet).append("\n");
        }

        sb.append("DRAW COUNT: ").append(drawCount).append("\nDRAWS' NUMBERS:\n");
        for (int j = 0; j < drawCount; j++) {
            sb.append(" ").append(firstDrawNumber + j).append(" ");
        }

        sb.append("\nPRICE: ").append(Formatter.centsToString(ticketPrice));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ticket other = (Ticket) o;
        return this.ticketID.equals(other.ticketID());
    }

    @Override
    public int hashCode() {
        return ticketID.hashCode();
    }

    public TicketID ticketID() {
        return ticketID;
    }
    public int firstDrawNumber() {
        return firstDrawNumber;
    }
    public int drawCount() {
        return drawCount;
    }
    public long ticketPrice() {
        return ticketPrice;
    }
    public List<SixNumbers> bets() {
        return List.copyOf(bets);
    }
}