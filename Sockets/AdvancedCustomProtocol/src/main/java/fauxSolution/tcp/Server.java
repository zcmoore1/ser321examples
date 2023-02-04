package fauxSolution.tcp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Random;

import javax.imageio.ImageIO;

import org.json.*;

public class Server {
  /*
   * request: { "selected": <int: 1=greeting, 2=quote, 3=image, 4=random> }
   * 
   * response: {"datatype": <int: 1-string, 2-byte array>, "type": <"joke",
   * "quote", "image">, "data": <thing to return> }
   * 
   * error response: {"error": <error string> }
   */

  public static JSONObject greeting(String name) {
    JSONObject json = new JSONObject();
    json.put("datatype", 1);
    json.put("type", "greeting");
    json.put("data", ("Hello " + name + ". Start a game by submitting 1 - See leaderboards by submitting 2" ));
    return json;
  }

  public static JSONObject leaderboard() {
    JSONObject json = new JSONObject();
    json.put("datatype", 10);
    json.put("type", "quote");
    json.put("data",
        "A good programmer is someone who always looks both ways before crossing a one-way street. (Doug Linder)");
    return json;
  }

  public static JSONObject image() throws IOException {
    JSONObject json = new JSONObject();
    json.put("datatype", 2);

    json.put("type", "image");

    File file = new File("img/To-Funny-For-Words1.png");
    if (!file.exists()) {
      System.err.println("Cannot find file: " + file.getAbsolutePath());
      System.exit(-1);
    }
    // Read in image
    BufferedImage img = ImageIO.read(file);
    byte[] bytes = null;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(img, "png", out);
      bytes = out.toByteArray();
    }
    if (bytes != null) {
      Base64.Encoder encoder = Base64.getEncoder();
      json.put("data", encoder.encodeToString(bytes));
      return json;
    }
    return error("Unable to save image to byte array");
  }

  public static JSONObject random() throws IOException {
    Random rand = new Random();
    int random = rand.nextInt(3);
    JSONObject json = new JSONObject();
    if (random == 0) {
      json = greeting("n");
    } else if (random == 1) {
      json = leaderboard();
    } else if (random == 2) {
      json = image();
    }
    return json;
  }

  public static JSONObject error(String err) {
    JSONObject json = new JSONObject();
    json.put("error", err);
    return json;
  }

  public static void main(String[] args) throws IOException {
    ServerSocket serv = null;
    try {
      serv = new ServerSocket(9000);
      // NOTE: SINGLE-THREADED, only one connection at a time
      while (true) {
        Socket sock = null;
        String name = null;
        try {
          sock = serv.accept(); // blocking wait
          System.out.println("Connection estabilished");
          OutputStream out = sock.getOutputStream();
          InputStream in = sock.getInputStream();
          
          JSONObject test = new JSONObject();
          test.put("datatype", 1);
          test.put("type", "reply");
          test.put("data", "Connected. Please submit your name: ");
          NetworkUtils.Send(out, JsonUtils.toByteArray(test));
          
          while (true) {
            byte[] messageBytes = NetworkUtils.Receive(in);
            JSONObject message = JsonUtils.fromByteArray(messageBytes);
            JSONObject returnMessage;
            
            
            if (message.has("selected")) {
              if (message.get("selected") instanceof Long || message.get("selected") instanceof Integer) {
                int choice = message.getInt("selected");
                switch (choice) {
                case (1):
                  String nam = message.getString("name");
                  returnMessage = greeting(nam);
                  break;
                case (3):
                  returnMessage = image();
                  break;
                case (2):
                  returnMessage = leaderboard();
                  break;
                case (4):
                  returnMessage = random();
                  break;
                default:
                  returnMessage = error("Invalid selection: " + choice + " is not an option");
                }
              } else {
                returnMessage = error("Selection must be an integer");
              } 
            } else {
              returnMessage = error("Invalid message received");
            }

            // we are converting the JSON object we have to a byte[]
            byte[] output = JsonUtils.toByteArray(returnMessage);
            NetworkUtils.Send(out, output);
          }
        } catch (Exception e) {
          System.out.println("Client disconnect");
        } finally {
          if (sock != null) {
            sock.close();
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (serv != null) {
        serv.close();
      }
    }
  }
}