package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.ThreadLogger;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Integer.MAX_VALUE; //TODO

    // our implement from here

    /**
     * true if a player have a legal set
     */
    private boolean legalSetMade = false;

    /**
     * queue of players who claim to have a set
     */
    private LinkedBlockingQueue<Integer> playersClaimSet = new LinkedBlockingQueue<>();
    private Thread dealerThread;


    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playerLock = new Object();
        featureSize = env.config.featureSize;
        this.sem = new Semaphore(1);
        cardDealing = true;
        //this.Pthreads = new ArrayList<>();

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        createPlayerThreads();
        dealerThread = Thread.currentThread(); //saving the dealer thread
        while (!shouldFinish()) { //Game end conditions met
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(true);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
//        try {
//            dealerThread.join(); // Wait for the thread to finish
//        } catch (InterruptedException e) {}
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        for (Player player : players)
            player.terminate();

        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
//                for (int card : cards){
//                    int slot = table.cardToSlot[card];
//                    table.removeCard(slot);
//                }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        cardDealing = true;
        Collections.shuffle(deck);
        int tableSize = env.config.tableSize;
        boolean missingCards = (table.countCards() < tableSize);
        for (int i = 0; deck.size() != 0 && missingCards & i < tableSize; i++) {
            Integer card = table.slotToCard[i];
            if (card == null) {
                table.placeCard(deck.remove(0), i);//fill empty table slots with cards from the deck and deleting it from the list
            }
        }
        cardDealing = false;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if (reset) { // reset the timer due to a set being found by a player or due to time run out
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
        } else { // no need to reset, only update to time that passed
            long timeLeft = reshuffleTime - System.currentTimeMillis();
            boolean warn = timeLeft < env.config.turnTimeoutWarningMillis; // time is about to run out
            env.ui.setCountdown(timeLeft, warn);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        for (Integer card : table.slotToCard) { // return each card from the table to the deck
            deck.add(card);
        }
        for (int i = 0; i < env.config.tableSize; i++) {
            table.removeCard(i);
            table.resetTokens();
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        int maxPoints = -1;
        int countWinners = 0;
        for (Player player : players) {
            if (maxPoints < player.score()) {
                maxPoints = player.score();
                countWinners = 1;
            } else if (maxPoints == player.score())
                countWinners++;
        }
        int indexArray = 0;
        int[] winners = new int[countWinners];
        for (Player player : players) {
            if (maxPoints == player.score()) {
                winners[indexArray] = player.id;
                indexArray++;
            }
        }
        env.ui.announceWinner(winners);
    }

    private void createPlayerThreads() {
        for (Player p : players) {
            ThreadLogger playerThread = new ThreadLogger(p, "player " + p.id, env.logger);
            //Pthreads.add(playerThread);
            playerThread.start();
        }
    }

    private void isLegalSetMade(int playerId) {
        for (Player p : players) {
            if (p.id == playerId) {
                int[] slots = p.getActions().stream().mapToInt(Integer::intValue).toArray(); // convert the actions queue to array of slots with token
                int[] cards = table.cardsTokenedByPlayer(slots); // convert the token slots to cards
                legalSetMade = env.util.testSet(cards); // check if the player found a legal set
                if (legalSetMade) { // the set is legal - remove cards + reward
                    dealerThread.interrupt();
                    p.point();
                } else // the set is illegal - penalize
                    p.penalty();
                break;
            }
        }
    }

    // TODO check if necessary
    public void setPlayersClaimSet(int playerId) {
        synchronized (playersClaimSet) {
            playersClaimSet.add(playerId);
            isLegalSetMade(playerId);
            //playersClaimSet.notifyAll();
        }

    }


//    public void terminatePlayersThreads(){
//        for (int i = Pthreads.size() - 1; i >= 0; i--) {
//            Thread thread = Pthreads.get(i);
//            // gracefully terminate each thread
//            try {
//                thread.join(); // wait for the player thread to finish
//            } catch (InterruptedException e) {}
//        }
//    }
}
