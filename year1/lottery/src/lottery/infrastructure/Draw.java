package lottery.infrastructure;

import lottery.ticket.SixNumbers;

public class Draw {

    private final int drawNumber;
    private final SixNumbers numbers;

    private final int[] hitGrades;
    private long[] gradePools;
    private long[] prizes;

    public Draw(int drawNumber) {
        this.drawNumber = drawNumber;
        this.numbers = SixNumbers.random();
        this.hitGrades = new int[4];
        this.gradePools = new long[4];
        this.prizes = new long[4];
    }

    public void registerHit(int grade) {
        hitGrades[grade - 1]++;
    }

    public int drawNumber() {
        return drawNumber;
    }
    public SixNumbers numbers() {
        return numbers;
    }

    public int[] hitGrades() {
        return hitGrades.clone();
    }
    public long[] gradePools() {
        return gradePools.clone();
    }
    public long[] prizes() {
        return prizes.clone();
    }

    public void setGradePools(long[] puleStopni) {
        this.gradePools = puleStopni.clone();
    }
    public void setPrizes(long[] nagrody) {
        this.prizes = nagrody.clone();
    }

    @Override
    public String toString() {
        return "Draw no. " + drawNumber + "\nResults: " + numbers;
    }
}