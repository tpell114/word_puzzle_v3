import java.rmi.*;

public interface CrissCrossPuzzleInterface extends Remote {

    public Integer startGame(String player, ClientCallbackInterface client, Integer numWords, Integer difficultyFactor, Integer seqNum) throws RemoteException;
    public Boolean joinGame(Integer gameID, String player, ClientCallbackInterface client, Integer seqNum) throws RemoteException;
    public void issueStartSignal(Integer gameID, Integer seqNum) throws RemoteException; //need to work on dup
    public char[][] getInitialPuzzle(Integer gameID, Integer seqNum) throws RemoteException; //needs dup
    public Integer getGuessCounter(Integer gameID, Integer seqNum) throws RemoteException; //needs dup
    public void playerGuess(String username, Integer gameID, String guess, Integer seqNum) throws RemoteException;
    public void playerQuit(Integer gameID, String username, Integer seqNum) throws RemoteException; 
    public Boolean addWord(String word, Integer seqNum) throws RemoteException;  //needs dup
    public Boolean removeWord(String word,Integer seqNum) throws RemoteException; //needs dup
    public Boolean checkWord(String word, Integer seqNum) throws RemoteException; //needs dup
    public void playerHeartbeat(Integer gameID, String username, Integer seqNum) throws RemoteException;


}
