import java.rmi.*;
import java.rmi.server.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends UnicastRemoteObject implements CrissCrossPuzzleInterface{

    ConcurrentHashMap<Integer, PuzzleObject> gamesMap = new ConcurrentHashMap<>();
    WordRepositoryInterface wordRepo;
    AccountServiceInterface accountService;

    protected Server() throws RemoteException {
        super();
        try {
            wordRepo = (WordRepositoryInterface) Naming.lookup("rmi://localhost/WordRepository");
            accountService = (AccountServiceInterface) Naming.lookup("rmi://localhost:1099/AccountService");
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(() -> heartbeatListener()).start();
    }

    public static void main(String[] args) {

        try {
            Server server = new Server();
            System.out.println("The game server is running...");
            Naming.rebind("rmi://localhost:1099/Server", server);
            System.out.println("Server is registered with the RMI registry with URL: rmi://localhost:1099/Server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a new game by generating a random gameID and creating a new PuzzleObject.
     * The method waits for 5 seconds if the generated gameID is already taken.
     * If the server is full after 5 seconds of waiting, it throws a RemoteException.
     * 
     * @param username the username of the player who requests to start a game
     * @param client the callback interface of the client who requests to start a game
     * @param numWords the number of words in the puzzle
     * @param difficultyFactor the difficulty factor of the puzzle
     * @return the gameID of the newly started game
     * @throws RemoteException if the server is full
     */
    public Integer startGame(String username, ClientCallbackInterface client, Integer numWords, Integer difficultyFactor) throws RemoteException {

        Random random = new Random();
        Integer gameID;
        long startTime = System.currentTimeMillis();
        long timeout = 5000;
    
        while (System.currentTimeMillis() - startTime < timeout) {

            gameID = random.nextInt(99) + 1;

            if (!gamesMap.containsKey(gameID)) {
                gamesMap.put(gameID, new PuzzleObject(username, client, gameID, numWords, difficultyFactor));
                System.out.println("Starting a new game -> ID: " + gameID + 
                                   ", Number of words: " + numWords + 
                                   ", Difficulty factor: " + difficultyFactor);
                return gameID;
            }  
        }
        throw new RemoteException("Server is full. Please try again later.");
    }
    

    /**
     * Allows a player to join an existing game by specifying a valid game ID.
     * If the game ID is valid, the player is added to the game and notified
     * of the total number of players in the game.
     * If the game ID is invalid, the method returns false.
     * If the player is already in the game, the method returns false.
     * If the player is successfully added to the game, the method returns true.
     * 
     * @param gameID the ID of the game to join
     * @param username the username of the player who requests to join the game
     * @param client the callback interface of the client who requests to join the game
     * @return true if the player is successfully added to the game, false otherwise
     * @throws RemoteException if an error occurs during communication with the
     *         server
     */
    public Boolean joinGame(Integer gameID, String username, ClientCallbackInterface client) throws RemoteException {

        if(gamesMap.containsKey(gameID)){

            PuzzleObject game = gamesMap.get(gameID);

            if (game.getAllPlayers().containsKey(username)) {
                return false;
            }

            game.addPlayer(username, client);
            System.out.println("Added player: " + username + " to game ID: " + gameID);

            Map<String, ClientCallbackInterface> allPlayers = game.getAllPlayers();

            for (String player : allPlayers.keySet()) {
                allPlayers.get(player).onPlayerJoin(username, allPlayers.size());
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Notifies all players in a specified game that the game has started.
     * This method retrieves all players associated with the given game ID
     * and calls the onGameStart callback for each player, indicating that
     * the game is now in progress.
     *
     * @param gameID the ID of the game to start
     * @throws RemoteException if an error occurs during remote communication
     */
    public void issueStartSignal(Integer gameID) throws RemoteException {

        try {
            Map<String, ClientCallbackInterface> allPlayers = gamesMap.get(gameID).getAllPlayers();

            for (String player : allPlayers.keySet()) {
                allPlayers.get(player).onGameStart();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the initial puzzle state of the game associated with the given game ID.
     * This method is called by the client after a game is started or joined.
     * The method returns a 2D char array representing the puzzle state and
     * does not modify the state of the game.
     * @param gameID the ID of the game to retrieve the initial puzzle state from
     * @return a 2D char array representing the initial puzzle state of the game
     * @throws RemoteException if an error occurs during remote communication
     */
    public char[][] getInitialPuzzle(Integer gameID) throws RemoteException {
        return gamesMap.get(gameID).getPuzzleSlaveCopy();
    }

    /**
     * Returns the number of guesses left for the game associated with the given game ID.
     * This method is called by the client to retrieve the number of guesses left for the game.
     * The method returns the number of guesses left and does not modify the game state.
     *
     * @param gameID the ID of the game to retrieve the number of guesses left from
     * @return the number of guesses left for the game
     * @throws RemoteException if an error occurs during remote communication
     */
    public Integer getGuessCounter(Integer gameID) throws RemoteException {
        return gamesMap.get(gameID).getGuessCounter();
    }
    
    /**
     * Handles a guess made by a player in a game. The guess can be a single character
     * or a word. If the guess is a character, the server checks if the character is
     * in the puzzle. If the character is in the puzzle, the server updates the puzzle
     * and checks if the game is won. If the character is not in the puzzle, the server
     * decrements the guess counter and checks if the game is lost. If the guess is a
     * word, the server checks if the word is in the puzzle. If the word is in the
     * puzzle, the server updates the puzzle and checks if the game is won. If the
     * word is not in the puzzle, the server decrements the guess counter and checks
     * if the game is lost.
     * 
     * @param username the username of the player who made the guess
     * @param gameID the ID of the game in which the guess was made
     * @param guess the guess made by the player
     * @throws RemoteException if an error occurs during remote communication
     */
    public Boolean playerGuess(String username, Integer gameID, String guess, Integer sequence) throws RemoteException {

        PuzzleObject game = gamesMap.get(gameID);
        Integer lastSequence = game.getPlayerSequence(username);
        String trimmedGuess = guess.trim();
        Boolean solvedFlag;
        
        if (sequence <= lastSequence) {
            System.out.println("Duplicate or out-of-order request from " + username + " (seq: " + sequence + ", last: " + lastSequence + ")");
            return false;
        }

        game.updatePlayerSequence(username, sequence);

        System.out.println("Received guess: " + guess + " for game ID: " + gameID);
        
        try {
            if (trimmedGuess.length() == 1){    //player guessed a character

                solvedFlag = game.guessChar(username, trimmedGuess.charAt(0));

                if (!solvedFlag) {
                    if (game.getGuessCounter() == 0) {
                        handleGameLoss(game, gameID);
                    } else {
                        handleGameRunning(game);
                    }
                } else {
                    System.out.println("Starting game win sequence...");
                    handleGameWin(game, gameID);
                }
            } else {    //player guessed a word

                solvedFlag = game.guessWord(username, trimmedGuess);

                if (!solvedFlag) {
                    if (game.getGuessCounter() == 0) {
                        handleGameLoss(game, gameID);
                    } else {
                        handleGameRunning(game);
                    }
                } else {
                    System.out.println("Starting game win sequence...");
                    handleGameWin(game, gameID);
                }
            }

        } catch (Exception e) {
            System.out.println("Error issuing callback: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Handles the game running state. Issues callbacks to players to update
     * their game state and notify them if it's their turn.
     * 
     * @param game the current PuzzleObject
     */
    private void handleGameRunning(PuzzleObject game){

        ClientCallbackInterface callbackCurrentPlayer = game.getActivePlayerCallback();
        String currentPlayer = game.getActivePlayer();
        game.incrementActivePlayer();
        String nextPlayer = game.getActivePlayer();

        try {
            if (currentPlayer.equals(nextPlayer)) {

                callbackCurrentPlayer.onYourTurn(game.getPuzzleSlaveCopy(), game.getGuessCounter(), game.getWordsGuessed(currentPlayer));
                System.out.println("Single Player -> Issued callback to player: " + game.getActivePlayer());
            
            } else {

                ClientCallbackInterface callbackNextPlayer;
                callbackNextPlayer = game.getActivePlayerCallback();
                callbackNextPlayer.onYourTurn(game.getPuzzleSlaveCopy(), game.getGuessCounter(), game.getWordsGuessed(nextPlayer));
                System.out.println("Multiplayer -> Issued callback to player: " + game.getActivePlayer());
                Map<String, ClientCallbackInterface> allPlayers = game.getAllPlayers();

                for (String player : allPlayers.keySet()) {

                    if(!player.equals(nextPlayer)){
                        allPlayers.get(player).onOpponentTurn(game.getPuzzleSlaveCopy(), game.getGuessCounter(), game.getWordsGuessed(player));
                        System.out.println("Multiplayer -> Issued callback to player: " + player);
                    }
                }
            } 
        } catch (Exception e) {
            System.out.println("Error issuing callback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the game win state by updating player scores and notifying all players
     * of the game win. If there is a single top player, they receive 2 points; otherwise,
     * all top players receive 1 point each. The method issues a callback to each player
     * with the final puzzle state, guess counter, word count, and scores. Finally, it
     * removes the game from the active games map.
     *
     * @param game the PuzzleObject representing the current game
     * @param gameID the ID of the game that was won
     */
    private void handleGameWin(PuzzleObject game, Integer gameID){

        try {
            List<String> topPlayers = game.getHighestScoredPlayers();

            if (topPlayers.size() == 1){

                accountService.updateUserScore(topPlayers.get(0), 2);
                System.out.println("Added 2 points to player: " + topPlayers.get(0));
            
            } else {
                
                for (String player : topPlayers) {
                    accountService.updateUserScore(player, 1);
                    System.out.println("Added 1 point to player: " + player);
                }
            }

            Map<String, ClientCallbackInterface> players = game.getAllPlayers();

            for (String player : players.keySet()) {
                players.get(player).onGameWin(game.getPuzzleSlaveCopy(), game.getGuessCounter(), game.getWordsGuessed(player), game.getAllScores());
            }

            System.out.println("Removed game ID: " + gameID);
            gamesMap.remove(gameID);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the game loss state by notifying all players of the game loss and
     * removing the game from the active games map. The method issues a callback to
     * each player with the final puzzle state, guess counter, word count, and scores.
     * 
     * @param game the PuzzleObject representing the current game
     * @param gameID the ID of the game that was lost
     */
    private void handleGameLoss(PuzzleObject game, Integer gameID){

        try {
            Map<String, ClientCallbackInterface> players = game.getAllPlayers();

            for (String player : players.keySet()) {
                players.get(player).onGameLoss(game.getPuzzleSlaveCopy(), game.getGuessCounter(), game.getWordsGuessed(player), game.getAllScores());
            }

            System.out.println("Removed game ID: " + gameID);
            gamesMap.remove(gameID);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Handles the player quitting by removing the player from the game and
     * notifying all other players of the player who quit. If the player who quit
     * was the active player, the next player is notified to start their turn.
     * If the game has no players left, it is removed from the active games map.
     * 
     * @param gameID the ID of the game the player is quitting
     * @param username the username of the player quitting
     * @throws RemoteException if an error occurs during communication with the
     *         client
     */
    public void playerQuit(Integer gameID, String username) throws RemoteException {

        PuzzleObject game = gamesMap.get(gameID);

        if(!gamesMap.get(gameID).removePlayer(username)){

            System.out.println("Removed player: " + username + " from game ID: " + gameID);

            if (username.equals(game.getActivePlayer())){

                game.incrementActivePlayer();
                ClientCallbackInterface nextPlayerCallback = game.getActivePlayerCallback();
                nextPlayerCallback.onYourTurn(game.getPuzzleSlaveCopy(), game.getGuessCounter(), game.getWordsGuessed(game.getActivePlayer()));
            }

            Map<String, ClientCallbackInterface> allPlayers = game.getAllPlayers();

            for (String player : allPlayers.keySet()) {
                allPlayers.get(player).onPlayerQuit(username, allPlayers.size());
            }

        } else {
            System.out.println("Removed player: " + username + " from game ID: " + gameID);
            System.out.println("No more players in game ID: " + gameID + ", removing game...");
            gamesMap.remove(gameID);
        }
    }

    /**
     * Adds a word to the word repository.
     * 
     * This method delegates the task of adding a word to the underlying 
     * WordRepository instance. The word is added if it does not already 
     * exist in the repository. The repository is then sorted.
     *
     * @param word the word to be added to the repository
     * @return true if the word was successfully added, false if it already exists
     * @throws RemoteException if a remote communication error occurs
     */
    public Boolean addWord(String word) throws RemoteException {
        return wordRepo.addWord(word);
    }

    /**
     * Removes a word from the word repository.
     * 
     * This method delegates the task of removing a word to the underlying 
     * WordRepository instance. The word is removed if it exists in the repository.
     * 
     * @param word the word to be removed from the repository
     * @return true if the word was successfully removed, false if it did not exist
     * @throws RemoteException if a remote communication error occurs
     */
    public Boolean removeWord(String word) throws RemoteException {
        return wordRepo.removeWord(word);
    }

    /**
     * Checks if a word exists in the word repository.
     * 
     * This method delegates the task of checking if a word exists to the underlying
     * WordRepository instance. The method returns true if the word exists in the
     * repository and false otherwise.
     * 
     * @param word the word to be checked for in the repository
     * @return true if the word exists in the repository, false if it does not exist
     * @throws RemoteException if a remote communication error occurs
     */
    public Boolean checkWord(String word) throws RemoteException {
        return wordRepo.checkWord(word);
    }

    @Override
    public void playerHeartbeat(Integer gameID, String username) throws RemoteException {

        //System.out.println("Heartbeat from " + username + " in game " + gameID);

        if (gamesMap.containsKey(gameID)) {
            gamesMap.get(gameID).updateHeartbeat(username);
        }

    }

    private void heartbeatListener() {

        long TOLERANCE_MS = 1000; // in milliseconds
        long TOLERANCE_NS = TOLERANCE_MS * 1000000; // in nanoseconds
        int FAILURE_THRESHOLD = 3;

        while (true) {

            try {
                Thread.sleep(TOLERANCE_MS);
    
                long now = System.nanoTime();
    
                for (PuzzleObject game : gamesMap.values()) {
    
                    for (String player : game.getAllPlayers().keySet()) {

                        Long lastHeartbeat = game.getPlayerHeartbeat(player);

                        if (lastHeartbeat == null) continue;

                        Long elapsed = now - lastHeartbeat;
                        Integer gameID = getKeyByValue(gamesMap, game);
                        String status = game.getPlayerStatus(player);

                        if (elapsed < TOLERANCE_NS) {
                            //active
                            if(gameID != null){

                                if(!status.equals("active")){
                                    gamesMap.get(gameID).updatePlayerStatus(player, "active");
                                    System.out.println("Player " + player + " is now active again in game " + gameID);
                                }
                
                            } else {
                                System.out.println("Failed to find game ID for active player: " + player);
                            }

                        } else if (elapsed > TOLERANCE_NS * FAILURE_THRESHOLD) {
                            //failed
                            if(gameID != null){
                                
                                if(!status.equals("failed")){
                                    gamesMap.get(gameID).updatePlayerStatus(player, "failed");
                                    playerTimeout(gameID, player, false, true);
                                }
                                
                            } else {
                                System.out.println("Failed to find game ID for suspected timed-out player: " + player);
                            }

                        } else {
                            //suspected
                            if(gameID != null){

                                if(!status.equals("suspected")){
                                    gamesMap.get(gameID).updatePlayerStatus(player, "suspected");
                                    playerTimeout(gameID, player, true, false);
                                }
                                
                            } else {
                                System.out.println("Failed to find game ID for failed timed-out player: " + player);
                            }
                        }
                    }
                }
            
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private static <key, value> key getKeyByValue(Map<key, value> map, value value) {

        for (Map.Entry<key, value> entry : map.entrySet()) {
            if (Objects.equals(entry.getValue(), value)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void playerTimeout(Integer gameID, String username, Boolean suspected, Boolean failed){

        System.out.println("Player " + username + " in game " + gameID + " has timed out. (suspected: " + suspected + ", failed: " + failed + ")");

        PuzzleObject game = gamesMap.get(gameID);
        Map<String, ClientCallbackInterface> allPlayers = game.getAllPlayers();

        try{

            if(suspected){

                for (String player : allPlayers.keySet()) {
    
                    if (!player.equals(username) && game.getPlayerStatus(player).equals("active")){
                        allPlayers.get(player).onPlayerTimeout(username, true, false);
                    }
                }
    
            } else if(failed){

                playerQuit(gameID, username);

                for (String player : allPlayers.keySet()) {
    
                    if (!player.equals(username) && game.getPlayerStatus(player).equals("active")){
                        allPlayers.get(player).onPlayerTimeout(username, false, true);
                    }
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Integer getSequence(String username, Integer gameID) throws RemoteException {
        return gamesMap.get(gameID).getPlayerSequence(username);
    }







}





