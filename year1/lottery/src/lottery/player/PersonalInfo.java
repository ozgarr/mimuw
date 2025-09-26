package lottery.player;

import java.util.Random;

public record PersonalInfo(String name, String surname, long idNumber) {

    private static final Random RAND = new Random();

    public static PersonalInfo generateRandomInfo() {
        return new PersonalInfo(generateName(), generateSurname(), generateIDNumber());
    }

    private static String generateName() {
        String[] names = {
                "Adam", "Antoni", "Andrzej", "Bartosz", "Błażej", "Damian",
                "Daniel", "Dominik", "Grzegorz", "Hubert", "Jakub",
                "Jan", "Kacper", "Karol", "Krzysztof", "Łukasz", "Maciej",
                "Marek", "Marcin", "Michał", "Paweł", "Piotr", "Rafał",
                "Sebastian", "Szymon", "Tomasz", "Oskar", "Wiktor", "Agnieszka",
                "Alicja", "Anna", "Barbara", "Beata", "Celina", "Elżbieta",
                "Emilia", "Ewa", "Grażyna", "Iga", "Izabela", "Joanna",
                "Julia", "Justyna", "Katarzyna", "Kinga", "Małgorzata",
                "Maria", "Marta", "Mila", "Monika", "Natalia", "Paulina",
                "Sylwia", "Zuzanna"
        };
        int index = RAND.nextInt(names.length);
        return names[index];
    }

    private static String generateSurname() {
        String[] surnames = {
                "Nowak", "Mazur", "Kaczmarek", "Kubiak", "Pawlak", "Król", "Dudek", "Lis", "Wójcik",
                "Bingo", "Baran", "Gajda", "Urban", "Wilk", "Kołodziejczyk", "Musiał", "Gnat", "Sikora",
                "Zięba", "Żak", "Bednarz", "Brożek", "Czajka", "Guszczin", "Wałęsa", "Bóbr", "Rataj",
                "Narutowicz", "Rydz", "Raczkiewicz", "Sabbat", "Bierut", "Lange", "Ochab", "Borusewicz",
                "Schetyna", "Marczuk", "Jongo", "BONGO"
        };
        int index = RAND.nextInt(surnames.length);
        return surnames[index];
    }

    private static long generateIDNumber() {
        long idNumber = 0;
        long year = RAND.nextLong(100);
        long month = RAND.nextLong(12) + 1;
        if (year <= 6) {
            month += RAND.nextLong(2) * 20;
        }
        long day = RAND.nextLong(daysInMonth(month)) + 1;

        idNumber += year * 1_000_000_000;
        idNumber += month * 1_000_000_0;
        idNumber += day * 1_000_00;
        idNumber += RAND.nextLong(10000) * 10;
        idNumber += controlDigit(idNumber);

        return idNumber;
    }

    private static long daysInMonth(long month) {
        if (month == 2) {
            return 28;
        } else if ((month < 8 && month % 2 == 0) || (month >= 8 && month % 2 == 1)) {
            return 30;
        } else {
            return 31;
        }
    }

    private static long controlDigit(long idNumber) {
        long controlDigit = 0;
        long weight = 1;
        for (int i = 0; i < 10; i++) {
            controlDigit += weight * ((idNumber / powerOfTen(10 - i)) % 10);
            if (weight == 3) weight += 2;
            weight = (weight + 2) % 10;  // Weights: 1-3-7-9-1-3-7-9-1-3
        }
        return (10 - (controlDigit % 10)) % 10;
    }

    private static long powerOfTen(long exponent) {
        long base = 10;
        long result = 1;
        while (exponent > 0) {
            if (exponent % 2 == 1) {
                result *= base;
            }
            base *= base;
            exponent /= 2;
        }
        return result;
    }

    @Override
    public String toString() {
        return name + " " + surname + "\nID: " + String.format("%011d", idNumber);
    }
}