package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

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
    private Queue<Integer> incomingActions;

    /**
     * the game Dealer
     */
    private Dealer dealer;


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
        this.incomingActions = new LinkedList<>();
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
            if(incomingActions.size() == 3){ //third token placed
                dealer.notify(); //wait for point or penalty TODO
                incomingActions = new LinkedList<>(); //clear key input queue
            }

        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
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
                int randomNumber = random.nextInt(12) + 1;
                keyPressed(randomNumber);
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
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
        Queue<Integer> temp = new LinkedList<>();
        boolean newElement = true;
        while (!incomingActions.isEmpty()){
            Integer current = incomingActions.remove();
            if (current != slot)
                temp.add(current);
            else{
                table.removeToken(id, slot);
                newElement = false;
            }
        }
        incomingActions = temp;
        if (incomingActions.size() < 3 && newElement) {
            incomingActions.add(slot);
            table.placeToken(id, slot);
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
        env.ui.setFreeze(id, millis);
        try {
            Thread.sleep(millis);}
        catch (InterruptedException e) {}
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        long millis = env.config.penaltyFreezeMillis;
        env.ui.setFreeze(id, millis);
        try {
            Thread.sleep(millis);}
        catch (InterruptedException e) {}

    }

    public int score() {
        return score;
    }

    public Queue<Integer> getActions(){return incomingActions;}
}
