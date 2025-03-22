import java.rmi.*;

public interface CrissCrossPuzzleInterface extends Remote {

    public Integer startGame(String player, ClientCallbackInterface client, Integer numWords, Integer difficultyFactor) throws RemoteException;
    public Boolean joinGame(Integer gameID, String player, ClientCallbackInterface client) throws RemoteException;
    public void issueStartSignal(Integer gameID) throws RemoteException;
    public char[][] getInitialPuzzle(Integer gameID) throws RemoteException;
    public Integer getGuessCounter(Integer gameID) throws RemoteException;
    public void playerGuess(String username, Integer gameID, String guess, Integer seqNum) throws RemoteException;
    public void playerQuit(Integer gameID, String username) throws RemoteException;
    public Boolean addWord(String word) throws RemoteException;
    public Boolean removeWord(String word) throws RemoteException;
    public Boolean checkWord(String word) throws RemoteException;
    public void playerHeartbeat(Integer gameID, String username, Integer seqNum) throws RemoteException;


}
