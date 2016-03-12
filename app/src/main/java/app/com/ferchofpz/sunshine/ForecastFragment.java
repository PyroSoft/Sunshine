package app.com.ferchofpz.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 * HTTP Request: http://api.openweathermap.org/data/2.5/forecast/daily?q=bogota&mode=json&units=metric&cnt=7&appid=7ed9191a06e0668093f9271a8e079934
 */
public class ForecastFragment extends Fragment {

    public static ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Add this line in order for this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //ArrayAdapter<String> mForecastAdapter;
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView LForecast = (ListView) rootView.findViewById(R.id.listview_forecast);

        /*String[] forecastArray = new String[] {
                "1st day weather",
                "2nd day weather",
                "3rd day weather",
                "4th day weather",
                "5th day weather",
                "6th day weather",
                "7th day weather",
        };

        List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));*/

        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(), //Current context (this activity)
                R.layout.list_item_forecast, //The name of the layout
                R.id.list_item_forecast_textview, //The ID of the TextView to populate
                new ArrayList<String>()); //The array
        LForecast.setAdapter(mForecastAdapter);
        updateData();

        LForecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*
                Toast toast = Toast.makeText(getActivity(),mForecastAdapter.getItem(position),Toast.LENGTH_SHORT);
                toast.show();
                */
                Intent intent = new Intent(getActivity(), DetailActivity.class); //Contexto y la clase que se va a llamar explicitamente
                intent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(position)); //Datos a enviar
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater){
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        switch(id){
            case R.id.action_refresh:
                updateData();
                return true;

            case R.id.action_settings:
                Intent intent = new Intent(getActivity(),SettingsActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateData(){
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //If there's no value stored then we fall back to the default.
        String postCode = settings.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_default_display_name));
        weatherTask.execute(postCode);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateData();
    }

    //New thread
    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>{

        //Se declara como constante el nombre de la clase para uso en logcat
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            // HTTP REQUEST:
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            //Will contain the way JSON response as a string
            String forecastJsonString;

            try{
                //Building the url
                //String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=bogota&mode=json&units=metric&cnt=7";
                //String apiKey = "&appid="+BuildConfig.OPEN_WEATHER_MAP_API_KEY;

                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter("q",params[0])
                        .appendQueryParameter("mode","json")
                        .appendQueryParameter("units","metric")
                        .appendQueryParameter("cnt","7")
                        .appendQueryParameter("appid",BuildConfig.OPEN_WEATHER_MAP_API_KEY);

                //URL constructor:
                String baseUrl = builder.build().toString();
                URL url = new URL(baseUrl);
                //Log.v(LOG_TAG,"Built URI: "+baseUrl);

                //Create the request and open the connection
                //The connection is opened by the URL class and is returned as a HttpUrlConnection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                //Reads the inputStream from the HttpUrlConnection
                InputStream inputStream = urlConnection.getInputStream();
                if(inputStream == null){
                    //Nothing to do
                    return null;
                }

                //Converts the inputStream into a StringBuffer using a bufferedReader and a InputStreamReader
                StringBuffer stringBuffer = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = reader.readLine()) != null){
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    stringBuffer.append(line+"\n");
                }

                if(stringBuffer.length()==0){
                    //Stream was empty, no point in parsing.
                    return null;
                }
                forecastJsonString = stringBuffer.toString();

                //Get the max temperature for 7 days:
                try {
                    //Log.v(LOG_TAG,"Json: "+forecastJsonString);
                    return getWeatherDataFromJson(forecastJsonString,7);
                }catch(JSONException j){
                    Log.e(LOG_TAG,j.getMessage());
                }

            }catch(IOException e){
                Log.e(LOG_TAG,"Error",e);
                return null;
            }finally {
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
                if(reader != null){
                    try{
                        reader.close();
                    }catch(final IOException e){
                        Log.e(LOG_TAG,"Error closing stream",e);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            //List<String> listForecast = new ArrayList<String>(Arrays.asList(result));
            //mForecastAdapter.clear();
            //mForecastAdapter.addAll(listForecast);
            if(result != null){
                mForecastAdapter.clear();
                for(String dayForecastStr : result){
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            //create a Gregorian Calendar, which is in current date
            GregorianCalendar gc = new GregorianCalendar();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                //get that date, format it, and "save" it on variable day
                Date time = gc.getTime();
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
                day = shortenedDateFormat.format(time);

                //add 1 date to current date of calendar
                gc.add(GregorianCalendar.DATE,1);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            /*
            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            */
            return resultStrs;
        }
    }
}