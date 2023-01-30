package org.planetaccounting.saleAgent.order;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.MainActivity;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.api.ApiService;
import org.planetaccounting.saleAgent.databinding.OrdersListActivityBinding;
import org.planetaccounting.saleAgent.events.OpenOrderDetailEvent;
import org.planetaccounting.saleAgent.helper.LocaleHelper;
import org.planetaccounting.saleAgent.model.OrderDetailPost;
import org.planetaccounting.saleAgent.model.invoice.InvoicePost;
import org.planetaccounting.saleAgent.model.order.Order;
import org.planetaccounting.saleAgent.model.stock.StockPost;
import org.planetaccounting.saleAgent.persistence.RealmHelper;
import org.planetaccounting.saleAgent.utils.Preferences;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Locale;
import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by macb on 06/02/18.
 */

public class OrdersListActivity extends Activity implements org.planetaccounting.saleAgent.order.OrdersListAdapter.CancelOrder {
    OrdersListActivityBinding binding;

    ArrayList<Order> orders = new ArrayList<>();
    OrdersListAdapter adapter;

    @Inject
    ApiService apiService;
    @Inject
    Preferences preferences;
    @Inject
    RealmHelper realmHelper;

    Locale myLocale;
    String currentLanguage = "sq", currentLang;
    public static final String TAG = "bottom_sheet";

    private PrintManager printManager;
    InvoicePost invoicePost;
    Order orderPost;
    WebView webView;
    FrameLayout fragment;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.orders_list_activity);
        Kontabiliteti.getKontabilitetiComponent().inject(this);

        printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        binding.invoiceList.setLayoutManager(mLayoutManager);
        adapter = new OrdersListAdapter(orders);
        adapter.setListener(this);
        binding.invoiceList.setAdapter(adapter);
        webView = findViewById(R.id.web);

        getOrders();

        currentLanguage = getIntent().getStringExtra(currentLang);
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
            Toast.makeText(OrdersListActivity.this, R.string.language_already_selected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private void getOrders() {
        showLoader();
        apiService.getOrders(new StockPost(preferences.getToken(), preferences.getUserId()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ordersResponse -> {
                    orders = ordersResponse.getData();
                    adapter.setOrders(orders);
                    hideLoader();
                }, Throwable::printStackTrace);
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
    public void onEvent(OpenOrderDetailEvent event){
        Intent i = new Intent(getApplicationContext(), OrderListDetail.class);
        i.putExtra("id", event.getOrderId());
        i.putExtra("type",event.getOrderType());
        startActivity(i);
    }
    private void showLoader() {
        binding.loader.setVisibility(View.VISIBLE);
    }

    private void hideLoader() {
        binding.loader.setVisibility(View.GONE);
    }

    @Override
    public void onCancelPressed(Order order) {
        showLoader();
        apiService.cancelOrder(new OrderDetailPost(preferences.getToken(), preferences.getUserId(), order.getId(), order.getType()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ordersResponse -> {
                    hideLoader();
                    if (ordersResponse.getSuccess()) {
                        Toast.makeText(this, R.string.order_was_canceled, Toast.LENGTH_SHORT).show();
                        getOrders();

                    } else  {
                        Toast.makeText(this,ordersResponse.error.getText(),Toast.LENGTH_SHORT).show();

                    }
                }, Throwable::printStackTrace);
    }

//    private void printOrders(int id, boolean isPrint){
//
//        OrderForReportObject orderForReportObject = new OrderForReportObject();
//        orderForReportObject.setToken(preferences.getToken());
//        orderForReportObject.setUser_id(preferences.getUserId());
//        orderForReportObject.setId(String.valueOf(id));
//        apiService.getRaportOrderDetail(orderForReportObject)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(responseBody -> {
//                    if (responseBody.getSuccess()){
//
//                        orderPost = responseBody.data;
//
//                        Client client = realmHelper.getClientFromName(orderPost.getPariteName());
//
//                        if (isPrint){
//                            OrderPrintUtil util = new OrderPrintUtil(orderPost, webView, this, client, printManager);
//                        }
//                    }
//
//                });
//    }
}
