//package net.runelite.client.plugins.rcPlusBloods;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//public class SlackUtils {
//    private static String slackToken = "";
//
//    public static void sendMessage(SlackMessage message) {
//        getStringFromFile();
//        CloseableHttpClient client = HttpClients.createDefault();
//        String slackWebhookUrl = "https://hooks.slack.com/services/";
//        HttpPost httpPost = new HttpPost(slackWebhookUrl + slackToken);
//
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            String json = objectMapper.writeValueAsString(message);
//
//            StringEntity entity = new StringEntity(json);
//            httpPost.setEntity(entity);
//            httpPost.setHeader("Accept", "application/json");
//            httpPost.setHeader("Content-type", "application/json");
//
//            client.execute(httpPost);
//            client.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void getStringFromFile() {
//        try {
//            slackToken = new String(Files.readAllBytes(Paths.get("slackToken.env")), StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}