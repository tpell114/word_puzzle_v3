import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

public class Client extends UnicastRemoteObject implements ClientCallbackInterface {

    private CrissCrossPuzzleInterface server;
    private AccountServiceInterface accountService;
    private ScoreboardInterface scoreboard;
    private String username;
    private Integer gameID;
    Boolean myTurn;
    Boolean gameStartFlag;
    private volatile Boolean gameOverFlag;
    private Boolean supressHeartbeat;
    private Integer currentSequence;


    public Client() throws RemoteException {
        super();
        gameID = -1;
        supressHeartbeat = false;
    }

    public static void main(String[] args) throws RemoteException {
     
        Client client = new Client();
        Boolean exitFlag = false;
        String option;

        try {
            client.server = (CrissCrossPuzzleInterface)Naming.lookup("rmi://localhost:1099/Server");
            client.accountService = (AccountServiceInterface)Naming.lookup("rmi://localhost:1099/AccountService");
            client.scoreboard = (ScoreboardInterface)Naming.lookup("rmi://localhost:1099/ScoreboardService");

            client.userSignIn();

            while(!exitFlag) {
                System.out.println(Constants.MAIN_MENU_MESSAGE);
                option = System.console().readLine();
    
               switch (option) {
                    case "1":
                        System.out.println("\nStarting a new game...");
                        client.startGame();
                        break;

                    case "2":
                        System.out.println("\nJoining a game...");
                        client.joinGame();
                        break;
                
                    case "3":
                        System.out.println("\nViewing statistics...");
                        client.viewStats();
                        break;

                    case "4":
                        System.out.println("\nViewing leaderboard...");
                        client.viewScoreboard();
                        break;
    
                    case "5":
                        System.out.println("\nModifying word repository...");
                        client.modifyWordRepo();
                        break;
    
                    case "6":
                        System.out.println("\nGoodbye!");
                        client.handleExit();
                        exitFlag = true;
                        break;
                }
            }

        } catch (Exception e) {
            client.handleExit();
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
    }


    /**
     * Prompts the user to sign in and register them with the account service if
     * they haven't already been registered.
     *
     * @throws RemoteException if an error occurs when calling the account service
     */
    private void userSignIn() {

        try {
            System.out.println(Constants.USER_SIGN_IN_MESSAGE);
            this.username = System.console().readLine();

            if(accountService.registerUser(username)){
                System.out.println("\nWelcome, " + username + "!");
            } else {
                System.out.println("\nWelcome back, " + username + "!");
            }
            
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a new game by prompting the user for the number of words in the
     * puzzle and the failed attempt factor, and then waits for other players to
     * join or for the user to press a key to start the game.
     *
     * @throws RemoteException if an error occurs when calling the server
     */
    private void startGame() {

        gameStartFlag = false;

        System.out.println("\nHow many words would you like in the puzzle? (Enter a number between 2 and 5)");
        String numWords = System.console().readLine();

        while (!numWords.matches("[2-5]")) {
            System.out.println("Invalid input.");
            System.out.println("\nHow many words would you like in the puzzle? (Enter a number between 2 and 5)");
            numWords = System.console().readLine();
        }

        System.out.println("\nEnter a failed attempt factor (Enter a number between 1 and 5)");
        String failedAttemptFactor = System.console().readLine();

        while (!failedAttemptFactor.matches("[1-5]")) {
            System.out.println("Invalid input.");
            System.out.println("\nEnter a failed attempt factor (Enter a number between 1 and 5)");
            failedAttemptFactor = System.console().readLine();
        }

        try {
            gameID = server.startGame(this.username, this, Integer.valueOf(numWords), Integer.valueOf(failedAttemptFactor));
            System.out.println("\nStarted game with ID: " + gameID + "\n" + Constants.GAME_START_MESSAGE);

            while (true) {
                if (System.console().readLine().equals("~")) return; else break;
            }

            server.issueStartSignal(gameID);
            System.out.println("It's your turn!\n");
            printPuzzle(server.getInitialPuzzle(gameID));
            System.out.println("Counter: " + server.getGuessCounter(gameID) + "\nWord guessed: 0");
            this.currentSequence = server.getSequence(username, gameID);
            System.out.println("[Test] Current sequence: " + this.currentSequence);
            myTurn = true;
            playGame();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows the user to join an existing game by entering a valid game ID.
     * If the game ID is valid, the player is added to the game and waits for the game to start.
     * If the entered game ID is 0, the user is returned to the main menu.
     * Upon successful joining, the initial puzzle state is displayed and the player is notified
     * to wait for their turn.
     *
     * @throws Exception if an error occurs during joining, game startup, or communication with the server
     */
    private void joinGame() {

        try {
            System.out.println("\nEnter the ID of the game you would like to join. Or enter 0 to return to the main menu: ");
            this.gameID = Integer.valueOf(System.console().readLine());

            while(!server.joinGame(gameID, this.username, this)){

                if(this.gameID == 0){
                    return;
                }

                System.out.println("Invalid game ID.");
                System.out.println("\nEnter the ID of the game you would like to join. Or enter 0 to return to the main menu: ");
                this.gameID = Integer.valueOf(System.console().readLine());
            }

            System.out.println("You have joined game ID: " + gameID 
                            + "\nPlease wait for the game to start...");

            synchronized (this){wait();}

            printPuzzle(server.getInitialPuzzle(gameID));
            System.out.println("Counter: " + server.getGuessCounter(gameID) 
                            + "\nWord guessed: 0" 
                            + "\nPlease wait for your turn.");
            myTurn = false;
            playGame();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called by the server when the game is ready to start. This method is
     * synchronized because it is called from a separate thread, and notifies
     * the waiting thread that the game has started.
     * 
     * @throws RemoteException if an error occurs during communication with the
     *         server
     */
    public synchronized void onGameStart() throws RemoteException {
        System.out.println("\nGame has started!");
        notifyAll();
    }

    /**
     * Called by the server when a new player joins the game. This method notifies
     * the player of the new player and the updated total number of players.
     *
     * @param player the name of the player who joined
     * @param numPlayers the updated number of players in the game
     * @throws RemoteException if an error occurs during communication with the 
     *         server
     */
    @Override
    public void onPlayerJoin(String player, Integer numPlayers) throws RemoteException {
        System.out.println("\nPlayer: " + player + " has joined the game. Total players: " + numPlayers);
    }

    /**
     * Called by the server when a player quits the game. This method notifies
     * the player of the player who quit and the updated total number of players.
     * 
     * @param player the name of the player who quit
     * @param numPlayers the updated number of players in the game
     * @throws RemoteException if an error occurs during communication with the
     *         server
     */
    public void onPlayerQuit(String player, Integer numPlayers) throws RemoteException {
        System.out.println("\nPlayer: " + player + " has quit the game. Players remaining: " + numPlayers);
    }

    /**
     * This method initiates and manages the main game loop for the client.
     * It starts a separate thread to listen for the user input to quit the game.
     * The game runs in a loop until a game-over condition is met.
     * 
     * The client takes turns guessing words or letters, and interacts with the server
     * to check word existence and submit guesses. If the client inputs a '~',
     * it indicates quitting the game.
     * 
     * The method handles synchronization to wait for the player's turn
     * and manages game state flags to control the game flow.
     */
    private void playGame() {

        gameOverFlag = false;
        supressHeartbeat = false;

        Thread heartbeatThread = new Thread(() -> {

            while (!gameOverFlag) {

                try {

                    if (username != null && !supressHeartbeat) {
                        server.playerHeartbeat(gameID, username);
                    }

                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    System.out.println("Failed to send heartbeat.");
                }
            }
        });

        heartbeatThread.start();
        
        Thread monitorThread = new Thread(() -> {

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            try {
                while (!gameOverFlag) {

                    if (reader.ready()) {

                        String input = reader.readLine();

                        if (input.equals("~")) {

                            System.out.println("Left game ID: " + gameID);
                            gameOverFlag = true;
                            myTurn = false;
                            server.playerQuit(gameID, this.username);
                            gameID = -1;
                            synchronized (this) {notifyAll();}
                            break;

                        } else if (input.equals("\\")) {

                            supressHeartbeat = !supressHeartbeat;
                            System.out.println("[Test] Heartbeat " + (supressHeartbeat ? "suppressed" : "resumed"));
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        monitorThread.start();
        
        try {
            while(!gameOverFlag){

                if(myTurn) {

                    String guess = getValidGuess();

                    while (!guess.equals("~") && !guess.equals("\\")){

                        if (guess.charAt(0) == '?'){

                            if(server.checkWord(guess.substring(1))){
                                System.out.println("\nWord '" + guess.substring(1) + "' exists in the word repository.");
                            } else {
                                System.out.println("\nWord '" + guess.substring(1) + "' does not exist in the word repository.");
                            }

                            guess = getValidGuess();

                        } else {
                            
                            myTurn = false;

                            currentSequence = server.getSequence(username, gameID);
                            
                            //System.out.println("[Test] Current sequence: " + currentSequence);

                            // 50% chance of repeating the same sequence number
                            if (Math.random() < 0.5) {
                                System.out.println("[Test] Simulating duplicate request with sequence: " + currentSequence);
                            } else {
                                currentSequence++;
                            }

                            Boolean accepted = server.playerGuess(this.username, gameID, guess, currentSequence);

                            if (!accepted) {
                                System.out.println("[Test] Server ignored duplicate guess. Trying again...");
                                myTurn = true;
                                continue;
                            }
                            
                            break;
                        }
                    }
                    
                    if (guess.equals("~")){
                        gameOverFlag = true;
                        myTurn = false;  //maybe?
                        server.playerQuit(gameID, this.username);
                        gameID = -1;
                        return;
                    } else if (guess.equals("\\")){
                        supressHeartbeat = !supressHeartbeat;
                        System.out.println("[Test] Heartbeat " + (supressHeartbeat ? "suppressed" : "resumed"));
                    }
                }

                if(!gameOverFlag && !myTurn) synchronized (this){wait();}  
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                monitorThread.join(2000);
                heartbeatThread.join(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Called by the server when it's the client's turn. This method notifies
     * the client of the updated game state and enables the client to make a
     * guess. The client is notified of the updated puzzle state, the number
     * of guesses left, and the number of words guessed by all players.
     * 
     * @param puzzle the current state of the puzzle
     * @param guessCounter the number of guesses left
     * @param wordCounter the number of words guessed by all players
     * @throws RemoteException if an error occurs during communication with the
     *         server
     */
    @Override
    public synchronized void onYourTurn(char[][] puzzle, Integer guessCounter, Integer wordCounter) throws RemoteException {
        System.out.println("\nIt's your turn!\n");
        printPuzzle(puzzle);
        System.out.println("Counter: " + guessCounter
                        + "\nWord guessed: " + wordCounter);
        System.out.println("[Test] Current sequence: " + this.currentSequence);
        myTurn = true;
        notifyAll();
    }

    /**
     * Called by the server when it is the opponent's turn. This method notifies
     * the client of the updated game state and informs them to wait for their turn.
     * The client is updated with the current puzzle state, the number of guesses
     * left, and the number of words guessed by all players.
     *
     * @param puzzle the current state of the puzzle
     * @param guessCounter the number of guesses left
     * @param wordCounter the number of words guessed by all players
     * @throws RemoteException if an error occurs during communication with the
     *         server
     */
    @Override
    public synchronized void onOpponentTurn(char[][] puzzle, Integer guessCounter, Integer wordCounter) throws RemoteException {
        System.out.println("\nIt's your opponent's turn!\n");
        printPuzzle(puzzle);
        System.out.println("Counter: " + guessCounter
                        + "\nWord guessed: " + wordCounter
                        + "\nPlease wait for your turn. (enter ~ to quit)");
        myTurn = false;
    }

    /**
     * Called by the server when the game has been won. This method
     * notifies the client of the final game state and the final scores
     * of all players. The client is informed that the game is over and
     * the final puzzle state is displayed. The client is also notified
     * of the final scores of all players.
     *
     * @param puzzle the final state of the puzzle
     * @param guessCounter the number of guesses left
     * @param wordCounter the number of words guessed by all players
     * @param scores the final scores of all players
     * @throws RemoteException if an error occurs during communication with the
     *         server
     */
    @Override
    public synchronized void onGameWin(char[][] puzzle, Integer guessCounter, Integer wordCounter, Map<String, Integer> scores) throws RemoteException {

        gameOverFlag = true;
        gameID = -1;

        System.out.println("\nPuzzle completed!");
        printPuzzle(puzzle);
        System.out.println("Counter: " + guessCounter
                        + "\nWord guessed: " + wordCounter
                        + "\n\nFinal scores:\n");

        for (String player : scores.keySet()) System.out.println(player + ": " + scores.get(player));
        notifyAll();
    }

    /**
     * Called by the server when the game has been lost. This method
     * notifies the client of the final game state and the final scores
     * of all players. The client is informed that the game is over and
     * the final puzzle state is displayed. The client is also notified
     * of the final scores of all players.
     *
     * @param puzzle the final state of the puzzle
     * @param guessCounter the number of guesses left
     * @param wordCounter the number of words guessed by all players
     * @param scores the final scores of all players
     * @throws RemoteException if an error occurs during communication with the
     *         server
     */
    public void onGameLoss(char[][] puzzle, Integer guessCounter, Integer wordCounter , Map<String, Integer> scores) throws RemoteException {
        
        gameOverFlag = true;
        gameID = -1;

        printPuzzle(puzzle);
        System.out.println("Counter: " + guessCounter
                        + "\nWord guessed: " + wordCounter
                        + "\n\nGame lost. Final scores:\n");

        for (String player : scores.keySet()) System.out.println(player + ": " + scores.get(player));
        notifyAll();
    }

    /**
     * Prints the given puzzle to the console.
     *
     * @param puzzle the 2D array to print
     */
    private void printPuzzle(char[][] puzzle) {

        for (int i = 0; i < puzzle.length; i++) {
            for (int j = 0; j < puzzle[i].length; j++) {
                System.out.print(puzzle[i][j]);
            }
            System.out.println();
        }
    }

    /**
     * Prompts the user for a letter guess and ensures it is valid. Valid guesses
     * are strings of letters, the string "?", or the string "~". If the user
     * enters an invalid guess, this method will repeatedly prompt the user until
     * a valid guess is entered.
     * 
     * @return the user's valid guess
     */
    private String getValidGuess(){

        System.out.println(Constants.GUESS_MESSAGE);
        String guess = System.console().readLine().toLowerCase().trim();

        while ((!guess.matches("^[a-zA-Z?~\\\\]*$") || guess.equals(""))){
            System.out.println("Invalid input."
                            + "\n" + Constants.GUESS_MESSAGE);
            guess = System.console().readLine().toLowerCase().trim();
        }

        return guess;
    }

    /**
     * Modifies the word repository by repeatedly prompting the user for a valid
     * command and executing it. The user is prompted for a command until they
     * enter "~" to quit. The commands are as follows:
     * 
     * +word: Adds the given word to the word repository
     * -word: Removes the given word from the word repository
     * ?word: Checks if the given word exists in the word repository
     * 
     * This method will continue to prompt the user for a command until they
     * enter "~" to quit.
     */
    private void modifyWordRepo(){

        try {
            System.out.println(Constants.WORD_REPO_MESSAGE);
            String input = System.console().readLine();

            while (!input.equals("~") && (input.isEmpty() || !input.matches("^[+-?][a-zA-Z]*$"))) {
                System.out.println("Invalid input."
                                + "\n" + Constants.WORD_REPO_MESSAGE);
                input = System.console().readLine();
            }

            while (!input.equals("~")) {

                if (input.charAt(0) == '+') {

                    if (server.addWord(input.substring(1))){
                        System.out.println("\nSuccessfully added word '" + input.substring(1) + "' to the word repository.");
                    } else {
                        System.out.println("\nFailed to add word '" + input.substring(1) + "' to the word repository, it may already exist.");
                    }

                } else if (input.charAt(0) == '-') {
                    
                    if (server.removeWord(input.substring(1))){
                        System.out.println("\nSuccessfully removed word '" + input.substring(1) + "' from the word repository.");
                    } else {
                        System.out.println("\nFailed to remove word '" + input.substring(1) + "' from the word repository, it may not exist.");
                    }
                    
                } else if (input.charAt(0) == '?') {

                    if (server.checkWord(input.substring(1))){
                        System.out.println("\nWord '" + input.substring(1) + "' exists in the word repository.");
                    } else {
                        System.out.println("\nWord '" + input.substring(1) + "' does not exist in the word repository.");
                    }
                }

                System.out.println(Constants.WORD_REPO_MESSAGE);
                input = System.console().readLine();

                while (!input.equals("~") && (input.isEmpty() || !input.matches("^[+-?][a-zA-Z]*$"))) {
                    System.out.println("Invalid input."
                                    + "\n" + Constants.WORD_REPO_MESSAGE);
                    input = System.console().readLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays the user's current score.
     * 
     * @throws RemoteException if an error occurs when calling the account service
     */
    private void viewStats(){

        try {
            System.out.println("Your current score is: " + accountService.getUserScore(username));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays up to the top 5 scores from the scoreboard.
     * 
     * @throws RemoteException if an error occurs when calling the scoreboard
     */
    private void viewScoreboard(){

        try {
            List<Map.Entry<String, Integer>> topN = scoreboard.getScores(5);

            if(topN.isEmpty()){
                System.out.println("Scoreboard empty");
            } else {

                System.out.println("\nScoreboard (top " + topN.size() + "):\n");

                for (Map.Entry<String, Integer> entry : topN) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the client's disconnection. If the user was in an active game, it
     * calls playerQuit() to notify the server that the user has quit. Finally,
     * it calls unexportObject() to break the RMI connection.
     * 
     * @throws Exception if an error occurs while disconnecting
     */
    private void handleExit(){

        try {
            if(gameID != -1){
                server.playerQuit(gameID, username);
            }

            UnicastRemoteObject.unexportObject(this, true);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPlayerTimeout(String player, Boolean suspected, Boolean failed) throws RemoteException {

        if(suspected){
            System.out.println("Opponent " + player + " has timed out. (suspected: " + suspected + ", failed: " + failed + ")");
        
        } else if(failed){
            System.out.println("Opponent " + player + " has timed out. (suspected: " + suspected + ", failed: " + failed + ")");
        }

    }

    
}
