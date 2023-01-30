package org.planetaccounting.saleAgent.stock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.widget.Toast;


import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.MainActivity;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.api.ApiService;
import org.planetaccounting.saleAgent.databinding.StockMainLayoutBinding;
import org.planetaccounting.saleAgent.helper.LocaleHelper;
import org.planetaccounting.saleAgent.model.stock.Item;
import org.planetaccounting.saleAgent.persistence.RealmHelper;
import org.planetaccounting.saleAgent.utils.Preferences;
import org.planetaccounting.saleAgent.utils.StockPrintUtil;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.RealmResults;

/**
 * Created by macb on 09/12/17.
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)

public class StockActivity extends Activity {

    StockMainLayoutBinding binding;

    @Inject
    ApiService apiService;
    @Inject
    Preferences preferences;
    @Inject
    RealmHelper realmHelper;
    RealmResults<Item> stockItems;
    ArrayList<Item> searchResults = new ArrayList<>();
    StockListAdapter adapter;
    private PrintManager printManager;

    Locale myLocale;
    String currentLanguage = "sq", currentLang;
    public static final String TAG = "bottom_sheet";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, org.planetaccounting.saleAgent.R.layout.stock_main_layout);
        Kontabiliteti.getKontabilitetiComponent().inject(this);
        stockItems = realmHelper.getStockItemsWithoutAction();

        adapter = new StockListAdapter(StockActivity.this, stockItems);
        binding.articleRecyler.setLayoutManager(new LinearLayoutManager(StockActivity.this));
        binding.articleRecyler.setAdapter(adapter);
        binding.searchEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchResults.clear();
                for (int i3 = 0; i3 < stockItems.size(); i3++) {
                    if (stockItems.get(i3).getName().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                        searchResults.add(stockItems.get(i3));
                    }
                }
                if (charSequence.length() > 0) {
                    adapter = new StockListAdapter(StockActivity.this, searchResults);
                    binding.articleRecyler.setAdapter(adapter);
                } else {
                    adapter = new StockListAdapter(StockActivity.this, stockItems);
                    binding.articleRecyler.setAdapter(adapter);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        binding.printTextview.setOnClickListener(v ->{

                    StockPrintUtil print = new StockPrintUtil(stockItems,binding.web,this,printManager);
                }
        );

        currentLanguage = getIntent().getStringExtra(currentLang);
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
            Toast.makeText(StockActivity.this, R.string.language_already_selected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

}
