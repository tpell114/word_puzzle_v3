import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIRegistry {
    public static void main(String[] args) {

        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            System.out.println("RMI registry started on port 1099");

            synchronized (RMIRegistry.class) {
                RMIRegistry.class.wait();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
