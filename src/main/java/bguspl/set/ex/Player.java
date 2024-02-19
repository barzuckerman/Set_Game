package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    // our implement from here

    /**
     * queue for the player actions
     */
    private LinkedBlockingQueue<Integer> incomingActions;

    /**
     * the game Dealer
     */
    private Dealer dealer;

    /**
     * queue for the player slot with token
     */
    private LinkedBlockingQueue<Integer> playerTokens;


    /**
     * a flag to see if the player's thread on freeze
     */
    private boolean isFreeze = false;
    /**
     * in order not to make magic numbers
     */
    private final int featureSize;


    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.incomingActions = new LinkedBlockingQueue<>();
        featureSize = env.config.featureSize;
        this.playerTokens = new LinkedBlockingQueue<>(featureSize);
        this.dealer = dealer;
        this.score = 0;

    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            if (!dealer.cardDealing) {
                isFreeze = false;
                if (!incomingActions.isEmpty()) { // if there is a new key pressed
                    int desiredToken = incomingActions.poll();
                    if (table.isPlacedToken(id, desiredToken)) { // remove token
                        removeToken(desiredToken);
                        table.removeToken(id, desiredToken);
                    } else if (playerTokens.size() < featureSize) { // place token
                        table.placeToken(id, desiredToken);
                        playerTokens.add(desiredToken);
                        if (playerTokens.size() == featureSize) // if the current token is the third one
                            dealer.setPlayersClaimSet(this.id);
                    }
                }
                synchronized (this) {
                    notifyAll();
                }
            } else
                isFreeze = true;
        }
        if (!human) try {
            aiThread.join();
        } catch (InterruptedException ignored) {
        }
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                Random random = new Random();
                int randomNumber = random.nextInt(env.config.tableSize);
                this.keyPressed(randomNumber);
                try {
                    synchronized (this) {
                        wait(); //TODO why the thread waits?
                    }
                } catch (InterruptedException ignored) {
                }
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement

        if (!isFreeze)
            if (table.slotToCard[slot] != null) { // check that there is a card in the desired slot
                incomingActions.add(slot);
            }


    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        long millis = env.config.pointFreezeMillis;
        isFreeze = true;
        for (long i = millis; i > 0; i = i - 1000) {
            env.ui.setFreeze(id, i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        env.ui.setFreeze(id, 0);
        isFreeze = false;
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        long millis = env.config.penaltyFreezeMillis;
        isFreeze = true;
        for (long i = millis; i > 0; i = i - 1000) {
            env.ui.setFreeze(id, i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        env.ui.setFreeze(id, 0);
        isFreeze = false;
    }

    public int score() {
        return score;
    }

    public LinkedBlockingQueue<Integer> getPlayerTokens() {
        return playerTokens;
    }

    public void resetQueue() {
        playerTokens.clear(); //clear key input queue
    }

    public void removeToken(int slot) {
        LinkedBlockingQueue<Integer> temp = new LinkedBlockingQueue<Integer>(featureSize);
        while (!playerTokens.isEmpty()) {
            Integer current = playerTokens.poll();
            if (current != slot) {
                temp.add(current);
            }
        }
        playerTokens = temp;
    }
}
