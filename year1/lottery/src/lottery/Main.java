package lottery;

import lottery.player.*;
import lottery.infrastructure.StateBudget;
import lottery.infrastructure.LotteryHeadquarters;
import lottery.infrastructure.Retailer;
import lottery.ticket.Slip;
import lottery.ticket.SixNumbers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Main {

    private static final Random RAND = new Random();

    private static List<Retailer> createRetailers(int retailerCount) {
        List<Retailer> retailers = new ArrayList<>();
        for (int i = 0; i < retailerCount; i++) {
            retailers.add(new Retailer());
        }
        return retailers;
    }

    private static List<Player> createPlayers(int PlayerOfEachTypeCount, List<Retailer> retailers) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < PlayerOfEachTypeCount; i++) {
            players.add(new PlayerRandom(PersonalInfo.generateRandomInfo()));

            players.add(new PlayerMinimalist(PersonalInfo.generateRandomInfo(),
                   randomBalance(1_000_000_00), randomRetailer(retailers)));

            players.add(new PlayerSameNumbers(PersonalInfo.generateRandomInfo(),
                   randomBalance(1_000_000_00), SixNumbers.random(), randomRetailers(retailers)));

            Slip slip = new Slip(SixNumbers.randomList(RAND.nextInt(8) + 1),
                   RAND.nextInt(10) + 1);
            players.add(new PlayerSameSlip(PersonalInfo.generateRandomInfo(), randomBalance(1_000_000_00),
                   slip, randomRetailers(retailers), RAND.nextInt(5) + 1));
       }
       return players;
    }

    private static List<Retailer> randomRetailers(List<Retailer> retailers) {
        List<Retailer> copy = new ArrayList<>(retailers);
        Collections.shuffle(copy);
        return copy.subList(0, RAND.nextInt(copy.size()) + 1);
    }

    private static Retailer randomRetailer(List<Retailer> retailers) {
        return retailers.get(RAND.nextInt(retailers.size()));
    }

    private static long randomBalance(long limit) {
        return RAND.nextLong(limit);
    }

    private static void simulate(List<Player> players, int drawCount) {
        LotteryHeadquarters headquarters = LotteryHeadquarters.getInstance();
        for (int i = 0; i < drawCount; i++) {
            for (Player player : players) {
                player.buyTicket();
            }

            headquarters.drawNumbers();

            for (Player player : players) {
                player.collectAllFinishedTickets();
            }
        }
    }

    private static void printMillionaires(List<Player> players) {
        System.out.println("Millionaires:");

        boolean millionairesExist = false;
        for (Player player : players) {
            if (player.balance() > 1_000_000_00L) {
                millionairesExist = true;
                System.out.println(player);
            }
        }
        if (!millionairesExist) {
            System.out.println("nobody became a millionaire :(");
        }
    }

    public static void main(String[] args) {
        LotteryHeadquarters headquarters = LotteryHeadquarters.getInstance();
        StateBudget stateBudget = StateBudget.getInstance();

        List<Retailer> retailers = createRetailers(10);
        List<Player> players = createPlayers(200, retailers);

        simulate(players, 20);

        headquarters.printResultsOfAllDraws();
        System.out.println();

        headquarters.giveBalanceDetails();
        stateBudget.listStateFinanceRecords();
        System.out.println();

        printMillionaires(players);
    }
}