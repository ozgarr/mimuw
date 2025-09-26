package lottery.player;

import lottery.utility.Formatter;
import lottery.ticket.TicketID;
import lottery.ticket.Ticket;
import lottery.exceptions.BadDataException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Player {

    protected long balance;
    protected final PersonalInfo personalInfo;

    protected Map<TicketID, Ticket> ownedTickets;

    public Player(PersonalInfo personalInfo, long balance) {
        if (balance < 0) throw new BadDataException("Player can't have a negative balance.");

        this.personalInfo = personalInfo;
        this.balance = balance;
        ownedTickets = new HashMap<>();
    }

    public abstract void buyTicket();

    public void collectTicket(Ticket ticket) {
        ticket.ticketID().retailer().givePrize(this, ticket);
    }

    public void collectAllTickets() {
        List<Ticket> toCollect = new ArrayList<>(ownedTickets.values());

        for (Ticket ticket : toCollect) {
            collectTicket(ticket);
        }
    }

    public void collectAllFinishedTickets() {
        List<Ticket> toCollect = new ArrayList<>();

        for (Ticket ticket : ownedTickets.values()) {
            if (ticket.allDrawsDone()) {
                toCollect.add(ticket);
            }
        }

        for (Ticket ticket : toCollect) {
            collectTicket(ticket);
        }
    }

    @Override
    public String toString() {
        return personalInfo.toString() + "\nBalance: " + Formatter.centsToString(balance);
    }

    public boolean tryToPay(long amount) {
        if (amount < 0) throw new BadDataException("Amount can't be negative.");
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }
    public void receiveAmount(long amount) {
        balance += amount;
    }

    public void addTicket(Ticket ticket) {
        ownedTickets.put(ticket.ticketID(), ticket);
    }
    public void removeTicket(Ticket ticket) {
        ownedTickets.remove(ticket.ticketID());
    }

    public PersonalInfo personalInfo() {
        return personalInfo;
    }
    public long balance() {
        return balance;
    }
    public List<Ticket> ownedTickets() { return new ArrayList<>(ownedTickets.values()); }
}