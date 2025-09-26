package lottery.ticket;

import lottery.infrastructure.Retailer;

import java.util.Objects;
import java.util.Random;

public class TicketID {

    private final int ticketNumber;
    private final int retailerNumber;
    private final int randomMarker;
    private final int controlSum;
    private final Retailer retailer;

    public TicketID(int ticketNumber, Retailer retailer) {
        this.ticketNumber = ticketNumber;
        this.retailerNumber = retailer.retailerNumber();
        this.retailer = retailer;

        Random rand = new Random();
        this.randomMarker = rand.nextInt(1_000_000_000);
        this.controlSum = calculateControlSum();
    }

    private int calculateControlSum() {
        return (sumOfDigits(ticketNumber) + sumOfDigits(retailerNumber) + sumOfDigits(randomMarker)) % 100;
    }

    private int sumOfDigits(int number) {
        int result = 0;
        while (number > 0) {
            result += number % 10;
            number /= 10;
        }
        return result;
    }

    @Override
    public String toString() {
        return ticketNumber + "-" + retailerNumber + "-" +
                String.format("%09d", randomMarker) + "-" +
                String.format("%02d", controlSum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TicketID other = (TicketID) o;
        return this.ticketNumber == other.ticketNumber() && this.retailerNumber == other.retailerNumber()
                && this.randomMarker == other.randomMarker() && this.controlSum == other.controlSum();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketNumber, retailerNumber, randomMarker, controlSum);
    }

    public Retailer retailer() {
        return retailer;
    }
    public int ticketNumber() {
        return ticketNumber;
    }
    public int retailerNumber() {
        return retailerNumber;
    }
    public int randomMarker() {
        return randomMarker;
    }
    public int controlSum() {
        return controlSum;
    }
}