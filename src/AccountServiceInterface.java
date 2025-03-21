import java.rmi.*;
import java.util.Map;

public interface AccountServiceInterface extends Remote {

    Boolean registerUser(String username) throws RemoteException;
    Integer getUserScore(String username) throws RemoteException;
    void updateUserScore(String username, Integer value) throws RemoteException;
    Map<String, Integer> getAllUsers() throws RemoteException;
    
}
