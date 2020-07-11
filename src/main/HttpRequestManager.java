package main;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpRequestManager {

    private static final String REQUEST_DELIMITER = "------------------------------------------------------------------";

    public static void main(String[] args) {
        new HttpRequestManager().executeCommand();
    }

    public String executeCommand() {
        StringBuilder responseBody = new StringBuilder();
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        List<HttpRequest> httpRequests = generateRequests();
        httpRequests.stream().forEach(req -> {
            try {
                HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                responseBody.append(res.body()).append(REQUEST_DELIMITER);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        return responseBody.toString();
    }


    private List<HttpRequest> generateRequests() {
        List<String> requestContents = parseRequestFile();
        return requestContents.stream().map(this::extractHeadersAndParameters).collect(Collectors.toList());
    }

    private HttpRequest extractHeadersAndParameters(String s) {
        HttpRequest httpRequest = null;
        List<String> lines = s.lines().filter(line -> !line.isEmpty() && !line.isBlank()).collect(Collectors.toList());
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
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).uri(URI.create(uri)).build();
                break;
        }
        headers.forEach(requestBuilder::header);
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
