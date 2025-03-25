# Protocol Design Document: Multiplayer Criss-Cross Word Puzzle with Java RMI 

## Introduction
### Objective: This document outlines the design and implementation of a distributed multiplayer Criss-Cross word puzzle game using Java RMI.
### Scope: The system enables single-player and multi-player gameplay where users take turns guessing words or letters. The system includes microservices for word repository, user account management, and a scoreboard.
### Technologies Used: Java RMI, ConcurrentHashMap, Remote Interfaces

## System Architecture
### Components
*  Client: Handles user interactions and communicates with the game server, account service, and scoreboard via RMI.
*  Server: Manages game logic, tracks game state, and coordinates multiple players.
*  Account Service: Manages user registration and scores.
*  Word Repository: Stores and retrieves words for puzzle generation.
*  Scoreboard: Computes player rankings.
*  RMI Registry: Registers all remote services.

### Data Exchange and Method Invocation Flow
#### User Registration and Score Management

|Client Call| Method (Server Side)| Parameters Sent | Return Value | Action on the Server|
|-------------|---------------------|-----------------|--------------|---------------------|
|User starts game and registers|registerUser(String username) (AccountService)| username (String)| Boolean (true if registered, false if already exists) |Adds the user to ConcurrentHashMap<String, Integer> with an initial score of 0 if they don’t already exist.                     |
|User requests their score        | getUserScore(String username) (AccountService) | username (String) | Integer (current score of user) |  Retrieves and returns the user's score.   |
|Server updates score after a game  | updateUserScore(String username, Integer value) (AccountService) | username (String), value (Integer, score update amount) | void  | Updates user score in ConcurrentHashMap.                    |
|Client requests the leaderboard |  getScores(Integer n) (Scoreboard)  |  n (Integer, number of top players to return) |   List<Map.Entry<String, Integer>> (sorted scores) | Retrieves the top n users from AccountService and sorts them.                    |


#### Game Setup and Joining a Game

|Client Call| Method (Server Side)| Parameters Sent | Return Value | Action on the Server|
|-----------|---------------------|-----------------|--------------|---------------------|
| Client requests to start a game|  startGame(String username, ClientCallbackInterface client, Integer numWords, Integer difficultyFactor) (Server)|username (String), client (RMI callback), numWords (Integer), difficultyFactor (Integer)  |  Integer (gameID)            | Generates a random gameID, creates a PuzzleObject, and initializes the game puzzle.                    |
| Client requests to join an existing game | joinGame(Integer gameID, String username, ClientCallbackInterface client) (Server) | gameID (Integer), username (String), client (RMI callback) | Boolean (true if joined, false if gameID invalid)             | Adds the player to the existing game’s players list. Sends a callback to all existing players notifying them of the new player.                    |
| Server starts the game (after enough players join)  | issueStartSignal(Integer gameID) (Server)| gameID (Integer)   |  void            |  Notifies all clients via RMI callback that the game has started.                   |

#### Gameplay - Player Actions

|Client Call| Method (Server Side)| Parameters Sent | Return Value | Action on the Server|
|-----------|---------------------|-----------------|--------------|---------------------|
| Client requests the initial puzzle|  getInitialPuzzle(Integer gameID) (Server)  | gameID (Integer) |  char[][] (puzzle state) |  Returns the current puzzle state from PuzzleObject. |
| Client makes a letter guess  | playerGuess(String username, Integer gameID, String guess) (Server)| username (String), gameID (Integer), guess (String)| void |    Checks if the guess is correct. If correct, updates puzzleSlave. If incorrect, decreases guess counter. Calls handleGameWin() or handleGameLoss() if conditions are met.|
|  Server updates player turn  | onYourTurn(char[][] puzzle, Integer guessCounter, Integer wordCounter) (Client Callback)| puzzle (char[][]), guessCounter (Integer), wordCounter (Integer) |  void  |   Notifies the next player that it's their turn and updates the puzzle state.                  |
| Server notifies all other players|  onOpponentTurn(char[][] puzzle, Integer guessCounter, Integer wordCounter) (Client Callback)| puzzle (char[][]), guessCounter (Integer), wordCounter (Integer)| void|Updates other players with the latest game state while they wait for their turn.|

