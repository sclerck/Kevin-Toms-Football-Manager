package org.sclerck.ktfm;

import org.sclerck.ktfm.model.Team;

import static org.sclerck.ktfm.FootballManager.MAX_PLAYERS;
import static org.sclerck.ktfm.FootballManager.divMult;
import static org.sclerck.ktfm.FootballManager.myTeam;
import static org.sclerck.ktfm.FootballManager.player;
import static org.sclerck.ktfm.FootballManager.statusCode;
import static org.sclerck.ktfm.FootballManager.team;
import static org.sclerck.ktfm.Utils.cls;
import static org.sclerck.ktfm.Utils.inputInt;
import static org.sclerck.ktfm.Utils.pause;
import static org.sclerck.ktfm.Utils.waitEnter;

/** Everything to do with money: balance, loans, wages, ground rent, prize money and season bonuses. */
final class Finance {
    private static final int MAX_LOAN = 250000;
    private static final int STARTING_BALANCE = 50000;

    private static int groundRent;

    private Finance() { }

    // ─── Balance and debt of the managed team ─────────────────────────────
    static int balance() {
        return team(myTeam).getBalance();
    }

    static int debt() {
        return team(myTeam).getLoan();
    }

    static void adjustFinances(int balanceChange, int debtChange) {
        team(myTeam).setFinances(balance() + balanceChange, debt() + debtChange);
    }

    // ─── Print money (subroutine 1000) ────────────────────────────────────
    static void printMoney() {
        System.out.println("  Money: £" + balance() + "   Loan: £" + debt());
    }

    /** Gives a newly chosen team its opening finances. */
    static void openAccount(Team team) {
        team.setFinances(STARTING_BALANCE, 0);
    }

    /** Resets season finances; a bankrupt club is re-funded for its division. */
    static void startSeason() {
        groundRent = 500 * divMult;
        if (balance() == 0) team(myTeam).setFinances(STARTING_BALANCE * divMult, debt());
    }

    // ─── Loans ────────────────────────────────────────────────────────────
    /** Maximum total loan for a division multiplier (5 - division). */
    static int maxLoan(int divMult) {
        return MAX_LOAN * divMult;
    }

    static int interest(int debt) {
        return debt / 100;
    }

    static boolean isDebtLimitExceeded(int debt, int divMult) {
        return debt > maxLoan(divMult);
    }

    // ─── Loan (subroutine 2800) ────────────────────────────────────────────
    static void obtainLoan(int divMult) {
        cls();
        printMoney();
        int amount = inputInt("How much do you want to borrow? £");
        cls();
        if (amount <= 0) {
            System.out.println("Enter a positive amount.");
            pause(1500); return;
        }
        int maxLoan = maxLoan(divMult);
        if (amount + debt() > maxLoan) {
            System.out.println("Maximum loan is £" + maxLoan);
            pause(1500); return;
        }
        adjustFinances(amount, amount);
        printMoney();
        waitEnter();
    }

    // ─── Pay off loan (subroutine 2900) ───────────────────────────────────
    static void payOffLoan() {
        cls();
        printMoney();
        int amount = inputInt("How much do you want to repay? £");
        cls();
        if (amount <= 0) { System.out.println("Enter a positive amount."); pause(1500); return; }
        if (amount > debt()) { System.out.println("You don't owe that much!"); pause(1500); return; }
        if (amount > balance()) { System.out.println("You haven't enough money!"); pause(1500); return; }
        adjustFinances(-amount, -amount);
        printMoney();
        waitEnter();
    }

    // ─── Wages and weekly finances (subroutine 8000) ──────────────────────
    static void payWages() {
        cls();
        int wages = 0;
        for (int i = 1; i <= MAX_PLAYERS; i++) {
            if (statusCode(i) > 0) wages += player(i).getSkill() * 100 * divMult;
        }
        int interest = interest(debt());

        System.out.println("── WEEKLY FINANCES ─────────────────");
        System.out.println("  Wage bill    : £" + wages);
        System.out.println("  Ground rent  : £" + groundRent);
        System.out.println("  Loan interest: £" + interest);

        int balance = balance() - wages - groundRent - interest;
        int debt = debt();

        System.out.println("  Weekly balance: £" + (balance - debt));

        if (balance < 0) {
            debt -= balance;
            balance = 0;
            System.out.println("  ⚠ Shortfall added to loan!");
        }
        team(myTeam).setFinances(balance, debt);
        if (isDebtLimitExceeded(debt(), divMult)) {
            System.out.println("  ❌ DEBT EXCEEDED LIMIT - GAME OVER");
            waitEnter();
            System.exit(0);
        }
        waitEnter();
    }

    static int seasonBonus(int leaguePosition) {
        return (17 - leaguePosition) * 5000 * divMult;
    }

    static void awardSeasonBonus(int leaguePosition) {
        int bonus = seasonBonus(leaguePosition);
        adjustFinances(bonus, 0);
        System.out.println("Season bonus: £" + bonus);
    }
}
