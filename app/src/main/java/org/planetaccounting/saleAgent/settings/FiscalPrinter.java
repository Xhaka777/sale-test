package org.planetaccounting.saleAgent.settings;

import android.annotation.SuppressLint;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.api.ApiService;
import org.planetaccounting.saleAgent.databinding.ActivityFiscalPrinterBinding;
import org.planetaccounting.saleAgent.databinding.FiscalPrinterItemBinding;
import org.planetaccounting.saleAgent.fiscalCoupon.FiscalCoupon;
import org.planetaccounting.saleAgent.fiscalCoupon.PrintTremol;
import org.planetaccounting.saleAgent.model.fiscalPrinter.Printer;
import org.planetaccounting.saleAgent.persistence.RealmHelper;
import org.planetaccounting.saleAgent.utils.Preferences;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import TremolZFP.FP;
import TremolZFP.FPcore;
import TremolZFP.StatusRes;
import io.realm.Realm;
import io.realm.RealmResults;

public class FiscalPrinter extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    ActivityFiscalPrinterBinding binding;

    @Inject
    RealmHelper realmHelper;
    @Inject
    ApiService apiService;
    @Inject
    Preferences preferences;

    private Spinner printerSpinner;
    ArrayAdapter<String> adapter;
    public FPcore fPcore;
    public FP fp;
    StatusRes status;
    //na duhet per me thirr te dhena nga dataBaza
    Realm realm;
    private boolean buttonsShown = false;

    public FiscalPrinter(){
        fPcore = new FPcore();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_fiscal_printer);
        Kontabiliteti.getKontabilitetiComponent().inject(this);

        realm = Realm.getDefaultInstance();

        //mbushi printerat me te dhena...
        Printer printer = new Printer("0","Zgjedhni Printerin...", "kun-hiq", 0);
        Printer printer1 = new Printer("1", "Tremol", "FP-15", 0);
        Printer printer2 = new Printer("2", "Dateks", "FP-700", 0);

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.insertOrUpdate(printer);
                realm.insertOrUpdate(printer1);
                realm.insertOrUpdate(printer2);
            }
        });
        realm.close();

        //declared spinner for printers
        printerSpinner = findViewById(R.id.dropdown_spinner);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getPrinterNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        printerSpinner.setAdapter(adapter);

        //dy llojet e printerave :
        //Tremol FP-15
        //Dateks Fp-700

        printerSpinner.setOnItemSelectedListener(this);

        // Set a listener to update the selected printer when the user makes a selection
        printerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0) {
                    return;
                } else {

                    //Handle the selected item
                    //printeri i selektuar ne dropdown...
                    String selectedPrinterName = parent.getItemAtPosition(position).toString();

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            //Get all printers from the database
                            RealmResults<Printer> printers = realm.where(Printer.class).findAll();

                            //Iterate over the printers and set their selected values to 0, except for the selected printer
                            for (Printer printer : printers) {
                                if (printer.getName().equals(selectedPrinterName)) {
                                    printer.setSelected(1);
                                    //Check is the selected printer has 'selected' atribute set to 1
                                    if (printer.getSelected() == 1 && !(printer.getName().equals("Zgjedhni Printerin..."))) {
                                        //Calling my method for itemBinding
                                        if (!buttonsShown) {
                                            addPrinterInfo();
                                            //Set the boolean variable to true to indicate that the buttons have been shown
                                            buttonsShown = true;
                                        }
                                    }
                                } else {
                                    printer.setSelected(0);
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedOption = parent.getItemAtPosition(position).toString();
        Toast.makeText(this, "You Selected: " + selectedOption, Toast.LENGTH_SHORT).show();
//        addPrinterInfo();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //kur activiteti jone bohet destroy pjesa e databazes mu bo close
    //per arsye qe mos me pas leaks...
    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    //Helper method to get a list of printer names from the database...
    private List<String> getPrinterNames(){
        List<String> printerNames = new ArrayList<>();
        for (Printer printer : realm.where(Printer.class).findAll()){
            printerNames.add(printer.getName());
        }
        return  printerNames;
    }

    @SuppressLint("ResourceAsColor")
    private void addPrinterInfo(){
        FiscalPrinterItemBinding itemBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.fiscal_printer_item,binding.fiscalPrinterHolder, false);

        //butoni per me connect telin me print
        itemBinding.findDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        //butoni qe e thirr komanden cut-paper (kun hiq)
        itemBinding.cutPaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print_tremol_commands("CUT");
            }
        });

        //butoni qe e thirr komanden per statusin me printer
        //a esht Online apo offline
        itemBinding.statusButton.setOnClickListener(new View.OnClickListener() {
            final String server_address = "http://localhost:4444/";
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                fp = make_connection(server_address);
                try {
//                    StatusRes statusRes = new StatusRes();
//                    boolean sn = fp.ReadStatus().Article_report_is_not_zeroed;
                    status = fp.ReadStatus();
                    if (status.Article_report_is_not_zeroed){
                        itemBinding.statusButton.setBackground(itemBinding.statusButton.getContext().getResources().getDrawable(R.drawable.status_active));
                    }else{
                        itemBinding.statusButton.setBackground(itemBinding.statusButton.getContext().getResources().getDrawable(R.drawable.status_inactive));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //butoni qe e thirr komanden me bo test printerin a esht gjall a jo...
        itemBinding.testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print_tremol_commands("TEST");
            }
        });


        itemBinding.getRoot().setTag(binding.fiscalPrinterHolder.getChildCount());
        binding.fiscalPrinterHolder.addView(itemBinding.getRoot());
    }

    public FP make_connection(String server_address) {
        FP fp = new FP();
        fp.setServerAddress(server_address);
        return fp;
    }

    public FP status_printer(String status) throws Exception {
        FP fp = new FP();
        fp.ReadStatus();

        return fp;
    }

    //komanda kryesore qe na vyne per mi thirr komandat tjera neper butona...
    public void print_tremol_commands(String command){
        PrintTremol coupon = new PrintTremol(false, command);
        try {
            coupon.print_coupon();
        }catch (Exception e){
            System.out.println("gati jem :" + e);
        }
    }
}