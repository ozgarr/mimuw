package testowanie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import lottery.player.PersonalInfo;
import lottery.player.Player;
import lottery.player.PlayerSameSlip;
import lottery.infrastructure.StateBudget;
import lottery.infrastructure.LotteryHeadquarters;
import lottery.infrastructure.Retailer;
import lottery.ticket.Slip;
import lottery.ticket.Ticket;
import lottery.ticket.SixNumbers;
import lottery.exceptions.BadDataException;
import lottery.exceptions.BadBehaviourException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TotolotekTest {

    private static final LotteryHeadquarters CENTRALA = LotteryHeadquarters.getInstance();
    private static final StateBudget BUDŻET = StateBudget.getInstance();

    @BeforeEach
    public void resetInstancji() {
        CENTRALA.reset();
        BUDŻET.reset();
    }

    @Test
    public void testGłówny() {
        Retailer kolektura1 = new Retailer();
        List<Retailer> l = new ArrayList<>();
        l.add(kolektura1);
        Player gracz1 = new PlayerSameSlip(PersonalInfo.generateRandomInfo(), 1_000_000_00L,
                new Slip(SixNumbers.randomList(8), 3), l, 1);
        for (int i = 0; i < 1000; i++) {
            gracz1.buyTicket();
        }
        assertEquals(1_000_000_00 - 3_00 * 1000 * 8 * 3, gracz1.balance()); // cena * kupony * bets * losowania
        assertEquals(1000 * 8 * 3 * 2_40, CENTRALA.balance());
        assertEquals(60 * 1000 * 8 * 3, BUDŻET.income()); // podatek * kupony * bets * losowania

        CENTRALA.drawNumbers();
        assertEquals(1000 * 8 * 2_40 * 51 / 100 * 8 / 100, // kupony * bets * cena * 51% * 8%
                CENTRALA.getDraw(1).gradePools()[1]); // pula II. stopnia
        CENTRALA.drawNumbers();
        CENTRALA.drawNumbers();
        assertEquals(1000 * 8 * 2_40 * 51 / 100 * 48 / 100, // kupony * bets * cena * 51% * (100% - 44% - 8%)
                CENTRALA.getDraw(3).gradePools()[2]); // pula III. stopnia

        Player gracz2 = new PlayerSameSlip(PersonalInfo.generateRandomInfo(), 2_90L,
                new Slip(SixNumbers.randomList(8), 3), l, 1);
        gracz2.buyTicket();
        assertEquals(2_90, gracz2.balance());
        assertTrue(gracz2.ownedTickets().isEmpty());

        Retailer kolektura2 = new Retailer();
        Ticket kupon = gracz1.ownedTickets().get(1);
        assertThrows(BadBehaviourException.class, () -> kolektura2.givePrize(gracz1, kupon));

        kolektura1.givePrize(gracz1, kupon);
        assertThrows(BadBehaviourException.class, () -> kolektura2.givePrize(gracz1, kupon));
    }

    @Test
    void testPoprawneLiczby() {
        SixNumbers t = new SixNumbers(Set.of(5, 21, 49, 23, 1, 44));
        assertTrue(t.areNumbersCorrect());
    }

    @Test
    void testNiepoprawneLiczbyZaMało() {
        SixNumbers t = new SixNumbers(Set.of(5, 6, 7));
        assertFalse(t.areNumbersCorrect());
    }

    @Test
    void testNiepoprawneLiczbyPozaZakresem() {
        SixNumbers t1 = new SixNumbers(Set.of(5, 12, 19, 23, 31, 100));
        assertFalse(t1.areNumbersCorrect());
        SixNumbers t2 = new SixNumbers(Set.of(0, 5, 12, 19, 23, 31, 11));
        assertFalse(t2.areNumbersCorrect());
    }

    @Test
    void testIleTrafionychLiczb() {
        SixNumbers zakład = new SixNumbers(Set.of(19, 5, 23, 12, 31, 44));
        SixNumbers losowanie = new SixNumbers(Set.of(4, 5, 19, 22, 23, 48));
        assertEquals(3, zakład.hitCount(losowanie));
    }

    @Test
    void testEqualsIHashCode() {
        SixNumbers a = new SixNumbers(Set.of(1, 2, 3, 4, 5, 6));
        SixNumbers b = new SixNumbers(Set.of(6, 5, 4, 3, 2, 1));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    private SixNumbers prawidłowyZakład(int przesunięcie) {
        return new SixNumbers(Set.of(1 + przesunięcie, 2 + przesunięcie, 3 + przesunięcie,
                4 + przesunięcie, 5 + przesunięcie, 6 + przesunięcie));
    }

    private SixNumbers nieprawidłowyZakład() {
        return new SixNumbers(Set.of(1, 2, 3));
    }

    @Test
    void testTworzeniePoprawnegoBlankietu() {
        List<SixNumbers> zakłady = List.of(
                prawidłowyZakład(0),
                prawidłowyZakład(10)
        );
        Slip blankiet = new Slip(zakłady, 5);
        assertEquals(2, blankiet.bets().size());
        assertEquals(5, blankiet.drawCount());
    }

    @Test
    void testCenaPoprawna() {
        List<SixNumbers> zakłady = List.of(
                prawidłowyZakład(0),
                prawidłowyZakład(10),
                prawidłowyZakład(20)
        );
        Slip blankiet = new Slip(zakłady, 4);
        assertEquals(3 * 4 * 3_00, blankiet.price());
    }

    @Test
    void testNieprawidłoweZakładySąOdrzucane() {
        List<SixNumbers> zakłady = List.of(
                nieprawidłowyZakład(),
                prawidłowyZakład(0),
                nieprawidłowyZakład()
        );
        Slip blankiet = new Slip(zakłady, 3);
        assertEquals(1, blankiet.bets().size());
    }

    @Test
    void testZbytWieleZakładówWyjątek() {
        List<SixNumbers> zaDużo = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            zaDużo.add(prawidłowyZakład(i));
        }

        assertThrows(BadDataException.class, () -> new Slip(zaDużo, 2));
    }

    @Test
    void testNiepoprawnaLiczbaLosowań() {
        List<SixNumbers> zakłady = List.of(prawidłowyZakład(0));

        assertThrows(BadDataException.class, () -> new Slip(zakłady, 0));
        assertThrows(BadDataException.class, () -> new Slip(zakłady, 11));
    }

    @Test
    void testBrakPrawidłowychZakładówWyjątek() {
        List<SixNumbers> zakłady = List.of(nieprawidłowyZakład(), nieprawidłowyZakład());
        assertThrows(BadDataException.class, () -> new Slip(zakłady, 2));
    }
}