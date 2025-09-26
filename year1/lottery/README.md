# Lottery
A Java simulation of a lottery system inspired by the polish *Totolotek*.  
It models draws, tickets, players, retailers, prize distribution, and taxes.

## Features

- **Lottery Draws**
  - Cyclic draws of 6 unique numbers from 1–49.
  - Automatic calculation of prize pools and winnings for 3, 4, 5, and 6 matches.
  - Jackpot rollover (if no 6-number match occurs).

- **Lottery headquarters**
  - Manages draws, prize calculations, and finances.
  - Handles subsidies from the state budget if funds run out.
  - Publishes draw results, prize amounts, and payout statistics.

- **Tickets and slips**
  - Tickets can include multiple bets (up to 8) and cover multiple draws (up to 10).
  - Supports both manual bets (via slips) and random bets.
  - Unique ticket identifiers with a control sum.
  - Ticket printing with bet details, draw numbers, and total cost.

- **Prize Rules**
  - Ticket price: $3 per bet ($0.60 tax, $2.40 lottery pool).
  - 51% of the pool allocated for prizes, distributed among different prize levels.
  - Guaranteed minimum jackpot ($2,000,000).
  - Automatic tax handling: 10% withheld from prizes ≥ $2280.

- **Players**
  - Different player strategies implemented:
    - *Minimalist*: always buys a single random ticket for the next draw.
    - *Random*: buys a random number of random tickets.
    - *Same-numbers*: always plays the same 6 numbers for 10 consecutive draws.
    - *Same-slip*: uses a personal betting form at fixed intervals.
  - Players manage their own funds and owned tickets.

- **Lottery retailers**
  - Sell tickets and validate winnings.
  - Each outlet maintains its own record of sold tickets.
  - Winnings are paid out only in the outlet where the ticket was purchased.

- **State Budget**
  - Collects taxes from bets and high-value prizes.
  - Provides subsidies to the lottery when needed.
  - Tracks total collected taxes and subsidies paid.
