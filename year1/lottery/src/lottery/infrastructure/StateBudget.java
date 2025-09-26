package lottery.infrastructure;

import lottery.utility.Formatter;

public class StateBudget {

    private long income;
    private long subsidies;

    private StateBudget() {
        income = 0;
        subsidies = 0;
    }

    private static class Holder {
        private static final StateBudget INSTANCE = new StateBudget();
    }
    public static StateBudget getInstance() {
        return Holder.INSTANCE;
    }

    public void listStateFinanceRecords() {
        System.out.println("The state received " + Formatter.centsToString(income) + "\nThe state gave the lottery " +
                "headquarters " +
                Formatter.centsToString(subsidies) + " in subsidies.");
    }

    public void reset() {
        income = 0;
        subsidies = 0;
    }

    public void giveSubsidy(long amount) {
        subsidies += amount;
    }
    public void receiveTax(long amount) {
        income += amount;
    }

    public long income() {
        return income;
    }

    public long subsidies() {
        return subsidies;
    }
}