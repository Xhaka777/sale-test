package org.planetaccounting.saleAgent.order;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.MainActivity;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.aksionet.ActionArticleItems;
import org.planetaccounting.saleAgent.aksionet.ActionBrandItem;
import org.planetaccounting.saleAgent.aksionet.ActionCategoryItem;
import org.planetaccounting.saleAgent.aksionet.ActionData;
import org.planetaccounting.saleAgent.aksionet.ActionSteps;
import org.planetaccounting.saleAgent.api.ApiService;
import org.planetaccounting.saleAgent.databinding.ActionInvItemBinding;
import org.planetaccounting.saleAgent.databinding.ActivityOrderOriginalBinding;
import org.planetaccounting.saleAgent.databinding.InvoiceItemBinding;
import org.planetaccounting.saleAgent.helper.LocaleHelper;
import org.planetaccounting.saleAgent.invoice.InvoiceActivity;
import org.planetaccounting.saleAgent.invoice.InvoiceActivityOriginal;
import org.planetaccounting.saleAgent.model.InvoiceItem;
import org.planetaccounting.saleAgent.model.clients.Client;
import org.planetaccounting.saleAgent.model.invoice.InvoiceItemPost;
import org.planetaccounting.saleAgent.model.invoice.InvoicePost;
import org.planetaccounting.saleAgent.model.invoice.OrderPostObject;
import org.planetaccounting.saleAgent.model.order.CheckQuantity;
import org.planetaccounting.saleAgent.model.role.InvoiceRole;
import org.planetaccounting.saleAgent.model.stock.Item;
import org.planetaccounting.saleAgent.model.stock.SubItem;
import org.planetaccounting.saleAgent.persistence.RealmHelper;
import org.planetaccounting.saleAgent.utils.ActivityPrint;
import org.planetaccounting.saleAgent.utils.PlanetLocationManager;
import org.planetaccounting.saleAgent.utils.Preferences;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class OrderActivityOriginal extends AppCompatActivity {

    public enum InvoiceState {
        POROSI
    }

    public static final String ACTION_PRINT = "action_print";
    public static final String PRINT_Z_RAPORT = "print_z_raport";
    public static final String ACTION_ADD_ITEMS = "add_items";
    public static final String ACTION = "action";
    public static final String ITEMS = "items";
    public static String isBill = "0";
    public String cash = "0";
    public String isPaid = "0";

    ActivityOrderOriginalBinding binding;
    @Inject
    ApiService apiService;
    @Inject
    Preferences preferences;
    @Inject
    RealmHelper realmHelper;

    ArrayList<InvoiceItem> stockItems = new ArrayList<>();
    ArrayList<InvoiceItem> tempStockItems = new ArrayList<>();
    RealmResults<Item> stockArticles;
    Client client;
    int checked = 0;
    String fDate;
    String dDate;
    String shDate;
    private DatePickerDialog.OnDateSetListener dateSh;
    private Calendar calendar;

    int stationPos;
    String vleraPaTvsh = "0";
    String vleraZbritur = "0";
    String vleraETvsh = "0";
    String totaliFatures = "0";
    String sasiaTotale = "0";
    String nrArtikujveTotal = "0";

    private PrintManager printManager;
    ActionData actionData;
    private InvoiceState invoiceState = InvoiceState.POROSI;
    PlanetLocationManager planetLocationManager;

    private InvoiceRole invoiceRole;

    String[] stocksName;
    String[] stocksQuanitity;

    boolean isPrint = true;

    Locale myLocale;
    String currentLanguage = "sq", currentLang;
    public static final String TAG = "bottom_sheet";

    int count = 0;
    int countName = 0;
    int countSasia = 0;
    String companyStation;
    public String[] stations;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //alternative....
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_order_original);

        Kontabiliteti.getKontabilitetiComponent().inject(this);

        getStocksName();
        getStocksQuantity();

//        invoiceRole = realmHelper.getRole().getOrder();
        stockArticles = realmHelper.getStockItems();

        Date cDate = new Date();
        calendar = Calendar.getInstance();

        fDate = new SimpleDateFormat("dd-mm-yyyy").format(cDate);
        dDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(cDate);
        binding.dataEdittext.setText(fDate);
        binding.emriKlientit.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, realmHelper.getClientsNames()));

        planetLocationManager = new PlanetLocationManager(this);
        binding.dataL.setOnClickListener(view -> getdata());


        //Client change
        binding.emriKlientit.setOnItemClickListener((adapterView, view, i, l) -> {
            client = realmHelper.getClientFromName(binding.emriKlientit.getText().toString().substring(0, binding.emriKlientit.getText().toString().indexOf(" nrf:")));

            if (realmHelper.getClientStations(client.getName()).length > 0) {
                binding.njesiaEdittext.setAdapter(new ArrayAdapter<String>(this,
                        android.R.layout.simple_dropdown_item_1line, realmHelper.getClientStations(client.getName())));
                binding.njesiaEdittext.setEnabled(true);
                binding.njesiaEdittext.requestFocus();
                binding.njesiaEdittext.showDropDown();
                binding.njesiaEdittext.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        stationPos = position;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            } else {
                binding.njesiaEdittext.setText("");
                binding.njesiaEdittext.setHint("");
                binding.njesiaEdittext.setEnabled(false);
            }
            binding.zbritjaKlientit.setText("Zbritja e klientit: " + client.getDiscount() + " %");
        });

        binding.shtoTextview.setOnClickListener(view -> {
            if (client != null) {
                if (invoiceState == InvoiceState.POROSI) {
                    checkedQuantity();
                } else {
                    addOrderItem();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.ju_lutem_zgjedhni_klientin, Toast.LENGTH_SHORT).show();
            }
        });

        //Print Invoice
//        binding.fatureButton.setOnClickListener(view -> {
//            OrderActivityOriginal.isBill = "0"
//                    if(stockItems.size() > 0){
//                        if(checkSasia()){
//                            showCashDialog();
//                        }else{
//                            Toast.makeText(getApplicationContext(), R.string.nje_ose_me_shume_artikuj_kan_sasine_zero, Toast.LENGTH_SHORT).show();
//                        }
//                    }else{
//                        Toast.makeText(getApplicationContext(), R.string.shtoni_se_paku_nje_artikull, Toast.LENGTH_SHORT).show();
//                    }
//        });
        printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
        //print the bill
