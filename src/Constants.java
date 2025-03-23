public class Constants {

    // Message constants
    public static final String MAIN_MENU_MESSAGE = "\nSelect from the following options:\n"
                                                    +"1. Play a new game\n"
                                                    +"2. Join a game\n"
                                                    +"3. View personal statistics\n"
                                                    +"4. View leaderboard\n"
                                                    +"5. Modify word repository\n"
                                                    +"6. Exit\n";

    public static final String USER_SIGN_IN_MESSAGE = "\nWelcome to Word Puzzle!\n"
                                                        +"=======================\n"
                                                        +"Please enter your name:\n";

    public static final String GUESS_MESSAGE = "\nPlease guess a letter or a word (enter ~ to quit)\n"
                                                + "you can also verify if a word exists by prefixing a word with '?' eg. ?apple\n";

    public static final String WORD_REPO_MESSAGE = "\nAdd words to the repo by prefixing a word with '+'  eg. +apple\n"
                                                    + "remove words from the repo by prefixing a word with '-' eg. -apple\n"
                                                    + "check if a word exists by prefixing a word with '?' eg. ?apple\n"
                                                    + "enter '~' to return to menu";

    public static final String GAME_START_MESSAGE = "\nShare this ID with your friends to join the game.\n"
                                                        + "\nPress any key to start the game, or wait for other players to join...\n"
                                                        + "You can press ~ to return to the main menu.";
                                                    
    private Constants() {
        // This class should not be instantiated
    }
}