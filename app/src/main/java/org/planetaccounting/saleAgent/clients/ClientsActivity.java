package org.planetaccounting.saleAgent.clients;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.*;

import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.MainActivity;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.api.ApiService;
//import org.planetaccounting.saleAgent.clients.infos.FragmentAdapter;
//import org.planetaccounting.saleAgent.databinding.ClientLocationLayoutBinding;
//import org.planetaccounting.saleAgent.databinding.ClientUnitItemBinding;
import org.planetaccounting.saleAgent.databinding.ClientsActivityLayoutBinding;
import org.planetaccounting.saleAgent.events.OpenClientsCardEvent;
//import org.planetaccounting.saleAgent.helper.LocaleHelper;
//import org.planetaccounting.saleAgent.model.clientState.State;
//import org.planetaccounting.saleAgent.model.clientState.StateResponse;
import org.planetaccounting.saleAgent.helper.LocaleHelper;
import org.planetaccounting.saleAgent.model.clients.Client;
import org.planetaccounting.saleAgent.persistence.RealmHelper;
import org.planetaccounting.saleAgent.utils.Preferences;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.RealmResults;

/**
 * Created by planetaccounting on 13/12/17.
 */

public class ClientsActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    ClientsActivityLayoutBinding binding;
//    ClientLocationLayoutBinding bindingLocation;

    @Inject
    ApiService apiService;
    @Inject
    Preferences preferences;
    @Inject
    RealmHelper realmHelper;

    RealmResults<Client> clients;
    ArrayList<Client> searchResults = new ArrayList<>();
//    List<State> stateList = new ArrayList<>();
    String stationID = "2";

    private Context ctx;
    private DatePickerDialog.OnDateSetListener dateSh;
    private Calendar calendar;
    private java.util.Timer timer;
    String fDate;
    String dDate;
    String shDate;


    Locale myLocale;
    String currentLanguage = "sq", currentLang;
    public static final String TAG = "bottom_sheet";

    TabLayout tabLayout;
    ViewPager viewPager;
//    FragmentAdapter viewPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.clients_activity_layout);
        Kontabiliteti.getKontabilitetiComponent().inject(this);
        clients = realmHelper.getClients();

        //pjesa per paraqitjen e dates aktuale kur krijojme kliente...
        Date cDate = new Date();
        fDate = new SimpleDateFormat("dd-MM-yyyy").format(cDate);
        dDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(cDate);
        binding.dataEdittext.setText(fDate);
        calendar = Calendar.getInstance();
        dateSh = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                fDate = new SimpleDateFormat("dd-MM-yyyy").format(calendar.getTime());
                dDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(calendar.getTime());
                binding.dataEdittext.setText(fDate);
            }
        };

        binding.dataEdittext.setOnClickListener(v -> getData());

        binding.numriKlientit.setText(preferences.getEmployNumber() + "-");


//        viewPager = findViewById(R.id.viewpager);
//        tabLayout = findViewById(R.id.sliding_tabs);

//        viewPagerAdapter = new FragmentAdapter(getSupportFragmentManager());
//        viewPager.setAdapter(viewPagerAdapter);

        //connect TabLayout with ViewPager
        tabLayout.setupWithViewPager(viewPager);

//        binding.shtetiKlientit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                stationID = stateList.get(position).getId();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });

        binding.klientiCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.stateLayout.setVisibility(View.GONE);
                    binding.firstLinearL.setVisibility(View.GONE);
                    binding.secondLinearL.setVisibility(View.GONE);
                    binding.thirdLinearL.setVisibility(View.GONE);
                } else {
                    binding.stateLayout.setVisibility(View.VISIBLE);
                    binding.firstLinearL.setVisibility(View.VISIBLE);
                    binding.secondLinearL.setVisibility(View.VISIBLE);
                    binding.thirdLinearL.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.furnitoriCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    binding.furnitorRelativeLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.furnitorRelativeLayout.setVisibility(View.GONE);
                }
            }
        });

//        getClientState();
//        showDropDownStateList();

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                binding.shtetiKlientit.showDropDown();
//                binding.shtetiKlientit.requestFocus();
//            }
//        }, 500);

        currentLanguage = getIntent().getStringExtra(currentLang);
    }

    //method for data (Calendar)

    private void getData() {
        new DatePickerDialog(getApplicationContext(), dateSh, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    //methods to change the languages

    public void setLocale(String localeName) {
        if (!localeName.equals(currentLang)) {
            Context context = LocaleHelper.setLocale(this, localeName);
            //Resources resources = context.getResources();
            myLocale = new Locale(localeName);
            Resources res = context.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = myLocale;
            res.updateConfiguration(conf, dm);
            Intent refresh = new Intent(this, MainActivity.class);
            refresh.putExtra(currentLang, localeName);
            startActivity(refresh);
        } else {
            Toast.makeText(ClientsActivity.this, R.string.language_already_selected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(OpenClientsCardEvent event) {
        Intent i = new Intent(getApplicationContext(), ClientsDetailActivity.class);
        i.putExtra("client", event.getClient());
        startActivity(i);

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

    }

//    private void showDropDownStateList() {
//        binding.shtetiKlientit.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                binding.shtetiKlientit.showDropDown();
//                return false;
//            }
//        });
//    }


//    public void getClientState() {
//        apiService.getStates(new StockPost(preferences.getToken(), preferences.getUserId()))
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Action1<StateResponse>() {
//                    @Override
//                    public void call(StateResponse stateResponse) {
//                        stateList = stateResponse.getStates();
//                        stationID = stateList.get(0).getId();
//                        String[] state = new String[stateList.size()];
//
//                        for (int i = 0; i < stateList.size(); i++) {
//                            state[i] = stateList.get(i).getName();
//
//                            if (stateList.get(i).getId().equals("22")) {
//                                binding.shtetiKlientit.setText(stateList.get(i).getName());
//                                ;
//                            }
//                        }
//
//                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ClientsActivity.this,
//                                android.R.layout.simple_dropdown_item_1line, state);
//
//                        binding.shtetiKlientit.setAdapter(adapter);
//                    }
//                }, Throwable::printStackTrace);
//    }


//    private void getClients() {
//        apiService.getClients(new StockPost(preferences.getToken(), preferences.getUserId()))
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(clientsResponse -> {
//                            adapter.setClients(clientsResponse.getClients());
//                            realmHelper.saveClients(clientsResponse.getClients());
//                        },
//                        Throwable::printStackTrace);
//    }

}