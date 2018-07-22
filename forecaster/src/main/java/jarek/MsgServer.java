package jarek;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

public class MsgServer {
  public static void main(String[] args) throws Exception {
    new MsgServer().go(args);
  }

  private void go(String[] args) throws Exception {
    runServer(Integer.parseInt(args[0]));
  }

  private void runServer(int port) throws Exception {
    ServerSocket serverSocket = new ServerSocket(port);
    while (true) {
      final Socket clientSocket = serverSocket.accept();
      if (clientSocket != null) {
        System.out.println("client connected");
        new Thread(new Runnable() {
          @Override
          public void run() {
            serveSocket(clientSocket);
          }
        }).start();
      }
    }
  }

  private void serveSocket(Socket clientSocket) {
    try {
      InputStream istr = clientSocket.getInputStream();
      OutputStream ostr = clientSocket.getOutputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(istr, "UTF-8"));
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(ostr, "UTF-8"));
      while (true) {
        String line = br.readLine();
        if (line == null) {
          System.out.println("Connection closed.");
          break;
        }
        if (line.equals("exit")) {
          br.close();
          break;
        }
        if (line.equals("quit")) {
          System.exit(0);
        }
        if (line.startsWith("GET / HTTP")) {
          //Thread.sleep(100);
          bw.write("HTTP/1.1 200 wporzo\n");
          //bw.write("Content-Length: 5\n");
          bw.write("\n");
          bw.write("hello at " + Calendar.getInstance().getTime() + "\n");
          for(int i = 1; i <= 3; i++) {
            Thread.sleep(1000);
            bw.write("" + i + "\n");
            bw.flush();
          }
          bw.flush();
          bw.close();
        }
        System.out.println("received: " + line);
      }
    } catch (Exception e) {
      System.out.println("Client socket finished with exception: " + e);
    }
  }
}
