import java.rmi.Naming;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PuzzleObject {

    private final Lock lock = new ReentrantLock();
    private WordRepositoryInterface wordRepo;
    private Integer gameID;
    private Integer numWords;
    private Integer difficultyFactor;
    private Integer guessCounter;
    private Map<String, ClientCallbackInterface> players = new LinkedHashMap<>();
    private Map<String, Integer> scores = new LinkedHashMap<>(); // <username, words guessed>
    private String activePlayer;
    private String stem;
    private List<String> horizontalWords = new ArrayList<>();
    private List<String> completedWords = new ArrayList<>();
    private char[][] puzzleMaster;
    private char[][] puzzleSlave;

    public PuzzleObject(String username, ClientCallbackInterface client, Integer gameID, Integer numWords, Integer difficultyFactor) {
        this.players.put(username, client);
        this.scores.put(username, 0);
        this.activePlayer = username;
        this.gameID = gameID;
        this.numWords = numWords;
        this.difficultyFactor = difficultyFactor;
        
        initPuzzle();
    }

    /**
     * Adds a new player to the game, associating them with a ClientCallbackInterface
     * and initializing their score to 0.
     * 
     * @param username The username of the player to add.
     * @param client The ClientCallbackInterface the player will use to receive updates.
     */
    public void addPlayer(String username, ClientCallbackInterface client) {
        this.players.put(username, client);
        this.scores.put(username, 0);
    }

    /**
     * Processes a character guess in the puzzle.
     * Decrements the guess counter and checks if the guessed character
     * is present in the puzzleMaster grid. If found, updates the corresponding
     * positions in the puzzleSlave grid with the guessed character.
     * 
     * @param guess The character guessed by the player.
     * @return true if the puzzleSlave matches the puzzleMaster after the guess,
     *         indicating the puzzle is solved; otherwise, returns false.
     */
    public Boolean guessChar(String username, char guess){

        System.out.println("Game ID: " + gameID + " guessing " + guess);
        this.guessCounter--;

        for (int i = 0; i < puzzleMaster.length; i++) {

            for (int j = 0; j < puzzleMaster[i].length; j++) {

                if (puzzleMaster[i][j] == guess) {

                    puzzleSlave[i][j] = guess;

                    String masterRow = new String(puzzleMaster[i]).replaceAll("^-+|-+$", "");
                    String slaveRow = new String(puzzleSlave[i]).replaceAll("^-+|-+$", "");

                    if(slaveRow.equals(masterRow) && horizontalWords.contains(slaveRow) && !completedWords.contains(slaveRow)){
                        completedWords.add(slaveRow);
                        scores.put(username, scores.get(username) + 1);
                        System.out.println("Added 1 word guessed to: " + username);
                    }

                    Integer middleColumn = puzzleMaster[i].length/2;
                    String masterColumn = "";
                    String slaveColumn = "";

                    for (int k = 0; k < puzzleMaster.length; k++) {
 
                        masterColumn += puzzleMaster[k][middleColumn];
                        slaveColumn += puzzleSlave[k][middleColumn];
                    }

                    if (slaveColumn.equals(masterColumn) && !completedWords.contains(slaveColumn)){
                        completedWords.add(slaveColumn);
                        scores.put(username, scores.get(username) + 1);
                        System.out.println("Added 1 word guessed to: " + username);
                    }
                }
            }
        }

        if (Arrays.deepEquals(puzzleSlave, puzzleMaster)) {
            System.out.println("Puzzle slave matches puzzle master!");
            return true;
        }
        return false;
    }

    /**
     * Processes a word guess in the puzzle.
     * Decrements the guess counter and checks if the guessed word
     * is either the stem word or one of the horizontal words in the puzzleMaster grid.
     * If found, updates the corresponding positions in the puzzleSlave grid with the
     * characters of the guessed word.
     * 
     * @param guess The word guessed by the player.
     * @return true if the puzzleSlave matches the puzzleMaster after the guess,
     *         indicating the puzzle is solved; otherwise, returns false.
     */
    public Boolean guessWord(String username, String guess){

        System.out.println("Game ID: " + gameID + " guessing " + guess);
        this.guessCounter--;

        if (guess.equals(this.stem)) {

            for (int i = 0; i < puzzleMaster.length; i++) {
                puzzleSlave[i][puzzleMaster[i].length/2] = stem.charAt(i);
            }

            if(!completedWords.contains(stem)){
                completedWords.add(stem);
                scores.put(username, scores.get(username) + 1);
                System.out.println("Added 1 word guessed to: " + username);
            }

        } else if (horizontalWords.contains(guess)) {

            for (int i = 0; i < puzzleMaster.length; i += 2) {

                String line = "";

                for (int j = 0; j < puzzleMaster[i].length; j++) {
                    line += puzzleMaster[i][j];
                }

                if (line.contains(guess)) {
                    for (int j = 0; j < puzzleMaster[i].length; j++) {
                        puzzleSlave[i][j] = puzzleMaster[i][j];
                    }

                    if(!completedWords.contains(guess)){
                        completedWords.add(guess);
                        scores.put(username, scores.get(username) + 1);
                        System.out.println("Added 1 word guessed to: " + username);
                    }
                }
            }
        }

        if (Arrays.deepEquals(puzzleSlave, puzzleMaster)) {
            System.out.println("Puzzle slave matches puzzle master!");
            return true;
        }
        return false;
    }

    /**
     * Gets the username of the player whose turn it currently is.
     * 
     * @return The username of the active player.
     */
    public String getActivePlayer(){
        return activePlayer;
    }

    /**
     * Gets the callback interface of the player whose turn it currently is.
     * 
     * @return The callback interface of the active player.
     */
    public ClientCallbackInterface getActivePlayerCallback(){
        return players.get(activePlayer);
    }

    /**
     * Gets a map of all players in the game and their corresponding callback interfaces. The keys are the usernames of the players, and the values are the callback interfaces that the server can use to notify players of events.
     * 
     * @return A map of all players in the game and their callback interfaces.
     */
    public Map<String, ClientCallbackInterface> getAllPlayers(){
        return new LinkedHashMap<>(players);
    }

    /**
     * Increments the active player to the next player in the list of players
     * registered in the game. If the list is empty, does nothing.
     * 
     * @see #getActivePlayer()
     */
    public void incrementActivePlayer() {
        
        List<String> keys = new ArrayList<>(players.keySet());

        if (!keys.isEmpty()) {
            int currentIndex = keys.indexOf(activePlayer);
            int nextIndex = (currentIndex + 1) % keys.size();
            activePlayer = keys.get(nextIndex);
            System.out.println("Next player: " + activePlayer);
        }
    }
    
    /**
     * Retrieves the current number of guesses remaining for the game.
     * This value decreases with each incorrect guess made by players.
     *
     * @return The number of remaining guesses.
     */
    public Integer getGuessCounter(){
        return guessCounter;
    }

    /**
     * Retrieves a copy of the current puzzle state, which is the best guess the 
     * players have made so far. The copy is a 2D array of characters, where the 
     * first dimension is the row and the second is the column. The characters in 
     * the array are either the letters guessed so far, or '-' if the corresponding 
     * letter has not been guessed yet.
     * 
     * @return A copy of the current puzzle state as a 2D array of characters.
     */
    public char[][] getPuzzleSlaveCopy() {

        lock.lock();
        char[][] copy = new char[puzzleSlave.length][puzzleSlave[0].length];

        for (int i = 0; i < puzzleSlave.length; i++) {
            copy[i] = Arrays.copyOf(puzzleSlave[i], puzzleSlave[i].length);
        }

        lock.unlock();
        return copy;
    }
    
    /**
     * Initializes the puzzle by setting up the stem word and horizontal words
     * using the WordRepositoryInterface. The method calculates the initial
     * guess counter based on the length of the stem and horizontal words
     * multiplied by the difficulty factor. It retrieves words from the
     * repository to populate the horizontalWords list and ensures it contains
     * the correct number of words. It then calls initPuzzleMaster and
     * initPuzzleSlave to set up the puzzle grids.
     * 
     * @throws Exception if an error occurs during the lookup or retrieval of words
     */
    private void initPuzzle() {

        try {
            this.wordRepo = (WordRepositoryInterface) Naming.lookup("rmi://localhost/WordRepository");
            this.stem = this.wordRepo.getWord((this.numWords - 1) * 2);
            this.guessCounter = this.stem.length() * difficultyFactor;

            String word;
            for (int i = 0; i < stem.length(); i += 2) {
                word = this.wordRepo.getWord(String.valueOf(stem.charAt(i)));
                horizontalWords.add(word);
                this.guessCounter += word.length() * difficultyFactor;
                if (horizontalWords.size() == numWords - 1) break;
            }

            initPuzzleMaster();
            initPuzzleSlave();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the puzzleMaster 2D array with the given stem and horizontal words.
     * The puzzleMaster is a 2D array of characters, where each row represents a line
     * in the puzzle and each column represents a letter in the puzzle.
     * The puzzleMaster is initialized such that each row has enough columns to fit the
     * longest horizontal word, and each column is initialized with a '.' character.
     * The stem is then placed vertically in the middle of the puzzleMaster, and each
     * horizontal word is placed at the correct position in the puzzleMaster such that
     * the intersecting letter of the horizontal word and the stem lines up.
     */
    private void initPuzzleMaster(){

        System.out.println(stem);
        System.out.println(horizontalWords);

        int ySize = stem.length();

        String longest = null;
        for (String word : horizontalWords) {
            if (longest == null || word.length() > longest.length()) {
                longest = word;
            }
        }

        int xSize = longest.length() * 2;
        this.puzzleMaster = new char[ySize][xSize];

        for (int i = 0; i < ySize; i++) {
            for (int j = 0; j < xSize; j++) {
                puzzleMaster[i][j] = '.';
            }
        }

        for (int i = 0; i < ySize; i++) {
            puzzleMaster[i][xSize/2] = stem.charAt(i);
        }

        int stemIndex = 0;
        for (int i = 0; i < horizontalWords.size(); i++) {

            String word = horizontalWords.get(i);
            char intersectChar = stem.charAt(stemIndex);
            int offset = word.indexOf(intersectChar);
            int startColumn = xSize/2 - offset;

            for (int j = 0; j < word.length(); j++) {
                puzzleMaster[stemIndex][startColumn + j] = word.charAt(j);
            }

            stemIndex += 2;
        }
     
        for (char[] row : puzzleMaster) {
            System.out.println(new String(row));
        }
    }

    /**
     * Initializes the puzzleSlave 2D array with the same dimensions as the
     * puzzleMaster. The puzzleSlave is initialized such that any '.' character
     * in the puzzleMaster is replaced with a '.' character in the puzzleSlave
     * and any other character in the puzzleMaster is replaced with a '-'
     * character in the puzzleSlave.
     */
    private void initPuzzleSlave(){

        puzzleSlave = new char[puzzleMaster.length][puzzleMaster[0].length];

        for (int i = 0; i < puzzleSlave.length; i++) {
            for (int j = 0; j < puzzleSlave[i].length; j++) {
                if (puzzleMaster[i][j] == '.') {
                    puzzleSlave[i][j] = '.';
                } else {
                    puzzleSlave[i][j] = '-';
                }
            }
        } 
    }

    /**
     * Removes a player from the game.
     * 
     * Player is removed from the players list and scores map.
     * 
     * @param username the username of the player to be removed
     * @return true if the player was successfully removed, false if the player
     *         was not in the game
     */
    public Boolean removePlayer(String username){

        this.players.remove(username);
        this.scores.remove(username);

        if(this.players.isEmpty()){
            return true;
        }

        return false;
    }

    /**
     * Retrieves the number of words guessed by a specific player.
     *
     * @param username the username of the player whose score is to be retrieved
     * @return the number of words guessed by the player, or null if the player
     *         is not found
     */
    public Integer getWordsGuessed(String username){
        return this.scores.get(username);
    }

    /**
     * Retrieves a list of the players with the highest score in the game.
     * 
     * @return a list of the top players, or an empty list if there are no
     *         players
     */
    public List<String> getHighestScoredPlayers(){

        int highestScore = Collections.max(scores.values());
        List<String> topPlayers = new ArrayList<>();

        for (String player : scores.keySet()) {
            if (scores.get(player) == highestScore) {
                topPlayers.add(player);
            }
        }

        return topPlayers;
    }

    /**
     * Retrieves a map of all players in the game and their scores.
     *
     * @return a map of all players and their scores, or an empty map if there
     *         are no players
     */
    public Map<String, Integer> getAllScores(){
        return new LinkedHashMap<>(scores);
    }
}
