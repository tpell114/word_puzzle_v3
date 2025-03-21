import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountService extends UnicastRemoteObject implements AccountServiceInterface {

    private ConcurrentHashMap<String, Integer> userScores;

    public AccountService() throws RemoteException {
        super();
        userScores = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        try {
            AccountService service = new AccountService();
            Naming.rebind("rmi://localhost:1099/AccountService", service);
            System.out.println("Account Service registered with RMI registry.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the user to ConcurrentHashMap<String, Integer> with an initial score of 0 if they don’t already exist.
     * 
     * @param username the username of the user to be registered
     * @return true if the user was successfully registered, false if the user already exists
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public Boolean registerUser(String username) throws RemoteException {

        if (!userScores.containsKey(username)) {

            userScores.put(username, 0);
            System.out.println("User registered: " + username);
            return true;
        }
        
        return false;
    }

    /**
     * Retrieves and returns the user's score.
     * 
     * @param username the username of the user whose score is to be retrieved
     * @return the user's score, or null if the user does not exist
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public Integer getUserScore(String username) throws RemoteException {
        return userScores.get(username);
    }

    /**
     * Updates user score in ConcurrentHashMap.                   
     * 
     * @param username the username of the user whose score is to be updated
     * @param value the amount to update the user's score by
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public void updateUserScore(String username, Integer value) throws RemoteException {
        userScores.put(username, userScores.get(username) + value);
    }

    /**
     * Retrieves and returns a map of all users and their scores.
     * 
     * @return a map of all users and their scores, or an empty map if there are no users
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public Map<String, Integer> getAllUsers() throws RemoteException {
        return new HashMap<>(userScores);
    }
    
}
