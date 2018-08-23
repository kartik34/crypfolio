package com.kartiksinghal.www.crypfolio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    final String URL = "https://min-api.cryptocompare.com/data/pricemultifull";
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String COIN = "coin";
    TextView mName;
    TextView mPercentChange;
    TextView mPrice;
    TextView mDollarChange;

    private String mCoinPrice;
    private String mCoinPercentChange;
    private String mCoinDollarChange;


    int count = 0;
    String params = "";
    ArrayList<String> coinsArrayList = new ArrayList<>();


    private String cryptoCoin = ""; //string version of coinsArrayList
    final String currency = "USD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mName =  findViewById(R.id.mName);
        mPercentChange =  findViewById(R.id.mPercentChange);
        mPrice =  findViewById(R.id.mPrice);
        mDollarChange = findViewById(R.id.mDollarChange);




    }

    @Override
    protected void onResume() {


        super.onResume();
        Log.d("Debug", "onResume() called");


        loadData();
        Intent myIntent = getIntent();



        String Coin = myIntent.getStringExtra("Coin");
        if(Coin != null && !Coin.equals("")){
            Log.d("check", "coin == null called");
            saveData(Coin);
            loadData();
            updateUI(coinsArrayList);


        }else if(coinsArrayList.get(0).equals("") ||coinsArrayList.get(0) == null){
            Intent noData;
            noData = new Intent(MainActivity.this, CoinAddController.class);
            startActivity(noData);

            Toast.makeText(MainActivity.this, "Enter Valid Coin to Start2", Toast.LENGTH_LONG).show();

        }else if(coinsArrayList.get(0) != "" && coinsArrayList.get(0) != null){
            Log.d("check", "other called");

            updateUI(coinsArrayList);

        }

    }
    private void getCoinData(String coin){

        Log.d("debug", "getCoinData() called");
        RequestParams params = new RequestParams();
        params.put("fsyms", coin);
        params.put("tsyms", currency);

        connect(params);
    }

    private void connect(RequestParams params){  //connect to CryptoCompare API and retrieve json body

        AsyncHttpClient client = new AsyncHttpClient();

        Log.d("debug", "connect() called");

        client.get(URL, params, new JsonHttpResponseHandler(){

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response){

                Log.d("Debug", "onSuccess() called");
                Log.d("Debug", "array size"+ coinsArrayList.size());
                Log.d("Debug", "API Response: " + response.toString());
                if(coinsArrayList.size() == 1){
                    testFirst(response, coinsArrayList);

                }else{

                    test(response, coinsArrayList);

                }

                Log.d("debug" , "Coinsarraylist: " + coinsArrayList.get(coinsArrayList.size()-1));
                Log.d("debug", "cryptoCoin: " + cryptoCoin);



                parseJson(response);



            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response){
                Log.e("Debug", "Fail"+ e.toString());
                Log.d("Debug", "Status code " + statusCode);
                Toast.makeText(MainActivity.this, "Request Failed", Toast.LENGTH_SHORT).show();

            }

        });

    }

    public void updateUI(ArrayList<String> list) { //update ui will feature all coins and data, not just one



        for (String x : list) {
            count++;
            Log.d("debug", x);

            if (count == 1) {
                params = x;

            } else {
                params = params + "," + x;
            }


        }

        getCoinData(params);


    }

    public void parseJson(JSONObject json){

        try{
            Log.d("debug", "parseJson() called");
            Log.d("debug", "JSON : "+ json.toString());

            loadData();
            Log.d("debug", "price: " + Double.toString(json.getJSONObject("RAW").getJSONObject(coinsArrayList.get(coinsArrayList.size()-1)).getJSONObject(currency).getDouble("PRICE")));


            ArrayList<coinItem> coinList = new ArrayList<>();


            for(String i : Lists.reverse(coinsArrayList)){

                double p = json.getJSONObject("RAW").getJSONObject(i).getJSONObject(currency).getDouble("PRICE");
                BigDecimal bdp = new BigDecimal(p);
                bdp = bdp.round(new MathContext(3));
                double roundedP = bdp.doubleValue();
                mCoinPrice = "$" + Double.toString(roundedP);



                double d = json.getJSONObject("RAW").getJSONObject(i).getJSONObject(currency).getDouble("CHANGE24HOUR");
                BigDecimal bd = new BigDecimal(d);
                bd = bd.round(new MathContext(3));
                double rounded = bd.doubleValue();
                mCoinDollarChange =  Double.toString(rounded);


                mCoinPercentChange = Double.toString(Math.round(json.getJSONObject("RAW").getJSONObject(i).getJSONObject(currency).getDouble("CHANGEPCT24HOUR")));

                coinList.add(new coinItem(mCoinPrice, mCoinPercentChange, mCoinDollarChange, i, currency));


            }
            mRecyclerView = findViewById(R.id.recyclerView);
            mLayoutManager = new LinearLayoutManager(this);
            mAdapter = new CoinAdapter(coinList);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mAdapter);


        }catch (JSONException e) {
            e.printStackTrace();


        }


    }

    //===================================================================================================================

    //                                  Don't touch

    //===================================================================================================================
    public void saveData(String coin){

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if(coinsArrayList.contains(coin) == false){

            Log.d("debug", "saveData() called");
            if(cryptoCoin != "" && cryptoCoin != null){
                cryptoCoin = sharedPreferences.getString(COIN, "");

                cryptoCoin = cryptoCoin + "," + coin;
                editor.putString(COIN,  cryptoCoin);
                editor.apply();

            }else{
                cryptoCoin = coin;
                editor.putString(COIN,  cryptoCoin);
                editor.apply();
            }

        }else{
            Intent myIntent;
            myIntent = new Intent(MainActivity.this, CoinAddController.class);
            startActivity(myIntent);

            Toast.makeText(MainActivity.this, "You have already saved this coin", Toast.LENGTH_LONG).show();
        }


    }

    public void loadData(){

        Log.d("debug", "loadData() called");


        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        cryptoCoin = sharedPreferences.getString(COIN, "");

        Log.d("currencies" , cryptoCoin);

        coinsArrayList = convertToArray(cryptoCoin);



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.crypfolio_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent myIntent;
        myIntent = new Intent(MainActivity.this, CoinAddController.class);
        startActivity(myIntent);

        return super.onOptionsItemSelected(item);
    }
    private String convertToString(ArrayList<String> list) {

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String s : list)
        {
            sb.append(delim);
            sb.append(s);
            delim = ",";
        }
        return sb.toString();
    }
    private ArrayList<String> convertToArray(String string) {

        ArrayList<String> list = new ArrayList<String>(Arrays.asList(string.split(",")));
        return list;
    }
    public void test(JSONObject json, ArrayList<String> array){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Log.d("debug", "test() called");


        try{
            Log.d("debug", "test() try called");
            Log.d("debug", array.get(array.size()-1));

            if(!json.getJSONObject("RAW").has(array.get(array.size()-1))){
                array.remove(array.size()-1);
                ArrayList<String> validData = convertToArray(cryptoCoin);
                validData.remove(validData.size()-1);
                cryptoCoin = convertToString(validData);
                editor.putString(COIN,  cryptoCoin);
                editor.apply();
                Log.d("debug", "failure, send back to search page");
                Intent myIntent;
                myIntent = new Intent(MainActivity.this, CoinAddController.class);
                startActivity(myIntent);
            }

        }catch (JSONException e) {

            e.printStackTrace();

        }
    }
    public void testFirst(JSONObject json, ArrayList<String> array){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Log.d("debug", "testFirst() called");

        Log.d("debug", array.get(array.size()-1));


        try{
            Log.d("debug", "testFirst() try called");
            Log.d("debug", array.get(array.size()-1));

            if(json.getString("Response").equals("Error")){

                Log.d("debug", "wssup inside else if");
                Log.d("debug", array.get(array.size()-1));

                cryptoCoin = "";
                editor.putString(COIN,  cryptoCoin);
                editor.apply();
                Intent myIntent;
                myIntent = new Intent(MainActivity.this, CoinAddController.class);

                startActivity(myIntent);


            }

        }catch (JSONException e) {

            e.printStackTrace();

        }
    }

}
