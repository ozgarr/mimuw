package lottery.infrastructure;

import lottery.utility.Formatter;
import lottery.player.Player;
import lottery.ticket.Ticket;
import lottery.ticket.SixNumbers;
import lottery.exceptions.BadDataException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LotteryHeadquarters {

    private static final long MINIMUM_FIRST_GRADE_PRIZE_POOL = 2_000_000_00L;
    private static final int INCOME_PERCENTAGE_FOR_PRIZES = 51;
    private static final int BUDGET_PERCENTAGE_FIRST_GRADE = 44;
    private static final int BUDGET_PERCENTAGE_SECOND_GRADE = 8;
    private static final long FOURTH_GRADE_PRIZE = 24_00L;
    private static final long MINIMUM_THIRD_GRADE_PRIZE = 36_00L;
    private static final long MINIMUM_TAXED_PRIZE = 2280_00L;

    private int lastDrawNumber;
    private final List<Draw> draws;
    private final List<Retailer> retailers;

    private long balance;
    private long rollover;
    private final long[] nextDrawsIncome;

    private int lastTicketNumber;
    private int lastRetailerNumber;

    private LotteryHeadquarters() {
        balance = 0;
        lastDrawNumber = 0;
        rollover = 0;
        lastTicketNumber = 0;
        lastRetailerNumber = 0;
        nextDrawsIncome = new long[10];
        draws = new ArrayList<>();
        retailers = new ArrayList<>();
    }
    
    private static class Holder {
        private static final LotteryHeadquarters INSTANCE = new LotteryHeadquarters();
    }
    public static LotteryHeadquarters getInstance() {
        return Holder.INSTANCE;
    }

    public void drawNumbers() {
        long budget = nextDrawsIncome[(++lastDrawNumber) % 10]
                * INCOME_PERCENTAGE_FOR_PRIZES / 100L;

        Draw draw = new Draw(lastDrawNumber);
        for (Retailer retailer : retailers) {
            retailer.calculateDrawResults(draw);
        }

        long[] gradePools = calculateGradePools(budget, draw.hitGrades());
        long[] prizes = calculatePrizes(gradePools, draw.hitGrades());
        draw.setPrizes(prizes);
        draw.setGradePools(gradePools);

        nextDrawsIncome[lastDrawNumber % 10] = 0;
        draws.add(draw);
    }

    private long[] calculateGradePools(long budget, int[] hitGrades) {
        long[] gradePools = new long[4];

        gradePools[0] = Math.max((budget * BUDGET_PERCENTAGE_FIRST_GRADE / 100), MINIMUM_FIRST_GRADE_PRIZE_POOL)
                + rollover;
        gradePools[1] = budget * BUDGET_PERCENTAGE_SECOND_GRADE / 100;
        gradePools[3] = FOURTH_GRADE_PRIZE * hitGrades[3];
        long remainder = budget - (budget * BUDGET_PERCENTAGE_FIRST_GRADE / 100) - gradePools[1] - gradePools[3];
        gradePools[2] = Math.max(remainder, MINIMUM_THIRD_GRADE_PRIZE * hitGrades[2]);

        return gradePools;
    }

    private long[] calculatePrizes(long[] gradePools, int[] hitGrades) {
        long[] prizes = new long[4];

        if (hitGrades[0] == 0) {
            rollover = gradePools[0];
        }   else {
            prizes[0] = gradePools[0] / hitGrades[0];
            rollover = 0;
        }
        for (int i = 1; i < 4; i++) {
            if (hitGrades[i] != 0) {
                prizes[i] = gradePools[i] / hitGrades[i];
            }
        }

        return prizes;
    }

    public void givePrize(Player player, SixNumbers bet, int drawNumber) {
        if (!bet.areNumbersCorrect()) throw new BadDataException("Bad set of numbers.");
        if (drawNumber > lastDrawNumber) throw new BadDataException("This draw hasn't happened yet.");

        int hitCount = bet.hitCount(getNumbers(drawNumber));
        if (hitCount < 3) return;

        long prize = getDraw(drawNumber).prizes()[prizeIndex(hitCount)];
        loseMoney(prize);

        if (prize >= MINIMUM_TAXED_PRIZE) {
            StateBudget stateBudget = StateBudget.getInstance();
            stateBudget.receiveTax(prize / 10); // Tax is 10%
            prize = prize * 9 / 10;
        }

        player.receiveAmount(prize);
    }

    private int prizeIndex(int hitCount) {
        return 6 - hitCount;
    }

    public void giveBalanceDetails() {
        System.out.println("Lottery headquarters have " + Formatter.centsToString(balance));
    }

    public void printDrawResults(Draw draw) {
        long[] gradePools = draw.gradePools();
        long[] prizes = draw.prizes();
        int[] hitGrades = draw.hitGrades();
        String[] grades = new String[] {"I.   ", "II.  ", "III. ", "IV.  "};

        System.out.println(draw);
        System.out.println("Combined prize pools:");
        for (int i = 0; i < 4; i++) {
            System.out.println(grades[i] + Formatter.centsToString(gradePools[i]));
        }
        System.out.println("Number of winners:");
        for (int i = 0; i < 4; i++) {
            System.out.println(grades[i] + hitGrades[i]);
        }
        System.out.println("Prize amounts:");
        for (int i = 0; i < 4; i++) {
            if (prizes[i] == 0) {
                System.out.println(grades[i] + "no hit");
            } else {
                System.out.println(grades[i] + Formatter.centsToString(prizes[i]));
            }
        }
    }

    public void printResultsOfAllDraws() {
        for (Draw draw : draws) {
            printDrawResults(draw);
            System.out.println();
        }
    }

    public void receiveMoneyForTicketSale(Ticket ticket) {
        long income = subtractTax(ticket.ticketPrice());
        int firstDrawNumber = ticket.firstDrawNumber();
        int drawCount = ticket.drawCount();
        for (int i = 0; i < drawCount; i++) {
            nextDrawsIncome[(firstDrawNumber + i) % 10] += income / drawCount;
        }
        getMoney(income);
    }

    private long subtractTax(long amount) {
        StateBudget stateBudget = StateBudget.getInstance();
        stateBudget.receiveTax(amount / 5); // Tax is 20%
        return amount * 4 / 5;
    }

    public void reset() {
        balance = 0;
        lastDrawNumber = 0;
        rollover = 0;
        lastTicketNumber = 0;
        lastRetailerNumber = 0;
        Arrays.fill(nextDrawsIncome, 0);
        draws.clear();
        retailers.clear();
    }

    public int lastTicketNumber() {
        return lastTicketNumber;
    }
    public void incrementLastTicketNumber() {
        lastTicketNumber++;
    }

    public int lastDrawNumber() {
        return lastDrawNumber;
    }
    public List<Draw> draws() {
        return List.copyOf(draws);
    }

    public int lastRetailerNumber() {
        return lastRetailerNumber;
    }
    public void incrementLastRetailerNumber() {
        lastRetailerNumber++;
    }

    public void addRetailer(Retailer retailer) {
        retailers.add(retailer);
    }
    public Retailer getRetailer(int index) {
        return retailers.get(index);
    }

    public SixNumbers getNumbers(int drawNumber) {
        return draws.get(drawNumber - 1).numbers();
    }
    public Draw getDraw(int drawNumber) {
        return draws.get(drawNumber - 1);
    }

    public long balance() {
        return balance;
    }

    public long rollover() {
        return rollover;
    }

    public void setBalance(long amount) {
        balance = amount;
    }
    public void getMoney(long amount) {
        balance += amount;
    }
    public void loseMoney(long amount) {
        balance -= amount;
        if (balance < 0) {
            StateBudget stateBudget = StateBudget.getInstance();
            stateBudget.giveSubsidy(-balance);
            balance = 0;
        }
    }
}