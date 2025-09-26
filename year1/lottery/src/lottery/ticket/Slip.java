package lottery.ticket;

import lottery.exceptions.BadDataException;

import java.util.List;
import java.util.ArrayList;

public record Slip(List<SixNumbers> bets, int drawCount) {

    private static final long BET_PRIZE = 3_00;

    public Slip(List<SixNumbers> bets, int drawCount) {
        List<SixNumbers> verified = verifyBets(bets);
        if (verified.isEmpty()) throw new BadDataException("No correct bet was given.");
        if (verified.size() > 8) throw new BadDataException("Too many bets were given, limit is 8.");
        if (drawCount < 1 || drawCount > 10)
            throw new BadDataException("Too many draws were given, keep it between 1-10.");

        this.bets = List.copyOf(verified);
        this.drawCount = drawCount;
    }

    private List<SixNumbers> verifyBets(List<SixNumbers> bets) {
        List<SixNumbers> verified = new ArrayList<>();
        for (SixNumbers bet : bets) {
            if (bet.areNumbersCorrect()) {
                verified.add(bet);
            }
        }
        return verified;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Bets:\n");
        int i = 1;
        for (SixNumbers bet : bets) {
            sb.append(i++).append(". ").append(bet).append("\n");
        }
        sb.append("Draw count: ").append(drawCount);
        return sb.toString();
    }

    public List<SixNumbers> bets() {
        return List.copyOf(bets);
    }

    public long price() {
        return (long) bets.size() * drawCount * BET_PRIZE;
    }
}