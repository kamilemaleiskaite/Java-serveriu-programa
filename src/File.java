import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class File {
    private static final String USERS_FILE = "users.txt";
    private static final String MESSAGES_FILE = "messages.txt";
    private static final String ROOMS_FILE = "rooms.txt";

    public static void addUserToFile(String username) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void addMessageToFile(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MESSAGES_FILE, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addRoomToFile(String roomName, List<String> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ROOMS_FILE, true))) {
            writer.write("Room: " + roomName);
            writer.newLine();
            for (String user : users) {
                writer.write(user);
                writer.newLine();
            }
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
