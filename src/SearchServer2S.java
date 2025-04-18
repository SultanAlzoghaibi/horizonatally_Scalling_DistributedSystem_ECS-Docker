import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// AWS
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ecs.model.*;



import software.amazon.awssdk.services.ecs.model.*;
import software.amazon.awssdk.services.ecs.model.*;
// EC2 SDK (to get public IP from ENI)

import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.*;

// AWS region
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.EcsClient;


public class SearchServer2S {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private ServerSocket ss;
    private int numPlayers;
    private ServerSideConnection player1;
    // these a the server side equilivant to the package drop off points
    private ServerSideConnection player2;
    private int turnsMade;
    private int maxTurns;
    private ArrayList<SscPlayerData> sscPlayerDataArrayList = new ArrayList<>();
    Map<String, String> ipToTaskArn = new HashMap<>();

    private char[][] server2dChar;
    private ArrayList<PlayerData> playerDataArrayList = new ArrayList<>();
    private PlayerData tempPlayerData;
    private int portNumIncrement;
    private HashMap<String, ArrayList<SscPlayerData>> gameQueues = new HashMap<>();
    private HashMap<String, Queue<String>> gameServerIpQueues = new HashMap<>();

    private char[] gameBoard;
    private int WARMPOOLINGSIZE = 2;

