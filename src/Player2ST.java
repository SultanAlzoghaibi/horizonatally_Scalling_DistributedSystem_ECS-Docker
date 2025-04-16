import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;

import java.net.Socket;
import java.util.*;


public class Player2ST extends Application {

    private static final int SHIFT_AMOUNT = 100;
    private int width = 200, height = 400;

    private TextArea message, testText;
    private Button startb00;
    private Button startChess;
    private Button startCheckers;
    private Button startTictactoe;
    private Button startConnect4;

    private Button b01, b02, b03, b04, b05, b06, b07, b08, b09;

    private CscToSearchServer cscSS;
    private CscToGameServer cscGS;
    private int playerID;
    private int otherPlayerID;
    private int[] values;
    private char[][] server2dChar;
    private int maxTurns;
    private int turnsMade;

    private boolean buttonsEnabled;
    private boolean gameIsActive;
    private int gameServerPort;
    private String gameServerIP;
    private Stage primaryStage;
    private String gameMode;

    private PracticeGameObj practiceGameObj;
    private TextArea chatArea;
    private Button sendchat;
    private List<Button> gameButtons = new ArrayList<>();
    PlayerData playerData;



    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        playerMenu(primaryStage);
    }
    public PlayerData createPlayerData() {

        Random rand = new Random();
        StringBuilder username = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            char randomChar = (char) ('A' + rand.nextInt(26)); // Random letter from A-Z
            username.append(randomChar);
        }

        PlayerData playerData = new PlayerData(
                rand.nextInt(100),
                username.toString(),
                "chess"
        );
        playerData.printPlayerData();
        return playerData;
    }

    public void playerMenu(Stage primaryStage) {
        // CHAT GTP TRANSLATED THIS FROM SWING TO JAVAFX
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setTitle("The Game Menu");


        // Initialize arrays and variables
        turnsMade = 0;
        gameIsActive = false;
        buttonsEnabled = false;
        gameMode = "na";
        gameServerIP = "na";

        VBox menuLayout = new VBox();
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setSpacing(10);

        testText = new TextArea("lol");
        testText.setEditable(false);

        try {
            playerData = createPlayerData();
        }catch(Exception e) {
            System.out.println("error");
        }

        // "Start Game" button
        //startb00 = new Button("Start Matchmaking");
        startChess = new Button("Start Chess");
        startCheckers = new Button("Start Checkers");
        startTictactoe = new Button("Start Tictactoe");
        startConnect4 = new Button("Start Connect4");

        startb00 = new Button("Start b00");

        startb00.setOnAction(e -> {
            System.out.println("YOU PRESSED THE BUTTON");
            hnadleB00Click();
        });

        startChess.setOnAction(e -> {
            System.out.println("YOU PRESSED CHESS");
            playerData.setGameModeInterested("chess");
            matchmakingButtonPressed();
        });
        startCheckers.setOnAction(e -> {
            System.out.println("YOU PRESSED CHECKERS");
            playerData.setGameModeInterested("checkers");
            matchmakingButtonPressed();
        });
        startTictactoe.setOnAction(e -> {
            System.out.println("YOU PRESSED TICTACTOE");
            playerData.setGameModeInterested("tictactoe");
            matchmakingButtonPressed();
        });
        startConnect4.setOnAction(e -> {
            System.out.println("YOU PRESSED CONNECT4");
            playerData.setGameModeInterested("connect4");
            matchmakingButtonPressed();
        });

        menuLayout.getChildren().addAll(startb00, startChess, startCheckers, startTictactoe, startConnect4, testText);
        //menuLayout.getChildren().addAll(startChess, testText);

        Scene menuScene = new Scene(menuLayout, width, height);
        primaryStage.setScene(menuScene);

        // 🔹 Add dynamic window shifting on each run
        Random random = new Random();
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        int SHIFT_AMOUNT = 130; // Pixels to shift per instance
        int newX = (random.nextInt(5) * SHIFT_AMOUNT) % (int) (screenWidth - width);
        int newY = 100; // Fixed Y-position

        primaryStage.setX(newX);
        primaryStage.setY(newY);

        primaryStage.show();
        // end of CHAT GTP
    }

    public void matchmakingButtonPressed(){
        connectToSearchServer();   // Connect to the server
        hnadleB00Click();
        setUpLoadingScreen(primaryStage);
    }

    private void setUpLoadingScreen(Stage primaryStage) {
        gameIsActive = true;
        primaryStage.setTitle("MATCHMAKING LOADING...");
        // Main layout
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // "MATCHMAKING LOADING..." Message
        Label loadingMessage = new Label("MATCHMAKING LOADING...");

        // Add elements to layout
        root.getChildren().add(loadingMessage);

        // Scene setup
        Scene loadingScene = new Scene(root, width, height);
        primaryStage.setScene(loadingScene);
        primaryStage.show();
    }


    private void setUpGameScene(Stage primaryStage) {
        primaryStage.setTitle("Game - Player #" + playerID);
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        gameButtons.clear(); // Avoid duplicates

        message = new TextArea();
        message.setWrapText(true);
        message.setEditable(false);
        message.setMaxHeight(50);
        root.getChildren().add(message);

        // === Game Grid ===
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);


        if (gameMode.equals("tictactoe")) {
            practiceGameObj = new PracticeGameObj(false, new char[3][3],"test" );
            char[][] board = practiceGameObj.getBoard();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    Button cell = new Button();
                    cell.setPrefSize(60, 60);
                    cell.setStyle("-fx-font-size: 24px;");
                    gameButtons.add(cell);

                    int r = row;
                    int c = col;

                    cell.setText((board[r][c] == 'X' || board[r][c] == 'O') ? String.valueOf(board[r][c]) : "");

                    cell.setOnAction(e -> {
                        if (board[r][c] == '\0' && buttonsEnabled) {
                            char symbol = (playerID == 1) ? 'X' : 'O';
                            board[r][c] = symbol;
                            cell.setText(String.valueOf(symbol));
                            int cellNum = r * 3 + c + 1;
                            practiceGameObj.setInputString(String.valueOf(cellNum));
                            cscGS.sendPracticeGameObj();
                            buttonsEnabled = false;
                            toggleButtons();
                            new Thread(this::updateTurn).start();
                        }
                    });

                    grid.add(cell, c, r);
                }
            }
        } else if (gameMode.equals("connect4")) {
            practiceGameObj = new PracticeGameObj(false, new char[6][7], "connect4");

            root.setPadding(new Insets(15));
            root.setAlignment(Pos.CENTER);
            gameButtons.clear();

            message = new TextArea();
            message.setWrapText(true);
            message.setEditable(false);
            message.setMaxHeight(50);
            root.getChildren().add(message);

            // === Top Row: Drop Buttons ===
            HBox dropButtons = new HBox(10);
            dropButtons.setAlignment(Pos.CENTER);

            grid.setHgap(5);
            grid.setVgap(5);
            grid.setAlignment(Pos.CENTER);

            char[][] board = practiceGameObj.getBoard();

            // 7 Drop Buttons
            for (int col = 0; col < 7; col++) {
                int finalCol = col;
                Button drop = new Button("↓");
                drop.setPrefSize(40, 40);
                drop.setStyle("-fx-font-size: 16px;");
                drop.setOnAction(e -> {
                    if (!buttonsEnabled) return;

                    int rowToDrop = findAvailableRow(board, finalCol);
                    if (rowToDrop != -1) {
                        char symbol = (playerID == 1) ? 'X' : 'O';
                        board[rowToDrop][finalCol] = symbol;
                        practiceGameObj.setInputString(rowToDrop + "," + finalCol);
                    }
                });
                dropButtons.getChildren().add(drop);
            }
            root.getChildren().add(dropButtons);

            // === Game Grid ===
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    Button cell = new Button();
                    cell.setPrefSize(60, 60);
                    cell.setStyle("-fx-font-size: 20px;");
                    gameButtons.add(cell);
                    grid.add(cell, col, row);
                }
            }
            root.getChildren().add(grid);

            // === Chat Section (shared across game modes) ===
            Label chatLabel = new Label("Chat with opponent:");
            chatArea = new TextArea();
            chatArea.setEditable(false);
            chatArea.setWrapText(true);
            chatArea.setPrefHeight(150);

            TextField chatInput = new TextField();
            chatInput.setPromptText("Type your message...");
            chatInput.setPrefWidth(300);

            sendchat = new Button("Send");
            sendchat.setOnAction(e -> {
                String msg = chatInput.getText().trim();
                if (!msg.isEmpty()) {
                    String formatted = "player" + playerID + ": " + msg + "\n";
                    chatArea.appendText(formatted);
                    chatInput.clear();
                    if (cscGS != null) {
                        cscGS.sendChat(msg);
                    }
                }
            });

            HBox chatInputBox = new HBox(10, chatInput, sendchat);
            chatInputBox.setAlignment(Pos.CENTER);
            chatInputBox.setPadding(new Insets(10));
            root.getChildren().addAll(chatLabel, chatArea, chatInputBox);

            Scene scene = new Scene(root, 500, 600);
            primaryStage.setScene(scene);
            primaryStage.show();
        }

        root.getChildren().add(grid);

        // === Chat UI ===
        Label chatLabel = new Label("Chat with opponent:");
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(150);

        TextField chatInput = new TextField();
        chatInput.setPromptText("Type your message...");
        chatInput.setPrefWidth(300);

        sendchat = new Button("Send");
        sendchat.setOnAction(e -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty()) {
                String formatted = "player" + playerID + ": " + msg + "\n";
                chatArea.appendText(formatted);
                chatInput.clear();
                if (cscGS != null) {
                    cscGS.sendChat(msg);
                }
            }
        });

        HBox chatInputBox = new HBox(10, chatInput, sendchat);
        chatInputBox.setAlignment(Pos.CENTER);
        chatInputBox.setPadding(new Insets(10));

        root.getChildren().addAll(chatLabel, chatArea, chatInputBox);

        // === Final scene ===
        Scene scene = new Scene(root, 400, 550);
        primaryStage.setScene(scene);
        primaryStage.show();

        // === Turn setup ===
        if (playerID == 1) {
            message.setText("You are player 1, you go first");
            otherPlayerID = 2;
            buttonsEnabled = true;
        } else {
            message.setText("You are player 2, wait for your turn");
            otherPlayerID = 1;
            buttonsEnabled = false;
            new Thread(this::updateTurn).start();
        }
        toggleButtons();
    }

    private int findAvailableRow(char[][] board, int col) {
        for (int row = board.length - 1; row >= 0; row--) {
            if (board[row][col] == '\0') {
                return row;
            }
        }
        return -1; // No available row in this column
    }

    /**
     * Sets the onAction for each of the 9 game buttons.
     */
    private void setUpGameButtons() {
        b01.setOnAction(e -> handleButtonClick("1"));
        b02.setOnAction(e -> handleButtonClick("2"));
        b03.setOnAction(e -> handleButtonClick("3"));
        b04.setOnAction(e -> handleButtonClick("4"));
        b05.setOnAction(e -> handleButtonClick("5"));
        b06.setOnAction(e -> handleButtonClick("6"));
        b07.setOnAction(e -> handleButtonClick("7"));
        b08.setOnAction(e -> handleButtonClick("8"));
        b09.setOnAction(e -> handleButtonClick("9"));
    }

    /**
     * Called when a button (1-9) is clicked
     */
    private void hnadleB00Click(){
        connectToGameServer();
        if (cscGS != null) {
            setUpGameScene(primaryStage);
            //playerData.printPlayerData();
            //cscSS.sendPlayerData(playerData);
        }

    }

    private void handleButtonClick(String strBNum) {
        practiceGameObj.setInputString(strBNum);
        message.setText("You clicked button #" + practiceGameObj.getInputString() + " now wait for next player's turn");

        turnsMade++;
        System.out.println("Turns made: " + turnsMade);

        buttonsEnabled = false;
        toggleButtons();

        // Send button to server
        if (cscGS != null) {
            cscGS.sendPracticeGameObj();
        }

        // If P2 hits max turns, check winner
        if (playerID == 2 && turnsMade == maxTurns) {
            //checkWinner();
        } else {
            // Otherwise wait for the opponent
            new Thread(this::updateTurn).start();
        }
    }


    private void toggleButtons() {
        for (Button btn : gameButtons) {
            btn.setDisable(!buttonsEnabled);
        }
    }

    /**
     * Connect to the server
     */


    public void connectToSearchServer() {
        cscSS = new CscToSearchServer();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown detected. Closing Search Server connection...");
            closeSearchServerConnection();
        }));
    }
    public void connectToGameServer() {
        System.out.println("Connecting to Gserver...");
        cscGS = new CscToGameServer();
        System.out.println("Welcome to the game server");

        // 🔹 Add shutdown hook to close the connection when app stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown detected. Closing Game Server connection...");
            closeGameServerConnection();
        }));
    }
    private void closeGameServerConnection() {
        try {
            if (cscGS != null) {
                if (cscGS.getGameSocket() != null && !cscGS.getGameSocket().isClosed()) {
                    cscGS.getGameSocket().close();
                    System.out.println("Game socket closed.");
                }
                if (cscGS.getChatSocket() != null && !cscGS.getChatSocket().isClosed()) {
                    cscGS.getChatSocket().close();
                    System.out.println("Chat socket closed.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error closing sockets: " + e.getMessage());
        }
    }

    private void closeSearchServerConnection() {
        try {
            if (cscSS != null && cscSS.getSocket() != null && !cscSS.getSocket().isClosed()) {
                cscSS.getSocket().close();
                System.out.println("Search Server socket closed.");
            }
        } catch (IOException e) {
            System.out.println("Error closing Search Server socket: " + e.getMessage());
        }
    }
    private void updateBoardFromPracticeObj() {
        char[][] board = practiceGameObj.getBoard();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                Button cell = gameButtons.get(index);

                char val = board[row][col];
                cell.setText((val == '\0') ? "" : String.valueOf(val));
            }
        }
    }


    public void updateTurn() {
        cscGS.receivePracticeGameObj();

        // Run all UI updates on the JavaFX thread
        Platform.runLater(() -> {
            updateBoardFromPracticeObj(); // updates board GUI
            message.setText("Opponent clicked #" + practiceGameObj.getInputString() + ", your turn!");
            buttonsEnabled = true;
            toggleButtons();
        });
    }

    /**
     * The networking client side
     */
    private class CscToGameServer {
        private Socket gameSocket;
        private Socket chatSocket;

        // Game stream: for game objects
        private DataInputStream gameIn;
        private DataOutputStream gameOut;
        private ObjectOutputStream gameOutObj;
        private ObjectInputStream gameInObj;

        // Chat stream: for raw messages or chat objects
        private ObjectOutputStream chatOutObj;
        private ObjectInputStream chatInObj;

        public CscToGameServer() {
            System.out.println("Client side connection");
            try {
                gameSocket = new Socket("localhost", 30001);
                chatSocket = new Socket("localhost", 30002);
                gameIn = new DataInputStream(gameSocket.getInputStream());

                gameOutObj = new ObjectOutputStream(gameSocket.getOutputStream());
                gameInObj = new ObjectInputStream(gameSocket.getInputStream());

                chatOutObj = new ObjectOutputStream(chatSocket.getOutputStream());
                chatInObj = new ObjectInputStream(chatSocket.getInputStream());

                playerID = gameIn.readInt();
                System.out.println("Player ID: " + playerID);

                gameMode = gameIn.readUTF();
                System.out.println("gameMode: " + gameMode);

                if (Objects.equals(gameMode, "tictactoe")){


                } else if (Objects.equals(gameMode, "connect4")) {

                }

                startChatListener();

            } catch (IOException e) {
                System.out.println("IO exception from CSC constructor");
            }
        }

        private void startChatListener() {
            new Thread(() -> {
                while (true) {
                    try {
                        String receivedMsg = (String) chatInObj.readObject();
                        System.out.println("Received chat: " + receivedMsg);
                        if (playerID == 1){
                            otherPlayerID = 2;
                        }
                        else{
                            otherPlayerID = 1;
                        }
                        String formatted = "player" + otherPlayerID + ": " + receivedMsg + "\n";

                        Platform.runLater(() -> {
                            chatArea.appendText(formatted);
                        });

                        // If using JavaFX, update the UI with Platform.runLater(...)
                    } catch (IOException e) {
                        System.out.println("IO exception receiving chat message");
                        e.printStackTrace();
                        break;
                    } catch (ClassNotFoundException e) {
                        System.out.println("Class not found");
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void sendChat(String msg) {
            try {
                chatOutObj.writeObject(msg);
                chatOutObj.flush();
            } catch (IOException e) {
                System.out.println("IO exception sending chat message");
            }
        }

        public void sendPracticeGameObj(){
            try {
                gameOutObj.writeObject(practiceGameObj);
                gameOutObj.flush();
            } catch (IOException e) {
                System.out.println("Error sending practice game obj: ");
            }
        }

        public void receivePracticeGameObj(){
            try {
                System.out.println("Obj listening");
                Object tempObj = gameInObj.readObject(); // vague object gets "catched first_
                practiceGameObj = (PracticeGameObj) tempObj;
                System.out.println("reciver buttom num" + practiceGameObj.getInputString());

            } catch (IOException e){
                System.out.println("Error receiving practice game obj: ");
            } catch (ClassNotFoundException e) {
                System.out.println("object class not found");
            }
        }


        public void closeConnection() {
            try {
                gameInObj.close();
                System.out.println("Closing connection");
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection closeConnection");
            }
        }
        public Socket getChatSocket() {
            return chatSocket;
        }
        public Socket getGameSocket() {
            return gameSocket;
        }
    }

//------------CscToSearchServer-------------------CscToSearchServer-------------------CscToSearchServer---------------
    private class CscToSearchServer{
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private ObjectOutputStream objectDataOut;
        private ObjectInputStream objectDataIn;

        public CscToSearchServer() {
            System.out.println("Client side connection -- SearchServer");
            try {
                this.socket = new Socket("44.222.177.63", 30000);

                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());

                objectDataOut = new ObjectOutputStream(dataOut);
                objectDataIn = new ObjectInputStream(dataIn);

                //waitForGameServerPortThread();
                waitForGameServerIPThread();

            } catch (IOException e) {
                System.out.println("IO exception from CSC constructor");
            }
        }

        public void sendPlayerData(PlayerData playerData) {
            try {
                objectDataOut.writeObject(playerData);  // Write entire object
                objectDataOut.flush();
                System.out.print("PlayerData sent to server: ");
                playerData.printPlayerData();
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection sendPlayerData: " + e.getMessage());
            } catch (NullPointerException e) {
                System.out.println("NullPointerException in ClientSideConnection sendPlayerData: " + e.getMessage());
            }
        }

        private void waitForGameServerIPThread() {
            Thread portReceiver = new Thread(() -> {
                try {
                    // Block and wait until the port number arrives:
                    System.out.println("GameServer:::");
                    gameServerIP = dataIn.readUTF();
                    System.out.println("Received GameServer IP Address: " + gameServerIP);
                    if(!Objects.equals(gameServerIP, "na")) {
                        closeConnection();
                        connectToGameServer();

                        // Chatgtp said u can so primary stage unless its with this
                        // Platform.runLater javaFX special thread
                        Platform.runLater(() -> {
                            if (primaryStage != null) {
                                setUpGameScene(primaryStage);
                            }
                        });
                    }

                } catch (IOException e) {
                    System.out.println("Error receiving port: " + e.getMessage());
                }
            });
            portReceiver.start();
        }



        public void closeConnection() {
            try {
                socket.close();
                System.out.println("Closing connection");
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection closeConnection");
            }
        }

        public Socket getSocket() {
            return socket;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}