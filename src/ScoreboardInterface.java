import java.rmi.*;
import java.util.List;
import java.util.Map;

public interface ScoreboardInterface extends Remote {

    List<Map.Entry<String, Integer>> getScores(Integer n) throws RemoteException;
    
}
