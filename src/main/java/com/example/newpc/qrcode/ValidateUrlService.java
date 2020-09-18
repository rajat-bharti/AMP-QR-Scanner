package com.example.newpc.qrcode;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created to query virustotal.
 */

public class ValidateUrlService extends IntentService {

    public final static String LINK_VALIDATE = "link";
    private final static String USER_AGENT= "appcode";
    private String scan_id;
    public final static String SCAN_ID = "scan_id";
    private final static String API_KEY = "44b3955c79a11cf79a085fb46f72804c9b9f8350125bf0f44ceadc278e0464f2";

    public ValidateUrlService(String name) {
        super(name);
    }

    public ValidateUrlService(){ super("ValidateUrl");}

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String link = intent != null ? intent.getStringExtra("url") : null;
        try {

            JSONObject postDataParams = new JSONObject();
            try {

                postDataParams.put("url",link);
                postDataParams.put("apikey",API_KEY);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            URL url = new URL("https://www.virustotal.com/vtapi/v2/url/scan");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setChunkedStreamingMode(0);

            // For POST only - START
            OutputStream os = con.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();

            int responseCode = con.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                InputStream ins = con.getInputStream();

                InputStreamReader in = new InputStreamReader(ins);
                JsonReader jsoreader = new JsonReader(in);

                ExtractId(jsoreader);
                jsoreader.close();
                in.close();

            } else if (responseCode == 204){

                Log.d("Request exceeded","Request exceed");

            } else {

                con.disconnect();
                Log.d("post not work","POST request not worked");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent broadcastresult = new Intent();
        //broadcastresult.putExtra(LINK_VALIDATE,link);
        broadcastresult.putExtra(SCAN_ID,scan_id);
        broadcastresult.putExtra(LINK_VALIDATE,link);
        broadcastresult.setAction("com.example.newpc.qrcode.VALIDATE_VIA_INTENT");
        sendBroadcast(broadcastresult);
    }

    private void ExtractId(JsonReader jsonreader) throws IOException {
        try {
            jsonreader.beginObject(); // Start processing the JSON object
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (jsonreader.hasNext()) { // Loop through all keys
            String key = jsonreader.nextName(); // Fetch the next key
            if ("scan_id".equals(key)) { // Check if desired key
                // Fetch the value as a String
                scan_id = jsonreader.nextString();
                break; // Break out of the loop
            } else {
                jsonreader.skipValue(); // Skip values of other keys
            }
        }
        jsonreader.close();
    }

    private String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append('&');

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append('=');
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }
}
