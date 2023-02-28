package org.planetaccounting.saleAgent.raportet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.databinding.InvoiceListItemBinding;
import org.planetaccounting.saleAgent.databinding.RaportDetailItemBinding;
import org.planetaccounting.saleAgent.depozita.DepositPost;
import org.planetaccounting.saleAgent.events.RePrintInvoiceEvent;
import org.planetaccounting.saleAgent.events.UploadInvoiceEvent;
import org.planetaccounting.saleAgent.inkasimi.InkasimiDetail;
import org.planetaccounting.saleAgent.invoice.InvoiceListAdapter;
import org.planetaccounting.saleAgent.model.invoice.InvoicePost;
import org.planetaccounting.saleAgent.vendors.VendorPost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.RealmResults;

/**
 * Created by tahirietrit on 4/5/18.
 */

public class RaportetListAdapter extends RecyclerView.Adapter<RaportetListAdapter.ViewHolder> {

    private List<VendorPost> vendorPosts;
    private List<InkasimiDetail> inkasimiDetails;
    private List<DepositPost> depositPosts;
    private Context ctx;
    int type;
    private LayoutInflater mInflater;

    //type 0 = shpenzimet
    //type 1 =inkasimi
    //type 0 = shpenzimet
    public RaportetListAdapter(int type) {
        this.type = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ctx = parent.getContext();
        RaportDetailItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(ctx),
                R.layout.raport_detail_item, parent, false);
        return new ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RaportDetailItemBinding binding = holder.binding;

        if (type == 0) {
            binding.col1.setText(vendorPosts.get(position).getDate());
            binding.col2.setText(vendorPosts.get(position).getFurnitori());
            binding.col3.setText(vendorPosts.get(position).getType());
            binding.col4.setText(vendorPosts.get(position).getNo_invoice());
            binding.col5.setText("" + round(BigDecimal.valueOf(vendorPosts.get(position).getAmount())));
            binding.col6.setText("" + vendorPosts.get(position).getComment());

            if (vendorPosts.get(position).isSynced()) {
                binding.syncedIndicator.setImageResource(R.drawable.ic_close);

            } else {
                binding.syncedIndicator.setImageResource(R.drawable.ic_cancel);
            }
        } else if (type == 1) {
            binding.col1.setText(inkasimiDetails.get(position).getDate());
            binding.col2.setText(inkasimiDetails.get(position).getKlienti());
            binding.col3.setText(inkasimiDetails.get(position).getNjesia());
            binding.col4.setVisibility(View.GONE);
            binding.col5.setText("" + round(BigDecimal.valueOf(inkasimiDetails.get(position).getAmount())));
            binding.col6.setText("" + inkasimiDetails.get(position).getComment());

            if (inkasimiDetails.get(position).isSynced()) {
                binding.syncedIndicator.setImageResource(R.drawable.ic_close);
            } else {
                binding.syncedIndicator.setImageResource(R.drawable.ic_cancel);
            }
        } else if (type == 2) {
            binding.col1.setText(depositPosts.get(position).getDate());
            binding.col2.setText(depositPosts.get(position).getEmri_bankes());
            binding.col3.setText(depositPosts.get(position).getBranch());
            binding.col4.setVisibility(View.GONE);
            binding.col5.setText("" + round(BigDecimal.valueOf(depositPosts.get(position).getAmount())));
            binding.col6.setText("" + depositPosts.get(position).getComment());

            if (depositPosts.get(position).isSynced()) {
                binding.syncedIndicator.setImageResource(R.drawable.ic_close);
            } else {
                binding.syncedIndicator.setImageResource(R.drawable.ic_cancel);
            }
        }
//        binding.reprintInvoice.setOnClickListener(v -> EventBus.getDefault().post(new RePrintInvoiceEvent(invoices.get(position).getId())));
//        binding.getRoot().setOnClickListener(view -> {
//            EventBus.getDefault().post(new UploadInvoiceEvent(invoices.get(position).getId()));
//        });
    }

    public void setVendorPosts(List<VendorPost> vendorPosts) {
        this.vendorPosts = vendorPosts;
        notifyDataSetChanged();
    }

    public void setInkasimiDetails(List<InkasimiDetail> inkasimiDetails) {
        this.inkasimiDetails = inkasimiDetails;
        notifyDataSetChanged();
    }

    public void setDepositPosts(List<DepositPost> depositPosts) {
        this.depositPosts = depositPosts;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (type == 0) {
            return null != vendorPosts ? vendorPosts.size() : 0;
        } else if (type == 1) {
            return null != inkasimiDetails ? inkasimiDetails.size() : 0;
        } else {
            return null != depositPosts ? depositPosts.size() : 0;
        }
    }



    static class ViewHolder extends RecyclerView.ViewHolder {
        private RaportDetailItemBinding binding;

        ViewHolder(RaportDetailItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }

    public double cutTo2(double value) {

        return Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", value));
    }

    public static BigDecimal round(BigDecimal number){
        return number.setScale(2, RoundingMode.HALF_UP);
    }
}