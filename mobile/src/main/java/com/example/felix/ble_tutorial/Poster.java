package com.example.felix.ble_tutorial;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Poster extends AsyncTask<String, Void, String>
{
//    private TaskCompleted mCallback;
//    private boolean doesCallback;
//
//    public Poster(Context context, boolean doesCallback){
//        this.doesCallback = doesCallback;
//        if (doesCallback)
//            this.mCallback = (TaskCompleted) context;
//    }

    @Override
    protected String doInBackground(String... params)
    {

        HttpsURLConnection conn = null;
        String jsonStr = params[1];
        System.out.println(jsonStr);
        try
        {
            URL url = new URL(params[0]);
            conn = (HttpsURLConnection)url.openConnection();

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);

            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(jsonStr.toString());
            out.flush();
            out.close();

            int res = conn.getResponseCode();
            StringBuilder responseOutput = new StringBuilder();

            System.out.println("Res: " + res);
//            if(res == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    responseOutput.append(line);
                }
                br.close();

                System.out.println("Something to it: " + responseOutput.toString());
//            }

            if(res == 200){
                conn.disconnect();
                return responseOutput.toString();
            }
            else {

                conn.disconnect();
                return "";
            }
        }
        catch(IOException error) {
            System.out.print(error);
        }
        finally {
            conn.disconnect();
        }

        return "";
    }

//    @Override
//    protected void onPostExecute(String results) {
        //This is where you return data back to caller asynchronously
//        if (doesCallback)
//            mCallback.onTaskComplete(results);
//    }
}