    public SearchServer2S() {
        System.out.println("--search server--");
        numPlayers = 0;
        portNumIncrement = 0;



        gameServerIpQueues.put("chess", new LinkedList<>()); // Queues will be used
        gameServerIpQueues.put("tictactoe", new LinkedList<>()); // Queues will be used
        gameServerIpQueues.put("connect4", new LinkedList<>()); // Queues will be used
        gameServerIpQueues.put("checkers", new LinkedList<>()); // Queues will be used


        gameQueues.put("chess", new ArrayList<SscPlayerData>() );
        gameQueues.put("connect4", new ArrayList<SscPlayerData>() );
        gameQueues.put("tictactoe", new ArrayList<SscPlayerData>() );
        gameQueues.put("checkers", new ArrayList<SscPlayerData>() );

        System.out.println("gameQueues:" + gameQueues.toString());

        for (String gameMode : gameServerIpQueues.keySet()) {
            for (int i = 0; i < WARMPOOLINGSIZE; i++) {
                gameServerIpQueues.get(gameMode).add(launchGameServerOnECS(gameMode));
            }
        }
        // Every N minutes, clean up idle warm pool
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            for (String gameMode : gameServerIpQueues.keySet()) {
                Queue<String> pool = gameServerIpQueues.get(gameMode);
                System.out.println(gameMode + ": being chekced for too big warmpool! ♨ ");
               if(pool.size() == WARMPOOLINGSIZE + 2 ){
                    while (pool.size() > WARMPOOLINGSIZE) {
                        String unusedIp = pool.poll();
                        if (unusedIp != null) {
                            String taskARN = ipToTaskArn.get(unusedIp);
                            terminateGameServerOnECS(taskARN); // Stop ECS task
                            System.out.println("Terminated unused warm instance: " + unusedIp);
                        }
                    }
               }
            }
        }, 1, 1, TimeUnit.MINUTES);


        try{
            ss = new ServerSocket(30000);
        } catch(IOException e){
            System.err.println("Port 30000 already in use, pick another port or close the running instance.");
            e.printStackTrace();
            System.exit(1);
        }
        // From chatGTP as a way to close the socket if inteliji/Java does nto do it automatically
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(ss != null && !ss.isClosed()){
                    ss.close();
                    System.out.println("Server socket closed gracefully.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        // End of chatGTP
    }

    public void terminateGameServerOnECS(String taskArn) {
        try (EcsClient ecsClient = EcsClient.create()) {
            StopTaskRequest stopTaskRequest = StopTaskRequest.builder()
                    .task(taskArn)
                    .cluster("h-scalling") // Make sure this matches your actual cluster name
                    .reason("Scaling down warm pool")
                    .build();

            StopTaskResponse response = ecsClient.stopTask(stopTaskRequest);
            System.out.println("Successfully stopped task: " + taskArn);
        } catch (Exception e) {
            System.out.println("Failed to stop ECS task: " + taskArn);
            e.printStackTrace();
        }
    }

    public String getPublicIpFromTaskArn(String taskArn) {

        EcsClient ecsClient = EcsClient.builder()
                .region(Region.US_EAST_1)
                .build();

        Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        String eniId = null;

        // Wait for ENI to be attached (max 60s)
        long startTime = System.nanoTime();
        //System.out.print("ENI API call time: ");
        for (int i = 0; i < 600; i++) {
            try {
                DescribeTasksRequest describeRequest = DescribeTasksRequest.builder()
                        .cluster("h-scalling")
                        .tasks(taskArn)
                        .build();

                DescribeTasksResponse describeResponse = ecsClient.describeTasks(describeRequest);

                List<Attachment> attachments = describeResponse.tasks().get(0).attachments();
                if (!attachments.isEmpty()) {
                    eniId = attachments.stream()
                            .filter(a -> a.type().equals("ElasticNetworkInterface"))
                            .flatMap(a -> a.details().stream())
                            .filter(d -> d.name().equals("networkInterfaceId"))
                            .map(KeyValuePair::value)
                            .findFirst()
                            .orElse(null);

                    if (eniId != null) {
                        long endTime = System.nanoTime();
                        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
                        //System.out.printf("%.2f", durationSeconds);
                        //System.out.println();
                        //System.out.println("🔍 ENI ID found: " + eniId);
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error while fetching ENI: ");
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("int");
            }
        }

        if (eniId == null) {
            System.out.println("❌ ENI ID not found after timeout.");
            return null;
        }

        // ✅ Now wait for public IP to be available on that ENI (max 30s)
        //System.out.print("⌛ Waiting for public IP: ");
        startTime = System.nanoTime();

        for (int i = 0; i < 300; i++) {
            try {
                DescribeNetworkInterfacesResponse eniResponse = ec2Client.describeNetworkInterfaces(
                        DescribeNetworkInterfacesRequest.builder()
                                .networkInterfaceIds(eniId)
                                .build()
                );

                NetworkInterfaceAssociation assoc = eniResponse.networkInterfaces().get(0).association();
                if (assoc != null && assoc.publicIp() != null) {
                    String publicIp = assoc.publicIp();
                    long endTime = System.nanoTime();
                    double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
                   // System.out.printf("%.2f", durationSeconds);
                    //System.out.println();

                    //System.out.println("🌐 Public IP found: " + publicIp);
                    ipToTaskArn.put(publicIp, taskArn);

                    return publicIp;
                } else {

                }
            } catch (Exception e) {
                System.out.println("❌ Error checking for public IP: ");
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("int");
            }
        }

        System.out.println("❌ Public IP not associated after timeout.");
        return null;
    }


    public String launchGameServerOnECS(String gameMode) {
        try {
            // 1. Create ECS client
            long startTime = System.nanoTime();

            EcsClient ecsClient = EcsClient.builder()
                    .region(Region.US_EAST_1)
                    .build();

            // 2. VPC Networking config
            AwsVpcConfiguration vpcConfig = AwsVpcConfiguration.builder()
                    .subnets("subnet-0a3c6f71109e9e394")
                    .securityGroups("sg-08e5d65f17952b574")
                    .assignPublicIp(AssignPublicIp.ENABLED)
                    .build();

            NetworkConfiguration networkConfig = NetworkConfiguration.builder()
                    .awsvpcConfiguration(vpcConfig)
                    .build();

            // 3. Container override
            ContainerOverride override = ContainerOverride.builder()
                    .name("GameServerC") // MUST match your container name in ECS
                    .command(gameMode)
                    .build();

            TaskOverride taskOverride = TaskOverride.builder()
                    .containerOverrides(override)
                    .build();

            // 4. Launch ECS task
            RunTaskRequest request = RunTaskRequest.builder()
                    .cluster("h-scalling")
                    .launchType(LaunchType.FARGATE)
                    .taskDefinition("gameserver-task")
                    .networkConfiguration(networkConfig)
                    .overrides(taskOverride)
                    .build();

            RunTaskResponse response = ecsClient.runTask(request);

            if (!response.failures().isEmpty()) {
                System.out.println("🚨 Failed to run ECS task: " + response.failures());
                return null;
            }

            String taskArn = response.tasks().get(0).taskArn();
            System.out.println("✅ GameServer task launched: " + taskArn);


            long endTime = System.nanoTime();
            double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
            //System.out.print("launchGameServerOnECS Time: ");
            //System.out.printf("%.2f", durationSeconds);
            //System.out.println();

            return getPublicIpFromTaskArn(taskArn); // now handles ENI wait/retry internally

        } catch (EcsException e) {
            System.out.println("❌ AWS ECS Error: ");
            e.printStackTrace();
        }
        return null;
    }

    class SscPlayerData {
        PlayerData playerData;
        ServerSideConnection ssc;

        public SscPlayerData(PlayerData playerData, Runnable connection) {
            this.playerData = playerData;
            this.ssc = (ServerSideConnection) connection;
        }

        public void setPlayerData(PlayerData playerData) {
            this.playerData = playerData;
        }
        public PlayerData getPlayerData() {
            return playerData;
        }
        public ServerSideConnection getSsc() {
            return ssc;
        }
        public void setSsc(ServerSideConnection ssc) {
            this.ssc = ssc;
        }

        public void printConnectionDetails() {
            System.out.print("PlayerData: ");
            System.out.print("User ID: " + playerData.getUserId() + ", ");
            System.out.print("Username: " + playerData.getUsername() + ", ");
            playerData.printPlayerData();

            System.out.print(" || Connection: ");
            System.out.print("Player ID: " + ssc.getPlayerID() + ", ");
            System.out.println("Socket: " + ssc.getSocketPort());
        }



    }


    public void acceptConnections(){

        try {
            System.out.println("waiting for connections");

            while (numPlayers < 10000) {
                Socket s = ss.accept();
                numPlayers++;
                System.out.println("webSocketNotes.Player #" + numPlayers + " has joined the game");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);

                Thread t = new Thread(ssc); //what ever is the in the ssc run in the new "THREAD"
                t.start();
            }

        }catch(IOException e){
            System.out.println("IOException from game server acceptConnections");
        }
    }




    public class ServerSideConnection implements Runnable{
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private int playerID;
        private ObjectOutputStream dataOutObj;
        private ObjectInputStream dataInObj;


        public ServerSideConnection(Socket s, int id){
            socket = s;
            playerID = id;


            try {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());

                dataOutObj = new ObjectOutputStream(dataOut);
                dataInObj = new ObjectInputStream(dataIn);

                System.out.println("Connected to Search Server on port: " + s.getPort());

            } catch (IOException e) {
                System.out.println("IOException from game server constructor: ServerSideConnection");
            }
        }
        public void run() {

                        try {
                            // ISSUE, 1 thread per person can be inefeiccient
                            tempPlayerData = (PlayerData) dataInObj.readObject();

                            tempPlayerData.setUserId(playerID);
                            System.out.print("receice player data: ");
                            SscPlayerData tempsscPlayerData = new SscPlayerData(tempPlayerData, this);

                            tempsscPlayerData.printConnectionDetails();
                            System.out.print("Before gameQueues:");
                            printGameQueues();
                            String gameMode = tempsscPlayerData.playerData.getGameModeInterested();

                            synchronized (gameQueues.get(gameMode)) {
                                gameQueues.get(gameMode).add(tempsscPlayerData);
                            }

                            System.out.print("After GameQueues: ");
                            printGameQueues();


                            Thread t = new Thread(() -> {
                                try {
                                    System.out.println("gameModeMatchMakingToElo(" + gameMode + ": sscPDArraylist)");
                                    Thread.sleep(1);
                                    gameModeMatchMakingToElo(gameMode);
                                    //matchMakingToElo();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });
                            t.start();

                        } catch (Exception e) {
                            System.out.println("ClassNotFoundException: " + e.getMessage());
                            ; //  Break the loop if an error occurs to avoid spamming
                            System.out.println("IOException from run() : ServerSideConnection " + e.getMessage());
            } finally {
                //closeConnection(); //  Ensure the socket is closed properly
            }
        }

        public void gameModeMatchMakingToElo(String gameMode) {
            synchronized (gameQueues.get(gameMode)) {
                ArrayList<SscPlayerData> sscPDArraylist = gameQueues.get(gameMode);

                if (sscPDArraylist.size() >= 2) {
                    System.out.println("sscPlayerDataArrayList >= 2");
                    SscPlayerData player1 = sscPDArraylist.removeFirst();
                    SscPlayerData player2 = sscPDArraylist.removeFirst();


                    System.out.println("IP ADRESS");
                    long startTime = System.nanoTime();
                    // for deguggin later
                    String ipAddress = gameServerIpQueues.get(gameMode).poll();
                    System.out.println("gamode : " + gameMode + " has a queue size or" + gameServerIpQueues.get(gameMode).size());

                    Thread t = new Thread(() -> {
                        System.out.println("thread of adding to queue");
                        if (gameServerIpQueues.get(gameMode).isEmpty()) {
                            gameServerIpQueues.get(gameMode).add(launchGameServerOnECS(gameMode));
                            gameServerIpQueues.get(gameMode).add(launchGameServerOnECS(gameMode));
                        }
                        gameServerIpQueues.get(gameMode).add(launchGameServerOnECS(gameMode));
                    });
                    t.start();

                    //String ipAddress = launchGameServerOnECS(gameMode);

                    long endTime = System.nanoTime();
                    double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
                    //System.out.printf("⏱️ TOTAL Time to get public IP: %.2f seconds: ", durationSeconds);
                    // System.out.println();
                    //System.out.println("🌐TOTAL Public IP of GameServer: " + ipAddress);

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    player1.getSsc().sendIPAddress(ipAddress);
                    player2.getSsc().sendIPAddress(ipAddress);
                    System.out.println("sent them ip: " + ipAddress);
                    //System.exit(0);
                }

            }
        }
        // i ASK CHATGTP a bit here, AS im not familar with the aws and
        // AWS SDK Java API docs i svery big and tentious when I tried.


        public void printGameQueues() {
            System.out.print("{");
            for (String gameQueue : gameQueues.keySet()) {
                System.out.print(gameQueue + ": " + gameQueues.get(gameQueue).size() + "|");
            }
            System.out.print("}");
        }


        public void sendIPAddress(String ip){

            try {
                dataOut.writeUTF(ip);
                dataOut.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private int getPlayerID(){
            return playerID;
        }

        private int getSocketPort(){
            return socket.getLocalPort();
        }
    }

    public void closeServer() {
        try {
            if (ss != null && !ss.isClosed()) {
                ss.close();
                System.out.println("Server socket closed.");
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        SearchServer2S searchS = new SearchServer2S();
        //searchS.startMatchMakingThreads(); //NOT tickrate


        // Add shutdown hook to ensure the server socket closes when stopped
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown detected. Closing server socket...");
            searchS.closeServer();
        }));

        try {
            searchS.acceptConnections();
        } catch (Exception e) {
            System.out.println("Server shutting down...");
        } finally {
            searchS.closeServer();
        }
    }
}
