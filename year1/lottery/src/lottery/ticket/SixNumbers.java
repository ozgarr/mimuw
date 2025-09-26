package lottery.ticket;

import lottery.exceptions.BadDataException;

import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public record SixNumbers(Set<Integer> numbers) {

    private static final Random RAND = new Random();
    private static final int MAX_NUMBER_OF_SETS = 13_983_816;
    private static final int MAX_VALUE_OF_NUMBER = 49;

    public SixNumbers {
        numbers = new TreeSet<>(numbers);
    }

    public static SixNumbers random() {
        Set<Integer> randomNumbers = new TreeSet<>();
        while (randomNumbers.size() < 6) {
            randomNumbers.add(RAND.nextInt(MAX_VALUE_OF_NUMBER) + 1);
        }
        return new SixNumbers(randomNumbers);
    }

    public static List<SixNumbers> randomList(int setCount) {
        if (setCount < 1) throw new BadDataException("Can't get a negative number of sets.");
        if (setCount > MAX_NUMBER_OF_SETS)
            throw new BadDataException("Not enough unique sets of numbers 1-49.");

        Set<SixNumbers> randomList = new HashSet<>();
        while (randomList.size() < setCount) {
            randomList.add(random());
        }

        return new ArrayList<>(randomList);
    }

    public int hitCount(SixNumbers randomNumbers) {
        Set<Integer> intersection = new TreeSet<>(this.numbers());
        intersection.retainAll(randomNumbers.numbers());
        return intersection.size();
    }

    public boolean areNumbersCorrect() {
        if (numbers.size() != 6) return false;
        for (int number : numbers) {
            if (number < 1 || number > MAX_VALUE_OF_NUMBER) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int number : numbers) {
            sb.append(String.format("%2d ", number));
        }
        return sb.toString();
    }
}