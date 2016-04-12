package app.com.ferchofpz.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 * HTTP Request: http://api.openweathermap.org/data/2.5/forecast/daily?q=bogota&mode=json&units=metric&cnt=7&appid=7ed9191a06e0668093f9271a8e079934
 */
public class ForecastFragment extends Fragment {

    public static ArrayAdapter<String> mForecastAdapter;
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

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
        updateWeather();

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
                updateWeather();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(getActivity(),SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_map:
                openPreferredLocationInMap();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openPreferredLocationInMap(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String postCode = settings.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_default_display_name));

        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q",postCode).build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if(intent.resolveActivity(getActivity().getPackageManager())!=null){
            startActivity(intent);
        }else{
            Log.d(LOG_TAG,"Couldn't call "+postCode+", no app found");
        }
    }

    public void updateWeather(){
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity(),mForecastAdapter);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //If there's no value stored then we fall back to the default.
        String postCode = settings.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_default_display_name));
        weatherTask.execute(postCode);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }
}