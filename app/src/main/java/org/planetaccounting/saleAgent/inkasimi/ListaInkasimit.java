package org.planetaccounting.saleAgent.inkasimi;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.api.ApiService;
import org.planetaccounting.saleAgent.databinding.OrdersListActivityBinding;
import org.planetaccounting.saleAgent.events.OpenOrderDetailEvent;
import org.planetaccounting.saleAgent.model.order.Order;
import org.planetaccounting.saleAgent.model.stock.StockPost;
import org.planetaccounting.saleAgent.order.OrderListDetail;
import org.planetaccounting.saleAgent.order.OrdersListAdapter;
import org.planetaccounting.saleAgent.utils.Preferences;
import java.util.ArrayList;
import javax.inject.Inject;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by tahirietrit on 4/5/18.
 */

public class ListaInkasimit extends Activity {
    OrdersListActivityBinding binding;

    ArrayList<Order> orders = new ArrayList<>();
    OrdersListAdapter adapter;

    @Inject
    ApiService apiService;
    @Inject
    Preferences preferences;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding  = DataBindingUtil.setContentView(this, R.layout.orders_list_activity);
        Kontabiliteti.getKontabilitetiComponent().inject(this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        binding.invoiceList.setLayoutManager(mLayoutManager);
        adapter = new OrdersListAdapter(orders);
        binding.invoiceList.setAdapter(adapter);
        getOrders();
    }
    private  void getOrders(){
        apiService.getOrders(new StockPost(preferences.getToken(), preferences.getUserId()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ordersResponse -> {
                    orders = ordersResponse.getData();
                    adapter.setOrders(orders);
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
        startActivity(i);
    }

}
