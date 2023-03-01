package org.planetaccounting.saleAgent.raportet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.planetaccounting.saleAgent.MainActivity;
import org.planetaccounting.saleAgent.PazariDitorActivity;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.clients.ClientsListActivity;
import org.planetaccounting.saleAgent.databinding.RaportActivityBinding;
import org.planetaccounting.saleAgent.helper.LocaleHelper;
import org.planetaccounting.saleAgent.invoice.InvoiceListActivity;
import org.planetaccounting.saleAgent.kthemallin.ReturnListActivity;
import org.planetaccounting.saleAgent.ngarkime.NgarkimeListAdapter;
import org.planetaccounting.saleAgent.ngarkime.ngarkimeActivity;
import org.planetaccounting.saleAgent.order.OrdersListActivity;

import static org.planetaccounting.saleAgent.MainActivity.isConnected;

import java.util.Locale;

/**
 * Created by tahirietrit on 4/5/18.
 */

public class RaportetActivity extends Activity {
    RaportActivityBinding binding;

    Locale myLocale;
    String currentLanguage = "sq", currentLang;
    public static final String TAG = "bottom_sheet";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.raport_activity);
        binding.listaFaturaveButton.setOnClickListener(view -> openInvoicesActivity());
        binding.listaPorosiveButton.setOnClickListener(view -> openOrderListActivity());
        binding.listaShpenzimeve.setOnClickListener(view -> openVendorListActivity());
        binding.listaInkasimeve.setOnClickListener(view -> openInkasimiActivity());
        binding.listaDepozitave.setOnClickListener(view -> openDepozitActivity());
        binding.listaKthimit.setOnClickListener(view -> openReturnsActivity() );
        binding.dailyMarket.setOnClickListener(view -> openDailyMarket());
        binding.listaKlientave.setOnClickListener(view -> openClientListActivity());
        binding.listaNgarkimeve.setOnClickListener(view -> openLoadListActivity());

        //for changing land
        currentLanguage = getIntent().getStringExtra(currentLang);

        //setting fonts
        setLatoRegularFont(this, binding.title);
        setLatoRegularFont(this, binding.listaFaturaveButton);
        setLatoRegularFont(this, binding.listaPorosiveButton);
        setLatoRegularFont(this, binding.listaShpenzimeve);
        setLatoRegularFont(this, binding.listaInkasimeve);
        setLatoRegularFont(this, binding.listaDepozitave);
        setLatoRegularFont(this, binding.listaKthimit);
        setLatoRegularFont(this, binding.dailyMarket);
        setLatoRegularFont(this, binding.listaKlientave);
        setLatoRegularFont(this, binding.listaNgarkimeve);

    }
    private void openInvoicesActivity() {
        Log.d("Hap Listen e faturav - ", " InvoiceListActivity");
        Intent i = new Intent(getApplicationContext(), InvoiceListActivity.class);
        startActivity(i);
    }

    private void openReturnsActivity() {
        Log.d("Hap Listen e kthimit - ", " InvoiceListActivity");
        Intent i = new Intent(getApplicationContext(), ReturnListActivity.class);
        startActivity(i);
    }

    private void openVendorListActivity() {
        Log.d("Hap Listen e faturav - ", " InvoiceListActivity");
        Intent i = new Intent(getApplicationContext(), ReportDetailActivity.class);
        i.putExtra("type", 0);
        startActivity(i);
    }

    private void openInkasimiActivity() {
        Log.d("Hap Listen e faturav - ", " InvoiceListActivity");
        Intent i = new Intent(getApplicationContext(), ReportDetailActivity.class);
        i.putExtra("type", 1);
        startActivity(i);
    }

    private void openDepozitActivity() {
        Log.d("Hap Listen e faturav - ", " InvoiceListActivity");
        Intent i = new Intent(getApplicationContext(), ReportDetailActivity.class);
        i.putExtra("type", 2);
        startActivity(i);
    }

    private void openOrderListActivity() {
        if (isConnected) {
            Intent i = new Intent(getApplicationContext(), OrdersListActivity.class);
            startActivity(i);
        } else {
            Toast.makeText(getApplicationContext(), R.string.ju_lutem_kyçuni_ne_internet_që_të_shikoni_porositë, Toast.LENGTH_SHORT).show();
        }

    }

    private void openClientListActivity(){
        Log.d("Hap Listen e Klientave-" , "ClientListActivity");
        Intent i = new Intent(this, ClientsListActivity.class);
        startActivity(i);
    }

    private void openDailyMarket() {
        Intent i = new Intent(this, PazariDitorActivity.class);
        startActivity(i);
    }

    //Pjesa qe na e hape Listen e ngarkesave...
    private void openLoadListActivity(){
        Intent i = new Intent(this, ngarkimeActivity.class);
        startActivity(i);
    }

    //methods to change the languages
    public void setLocale(String localeName){
        if(!localeName.equals(currentLang)){
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
        }else{
            Toast.makeText(RaportetActivity.this, R.string.language_already_selected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    public static void setLatoRegularFont(Context context, TextView textView){
        Typeface latoFont = Typeface.createFromAsset(context.getAssets(), "fonts/lato_regular.ttf");
        textView.setTypeface(latoFont);
    }
}