import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;


public class WordRepository extends UnicastRemoteObject implements WordRepositoryInterface {

    private List<String> words = new ArrayList<>();

    public WordRepository() throws RemoteException {
        super();
        loadWords("words.txt");
    }

    public static void main(String[] args) {

        try {
            WordRepository wordRepository = new WordRepository();
            Naming.rebind("rmi://localhost:1099/WordRepository", wordRepository);
            System.out.println("WordRepository is registered with the RMI registry with URL: rmi://localhost:1099/WordRepository");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads words from a specified file into the `words` list.
     * Each word is trimmed of leading and trailing spaces and
     * converted to lowercase before being added to the list.
     * The method prints the total number of words loaded or
     * an error message if the file cannot be read.
     *
     * @param filepath the path to the file containing the words
     */
    private void loadWords(String filepath) {

        System.out.println("Loading words from file");

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim().toLowerCase());
            }
            System.out.println("Loaded " + words.size() + " words.");
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Adds a word to the word repository if it does not already exist.
     *
     * This method checks if the given word exists in the word repository. If
     * the word does not exist, it is added to the repository and the repository
     * is sorted. The method returns true if the word was added and false if
     * it already exists in the repository.
     *
     * @param word the word to be added to the repository
     * @return true if the word was successfully added, false if it already exists
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public Boolean addWord(String word) throws RemoteException {

        if (!words.contains(word.toLowerCase())) {

            words.add(word.toLowerCase());
            Collections.sort(words);
            return true;
        }
        
        return false;
    }

    /**
     * Removes a word from the word repository if it exists.
     * 
     * This method checks if the given word exists in the word repository. If
     * the word exists, it is removed from the repository. The method returns
     * true if the word was removed and false if it does not exist in the
     * repository.
     * 
     * @param word the word to be removed from the repository
     * @return true if the word was successfully removed, false if it does not exist
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public Boolean removeWord(String word) throws RemoteException {
        return words.remove(word.toLowerCase());
    }

    /**
     * Checks if a word exists in the word repository.
     * 
     * This method checks if the given word exists in the word repository. If
     * the word exists, the method returns true; otherwise, it returns false.
     * 
     * @param word the word to be checked for in the repository
     * @return true if the word exists in the repository, false if it does not exist
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public Boolean checkWord(String word) throws RemoteException {
        return words.contains(word.toLowerCase());
    }

    /**
     * Retrieves a random word from the word repository that is at least
     * {@code minLength} characters long.
     * 
     * @param minLength the minimum length of the word to be retrieved
     * @return a random word from the repository that meets the minimum length
     *         requirement
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public String getWord(int minLength) throws RemoteException {
        
        Random random = new Random();
        int index = random.nextInt(words.size());
        String word = words.get(index);

        while(word.length() < minLength){
            index = random.nextInt(words.size());
            word = words.get(index);
        }

        return word;
    }

    /**
     * Retrieves a random word from the word repository that contains
     * the given substring.
     * 
     * @param contains the substring to search for in the word repository
     * @return a random word from the repository that contains the substring
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public String getWord(String contains) throws RemoteException {
        
        Random random = new Random();
        int index = random.nextInt(words.size());
        String word = words.get(index);

        while(!word.contains(String.valueOf(contains.toLowerCase()))){
            index = random.nextInt(words.size());
            word = words.get(index);
        }

        return word;
    }

}
