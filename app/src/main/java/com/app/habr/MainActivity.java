package com.app.habr;

import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Bundle;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{
    private RecyclerView mRVuser;
    private HabAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.SwipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        new GetXMLFromServer().execute();
        }

    @Override
    public void onRefresh() {
        new GetXMLFromServer().execute();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void ParseXML(String string){
        XmlPullParserFactory parserFactory;
        InputStream stream = null;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            InputStream is = getAssets().open("data.xml");
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(string));
            processParsing(parser);

        } catch (XmlPullParserException e) {

        } catch (IOException e) {
        }
    }

    private  class GetXMLFromServer extends AsyncTask<String,Void,String> {
        HttpHandler nh;

        @Override
        protected String doInBackground(String... strings) {
            String URL = "https://habr.com/rss/hubs/all/";
            String res = "";
            nh = new HttpHandler();
            InputStream is = nh.CallServer(URL);
            if (is != null) {
                res = nh.StreamToString(is);
            } else {
                res = "NotConnected";
            }
            return res;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.equals("NotConnected")) {
                Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_SHORT).show();
            } else {
                ParseXML(result);
            }
        }
    }

    private void processParsing(XmlPullParser parser) throws IOException, XmlPullParserException{
        ArrayList<HabrPost> habrPosts = new ArrayList<>();
        int eventType = parser.getEventType();
        HabrPost currentHabrPost = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String eltName = null;

            switch (eventType) {
                case XmlPullParser.START_TAG:
                    eltName = parser.getName();

                    if ("item".equals(eltName)) {
                        currentHabrPost = new HabrPost();
                        habrPosts.add(currentHabrPost);
                    } else if (currentHabrPost != null) {
                        if ("title".equals(eltName)) {
                            currentHabrPost.title = parser.nextText();
                        } else if ("guid".equals(eltName)) {
                            currentHabrPost.link = parser.nextText();
                        } else if ("description".equals(eltName)) {
                            currentHabrPost.description = parser.nextText();
                        }   else if ("category".equals(eltName)) {
                            currentHabrPost.category=parser.nextText();
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        DrawCards(habrPosts);
    }

    private void DrawCards(ArrayList<HabrPost>data){
        mRVuser = (RecyclerView) findViewById(R.id.habs);
        mAdapter = new HabAdapter(MainActivity.this, data);
        mRVuser.setAdapter(mAdapter);
        mRVuser.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        mAdapter.notifyDataSetChanged(); //уведомить адаптер об изменениях данных
    }
}