#### Game Completion and Exit Handling

|Client Call| Method (Server Side)| Parameters Sent | Return Value | Action on the Server|
|-----------|---------------------|-----------------|--------------|---------------------|
|Server detects game win|onGameWin(char[][] puzzle, Integer guessCounter, Integer wordCounter, Map<String, Integer> scores) (Client Callback)|puzzle (char[][]), guessCounter (Integer), wordCounter (Integer), scores (Map<String, Integer>)|void|Notifies all clients that the game has been won, updates scores, and ends the game session.|
|Server detects game loss|onGameLoss(char[][] puzzle, Integer guessCounter, Integer wordCounter, Map<String, Integer> scores) (Client Callback)| puzzle (char[][]), guessCounter (Integer), wordCounter (Integer), scores (Map<String, Integer>)                | void             | Notifies all players that the game has been lost, and ends the session.                    |
| Client quits the game| playerQuit(Integer gameID, String username) (Server)|gameID (Integer), username (String)|    void| Removes player from the game. If no players are left, deletes the PuzzleObject.                    |
| Client disconnects|handleExit() (Client)|None|void|Calls playerQuit() if the user was in an active game, then gracefully exits.|

#### Word Repository Operations

|Client Call| Method (Server Side)| Parameters Sent | Return Value | Action on the Server|
|-----------|---------------------|-----------------|--------------|---------------------|
| Client adds a word|  addWord(String word) (WordRepository)|  word (String)| Boolean (true if added, false if already exists)| Adds word to repository and sorts list.|
|  Client removes a word| removeWord(String word) (WordRepository)| word (String)| Boolean (true if removed, false if not found)|  Deletes word from repository.|
|Client checks if a word exists| checkWord(String word) (WordRepository)|word (String)|  Boolean (true if exists, false if not)|Searches for word in repository.|
| Server fetches a word for the puzzle| getWord(int minLength) (WordRepository)|  minLength (Integer)| String (random word matching criteria)|Retrieves a random word that meets the required length.|

### Summary of Data Flow

#### Client ↔ Server:
* The client interacts with the game server to start/join a game, make guesses, and receive game updates.


#### Server ↔Word Repository:
* The server allows players to add, remove, and check words (since these actions go through the game server).

#### Server↔ Account Service:
* The server retrieves and updates player scores.


#### Client↔Account Service:
* The client registers users and retrieves their scores.


#### Client ↔  Scoreboard:
* The client requests leaderboard rankings.

####  Scoreboard ↔ Account Service:
* The scoreboard retrieves scores from the Account Service and ranks players.

## Design Decisions & Justification

#### Decision category
* justification

#### Use of Java RMI Instead of Sockets
* RMI simplifies distributed system development by allowing objects to call methods on remote objects as if they were local.
* Eliminates the need for manual serialization/deserialization of data, unlike sockets.
* Built-in multi-threading makes handling multiple clients easier.

#### Microservices
* Modularity: Each service (Game Server, Account Service, Word Repository, Scoreboard) has a clear responsibility, making the system easier to modify.
* Size Scalability: The system can be expanded without affecting other components.
* Reduced Complexity: The game server does not have to handle user accounts, word storage, or leaderboards—it delegates these tasks.


#### Turn-Based Synchronization Using RMI Callbacks
* Efficiency: Instead of repeatedly asking the server for updates (polling), the server pushes updates to clients via RMI callbacks.
* Better Player Experience: Players get immediate notifications when it's their turn, reducing unnecessary network traffic.
* Concurrency Safety: Ensures only one player can make a move at a time.


####  Storing Game State in PuzzleObject
* Encapsulation: Keeps all game-related data inside a single object, making it easy to manage.
* Scalability: Multiple games can run in parallel without interfering with each other.
* Data Consistency: The object maintains the puzzle grid, active players, and scores, ensuring game logic remains synchronized.