//        binding.kuponButton.setOnClickListener(view -> {
//            OrderActivityOriginal.isBill = "1";
//            if(stockItems.size() > 0){
//                if(checkSasia()){
//                    showCashDialog();
//                }else{
//                    Toast.makeText(getApplicationContext(), R.string.nje_ose_me_shume_artikuj_kan_sasine_zero, Toast.LENGTH_SHORT).show();
//                }
//            }else{
//                Toast.makeText(getApplicationContext(), R.string.shtoni_se_paku_nje_artikull, Toast.LENGTH_SHORT).show();
//            }
//        });

        //Send the order
        binding.porositButton.setOnClickListener(v -> {
            if (stockItems.size() > 0) {
                if (checkSasia()) {
                    showBillDialog();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.nje_ose_me_shume_artikuj_kan_sasine_zero, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.shtoni_se_paku_nje_artikull, Toast.LENGTH_SHORT).show();
            }
        });

        new Handler().postDelayed(() -> {
            binding.emriKlientit.showDropDown();
            binding.emriKlientit.requestFocus();
        }, 500);

        actionData = realmHelper.getAksionet();

        dateSh = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String f = new SimpleDateFormat("dd-MM-yyyy").format(calendar.getTime());
                shDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(calendar.getTime());
            }
        };
    }

    public void setLocale(String localeName) {
        if (!localeName.equals(currentLang)) {
            Context context = LocaleHelper.setLocale(this, localeName);

            myLocale = new Locale(localeName);
            Resources resources = context.getResources();
            DisplayMetrics dm = resources.getDisplayMetrics();
            Configuration conf = resources.getConfiguration();
            conf.locale = myLocale;
            resources.updateConfiguration(conf, dm);
            Intent refresh = new Intent(this, MainActivity.class);
            refresh.putExtra(currentLang, localeName);
            startActivity(refresh);
        } else {
            Toast.makeText(this, R.string.language_already_selected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private void getStocksName() {
        if (invoiceState == InvoiceState.POROSI) {
            stocksName = realmHelper.getStockItemsName();
        }
    }

    private void getStocksQuantity() {
        if (invoiceState == InvoiceState.POROSI) {
            stocksQuanitity = realmHelper.getStockItemsQuantity();
        }
    }

    private void openPrintingActivity() {
        Intent i = new Intent(getApplicationContext(), ActivityPrint.class);
        i.putExtra(ACTION, ACTION_PRINT);
        i.putParcelableArrayListExtra(ITEMS, stockItems);
        startActivity(i);
    }

    //Form Mode
    //kete metode mujna edhe mos me perdor....
//    private void checkIdHaveData(){
//
//        switch (preferences.getLastCheck()){
//            case 0:
//                i invoiceState = InvoiceActivityOriginal.InvoiceState.POROSI;
//                binding.porosiaRadio.setChecked(true);
//                binding.porositButton.setVisibility(View.VISIBLE);
//                binding.dataL.setVisibility(View.VISIBLE);
//
//                binding.fatureButton.setVisibility(View.GONE);
//                binding.kuponButton.setVisibility(View.GONE);
//                preferences.saveLastCheck(-1);
//
//                break;
//
//            default:
//                preferences.saveLastCheck(-1);
//                break;
//        }
//    }

    //Show Date Picker
    private void getdata() {
        new DatePickerDialog(this, dateSh, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    //Check Quantity for all items
    public boolean checkSasia(){
        for(int i = 0; i < stockItems.size(); i++){
            double sasia ;
            try{
                sasia = Double.parseDouble(stockItems.get(i).getSasia());
            }catch (Exception e){
                sasia = Double.parseDouble(stockItems.get(i).getSasia());
            }
            if(sasia == 0){
                return false;
            }
        }
        return true;
    }

    private void addOrderItem(){
        final InvoiceItem[] invoiceItem = new InvoiceItem[1];

        InvoiceItemBinding itemBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.invoice_item, binding.invoiceItemHolder, false);

        itemBinding.emertimiTextview.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, stocksName));
//        setArticleRole(itemBinding);

        //Changing the name of item - Event
        itemBinding.emertimiTextview.setOnItemClickListener((adapterView, view, i, l) -> {
            invoiceItem[0] = new InvoiceItem(realmHelper.getItemsByName(itemBinding.emertimiTextview.getText().toString()));
            itemBinding.sasiaTextview.setText("1");
            invoiceItem[0].setSasia(itemBinding.sasiaTextview.getText().toString());
            int pos = (int) itemBinding.getRoot().getTag();
            findCodeAndPosition(invoiceItem[0]);

            count++;
            binding.artikujTeZgjedhur.setText("Nr. i artikujve te zgjedhur : " + (count));


            //Actiom with Collections
            if(invoiceItem[0].getType().equalsIgnoreCase("action")){
                binding.invoiceItemHolder.removeView(itemBinding.getRoot());
                tempStockItems.clear();
                for (int i1 = 0; i1 < invoiceItem[0].getItems().size(); i1++){
                    tempStockItems.add(new InvoiceItem(realmHelper.getItemsByName(itemBinding.emertimiTextview.getText().toString())));
                    tempStockItems.get(i1).setGroupItem(invoiceItem[0].getItems().get(i1).getGroupItem());
                }
                addActionViews(tempStockItems, 1);

            }else{
                stockItems.add(pos, invoiceItem[0]);
                checkIfArticleIsInAction(invoiceItem[0]);
                fillInvoiceItemData(itemBinding, invoiceItem[0]);
                itemBinding.sasiaTextview.requestFocus();
                itemBinding.sasiaTextview.setSelection(itemBinding.sasiaTextview.getText().length());

                calculation_form();
            }
        });
        itemBinding.emertimiTextview.showDropDown();
        itemBinding.emertimiTextview.requestFocus();
        itemBinding.emertimiTextview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                itemBinding.emertimiTextview.showDropDown();
                return false;
            }
        });
        itemBinding.shifraTextview.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, realmHelper.getStockItemsCodes()));
        itemBinding.shifraTextview.setOnItemClickListener((adapterView, view, i, l) -> {
            invoiceItem[0] = new InvoiceItem(realmHelper.getItemsByCode(itemBinding.shifraTextview.getText().toString()));
            itemBinding.sasiaTextview.setText("0");
            invoiceItem[0].setSasia("0");
            int pos = (int) itemBinding.getRoot().getTag();
            findCodeAndPosition(invoiceItem[0]);

            //Action with Collection
            if(invoiceItem[0].getType().equalsIgnoreCase("action")){
                binding.invoiceItemHolder.removeView(itemBinding.getRoot());
                tempStockItems.clear();
                for(int i1 = 0; i1 < invoiceItem[0].getItems().size(); i1++){
                    tempStockItems.add(new InvoiceItem(realmHelper.getItemsByName(itemBinding.emertimiTextview.getText().toString())));
                    tempStockItems.get(i1).setGroupItem(invoiceItem[0].getItems().get(i1).getGroupItem());

                }
                addActionViews(tempStockItems, 1);

            }else{
                stockItems.add(pos, invoiceItem[0]);
                checkIfArticleIsInAction(invoiceItem[0]);
                fillInvoiceItemData(itemBinding, invoiceItem[0]);
                itemBinding.sasiaTextview.requestFocus();
                itemBinding.sasiaTextview.setSelection(itemBinding.sasiaTextview.getText().length());
                calculation_form();
            }
        });
        itemBinding.njesiaTextview.setOnClickListener(view -> dialog(invoiceItem[0], itemBinding));

        itemBinding.sasiaTextview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //duhet me kqyr me hjek kete invoiceState pasi ne kete klas po punojm vetem me porosi
                if (invoiceState == InvoiceState.POROSI) {

//                    binding.loader.setVisibility(View.VISIBLE);

                    double sasia = 0;
                    if (itemBinding.sasiaTextview.getText().length() > 0) {
                        sasia = Double.parseDouble(itemBinding.sasiaTextview.getText().toString());
                    }
                    double availableQuantity = 0;
                    availableQuantity = Double.parseDouble(invoiceItem[0].getQuantity()) / invoiceItem[0].getItems().get(invoiceItem[0].getSelectedPosition()).getRelacion();

                    if (sasia <= availableQuantity && sasia > 0f) {
                        if (itemBinding.sasiaTextview.getText().length() == 0) {
                            invoiceItem[0].setSasia("0");
                        } else {
                            invoiceItem[0].setSasia(itemBinding.sasiaTextview.getText().toString());
                            if (invoiceItem[0].isAction() && sasia >= invoiceItem[0].getMinQuantityForDiscount()) {
                                invoiceItem[0].setDiscount(invoiceItem[0].getBaseDiscount() + invoiceItem[0].getItems().get(start).getDiscount());
                                double totalDiscount = Double.parseDouble(invoiceItem[0].getDiscount())
                                        + Double.parseDouble(invoiceItem[0].getExtraDiscount());
                                invoiceItem[0].setDiscount(String.valueOf(totalDiscount));
                            } else {
                                invoiceItem[0].setDiscount(invoiceItem[0].getBaseDiscount());
                            }
                        }
                        checkIfArticleIsInAction(invoiceItem[0]);
                        fillInvoiceItemData(itemBinding, invoiceItem[0]);
                        calculation_form();

                        calculateSasiaTotale();
                        calculateArtikujtTotal();
                        calculateTotal();
                        calculateVleraPaTvsh();
                        calculateVleraEZbritur();
                        calculateVleraETVSH();
                    } else {
                        invoiceItem[0].setSasia("0");
                        if (!itemBinding.sasiaTextview.getText().toString().equals("0") && !itemBinding.sasiaTextview.getText().toString().equals("") && !itemBinding.sasiaTextview.getText().toString().isEmpty()) {
                            Toast.makeText(getApplicationContext(), R.string.nuk_stok_te_mjaftueshem, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        itemBinding.sasiaTextview.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    if (invoiceState == InvoiceState.POROSI) {

                        binding.loader.setVisibility(View.VISIBLE);

                        String stockItemId = invoiceItem[0].getItems().get(invoiceItem[0].getSelectedPosition()).getId();
                        double sasia1 = Double.parseDouble(itemBinding.sasiaTextview.getText().toString());


                        CheckQuantity checkQuantity = new CheckQuantity(preferences.getUserId(), preferences.getToken(), sasia1 + "", preferences.getStationId(), dDate, stockItemId);

                        if (sasia1 > 0f) {
                            apiService.checkQuantity(checkQuantity).subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(invoiceUploadResponse -> {
                                        binding.loader.setVisibility(View.GONE);

                                        if (!invoiceUploadResponse.getSuccess()) {
                                            Toast.makeText(getApplicationContext(), invoiceUploadResponse.getError().getText(), Toast.LENGTH_SHORT).show();

                                        } else {

                                            double sasia = Double.parseDouble(itemBinding.sasiaTextview.getText().toString());


                                            if (itemBinding.sasiaTextview.getText().length() > 0) {
                                                sasia = Double.parseDouble(itemBinding.sasiaTextview.getText().toString());
                                            }
                                            double availableQuantity = 0;

                                            if (itemBinding.sasiaTextview.getText().length() == 0) {
                                                invoiceItem[0].setSasia("0");
                                            } else {
                                                invoiceItem[0].setSasia(itemBinding.sasiaTextview.getText().toString());
                                                if (invoiceItem[0].isAction() && sasia >= invoiceItem[0].getMinQuantityForDiscount()) {
                                                    invoiceItem[0].setDiscount(invoiceItem[0].getBaseDiscount());
                                                    double totalDiscount = Double.parseDouble(invoiceItem[0].getDiscount())
                                                            + Double.parseDouble(invoiceItem[0].getExtraDiscount());
                                                    invoiceItem[0].setDiscount(String.valueOf(totalDiscount));
                                                } else {
                                                    invoiceItem[0].setDiscount(invoiceItem[0].getBaseDiscount());
                                                }
                                            }
                                            checkIfArticleIsInAction(invoiceItem[0]);
                                            fillInvoiceItemData(itemBinding, invoiceItem[0]);

                                            calculation_form();
                                            calculateSasiaTotale();


                                        }
                                        binding.loader.setVisibility(View.GONE);
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            binding.loader.setVisibility(View.GONE);

                                            throwable.printStackTrace();
                                        }
                                    });

                        } else {
                            Toast.makeText(getApplicationContext(), R.string.jipeni_sasin, Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            }
        });

        itemBinding.zbritjaTextview.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    if(itemBinding.zbritjaTextview.getText().toString().trim().isEmpty()){
                        itemBinding.zbritjaTextview.setText("0");
                        invoiceItem[0].setDiscount(itemBinding.zbritjaTextview.getText().toString(), true);
                    }
                }
            }
        });

        itemBinding.zbritjaTextview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!itemBinding.zbritjaTextview.getText().toString().replace("%", "").isEmpty()){
                    float zbritja = Float.parseFloat(itemBinding.zbritjaTextview.getText().toString().replace("%",""));
                    float discount = Float.parseFloat(invoiceItem[0].getMaxDiscound());
                    if(zbritja > discount){
                        fillInvoiceItemData(itemBinding, invoiceItem[0]);
                        Toast.makeText(getApplicationContext(), "Zbritja maksimale esht: " + invoiceItem[0].getDiscount(), Toast.LENGTH_SHORT).show();
                    }
                    if((zbritja <= discount) && !(zbritja < 0)){
                        invoiceItem[0].setDiscount(itemBinding.zbritjaTextview.getText().toString(), true);
                    }
                }
            }
        });

        itemBinding.removeButton.setOnClickListener(view ->
        {
            doYouWantToDeleteThisArticleDialog(itemBinding.emertimiTextview.getText().toString(), itemBinding.sasiaTextview.getText().toString(), () -> {
                int pos = (int) itemBinding.getRoot().getTag();
                if (stockItems.size() > 0) {
                    try {
                        stockItems.remove(pos);
                    } catch (Exception e) {

                    }
                }
                binding.invoiceItemHolder.removeView(itemBinding.getRoot());
                calculation_form();
                calculateSasiaTotale();
                calculateArtikujtTotal();
                calculateTotal();
                calculateVleraPaTvsh();
                calculateVleraEZbritur();
                calculateVleraETVSH();

            });
        });
        itemBinding.getRoot().setTag(binding.invoiceItemHolder.getChildCount());
        binding.invoiceItemHolder.addView(itemBinding.getRoot());
    }

    public SubItem getArticleId(String article_id){
        for (int i = 0; i < stockArticles.size(); i++){
            for (int j = 0; j < stockArticles.get(i).getItems().size(); i++){
                if(stockArticles.get(i).getItems().get(j).getId().equals(article_id)){
                    return stockArticles.get(i).getItems().get(j);
                }
            }
        }
        return null;
    }

    private void fillInvoiceItemData(InvoiceItemBinding itemBinding, InvoiceItem invoiceItem) {
        itemBinding.emertimiTextview.setText(invoiceItem.getName());
        itemBinding.shifraTextview.setText(invoiceItem.getSelectedItemCode());
        itemBinding.njesiaTextview.setText(invoiceItem.getSelectedUnit());
        itemBinding.zbritjaTextview.setText(invoiceItem.getDiscount());
        List<Double> amontAndPrice = calculateValueOfItem(invoiceItem);
        itemBinding.vlera.setText("" + String.format(Locale.ENGLISH, "%.2f", BigDecimal.valueOf(amontAndPrice.get(0))));
        itemBinding.cmimiTvsh.setText("" + String.format(Locale.ENGLISH, "%.2f", BigDecimal.valueOf(amontAndPrice.get(1))));
        itemBinding.basePrice.setText(invoiceItem.getBasePrice() + "");
//        itemBinding.basePrice.setText("" + String.format(Locale.ENGLISH, "%.4f", BigDecimal.valueOf(invoiceItem.getBasePrice())));
    }

    private void findCodeAndPosition(InvoiceItem invoiceItem) {
        for (int i = 0; i < invoiceItem.getItems().size(); i++) {
            if (invoiceItem.getDefaultUnit().equalsIgnoreCase(invoiceItem.getItems().get(i).getUnit())) {
                checked = i;
                invoiceItem.setId(invoiceItem.getId());
                invoiceItem.setDiscount(invoiceItem.getItems().get(i).getDiscount());
                invoiceItem.setSelectedItemCode(invoiceItem.getItems().get(i).getNumber());
                invoiceItem.setSelectedUnit(invoiceItem.getItems().get(i).getUnit());
                invoiceItem.setSelectedPosition(checked);
                System.out.println("sasia11 " + invoiceItem.getItems().get(i).getRelacion());
                invoiceItem.setRelacion(String.valueOf(invoiceItem.getItems().get(i).getRelacion()));
                invoiceItem.setBaseDiscount(invoiceItem.getItems().get(i).getDiscount());
                invoiceItem.setBarcode(invoiceItem.getItems().get(i).getBarcode());
                invoiceItem.setRelacion(String.valueOf(invoiceItem.getItems().get(i).getRelacion()));

            }
        }
    }

    //this part is for to show DropDown when clicked editText for secound time and more ...

    private void shopDropDownList() {
        binding.emriKlientit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                binding.emriKlientit.showDropDown();
                return false;
            }
        });

        binding.njesiaEdittext.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                binding.njesiaEdittext.showDropDown();
                return false;
            }
        });

    }

    void dialog(InvoiceItem invoiceItem, InvoiceItemBinding binding) {
        try {
            String[] units = realmHelper.getItemUnits(invoiceItem.getName());
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(OrderActivityOriginal.this);
            alt_bld.setSingleChoiceItems(units, checked, (dialog, item) -> {
                System.out.println();
                checked = item;
                invoiceItem.setSelectedItemCode(invoiceItem.getItems().get(checked).getNumber());
                invoiceItem.setSelectedUnit(invoiceItem.getItems().get(checked).getUnit());
                invoiceItem.setSelectedPosition(checked);
                invoiceItem.setRelacion(String.valueOf(invoiceItem.getItems().get(checked).getRelacion()));
                checkIfArticleIsInAction(invoiceItem);
//                hideLoader();
                fillInvoiceItemData(binding, invoiceItem);
                calculation_form();

//                cheakQuantityForInvoice(invoiceItem, binding);

                dialog.dismiss();
            });
            AlertDialog alert = alt_bld.create();
            alert.show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.ju_lutem_zgjedhni_produktin, Toast.LENGTH_SHORT).show();
        }
    }

    private void doYouWantToDeleteThisArticleDialog(String name, String sasia, OrderActivityOriginal.DoYouWantToDeleteThisArticleListener doYouWantToDeleteThisArticleListener) {
        InvoiceItemBinding itemBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.invoice_item, binding.invoiceItemHolder, false);
        android.app.AlertDialog.Builder mBuilder = new android.app.AlertDialog.Builder(this);
        mBuilder.setTitle("");
        String message = getString(R.string.do_you_want_to_delete_this_article) + " " + name + " me sasi " + sasia;
        mBuilder.setMessage(message);
        // Setting Negative "NO" Button
        mBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to invoke NO event
                dialog.cancel();
            }
        });

        mBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                //Kur te selektojm Po mu discount numri i artikujve te zgjedhur...
                count++;
