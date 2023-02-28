package org.planetaccounting.saleAgent.kthemallin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.print.PrintManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.api.ApiService;
import org.planetaccounting.saleAgent.databinding.ActivityReturnListBinding;
import org.planetaccounting.saleAgent.escpostprint.EscPostPrintFragment;
import org.planetaccounting.saleAgent.events.RePrintInvoiceEvent;
import org.planetaccounting.saleAgent.invoice.InvoiceListActivity;
import org.planetaccounting.saleAgent.model.clients.Client;
import org.planetaccounting.saleAgent.model.invoice.InvoicePost;
import org.planetaccounting.saleAgent.persistence.RealmHelper;
import org.planetaccounting.saleAgent.raportet.raportmodels.InvoiceForReportObject;
import org.planetaccounting.saleAgent.raportet.raportmodels.RaportsPostObject;
import org.planetaccounting.saleAgent.raportet.raportmodels.ReportsList;
import org.planetaccounting.saleAgent.utils.InvoicePrintUtil;
import org.planetaccounting.saleAgent.utils.PaginationScrollListener;
import org.planetaccounting.saleAgent.utils.Preferences;
import org.planetaccounting.saleAgent.utils.ReturnPrintUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ReturnListActivity extends AppCompatActivity {

    ActivityReturnListBinding binding;

    @Inject
    RealmHelper realmHelper;
    @Inject
    Preferences preferences;
    @Inject
    ApiService apiService;
    RecyclerView recyclerView;
    ReturnListAdapter adapter;
    RecyclerView.LayoutManager mLayoutManager;
    String dDate;
    ArrayList<InvoicePost> ret;
    List<InvoicePost> returnPosts = new ArrayList<>();
    List<InvoicePost> unSyncedList = new ArrayList<>();
    ArrayList<InvoicePost> searchResults = new ArrayList<>();
    List<InvoicePost> savedInvoices;
    InvoicePost invoicePost;
    WebView webView;
    RelativeLayout loader;
    FrameLayout fragment;
    PrintManager printManager;
    private boolean isLoading = false;
    int totalPage = 0;
    int currentPage = 0;

    String from;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_return_list);

        ((Kontabiliteti) getApplication()).getKontabilitetiComponent().inject(this);
        printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
        Date cDate = new Date();
        dDate = new SimpleDateFormat("yyyy-MM-dd").format(cDate);

        recyclerView = (RecyclerView) findViewById(R.id.return_list);
        webView = (WebView) findViewById(R.id.web);
        loader = findViewById(R.id.loader);
        fragment = findViewById(R.id.fragment);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        String returns = realmHelper.getAllReturnInvoicesString();

        ret = (ArrayList<InvoicePost>) new Gson().fromJson(returns,
                new TypeToken<ArrayList<InvoicePost>>() {
                }.getType());

        getReturnReports();


        adapter = new ReturnListAdapter(ret);
        recyclerView.setAdapter(adapter);

        //qet pjesen duhet me analizu

        String returnsS = realmHelper.getReturnsString();
        Gson gson = new Gson();
        savedInvoices = (ArrayList<InvoicePost>) gson.fromJson(returnsS,
                new TypeToken<ArrayList<InvoicePost>>() {
                }.getType());
        unSyncedList = new ArrayList<>();
        for (int i = 0; i < savedInvoices.size(); i++) {
            if (!savedInvoices.get(i).getSynced()) {
                unSyncedList.add(savedInvoices.get(i));
            }
        }

        Button button = findViewById(R.id.sync);
        button.setOnClickListener(view -> uploadReturns());

        binding.searchEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchResults.clear();

                for (int j = 0; j < ret.size(); j++) {
                    if (ret.get(j).getPartie_name().toLowerCase().startsWith(s.toString().toLowerCase())) {
                        searchResults.add(ret.get(j));
                    }
                }
                if (s.length() > 0) {
                    adapter.setCompanies(searchResults);
                } else {
                    adapter.setCompanies(ret);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        recyclerView.addOnScrollListener(new PaginationScrollListener((LinearLayoutManager) mLayoutManager) {
            @Override
            protected void loadMoreItems() {
                loadNextPage();
            }

            @Override
            public int getTotalPageCount() {
                return totalPage;
            }

            @Override
            public boolean isLastPage() {
                return currentPage == totalPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void uploadReturns() {
        if (unSyncedList.size() > 0) {
            loader.setVisibility(View.VISIBLE);
            ReturnPostObject returnPostObject = new ReturnPostObject();
            returnPostObject.setToken(preferences.getToken());
            returnPostObject.setUser_id(preferences.getUserId());
            returnPostObject.setRetrunPost(savedInvoices);
            apiService.postReturn(returnPostObject)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(responseBody -> {
                        if (responseBody.getSuccess()) {
                            for (int i = 0; i < unSyncedList.size(); i++) {
                                unSyncedList.get(i).setSynced(true);
                                realmHelper.returnInvoice(unSyncedList.get(i));
                            }
                            loader.setVisibility(View.GONE);
                        } else {
                            loader.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(), responseBody.getError().getText(), Toast.LENGTH_SHORT).show();
                        }
                    }, throwable -> {
                        loader.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), "Faturat nuk u sinkronizuan!", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadNextPage() {
        isLoading = true;
        getReturnReports();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void getReturnReports() {
        RaportsPostObject raportsPostObject = new RaportsPostObject();
        raportsPostObject.setToken(preferences.getToken());
        raportsPostObject.setUser_id(preferences.getUserId());

        if (currentPage == 0) {
            if (!ret.isEmpty()) {
                raportsPostObject.setLast_document_number(ret.get(ret.size() - 1).getNo_invoice());
            }
            currentPage++;
            raportsPostObject.setPage(currentPage++);
        } else {
            raportsPostObject.setLast_document_number("");
            currentPage++;
            raportsPostObject.setPage(currentPage);
        }
        apiService.getRaportReturnList(raportsPostObject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(responseBody -> {
                    isLoading = false;

                    if (responseBody.getSuccess()) {
                        currentPage = responseBody.getCurrentPage();
                        totalPage = responseBody.getTotalPage();

                        for (ReportsList report : responseBody.data) {
                            InvoicePost invoice = new InvoicePost();
                            invoice.setReturnFromReports(report);
                            ret.add(invoice);
                        }
                        adapter.notifyItemRangeInserted(0, ret.size());
                        adapter.notifyDataSetChanged();

                    } else {
                        System.out.println("kun hiqqqqq!!!");
                    }
                }, throwable -> {
                    isLoading = false;

                });

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void printReturn(int id, boolean isPrint) {

        InvoiceForReportObject invoiceForReportObject = new InvoiceForReportObject();
        invoiceForReportObject.setToken(preferences.getToken());
        invoiceForReportObject.setUser_id(preferences.getUserId());
        invoiceForReportObject.setId(String.valueOf(id));
        apiService.getRaportReturnDetail(invoiceForReportObject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(responseBody -> {
                    if (responseBody.getSuccess()) {

                        invoicePost = responseBody.getData();

                        Client client = realmHelper.getClientFromName(invoicePost.getPartie_name());


                        if (isPrint) {
                            InvoicePrintUtil util = new InvoicePrintUtil(invoicePost, webView, this, client, printManager);
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), responseBody.getError().getText(), Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    Toast.makeText(getApplicationContext(), throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

                });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Subscribe
    public void onEvent(RePrintInvoiceEvent event) {
        if (event.getIsFromServer()) {
            showPrintDialog(event.getPosition());
        } else {
            String invoice = realmHelper.getInvoiceById(event.getPosition());
            Gson gson = new Gson();
            invoicePost = gson.fromJson(invoice, InvoicePost.class);
            Client client = realmHelper.getClientFromName(invoicePost.getPartie_name());

            showPrintDialog(new StatusClick() {
                @Override
                public void Printer() {
                    ReturnPrintUtil util = new ReturnPrintUtil(invoicePost, webView, ReturnListActivity.this, client, printManager);
                }

            });
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showPrintDialog(int id) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.printing_mode_dialog, null);
        dialogBuilder.setView(dialogView);
        Button print = (Button) dialogView.findViewById(R.id.print_button);
        Button print80mm = (Button) dialogView.findViewById(R.id.print_80_button);
        LinearLayout buttonHolder = (LinearLayout) dialogView.findViewById(R.id.button_holder);


        AlertDialog alertDialog = dialogBuilder.create();

        print.setOnClickListener(view -> {
            printReturn(id, true);
            alertDialog.dismiss();
        });


        alertDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showPrintDialog(ReturnListActivity.StatusClick listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.printing_mode_dialog, null);
        dialogBuilder.setView(dialogView);
        Button print = (Button) dialogView.findViewById(R.id.print_button);
        Button print80mm = (Button) dialogView.findViewById(R.id.print_80_button);
        LinearLayout buttonHolder = (LinearLayout) dialogView.findViewById(R.id.button_holder);


        AlertDialog alertDialog = dialogBuilder.create();

        print.setOnClickListener(view -> {
            listener.Printer();
            alertDialog.dismiss();
        });


        alertDialog.show();
    }

    interface StatusClick {
        void Printer();
    }
}
