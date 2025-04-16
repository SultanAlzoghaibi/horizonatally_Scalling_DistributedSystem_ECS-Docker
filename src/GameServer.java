import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class GameServer {
    private ServerSocket gameServerSocket;
    private ServerSocket chatServerSocket;
    private int numPlayers;
    private ServerSideConnection player1Ssc;    // these a the server side equilivant to the package drop off pouints i think
    private ServerSideConnection player2Ssc;

    private ServerSideConnection player1Chat;
    private ServerSideConnection player2Chat;
    static String gameMode;

    private int turnsMade;

    // store the  the button num that the player clicked on, befroe being sent to the other player
    // don in the run method while loop, for each turns
    private String player1ButtonNum;
    private String player2ButtonNum;
    private PracticeGameObj practiceGameObj;

    private HashMap<Integer, String> chatLogs;




    public GameServer() {
        System.out.println("--game server--");

        chatLogs = new HashMap<>();

        numPlayers = 0;
        turnsMade = 0;

        practiceGameObj = new PracticeGameObj(false, new char[2][2], "test");

        try{
            gameServerSocket = new ServerSocket(30001);
            chatServerSocket = new ServerSocket(30002);

        } catch(IOException e){
            System.out.println("IOException from game server constructor");
            e.printStackTrace();
        }
    }




    public void acceptConnections(){
        try {

            while (numPlayers < 2) {
                System.out.println("waiting for connections");
                Socket gameSocket = gameServerSocket.accept();
                System.out.println("game connection accepted");
                Socket chatSocket = chatServerSocket.accept();
                numPlayers++;

                System.out.println("Player #" + numPlayers + " has joined the game");
                ServerSideConnection ssc = new ServerSideConnection(gameSocket, chatSocket, numPlayers);

                if (numPlayers == 1) {
                    player1Ssc = ssc;

                } else {

                    player2Ssc = ssc;
                }

                Thread t = new Thread(ssc); //what ever is the in the ssc run in the new "THREAD"
                t.start();
            }
            System.out.println("2 player reach, no more looking for players");
        }catch(IOException e){
            System.out.println("IOException from game server acceptConnections");
        }
    }



    private class ServerSideConnection implements Runnable {
        private Socket gameSocket;
        private Socket chatSocket;

        private DataOutputStream dataOut;
        private ObjectOutputStream gameOutObj;
        private ObjectInputStream gameInObj;

        private ObjectOutputStream chatOutObj;
        private ObjectInputStream chatInObj;

        private int playerID;

        public ServerSideConnection(Socket gameSocket1, Socket chatSocket1, int id) {
            gameSocket = gameSocket1;
            chatSocket = chatSocket1;
            playerID = id;

            try {
                dataOut = new DataOutputStream(gameSocket.getOutputStream());
                gameOutObj = new ObjectOutputStream(gameSocket.getOutputStream());
                gameInObj = new ObjectInputStream(gameSocket.getInputStream());


                chatOutObj = new ObjectOutputStream(chatSocket.getOutputStream());
                chatInObj = new ObjectInputStream(chatSocket.getInputStream());
            } catch (IOException e) {
                System.out.println("IOException from game server constructor: ServerSideConnection");
            }
        }

        // Censorship Logic

        public void run() { // insctruction we want to run on a NEW thread
            try {
                System.out.println("sent player ID: " + playerID);
                dataOut.writeInt(playerID);
                dataOut.writeUTF(gameMode);
                dataOut.flush();

                new Thread(() -> {
                    handleChatThread();
                }).start();

                while (true) {
                    if (playerID == 1) {
                        practiceGameObj = (PracticeGameObj) gameInObj.readObject();  // Reads one char and converts to String // read it from player 1
                        player1ButtonNum = practiceGameObj.getInputString();
                        System.out.println("Payer 1 clicked button #" + player1ButtonNum);
                        // Update array
                        processGameLogic(1, player1ButtonNum);
                        player2Ssc.sendPracticeGameObj(); // sending server2dChar


                    } else {
                        practiceGameObj = (PracticeGameObj) gameInObj.readObject();
                        player2ButtonNum = practiceGameObj.getInputString();
                        System.out.println("PLayer 2 clicked button #" + player2ButtonNum);
                        processGameLogic(2, player2ButtonNum);

                        player1Ssc.sendPracticeGameObj();
                    }
                    turnsMade++;

                    if (false) {
                        System.out.println("GAME HAS ENDED");
                        break; // break from the game and end
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException from run() : ServerSideConnection");
            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException from run() : ServerSideConnection");
            }
        }

        public void sendPracticeGameObj() {
            try {
                gameOutObj.writeObject(practiceGameObj);
                gameOutObj.flush();
            } catch (IOException e) {
                System.out.println("IOException from game server sendPracticeGameObj");
            }
        }

        public void handleChatThread() {
            System.out.println(chatLogs.toString());
            receiveChats();

        }


        public void receiveChats() {
            try {
                while (true) {
                    Object obj = chatInObj.readObject(); // ✅
                    String msg = (String) obj;
                      // Apply censorship here

                    chatLogs.put(this.playerID, msg);  // Store censored message in chat logs
                    System.out.println("PUT Chat Player #" + this.playerID + ": " + msg);

                    // Send the censored message to the other player
                    if (this.playerID == 1 && player2Ssc != null) {
                        player2Ssc.sendChatMessage(msg);
                    } else if (this.playerID == 2 && player1Ssc != null) {
                        player1Ssc.sendChatMessage(msg);
                    }
                }
            } catch (IOException e) {
                System.out.println("Chat thread crashed for Player " + playerID);
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Object not found");
            }
        }

        public void sendChatMessage(String msg) {
            try {
                chatOutObj.writeObject(msg);  // Send the censored message
                chatOutObj.flush();

            } catch (IOException e) {
                System.out.println("Error sending message to player #" + playerID);
                e.printStackTrace();
            }
        }


        public void processGameLogic(int playerID, String input) {
            if(gameMode.equals("tictactoe")){
                tictactoePlaceMove(input, playerID == 1 ? 'X' : 'O');
            }
            if (gameMode.equals("connect4")) {
                connect4PlaceMove(input, playerID == 1 ? 'X' : 'O');
            }


        }

        // I ASKED chatgtp for these as the dont add value to the ecs h-scalling Distrubted sytem
        private void connect4PlaceMove(String input, char symbol) {
            int col = Integer.parseInt(input) - 1;

            // Retrieve the current board from PracticeGameObj
            // Assume board is a 6x7 char array representing the Connect4 grid.
            char[][] board = practiceGameObj.getBoard();

            // If the board is uninitialized or incorrectly sized, create a new 6x7 board filled with blanks (' ')
            if (board == null || board.length != 6 || board[0].length != 7) {
                board = new char[6][7];
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < 7; j++) {
                        board[i][j] = ' ';
                    }
                }
            }

            // Drop the piece to the lowest empty row in the selected column
            boolean movePlaced = false;
            for (int row = 5; row >= 0; row--) {
                if (board[row][col] == ' ' || board[row][col] == '\0') {
                    board[row][col] = symbol;
                    practiceGameObj.setBoard(board); // Update the PracticeGameObj's board
                    System.out.printf("Move placed at row %d, column %d with symbol '%c'%n", row, col, symbol);
                    movePlaced = true;
                    break;
                }
            }

            if (!movePlaced) {
                System.out.printf("Invalid move: column %d is already full.%n", col);
            }
        }

        private void tictactoePlaceMove(String input, char symbol) {
            // Convert input to 0-based move index
            int move = Integer.parseInt(input) - 1;
            int row = move / 3;
            int col = move % 3;

            // Retrieve the current board from PracticeGameObj
            // Assume board is a 3x3 char array representing the TicTacToe grid.
            char[][] board = practiceGameObj.getBoard();

            // If the board is uninitialized, create a new empty board filled with blanks (' ')
            if (board == null || board.length != 3) {
                board = new char[3][3];
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        board[i][j] = ' ';  // blank space represents an empty cell
                    }
                }
            }
            // Check if the cell is empty before placing the move

        }


        // END of chatgtp tentious work
    }

        public void closeServers() {
            try {
                if (gameServerSocket != null && !gameServerSocket.isClosed()) {
                    gameServerSocket.close();
                    System.out.println("Game server socket closed.");
                }
                if (chatServerSocket != null && !chatServerSocket.isClosed()) {
                    chatServerSocket.close();
                    System.out.println("Chat server socket closed.");
                }
            } catch (IOException e) {
                System.out.println("Error closing server sockets: " + e.getMessage());
            }
        }




    public static void main(String[] args) {
        // Set game mode from args if provided, else default to tictactoe
        if (args.length == 1) {
            gameMode = args[0];
        } else {
            System.out.println("No mode passed. Using default: tictactoe");
            gameMode = "tictactoe";
        }

        GameServer gs = new GameServer();

        // Add shutdown hook to close the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown detected. Closing server sockets...");
            gs.closeServers();
        }));

        try {
            System.out.println("Accepting connections...");
            gs.acceptConnections();
        } catch (Exception e) {
            System.out.println("Exception in GameServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
