package main;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class HttpRequestManager {

    private static final String REQUEST_DELIMITER = "------------------------------------------------------------------";
    private String jSessionID;

    public static void main(String[] args) {
        String result = new HttpRequestManager().executeCommand();
        System.out.println(result);
    }

    public String executeCommand() {
        List<String> requestContents = parseRequestFile();
        StringBuilder responseBody = new StringBuilder();
        List<String> responses = requestContents.stream().map(this::sendRequest).collect(Collectors.toList());
        System.out.println(responses);
        return responseBody.toString();
    }

    private String sendRequest(String requestContent) {
        HttpRequest req = generateRequest(requestContent);
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(40)).build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, List<String>> headerMap = res.headers().map();
            if (headerMap.containsKey("set-cookie")&& jSessionID==null) {
                String cookie = headerMap.get("set-cookie").get(0);
                String[] split = cookie.split(";");
                jSessionID = cookie;
            }
            return res.body();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

    private HttpRequest generateRequest(String requestContent) {
        HttpRequest httpRequest = null;
        List<String> lines = requestContent.lines()
                .filter(line -> !line.isEmpty() && !line.isBlank()).collect(Collectors.toList());
        String firstLine = lines.get(0);
        lines.remove(firstLine);
        String[] requestCommand = firstLine.split(" ");
        String httpMethod = requestCommand[0];
        String uri = requestCommand[1];
        String body = null;
        if (httpMethod.equalsIgnoreCase("POST")) {
            body = lines.get(lines.size() - 1);
            lines.remove(body);
        }
        Map<String, String> headers = new HashMap<>();
        lines.forEach(string -> string.lines().forEach(line -> {
            String[] header = line.split(":");
            headers.put(header[0], header[1]);
        }));
        httpRequest = buildHttpRequest(httpMethod, uri, headers, body);
        return httpRequest;
    }


    private HttpRequest buildHttpRequest(String httpRequest, String uri, Map<String, String> headers, String body) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        switch (httpRequest) {
            case "GET":
                requestBuilder.GET().uri(URI.create(uri));
                break;
            case "POST":
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body))
                        .uri(URI.create(uri)).build();
                break;
        }
        if (headers.containsKey("Cookie")) {
            headers.remove("Cookie");
        }
        headers.forEach(requestBuilder::header);
        if (jSessionID != null) {
            String[] split = jSessionID.split("=");
            requestBuilder.header("Cookie", jSessionID);
        }
        return requestBuilder.build();
    }

    private List<String> parseRequestFile() {
        try {
            File file = new File("src/TestRequests.txt");
            String content = Files.readString(Paths.get(file.getPath()));
            String[] split = content.split(REQUEST_DELIMITER);
            return Arrays.asList(split);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


}
