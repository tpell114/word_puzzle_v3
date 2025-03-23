import java.rmi.*;

public interface WordRepositoryInterface extends Remote {

    public Boolean addWord(String word) throws RemoteException;
    public Boolean removeWord(String word) throws RemoteException;
    public Boolean checkWord(String word) throws RemoteException;
    public String getWord(int minLength) throws RemoteException;
    public String getWord(String contains) throws RemoteException;

}
