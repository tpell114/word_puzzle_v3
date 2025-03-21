import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ClientCallbackInterface extends Remote {

    public void onYourTurn(char[][] puzzle, Integer guessCounter, Integer wordCounter) throws RemoteException;
    public void onOpponentTurn(char[][] puzzle, Integer guessCounter, Integer wordCounter) throws RemoteException;
    public void onGameWin(char[][] puzzle, Integer guessCounter, Integer wordCounter, Map<String, Integer> scores) throws RemoteException;
    public void onGameLoss(char[][] puzzle, Integer guessCounter, Integer wordCounter, Map<String, Integer> scores) throws RemoteException;
    public void onPlayerJoin(String player, Integer numPlayers) throws RemoteException;
    public void onPlayerQuit(String player, Integer numPlayers) throws RemoteException;
    public void onGameStart() throws RemoteException;

}
