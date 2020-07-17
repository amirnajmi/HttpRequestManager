package main;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
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
    private HttpClient httpClient;

    public static void main(String[] args) {
        List<String> strings = new HttpRequestManager().parseRequestFile();
        String firstReq = strings.get(0);
        List<String> collect = firstReq.lines().collect(Collectors.toList());
        String body = collect.get(collect.size() - 1);
        collect.remove(body);
        StringBuilder stringBuilder = new StringBuilder();
        collect.forEach(s -> {
            stringBuilder.append(s).append(System.lineSeparator());
        });
        String result = null;
        try {
            result = new HttpRequestManager().executeCommand(stringBuilder.toString(), body);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(result);
    }

    public String executeCommand(String headerContent, String body) throws IOException, InterruptedException {
        CookieHandler.setDefault(new CookieManager());
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(40))
                .cookieHandler(CookieHandler.getDefault()).build();
        return sendRequest(headerContent, body);

    }

    private String sendRequest(String headersContent, String body) throws IOException, InterruptedException {

        HttpRequest req = generateRequest(headersContent, body);
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return res.body();
    }

    private HttpRequest generateRequest(String headersContent, String body) {
        HttpRequest httpRequest = null;
        List<String> lines = headersContent.lines()
                .filter(line -> !line.isEmpty() && !line.isBlank()).collect(Collectors.toList());
        String firstLine = lines.get(0);
        lines.remove(firstLine);
        String[] requestCommand = firstLine.split(" ");
        String httpMethod = requestCommand[0];
        String uri = requestCommand[1];
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