#### Handling Player Scores with an External Service (AccountService)
* Persistence: Scores remain available even after a game session ends.
* Separation of Complexity: The game server only manages the current game, while the Account Service handles long-term player history.
* Easier Leaderboard Implementation: The Scoreboard Service can easily pull data from the Account Service without interfering with the game server.

#### Multi-Player Game Management with GameID
* Allows multiple groups of players to play at the same time.
* Ensures players join the correct game session.
* Stored in ConcurrentHashMap<Integer, PuzzleObject> so each game has its own isolated state.

#### Leaderboard Implementation via ScoreboardService
* Decouples leaderboard ranking from the game logic, making the game server lighter.
* Future Scalability: The leaderboard logic can be extended without modifying the game server.
* More efficient sorting: The Scoreboard only requests top N scores, rather than sorting all scores every time.


## Challenges & Solutions

| Challenges   |   Solutions |
|------------- |-------------|
|**Ensuring Fair Turn-Based Multiplayer Play** <br> -Multiple players compete for the same puzzle. <br> -Players must take turns, but RMI does not have built-in turn-based synchronization. <br> -Need a way to notify the correct player when it's their turn without others interfering.|-Implemented RMI callbacks (ClientCallbackInterface) to notify players when it's their turn. <br> -Used a turn-tracking mechanism inside PuzzleObject: <BR> + The active player is stored in a variable (activePlayer). <br> + When a player makes a move, the game calls incrementActivePlayer() to move to the next player. <br> +The next player is notified via RMI callback (onYourTurn()), while others receive onOpponentTurn(). <br>  **Result**: Players wait for their turn and are notified automatically, avoiding manual polling.      |
|**Preventing Race Conditions in Multi-Player Mode** <br> -Multiple players interact with the same game state. <br> Without proper synchronization, two players could modify the puzzle at the same time. <br> Need a way to ensure only one player modifies the game state at a time.| -Used synchronized methods in PuzzleObject to prevent simultaneous modifications. <br> Applied a ReentrantLock in PuzzleObject to ensure only one thread (player) can modify the puzzle at a time. <br> -The server rejects inputs from players whose turn it is not, preventing unfair guesses. <br> **Result**: The puzzle state updates correctly without conflicts.            |
| **Managing Concurrent Games Without Interference** <br> -Multiple games must run simultaneously, each with its own players and puzzle state. <br> -Need a way to separate game states while allowing multiple games to run on the same server.              | -Each game is assigned a unique gameID when created. <br> -Used ConcurrentHashMap<Integer, PuzzleObject> to store all active games. <br> -When a player joins, they provide a gameID, ensuring they are added to the correct game. <br>  **Result**: Multiple games can run at the same time without interfering with each other.  |
|**Handling Player Disconnects** <br> -If a player disconnects mid-game, it could leave the game in an invalid state. <br> -Need a way to handle player exits without breaking the game.   | -Implemented a playerQuit() method that: <br> +Removes the player from the game. <br> +If other players remain, the turn is passed to the next player. <br> +If all players leave, the game is removed from memory. <br> **Result**: The game does not break if a player disconnects, and active players can continue playing.
            


## Coding Standards

* Programming Language: Java
* IDE: Visual Studio Code
* Version Control: GitHub 
* Naming Conventions: Camel case

## Code Review Process
* Communication through discord 


## Statement of Contribution
| Name| Server|Client| Word Repo| Word repo interface| Account Service|AccountInterface|ClientcallbackInterface|Constants|CrisscrossPuzzleInterface|PuzzleObject|RMIRegistry|Scorebooard|Scoreboard Interface|Heartbeat|Sequence|Spaghetti|
-----|-------|------|-----------|--------------------|----------------|----------------|-----------------------|---------|-------------------------|------------|-----------|-----------|--------------------|---------|--------|---------|
|Tyler|Tyler |Tyler | Tyler     |Tyler               |                |                |Tyler                  |Tyler    |Tyler                    |Tyler       |Tyler      |           |                    |Tyler    |Tyler   |         |
|Juan |      |      | Juan      |                    |Juan            |Juan            |                       |         |                         |            |           |Juan       |Juan                |         |        |Juan     | 
