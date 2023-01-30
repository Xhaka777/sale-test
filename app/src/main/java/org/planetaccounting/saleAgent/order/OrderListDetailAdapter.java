package org.planetaccounting.saleAgent.order;

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.databinding.OrderlistDetailListItemBinding;
import org.planetaccounting.saleAgent.model.OrderDetailItem;
import org.planetaccounting.saleAgent.model.order.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by macb on 06/02/18.
 */

public class OrderListDetailAdapter extends RecyclerView.Adapter<OrderListDetailAdapter.ViewHolder> {

    ArrayList<OrderDetailItem> orderDetailItems = new ArrayList<>();
    private Context ctx;
    private CancelOrder listener;

    public OrderListDetailAdapter(ArrayList<OrderDetailItem> orderDetailItems) {
        this.orderDetailItems = orderDetailItems;
    }

    @Override
    public OrderListDetailAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ctx = parent.getContext();
        OrderlistDetailListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(ctx),
                R.layout.orderlist_detail_list_item, parent, false);
        return new OrderListDetailAdapter.ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(OrderListDetailAdapter.ViewHolder holder, int position) {
        OrderlistDetailListItemBinding binding = holder.binding;
        double vVlera = Double.parseDouble(String.valueOf(orderDetailItems.get(position).getAmount()));
        double vSasia = Float.parseFloat(String.valueOf(orderDetailItems.get(position).getQuantity()));
        double vCmimi = Double.parseDouble(String.valueOf(orderDetailItems.get(position).getPrice()));

        binding.shifra.setText(orderDetailItems.get(position).getNumber());
        binding.name.setText(orderDetailItems.get(position).getName());
        binding.sasia.setText(""+round(BigDecimal.valueOf(vSasia)));
        binding.barkod.setText(orderDetailItems.get(position).getBarcode());
        binding.njesia.setText(orderDetailItems.get(position).getUnit());
        binding.cmimi.setText(""+round(BigDecimal.valueOf(vCmimi)));
        binding.vlera.setText(""+round(BigDecimal.valueOf(vVlera)));
    }

    @Override
    public int getItemCount() {
        return orderDetailItems.size();
    }


    public void setOrders(ArrayList<OrderDetailItem> orderDetailItems) {
        this.orderDetailItems = orderDetailItems;
        System.out.println("items " +orderDetailItems.size());
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private OrderlistDetailListItemBinding binding;

        ViewHolder(OrderlistDetailListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }

    interface CancelOrder{
        void onCancelPressed(Order order);
    }

    public double cutTo2(double value) {
        return Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", value));
    }

    public static BigDecimal round(BigDecimal number){
        return number.setScale(2, RoundingMode.HALF_UP);
    }
}