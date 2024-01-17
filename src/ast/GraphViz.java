package ast;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Stack;

public class GraphViz {
    private FileWriter file;
    public int lastNode = -1;
    public Stack<Integer> buffer;
    public GraphViz(String filename) {
        try {
            file = new FileWriter(filename + ".dot");
            file.write("graph \"\"\n" +
                    "\t{\n" +
                    "\t\tfontname=\"Helvetica,Arial,sans-serif\"\n" +
                    "\t\tnode [fontname=\"Helvetica bold\"]\n" +
                    "\t\tedge [fontname=\"Helvetica,Arial,sans-serif\"]\n" +
                    "\t\t{\n" +
                    "\t\t\tlabel=\"AST\"\n");
            buffer = new Stack<>();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
   }

    public int addNode(String node, boolean isLeaf) {
        try {
            lastNode++;
            file.write("\t\t\tnode" + lastNode + ";\n");
            file.write("\t\t\tnode" + lastNode + " [label=\"" + node + "\" shape=" + (isLeaf ? "plaintext" : "egg") + " fontcolor=" + (isLeaf ? "mediumseagreen" : "black") + (!isLeaf ? " style=filled fillcolor=antiquewhite" : "") + "];\n");
            return lastNode;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
        return -1;
    }

    public void addEdge(int node1, int node2) {
        try {
            file.write("\t\t\tnode" + node1 + " -- node" + node2 + ";\n");
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }




    public void close() {
        try {
            file.write("\t\t}\n" +
                    "\t}\n");
            file.flush();
            file.close();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    public void export() {
        try {
            String graph = new String(Files.readAllBytes(Paths.get("ast.dot")));
            String layout = "dot";
            String format = "png";

            String url = "https://quickchart.io/graphviz";
            String body = "{\"graph\": \"" + graph.replaceAll("\"", "\\\\\"") + "\", \"layout\": \"" + layout + "\", \"format\": \"" + format + "\"}";


            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // Paramètres de la requête
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");

            // Envoi des données
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("An error occurred while sending data: " + e.getMessage());
            }

            // Lecture de la réponse
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream in = new BufferedInputStream(con.getInputStream());
                     FileOutputStream fos = new FileOutputStream("output.png")) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    System.out.println("PNG saved to output.png");
                }
            } else {
                try (InputStream errorStream = con.getErrorStream()) {
                    if (errorStream != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                            String line;
                            StringBuilder errorResponse = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                errorResponse.append(line);
                            }
                            System.out.println("Error response from server: " + errorResponse.toString());
                        }
                    }
                }
                System.out.println("Server returned HTTP response code: " + responseCode);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

}
