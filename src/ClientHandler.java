import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable{
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>(); //static nes norim kad arraylist priklausytu klasej o ne kiekvienam klases objektui
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private static Map<String, List<ClientHandler>> rooms = new HashMap<>();
    private String currentRoom = null;


    public ClientHandler(Socket socket) {
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this);
            broadcastMessage("SERVER: " + clientUsername + " prisijungė prie pokalbio!");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override public void run(){
        String messageFromClient;
        while(socket.isConnected()){
            try{
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                String[] parts = messageFromClient.split(": ", 2);
                String username = parts[0];
                String message = parts.length > 1 ? parts[1] : "";

                if (message.startsWith("@")) {
                    String targetUsername = message.split(" ")[0].substring(1);
                    String privateMessage = message.substring(targetUsername.length() + 2).trim();
                    sendPrivateMessage(targetUsername, privateMessage);
                    File.addMessageToFile("Privati: " + privateMessage);
                } else if (message.startsWith("/create ")) {
                    String roomName = message.substring(8).trim();
                    if (!roomName.isEmpty()) {
                        createRoom(roomName);
                        broadcastToAll("SERVER: " + username + " sukūrė kambarį '" + roomName + "'");
                        File.addMessageToFile("SERVERIO VEIKLA: SERVER: " + username + " sukūrė kambarį '" + roomName + "'");
                    }
                } else if (message.startsWith("/join ")) {
                    String roomName = message.substring(6).trim();
                    if (rooms.containsKey(roomName)) {
                        joinRoom(roomName);
                        sendMessageToClient("SERVER: Prisijungei prie kambario '" + roomName + "'");
                        File.addMessageToFile("SERVERIO VEIKLA: SERVER: Prisijungei prie kambario '" + roomName + "'");
                    } else {
                        sendMessageToClient("SERVER: Toks kambarys neegzistuoja.");
                        File.addMessageToFile("SERVERIO VEIKLA: SERVER: Toks kambarys neegzistuoja.");
                    }
                }else {
                    broadcastMessage(username + ": " + message);
                    File.addMessageToFile(username + ": " + message);

                }
            } catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void sendPrivateMessage(String targetUsername, String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.clientUsername.equals(targetUsername)) {
                try {
                    clientHandler.bufferedWriter.write("PRIVATI žinutė iš " + clientUsername + ": " + message);
                    File.addMessageToFile("PRIVATI žinutė iš " + clientUsername + ": " + message);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    return;
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
        sendMessageToClient("SERVER: Toks vartotojas neegzistuoja.");
        File.addMessageToFile("SERVER: Toks vartotojas neegzistuoja.");
    }

    private void sendMessageToClient(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void joinRoom(String roomName) {
        if (currentRoom != null) {
            rooms.get(currentRoom).remove(this);
        }
        currentRoom = roomName;
        rooms.get(roomName).add(this);
    }

    public void broadcastMessage(String messageToSend){
        if (currentRoom != null) {
            broadcastMessageToRoom(messageToSend);
            return;
        }
        for(ClientHandler clientHandler : clientHandlers){
            try{
                if(!clientHandler.clientUsername.equals(clientUsername)){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void broadcastToAll(String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.bufferedWriter.write(message);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }
    public void broadcastMessageToRoom(String messageToSend){
        if (currentRoom == null) {
            return;
        }
        List<ClientHandler> roomClients = rooms.get(currentRoom);
        for(ClientHandler clientHandler : roomClients){
            try{
                if(!clientHandler.clientUsername.equals(clientUsername)){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void createRoom(String roomName) {
        if (!rooms.containsKey(roomName)) {
            rooms.put(roomName, new ArrayList<>());
        }
        if (currentRoom != null) {
            rooms.get(currentRoom).remove(this);
        }
        rooms.get(roomName).add(this);
        currentRoom = roomName;
        List<String> usersInRoom = new ArrayList<>();
        for (ClientHandler client : rooms.get(roomName)) {
            usersInRoom.add(client.getClientUsername());
        }
        File.addRoomToFile(roomName, usersInRoom);
        try {
            bufferedWriter.write("SERVER: Sukūrėte ir prisijungėte prie kambario '" + roomName + "'");
            File.addMessageToFile("SERVERIO VEIKLA: SERVER: Sukūrėte ir prisijungėte prie kambario '" + roomName + "'");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }


    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " paliko pokalbį!!");
    }

    public String getClientUsername() {return clientUsername;}

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
        try{
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(socket != null){
                socket.close();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}