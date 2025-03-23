import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.stream.Collectors;

public class Scoreboard extends UnicastRemoteObject implements ScoreboardInterface {

    private AccountServiceInterface accountService;

    public Scoreboard() throws RemoteException {
        super();
        try {
            accountService = (AccountServiceInterface) Naming.lookup("rmi://localhost:1099/AccountService");
            System.out.println("Scoreboard Service connected to Account Service.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Scoreboard service = new Scoreboard();
            Naming.rebind("rmi://localhost:1099/ScoreboardService", service);
            System.out.println("Scoreboard Service registered with RMI registry.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the top n scores from the Account Service in descending order of their values.
     * 
     * @param n the number of top scores to retrieve
     * @return a list of entries, each containing the username and score of a player
     * @throws RemoteException if an error occurs during remote communication
     */
    public List<Map.Entry<String, Integer>> getScores(Integer n) throws RemoteException {

        Map<String, Integer> scores = accountService.getAllUsers();
        List<Map.Entry<String, Integer>> entriesList = new ArrayList<>(scores.entrySet());
        entriesList.sort(Map.Entry.<String, Integer>comparingByValue().reversed());  
        List<Map.Entry<String, Integer>> topN = entriesList.stream().limit(n).map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    
        return topN;
    }

}