//               count--;
//               binding.artikujTeZgjedhur.setText("Nr. i artikujve te zgjedhur : " + count);


                // Write your code here to invoke Yes event
                doYouWantToDeleteThisArticleListener.Yes();
                dialog.cancel();

            }
        });
        // Showing Alert Message
        mBuilder.show();
    }

    private List<Double> calculateValueOfItem(InvoiceItem invoiceItem) {
        double amount_with_vat = 0.000;
        BigDecimal priceSale = BigDecimal.valueOf(amount_with_vat);
        BigDecimal price_vat_real_sale = BigDecimal.valueOf(amount_with_vat);

        try {
            // Quantity
            BigDecimal quantity = new BigDecimal(invoiceItem.getSasia());

            // Price Sale (with VAT) untouchable
            priceSale = new BigDecimal(invoiceItem.getChildList().get(invoiceItem.getSelectedPosition()).getPriceVatSale());

            // VAT Rate
            double vat_rate = 0;
            vat_rate = Double.parseDouble(invoiceItem.getChildList().get(invoiceItem.getSelectedPosition()).getVatRate()) * 0.01;
            BigDecimal vatRate = new BigDecimal(vat_rate);
            vatRate = vatRate.setScale(5, RoundingMode.HALF_UP);

            // Client Discount
            double client_discount = Double.parseDouble(client.getDiscount()) * 0.01;
//            BigDecimal clientDiscount = new BigDecimal(Double.toString(client_discount));
            if (invoiceItem.getType().equalsIgnoreCase("action")) {
                client_discount = 0;
            }


            // Article Discount
            double article_discount = Double.parseDouble(invoiceItem.getDiscount()) * 0.01;
            BigDecimal articleDiscount = new BigDecimal(article_discount);


            // Price no VAT with Discount, Price Base, Price no VAT, Price Real Sale
            BigDecimal vat_rate_for_divide = vatRate.add(new BigDecimal(1));

            BigDecimal price_base = priceSale.divide(vat_rate_for_divide, 5, RoundingMode.HALF_UP);

            // Price no VAT
            invoiceItem.setVleraPaTvsh(price_base.doubleValue());
            boolean role_price_base_vat_show = true;
            invoiceItem.setBasePrice(price_base.doubleValue());


            BigDecimal article_discount_for_divide = new BigDecimal(String.valueOf(articleDiscount));
            BigDecimal price_no_vat_no_discount_article_discount = price_base.multiply(article_discount_for_divide);
            BigDecimal price_no_vat_no_discount_article = price_base.subtract(price_no_vat_no_discount_article_discount);

            BigDecimal client_discount_for_divide = new BigDecimal(client_discount);
            BigDecimal client_discount_for_divide_discount = price_no_vat_no_discount_article.multiply(client_discount_for_divide);
            BigDecimal price_no_vat = price_no_vat_no_discount_article.subtract(client_discount_for_divide_discount);


            if (article_discount == 1 || client_discount == 1) {
                price_no_vat = price_no_vat.subtract(price_no_vat);
            }


            price_vat_real_sale = price_no_vat.multiply(vat_rate_for_divide);
            price_vat_real_sale = price_vat_real_sale.setScale(5, RoundingMode.HALF_UP);

            // Amount with VAT - Imagine
            BigDecimal amount_vat_imagine = priceSale.multiply(quantity);
            amount_vat_imagine = amount_vat_imagine.setScale(5, RoundingMode.HALF_UP);

            // Amount with VAT - Sale Real
            BigDecimal amount_vat_total = price_vat_real_sale.multiply(quantity);
            amount_vat_total = amount_vat_total.setScale(5, RoundingMode.HALF_UP);
            amount_with_vat = amount_vat_total.doubleValue();

            // Amount no VAT
            BigDecimal amount_no_vat = price_no_vat.multiply(quantity);
            amount_no_vat = amount_no_vat.setScale(5, RoundingMode.HALF_UP);

            // Amount of VAT
            BigDecimal amount_of_vat = amount_vat_total.subtract(amount_no_vat);
            amount_of_vat = amount_of_vat.setScale(5, RoundingMode.HALF_UP);

            // Amount of Discount
            BigDecimal amount_of_discount = amount_vat_imagine.subtract(amount_vat_total);
            amount_of_discount = amount_of_discount.setScale(5, RoundingMode.HALF_UP);


            // Price Sale
            price_vat_real_sale = price_vat_real_sale.setScale(5, RoundingMode.HALF_UP);
            invoiceItem.setPriceWithvat(price_vat_real_sale.doubleValue());

            // Amount no VAT
            invoiceItem.setVleraPaTvsh(amount_no_vat.doubleValue());
            // Amount VAT - Real Sale
            amount_vat_total = amount_vat_total.setScale(5, RoundingMode.HALF_UP);
            invoiceItem.setVleraTotale(amount_vat_total.doubleValue());
            // Amount of Discount
            invoiceItem.setVleraEZbritur(amount_of_discount.doubleValue());
            // Amount of VAT
            invoiceItem.setVleraETvsh(amount_of_vat.doubleValue());
            // Price for fiscal Printer
            invoiceItem.setCmimiNeArk(priceSale.doubleValue());


        } catch (Exception e) {
            System.out.println(" Error :" + e.getMessage());

        }
//        DecimalFormat dtime = new DecimalFormat("#.###");
//
//        return Double.valueOf(dtime.format(amount_with_vat)) ;


        List<Double> finalValues = new ArrayList<>();

        finalValues.add(amount_with_vat);
        finalValues.add(price_vat_real_sale.doubleValue());
        return finalValues;

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void createOrderPostItem() {
        InvoicePost invoicePost = new InvoicePost();
        invoicePost.setPartie_id(client.getId());
        invoicePost.setPartie_name(client.getName());
        if (client.getStations().size() > 0) {
            invoicePost.setPartie_station_id(client.getStations().get(stationPos).getId());
        } else {
            invoicePost.setPartie_station_id("0");
        }
        invoicePost.setPartie_station_name(binding.njesiaEdittext.getText().toString());
        invoicePost.setComment(binding.komentiEdittext.getText().toString());
        invoicePost.setPartie_address(client.getAddress());
        invoicePost.setPartie_city(client.getCity());
        invoicePost.setPartie_state_id(client.getState());
        invoicePost.setPartie_zip(client.getZip());
        //add a few
        invoicePost.setSale_station_id(preferences.getStationId());
        invoicePost.setInvoice_date(dDate);
        invoicePost.setData_ship(shDate);
        invoicePost.setDiscount(client.getDiscount());
        invoicePost.setAmount_no_vat(vleraPaTvsh);
        invoicePost.setAmount_of_vat(vleraETvsh);
        invoicePost.setAmount_discount(vleraZbritur);
        invoicePost.setAmount_payed(cash);//vlera qe e jep kesh
        invoicePost.setLocation(planetLocationManager.getLatitude() + "," + planetLocationManager.getLongitude());
        invoicePost.setAmount_with_vat(totaliFatures);
        invoicePost.setId_saler(preferences.getUserId());
        //is bill 0 = fature, 1 = kupon
        invoicePost.setIs_bill(InvoiceActivity.isBill);
        double totalNoDiscount = 0;
        RealmList<InvoiceItemPost> invoiceItemPosts = new RealmList<>();
        for (int i = 0; i < stockItems.size(); i++) {
            totalNoDiscount += (Double.parseDouble(stockItems.get(i).getChildList().get(stockItems.get(i).getSelectedPosition()).getPriceVatSale()) * Double.parseDouble(stockItems.get(i).getSasia()));
            InvoiceItemPost invoiceItemPost = new InvoiceItemPost();
//            invoiceItemPost.setId(stockItems.get(i).getId());
            invoiceItemPost.setNo_order(String.valueOf(i));
            invoiceItemPost.setNo(stockItems.get(i).selectedItemCode);
            invoiceItemPost.setId_item_group(stockItems.get(i).getId());
            invoiceItemPost.setId_item(stockItems.get(i).getItems().get(stockItems.get(i).getSelectedPosition()).getId());
            invoiceItemPost.setName(stockItems.get(i).getName());
            invoiceItemPost.setQuantity(stockItems.get(i).getSasia());
            invoiceItemPost.setUnit(stockItems.get(i).getSelectedUnit());
//
//            double vleraPaTvsh = stockItems.get(i).getVleraPaTvsh() / Double.parseDouble(stockItems.get(i).getSasia());
//            BigDecimal vleraNoTvsh = new BigDecimal(vleraPaTvsh);
//            invoiceItemPost.setPrice(String.valueOf(vleraNoTvsh) );

            invoiceItemPost.setPrice_base(stockItems.get(i).getBasePrice() + "");
            System.out.println("zbritja " + stockItems.get(i).getDiscount());
            invoiceItemPost.setDiscount(stockItems.get(i).getDiscount());
            invoiceItemPost.setVat_id(stockItems.get(i).getItems().get(stockItems.get(i).getSelectedPosition()).getVatCodeSale());
            invoiceItemPost.setPrice_vat(String.valueOf(stockItems.get(i).getPriceWithvat()));
            invoiceItemPost.setVat_rate(stockItems.get(i).getItems().get(stockItems.get(i).getSelectedPosition()).getVatRate());
            invoiceItemPosts.add(invoiceItemPost);
        }
        invoicePost.setItems(invoiceItemPosts);
        ArrayList<InvoicePost> invoicePosts = new ArrayList<>();
        invoicePosts.add(invoicePost);
        OrderPostObject orderPostObject = new OrderPostObject();
        orderPostObject.setToken(preferences.getToken());
        orderPostObject.setUser_id(preferences.getUserId());
        orderPostObject.setOrders(invoicePosts);


        apiService.postOrderFromInvoice(orderPostObject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(responseBody -> {

                    if (responseBody.getSuccess()) {
                        Toast.makeText(getApplicationContext(), R.string.porosia_u_krye_me_sukses, Toast.LENGTH_SHORT).show();
                        finish();

                    } else {
                        Toast.makeText(getApplicationContext(), responseBody.getError().getText(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(this, "gjuajtje errori per porosine!!!", Toast.LENGTH_SHORT).show();
                    }

                }, throwable -> {
                    throwable.printStackTrace();
                    Toast.makeText(getApplicationContext(), R.string.diqka_shkoj_gabim, Toast.LENGTH_SHORT).show();
                });


    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showCashDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.cash_dialog_layout, null);
        dialogBuilder.setView(dialogView);
        Button keshButton = dialogView.findViewById(R.id.kesh_button);
        Button pritjeButton = dialogView.findViewById(R.id.later_button);
        Button konfirmoButton = dialogView.findViewById(R.id.ok_button);
        EditText keshEditText = dialogView.findViewById(R.id.shuma_editText);
//        pritjeButton.setEnabled(true);
        keshEditText.setText(totaliFatures);
        double totali = Double.parseDouble(totaliFatures);
        pritjeButton.setEnabled(Double.parseDouble(client.getPaymentDeadline()) > 0 && totali < Double.parseDouble(client.getLimitBalance()));
        LinearLayout buttonHolder = dialogView.findViewById(R.id.button_holder);
        LinearLayout keshHolder = dialogView.findViewById(R.id.kesh_holder);
        keshButton.setOnClickListener(view -> {
            buttonHolder.setVisibility(View.GONE);
            keshHolder.setVisibility(View.VISIBLE);
        });
        pritjeButton.setOnClickListener(view -> {
            cash = "0";
            if (Double.parseDouble(cash) > 0) {
                isPaid = "1";
            } else {
                isPaid = "0";
            }
            if (InvoiceActivity.isBill.equalsIgnoreCase("1")) {
                openPrintingActivity();
            } else {
//                showPrintDialog();
//            binding.web.setVisibility(View.VISIBLE);
//            binding.web.bringToFront();
            }

        });
        AlertDialog alertDialog = dialogBuilder.create();

        konfirmoButton.setOnClickListener(view -> {
            cash = keshEditText.getText().toString();
            double csh = Double.parseDouble(cash);
            if ((totali - csh) > Double.parseDouble(client.getLimitBalance())) {
                Toast.makeText(getApplicationContext(), (String.valueOf(R.string.duhet_te_inkasohet_se_paku)) + (totali - Double.parseDouble(client.getLimitBalance())), Toast.LENGTH_SHORT).show();
            } else if (keshEditText.getText().length() > 0) {
                cash = keshEditText.getText().toString();
                if (totali == csh) {
                    isPaid = "1";
                } else {
                    isPaid = "0";
                }
                if (InvoiceActivity.isBill.equalsIgnoreCase("1")) {
                    openPrintingActivity();
                } else {
                    alertDialog.dismiss();
//                    showPrintDialog();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.ju_lutem_jepni_vleren_e_paguar, Toast.LENGTH_SHORT).show();
            }
        });
        alertDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showBillDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.cash_dialog_layout, null);
        dialogBuilder.setView(dialogView);
        Button keshButton = dialogView.findViewById(R.id.kesh_button);
        Button pritjeButton = dialogView.findViewById(R.id.later_button);
        Button konfirmoButton = dialogView.findViewById(R.id.ok_button);
        LinearLayout buttonHolder = dialogView.findViewById(R.id.button_holder);
        LinearLayout keshHolder = dialogView.findViewById(R.id.kesh_holder);

        keshButton.setText("Porosi");
//        pritjeButton.setText("Kupon Fiskal");

        AlertDialog alertDialog = dialogBuilder.create();

        keshButton.setOnClickListener(view -> {

            InvoiceActivity.isBill = "1";
            createOrderPostItem();
            alertDialog.dismiss();

        });
        pritjeButton.setOnClickListener(view -> {
            InvoiceActivity.isBill = "0";
            createOrderPostItem();
            alertDialog.dismiss();

        });
        pritjeButton.setVisibility(View.GONE);

        alertDialog.show();
    }
//
//    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//    private void showPrintDialog() {
//        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
//        LayoutInflater inflater = this.getLayoutInflater();
//        View dialogView = inflater.inflate(R.layout.printing_mode_dialog, null);
//        dialogBuilder.setView(dialogView);
//        Button print = dialogView.findViewById(R.id.print_button);
//        Button print80mm = dialogView.findViewById(R.id.print_80_button);
//        LinearLayout buttonHolder = dialogView.findViewById(R.id.button_holder);
//
//
//        AlertDialog alertDialog = dialogBuilder.create();
//
//        print.setOnClickListener(view -> {
//            createPostItem(true);
//            alertDialog.dismiss();
//        });
//        print80mm.setOnClickListener(view -> {
//            createPostItem(false);
//            alertDialog.dismiss();
//        });
//
//        alertDialog.show();
//    }

//    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (InvoiceActivity.uploadThisMf) {
//            InvoiceActivity.uploadThisMf = false;
//            createPostItem(isPrint);
//        }
//
//        String invoices = realmHelper.getInvoicesString();
//
//        List<InvoicePost> savedInvoices = (ArrayList<InvoicePost>) gson.fromJson(invoices,
//                new TypeToken<ArrayList<InvoicePost>>() {
//                }.getType());
//        unSyncedList = new ArrayList<>();
//
//
//        for (int i = 0; i < savedInvoices.size(); i++) {
//            if (!savedInvoices.get(i).getSynced()) {
//                unSyncedList.add(savedInvoices.get(i));
//            }
//        }
//


    @SuppressLint("SetTextI18n")
    private void
    addActionViews(ArrayList<InvoiceItem> items, int count) {

        final InvoiceItem invoiceItem = items.get(0);
        final int[] actionCount = {count};
        int[] positions = new int[invoiceItem.getItems().size()];
        for (int h = 0; h < invoiceItem.getItems().size(); h++) {
            Item stock = realmHelper.getItemsByid(invoiceItem.getItems().get(h).getGroupItem());
            final SubItem[] item = {invoiceItem.getItems().get(h)};
            double availableQuantity = Double.parseDouble(stock.getQuantity()) / item[0].getRelacion();
            double sasia = Double.parseDouble(item[0].getQuantity()) * actionCount[0];
            if (sasia <= availableQuantity) {
                sasia = Double.parseDouble(item[0].getQuantity()) * actionCount[0];
            } else {
                sasia = 0;
            }

            final InvoiceItem[] im = new InvoiceItem[1];

            im[0] = items.get(h);
            im[0].setQuantity(stock.getQuantity());
            stockItems.add(items.get(h));
            ActionInvItemBinding itemBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.action_inv_item, binding.invoiceItemHolder, false);

            itemBinding.shifraTextview.setEnabled(false);
            itemBinding.emertimiTextview.setEnabled(false);
            itemBinding.njesiaTextview.setEnabled(false);
            itemBinding.sasiaTextview.setEnabled(false);

            itemBinding.shifraTextview.setText(item[0].getNumber());
            itemBinding.emertimiTextview.setText(item[0].getName());
            itemBinding.njesiaTextview.setText(item[0].getUnit());
            itemBinding.sasiaTextview.setText("" + sasia);
            itemBinding.zbritjaTextview.setText(item[0].getDiscount());
            itemBinding.basePrice.setText(invoiceItem.getBasePrice() + "");


            if (h == invoiceItem.getItems().size() - 1) {
                itemBinding.buttonHolder.setVisibility(View.VISIBLE);
            } else {
                itemBinding.buttonHolder.setVisibility(View.GONE);
            }
            itemBinding.addButton.setOnClickListener(view -> {
                actionCount[0]++;
                for (int i = positions.length; i > 0; i--) {
                    binding.invoiceItemHolder.removeViewAt(positions[i - 1]);
                    stockItems.remove(positions[i - 1]);
                }
                addActionViews(items, actionCount[0]);

            });
            itemBinding.subButton.setOnClickListener(view -> {
                if (actionCount[0] > 1) {
                    actionCount[0]--;
                    for (int i = positions.length; i > 0; i--) {
                        binding.invoiceItemHolder.removeViewAt(positions[i - 1]);
                        stockItems.remove(positions[i - 1]);
                    }
                    addActionViews(items, actionCount[0]);
                }
            });
            itemBinding.removeButton.setOnClickListener(view -> {
                for (int i = positions.length; i > 0; i--) {

                    binding.invoiceItemHolder.removeViewAt(positions[i - 1]);
                    stockItems.remove(positions[i - 1]);
                }
                calculateSasiaTotale();
                calculateArtikujtTotal();
                calculateTotal();
                calculateVleraPaTvsh();
                calculateVleraEZbritur();
                calculateVleraETVSH();
            });
            double price = Double.parseDouble(item[0].getPriceVatSale()) - (Double.parseDouble(item[0].getPriceVatSale()) * Double.parseDouble(item[0].getDiscount()) * 0.01);
            BigDecimal priceDec = new BigDecimal(price);
            priceDec = priceDec.setScale(2, RoundingMode.HALF_UP);
            itemBinding.cmimiTvsh.setText("" + priceDec);
            double vlera = price * Double.parseDouble(item[0].getQuantity());
            BigDecimal vleraDec = new BigDecimal(vlera);
            vleraDec = vleraDec.setScale(2, RoundingMode.HALF_UP);
            itemBinding.vlera.setText("" + vleraDec);
            positions[h] = binding.invoiceItemHolder.getChildCount();
            itemBinding.getRoot().setTag(binding.invoiceItemHolder.getChildCount());
            stockItems.get(positions[h]).setId(invoiceItem.getGroupItem());
            stockItems.get(positions[h]).setDiscount(invoiceItem.getItems().get(h).getDiscount());
            stockItems.get(positions[h]).setSelectedItemCode(invoiceItem.getItems().get(h).getNumber());
            stockItems.get(positions[h]).setSelectedUnit(invoiceItem.getItems().get(h).getUnit());
            stockItems.get(positions[h]).setSelectedPosition(h);
            stockItems.get(positions[h]).setBaseDiscount(invoiceItem.getDiscount());
            stockItems.get(positions[h]).setSasia(String.valueOf(sasia));
            stockItems.get(positions[h]).setName(invoiceItem.getItems().get(h).getName());
            stockItems.get(positions[h]).setBarcode(invoiceItem.getItems().get(h).getBarcode());
            stockItems.get(positions[h]).setAction(true);
            stockItems.get(positions[h]).setCollection(true);
            calculateValueOfItem(stockItems.get(positions[h]));
            itemBinding.basePrice.setText(String.valueOf(stockItems.get(positions[h]).getBasePrice()));

            binding.invoiceItemHolder.addView(itemBinding.getRoot());
            calculation_form();
            calculateTotal();
            calculateVleraPaTvsh();
            calculateVleraEZbritur();
            calculateVleraETVSH();
        }

    }


    private void checkedQuantity() {
        binding.loader.setVisibility(View.VISIBLE);
        if (stockItems.size() > 0) {
            InvoiceItem stock = stockItems.get(stockItems.size() - 1);
            String stockItemId = stockItems.get(stockItems.size() - 1).getItems().get(stockItems.get(stockItems.size() - 1).getSelectedPosition()).getId();

            CheckQuantity checkQuantity = new CheckQuantity(preferences.getUserId(), preferences.getToken(), stock.getSasia(), preferences.getStationId(), dDate, stockItemId);
            Log.e("endritiQoke", checkQuantity.toString());

            apiService.checkQuantity(checkQuantity).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(invoiceUploadResponse -> {
                        if (!invoiceUploadResponse.getSuccess()) {
                            Toast.makeText(getApplicationContext(), invoiceUploadResponse.getError().getText(), Toast.LENGTH_SHORT).show();

                        } else {
                            addOrderItem();
                            Toast.makeText(getApplicationContext(), R.string.artikulli_eshte_ne_stok, Toast.LENGTH_SHORT).show();
                        }
                        binding.loader.setVisibility(View.GONE);
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {

                            throwable.printStackTrace();
                        }
                    });
        } else {
            binding.loader.setVisibility(View.GONE);
            addOrderItem();
        }
    }

    @SuppressLint({"SetTextI18n", "NonConstantResourceId"})
    private void calculation_form() {
//        showLoader();


        // Init

        for (InvoiceItem stock : stockItems) {
            if (!stock.getType().equals("action")) {
                stock.setAction(false);
            }
        }
        // Action with Amount and Count - Step Action
        allActions();

        for (InvoiceItem stock : stockItems) {
            if (!stock.getType().equals("action")) {

                final int childCount = binding.invoiceItemHolder.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    ViewGroup v = (ViewGroup) binding.invoiceItemHolder.getChildAt(i);

                    final int childCount2 = v.getChildCount();

                    for (int j = 0; j < childCount2; j++) {
                        View v2 = v.getChildAt(j);

                        if (v2.getId() == R.id.emertimi_textview) {
                            AutoCompleteTextView emertimi = (AutoCompleteTextView) v2;
                            if (emertimi.getText().toString().equals(stock.getName())) {
                                for (int k = 0; k < childCount2; k++) {
                                    View v3 = v.getChildAt(k);
                                    List<Double> amontAndPrice = calculateValueOfItem(stock);

                                    switch (v3.getId()) {
                                        case R.id.zbritja_textview:
                                            AutoCompleteTextView zbrijta = (AutoCompleteTextView) v3;
                                            zbrijta.setText(stock.getDiscount());
                                            break;

                                        case R.id.base_price:
                                            AutoCompleteTextView base = (AutoCompleteTextView) v3;
                                            base.setText(stock.getBasePrice() + "");
                                            break;

                                        case R.id.cmimi_tvsh:
                                            AutoCompleteTextView cmimitvsh = (AutoCompleteTextView) v3;

                                            cmimitvsh.setText("" + String.format(Locale.ENGLISH, "%.2f", BigDecimal.valueOf(amontAndPrice.get(1))));
                                            break;

                                        case R.id.vlera:
                                            AutoCompleteTextView vlera = (AutoCompleteTextView) v3;
                                            vlera.setText("" + String.format(Locale.ENGLISH, "%.2f", BigDecimal.valueOf(amontAndPrice.get(0))));
                                            break;

                                    }
                                }

                            }
                        }
                    }


                }

            }


        }


        // Form Totals
        calculateTotal();
        calculateVleraPaTvsh();
        calculateVleraEZbritur();
        calculateVleraETVSH();
//        hideLoader();
    }

    private InvoiceItem checkIfArticleIsInAction(InvoiceItem invoiceItem) {
        //check product action
        for (int i = 0; i < actionData.getArticleItems().size(); i++) {
            if (actionData.getArticleItems().get(i).getType().equals("specific")) {
                boolean clientCat = actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                boolean itemId = invoiceItem.getItems().get(invoiceItem.getSelectedPosition()).getId().equalsIgnoreCase(actionData.getArticleItems().get(i).getItem_id());
                int clientId = Integer.parseInt(client.getId());
                int action_client_id = actionData.getArticleItems().get(i).getClientId();
                boolean isClientWithAction = clientId == action_client_id;
                boolean groupId = invoiceItem.getItems().get(invoiceItem.getSelectedPosition()).getGroupItem().equalsIgnoreCase(actionData.getArticleItems().get(i).getGroupItem());
                boolean type = invoiceItem.getType().equalsIgnoreCase("single");
                boolean relation = invoiceItem.getItems().get(invoiceItem.getSelectedPosition()).getRelacion() >= Double.parseDouble(actionData.getArticleItems().get(i).getRelacion());
                //guxh bab mos e prek se DIGJESH!!!!

                if (isClientWithAction && (itemId || (groupId && relation && type))) {
                    invoiceItem.setAction(true);
                    invoiceItem.setMinQuantityForDiscount(actionData.getArticleItems().get(i).getQuantity());
//                    invoiceItem.setExtraDiscount(actionData.getArticleItems().get(i).getDiscount());

                    double totalDiscount = Double.parseDouble(invoiceItem.getDiscount()) + Double.parseDouble(invoiceItem.getExtraDiscount());
                    invoiceItem.setDiscount(actionData.getArticleItems().get(i).getDiscount());
                    break;
                } else if ((clientCat) && (itemId || (groupId && relation && type)) && (action_client_id == 0)) {
                    invoiceItem.setAction(true);
                    invoiceItem.setMinQuantityForDiscount(actionData.getArticleItems().get(i).getQuantity());
//                    invoiceItem.setExtraDiscount(actionData.getArticleItems().get(i).getDiscount());
                    double totalDiscount = Double.parseDouble(invoiceItem.getDiscount())
                            + Double.parseDouble(invoiceItem.getExtraDiscount());
                    invoiceItem.setDiscount(actionData.getArticleItems().get(i).getDiscount());
                    break;
                } else {
                    invoiceItem.setAction(false);
                    invoiceItem.setMinQuantityForDiscount("0");
                    invoiceItem.setExtraDiscount("0");
                    invoiceItem.setDiscount("0");
                }
            }
        }
        //check brand action
        for (int i = 0; i < actionData.getArticleBrandItem().size(); i++) {
            if (actionData.getArticleBrandItem().get(i).getType().equals("specific")) {
                boolean clientCat = actionData.getArticleBrandItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleBrandItem().get(i).getClientCategory().equalsIgnoreCase("0");
                boolean has_brand = invoiceItem.getBrand().equalsIgnoreCase(actionData.getArticleBrandItem().get(i).getBrandId());
                boolean checkUnit = checkUnitForAction(invoiceItem, actionData.getArticleBrandItem().get(i).getUnit());


                if (has_brand && clientCat && checkUnit) {
                    invoiceItem.setAction(true);
                    invoiceItem.setMinQuantityForDiscount(actionData.getArticleBrandItem().get(i).getQuantity());
//                    invoiceItem.setExtraDiscount(actionData.getArticleBrandItem().get(i).getDiscount());
                    invoiceItem.setDiscount(actionData.getArticleBrandItem().get(i).getDiscount());
                    break;
                }

            }
        }
        //check category action
        for (int i = 0; i < actionData.getArticleCategoryItem().size(); i++) {
            if (actionData.getArticleCategoryItem().get(i).getType().equals("specific")) {
                boolean clientCat = actionData.getArticleCategoryItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                boolean has_item_category = invoiceItem.getCategory().equalsIgnoreCase(actionData.getArticleCategoryItem().get(i).getCategoryId());
                boolean checkUnit = checkUnitForAction(invoiceItem, actionData.getArticleCategoryItem().get(i).getUnit());

                if (has_item_category && clientCat && checkUnit) {
                    invoiceItem.setAction(true);
                    invoiceItem.setMinQuantityForDiscount(actionData.getArticleCategoryItem().get(i).getQuantity());
//                    invoiceItem.setExtraDiscount(actionData.getArticleCategoryItem().get(i).getDiscount());
                    invoiceItem.setDiscount(actionData.getArticleCategoryItem().get(i).getDiscount());

                    break;
                }

            }
        }
        return invoiceItem;
    }

    public Boolean checkUnitForAction(InvoiceItem invoiceItem, String unit) {

        if (unit.equals(invoiceItem.getSelectedUnit())) {
            return true;
        } else {

            SubItem actionItem = new SubItem();
            SubItem mainItem = new SubItem();

            mainItem.setUnit(invoiceItem.getSelectedUnit());
            actionItem.setUnit(unit);

            for (SubItem item : invoiceItem.getItems()) {

                if (item.getUnit().equals(actionItem.getUnit())) {
                    actionItem.setRelacion(String.valueOf(item.getRelacion()));
                }

                if (item.getUnit().equals(mainItem.getUnit())) {
                    mainItem.setRelacion(String.valueOf(item.getRelacion()));
                }
            }

            return mainItem.getRelacion() >= actionItem.getRelacion();
        }

    }

    public String[] removeNullElements(String[] firstArray) {
        ArrayList<String> list = new ArrayList<String>();
        for (String s : firstArray)
            if (s != null) {
                if (!s.equals("")) {
                    list.add(s);
                }
            }

        return firstArray = list.toArray(new String[list.size()]);
    }

    public void allActions() {

        ArticleActionWithAmount();
        ArticleActionWithQuantity();

        brandActionWithAmount();
        brandActionWithQuantity();

        CategoryActionWithAmount();
        CategoryActionWithQuantity();
    }

    public void brandActionWithAmount() {

        //check brand with amount
        for (int i = 0; i < actionData.getArticleBrandItem().size(); i++) {
            if (actionData.getArticleBrandItem().get(i).getType().equals("amount")) {
                ActionBrandItem brend = actionData.getArticleBrandItem().get(i);
                float amount = 0;
                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleBrandItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getBrand().equalsIgnoreCase(actionData.getArticleBrandItem().get(i).getBrandId())
                            && clientCat && !stockItems.get(j).isAction()) {
                        amount += stockItems.get(j).getVleraTotale();
                    }
                }

                // Steps
                float discount = 0;

                for (ActionSteps steps : brend.getSteps()) {
                    float from = Float.parseFloat(steps.from);
                    float to = Float.parseFloat(steps.to);
                    if ((from <= amount) && (amount <= to)) {

                        if ((discount < Float.parseFloat(steps.discount))) {
                            discount = Float.parseFloat(steps.discount);
                        }
                    }
                }


                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleBrandItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getBrand().equalsIgnoreCase(actionData.getArticleBrandItem().get(i).getBrandId())
                            && clientCat && !stockItems.get(j).isAction()) {

                        stockItems.get(j).setAction(true);
                        stockItems.get(j).setDiscount(String.valueOf(discount));
                    }
                }
            }
        }

    }

    public void brandActionWithQuantity() {

        //check brand with amount
        for (int i = 0; i < actionData.getArticleBrandItem().size(); i++) {
            if (actionData.getArticleBrandItem().get(i).getType().equals("quantity")) {
                ActionBrandItem brend = actionData.getArticleBrandItem().get(i);
                float amount = 0;
                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleBrandItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getBrand().equalsIgnoreCase(actionData.getArticleBrandItem().get(i).getBrandId())
                            && clientCat && !stockItems.get(j).isAction()) {
                        amount++;
                    }
                }


                // Steps
                float discount = 0;

                for (ActionSteps steps : brend.getSteps()) {
                    float from = Float.parseFloat(steps.from);
                    float to = Float.parseFloat(steps.to);
                    if ((from <= amount) && (amount <= to)) {

                        if ((discount < Float.parseFloat(steps.discount))) {
                            discount = Float.parseFloat(steps.discount);
                        }
                    }
                }

                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleBrandItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getBrand().equalsIgnoreCase(actionData.getArticleBrandItem().get(i).getBrandId())
                            && clientCat && !stockItems.get(j).isAction()) {

                        stockItems.get(j).setAction(true);
                        stockItems.get(j).setDiscount(String.valueOf(discount));
                    }
                }
            }
        }

    }

    public void ArticleActionWithAmount() {

        //check brand with amount
        for (int i = 0; i < actionData.getArticleItems().size(); i++) {
            if (actionData.getArticleItems().get(i).getType().equals("amount")) {
                ActionArticleItems article = actionData.getArticleItems().get(i);
                float amount = 0;
                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getId().equalsIgnoreCase(actionData.getArticleItems().get(i).getItem_id())
                            && clientCat && !stockItems.get(j).isAction()) {
                        amount += stockItems.get(j).getVleraTotale();
                    }
                }

                // Steps
                float discount = 0;

                for (ActionSteps steps : article.getSteps()) {
                    float from = Float.parseFloat(steps.from);
                    float to = Float.parseFloat(steps.to);
                    if ((from <= amount) && (amount <= to)) {

                        if ((discount < Float.parseFloat(steps.discount))) {
                            discount = Float.parseFloat(steps.discount);
                        }
                    }
                }


                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getId().equalsIgnoreCase(actionData.getArticleItems().get(i).getItem_id())
                            && clientCat && !stockItems.get(j).isAction()) {
                        stockItems.get(j).setAction(true);
                        stockItems.get(j).setDiscount(String.valueOf(discount));
                    }
                }
            }
        }

    }

    public void ArticleActionWithQuantity() {

        //check brand with amount
        for (int i = 0; i < actionData.getArticleItems().size(); i++) {
            if (actionData.getArticleItems().get(i).getType().equals("quantity")) {
                ActionArticleItems article = actionData.getArticleItems().get(i);
                float amount = 0;
                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getId().equalsIgnoreCase(actionData.getArticleItems().get(i).getItem_id())
                            && clientCat && !stockItems.get(j).isAction()) {
                        amount++;
                    }
                }

                // Steps
                float discount = 0;

                for (ActionSteps steps : article.getSteps()) {
                    float from = Float.parseFloat(steps.from);
                    float to = Float.parseFloat(steps.to);
                    if ((from <= amount) && (amount <= to)) {

                        if ((discount < Float.parseFloat(steps.discount))) {
                            discount = Float.parseFloat(steps.discount);
                        }
                    }
                }

                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getId().equalsIgnoreCase(actionData.getArticleItems().get(i).getItem_id())
                            && clientCat && !stockItems.get(j).isAction()) {
                        stockItems.get(j).setAction(true);
                        stockItems.get(j).setDiscount(String.valueOf(discount));
                    }
                }
            }
        }

    }

    public void CategoryActionWithAmount() {

        //check category with amount
        for (int i = 0; i < actionData.getArticleCategoryItem().size(); i++) {
            if (actionData.getArticleCategoryItem().get(i).getType().equals("amount")) {
                ActionCategoryItem category = actionData.getArticleCategoryItem().get(i);
                float amount = 0;
                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleCategoryItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getCategory().equalsIgnoreCase(actionData.getArticleCategoryItem().get(i).getCategoryId())
                            && clientCat && !stockItems.get(j).isAction()) {
                        amount += stockItems.get(j).getVleraTotale();
                    }
                }

                // Steps
                float discount = 0;

                for (ActionSteps steps : category.getSteps()) {
                    float from = Float.parseFloat(steps.from);
                    float to = Float.parseFloat(steps.to);
                    if ((from <= amount) && (amount <= to)) {

                        if ((discount < Float.parseFloat(steps.discount))) {
                            discount = Float.parseFloat(steps.discount);
                        }
                    }
                }


                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleCategoryItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getCategory().equalsIgnoreCase(actionData.getArticleCategoryItem().get(i).getCategoryId())
                            && clientCat && !stockItems.get(j).isAction()) {
                        stockItems.get(j).setAction(true);
                        stockItems.get(j).setDiscount(String.valueOf(discount));
                    }
                }
            }
        }

    }

    public void CategoryActionWithQuantity() {

        //check category with amount
        for (int i = 0; i < actionData.getArticleCategoryItem().size(); i++) {
            if (actionData.getArticleCategoryItem().get(i).getType().equals("quantity")) {
                ActionCategoryItem category = actionData.getArticleCategoryItem().get(i);
                float amount = 0;
                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleCategoryItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getCategory().equalsIgnoreCase(actionData.getArticleCategoryItem().get(i).getCategoryId())
                            && clientCat && !stockItems.get(j).isAction()) {
                        amount++;
                    }
                }

                // Steps
                float discount = 0;

                for (ActionSteps steps : category.getSteps()) {
                    float from = Float.parseFloat(steps.from);
                    float to = Float.parseFloat(steps.to);
                    if ((from <= amount) && (amount <= to)) {

                        if ((discount < Float.parseFloat(steps.discount))) {
                            discount = Float.parseFloat(steps.discount);
                        }
                    }
                }


                for (int j = 0; j < stockItems.size(); j++) {
                    boolean clientCat = actionData.getArticleCategoryItem().get(i).getClientCategory().equalsIgnoreCase(client.getClientCategory()) || actionData.getArticleItems().get(i).getClientCategory().equalsIgnoreCase("0");
                    if (stockItems.get(j).getCategory().equalsIgnoreCase(actionData.getArticleCategoryItem().get(i).getCategoryId())
                            && clientCat && !stockItems.get(j).isAction()) {
                        stockItems.get(j).setAction(true);
                        stockItems.get(j).setDiscount(String.valueOf(discount));
                    }
                }
            }
        }

    }

    @SuppressLint("SetTextI18n")
    public void calculateArtikujtTotal() {
        int artTotal = 0;
        for (int i = 0; i < stockItems.size(); i++) {
            String cap = stockItems.get(i).getName().trim();
            if (cap.length() > 0) {
                artTotal++;
            }
        }
        this.nrArtikujveTotal = String.valueOf(round(BigDecimal.valueOf(artTotal)));
//        this.nrArtikujtTotal = String.valueOf(round(BigDecimal.valueOf(artTotal)));
        binding.artikujTeZgjedhur.setText("Nr. i artikujve te zgjedhur : " + artTotal);
    }

    @SuppressLint("SetTextI18n")
    public void calculateSasiaTotale() {
        double quaTotal = 0;
        for (int i = 0; i < stockItems.size(); i++) {
            quaTotal += Double.parseDouble(stockItems.get(i).getSasia());
        }
        this.sasiaTotale = String.valueOf(round(BigDecimal.valueOf(quaTotal)));
        binding.artikujtSasiaTotale.setText("Sasia Totale : " + round(BigDecimal.valueOf(quaTotal)));
    }

    @SuppressLint("SetTextI18n")
    public void calculateTotal() {
        double total = 0;
        for (int i = 0; i < stockItems.size(); i++) {
            total += stockItems.get(i).getVleraTotale();
        }
        this.totaliFatures = String.valueOf(round(BigDecimal.valueOf(total)));
        binding.vleraTotale.setText("Vlera Totale: " + round(BigDecimal.valueOf(total)));
    }

    @SuppressLint("SetTextI18n")
    public void calculateVleraPaTvsh() {
        double vleraPaTvsh = 0;
        for (int i = 0; i < stockItems.size(); i++) {
            vleraPaTvsh += stockItems.get(i).getVleraPaTvsh();
        }
        this.vleraPaTvsh = String.valueOf(round(BigDecimal.valueOf(vleraPaTvsh)));
        binding.vleraPaTvsh.setText("Vlera pa TVSH: " + round(BigDecimal.valueOf(vleraPaTvsh)));
    }

    @SuppressLint("SetTextI18n")
    public void calculateVleraEZbritur() {
        double vleraEZbritur = 0;
        for (int i = 0; i < stockItems.size(); i++) {
            vleraEZbritur += stockItems.get(i).getVleraEZbritur();
        }
        this.vleraZbritur = String.valueOf(round(BigDecimal.valueOf(vleraEZbritur)));
        binding.vleraEZbritur.setText("Vlera e zbritur: " + round(BigDecimal.valueOf(vleraEZbritur)));
    }

    @SuppressLint("SetTextI18n")
    public void calculateVleraETVSH() {
        double vleraETvsh = 0;
        for (int i = 0; i < stockItems.size(); i++) {
            vleraETvsh += stockItems.get(i).getVleraETvsh();
        }
        this.vleraETvsh = String.valueOf(round(BigDecimal.valueOf(vleraETvsh)));
        binding.vleraETvsh.setText("Vlera e TVSH-së: " + round(BigDecimal.valueOf(vleraETvsh)));
    }


    public void addFragment(int view, Fragment fragment) {
        try {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(view, fragment);
            ft.commitAllowingStateLoss();
        } catch (Exception e) {
            e.getMessage();
        }

    }

    interface DoYouWantToDeleteThisArticleListener {
        void Yes();
    }

    public static BigDecimal round(BigDecimal number) {
        return number.setScale(2, RoundingMode.HALF_UP);
    }
}