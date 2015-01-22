package com.tiwence.cinenow.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by temarill on 16/01/2015.
 */
public class HttpUtils {

    public static String httpGet(String url) {
        StringBuilder response = new StringBuilder();
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse httpResponse = httpClient.execute(new HttpGet(url));
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                InputStream content = httpResponse.getEntity().getContent();
                BufferedReader br = new BufferedReader(new InputStreamReader(content));
                String line;
                while((line = br.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

}
