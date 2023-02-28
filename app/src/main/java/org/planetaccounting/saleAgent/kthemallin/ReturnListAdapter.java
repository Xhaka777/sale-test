package org.planetaccounting.saleAgent.kthemallin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.databinding.ReturnListItemBinding;
import org.planetaccounting.saleAgent.events.RePrintInvoiceEvent;
import org.planetaccounting.saleAgent.events.UploadInvoiceEvent;
import org.planetaccounting.saleAgent.invoice.InvoiceListAdapter;
import org.planetaccounting.saleAgent.model.invoice.InvoicePost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class ReturnListAdapter extends RecyclerView.Adapter<ReturnListAdapter.ViewHolder> {

    private ArrayList<InvoicePost> returns;
    private Context ctx;
    private int selectedPos = RecyclerView.NO_POSITION;

    public ReturnListAdapter(ArrayList<InvoicePost> returns) {
        this.returns = returns;
    }

    @NonNull
    @Override
    public ReturnListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        ctx = viewGroup.getContext();
        ReturnListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(ctx),
                org.planetaccounting.saleAgent.R.layout.return_list_item, viewGroup, false);
        return new ReturnListAdapter.ViewHolder(binding);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull ReturnListAdapter.ViewHolder viewHolder, int i) {
        ReturnListItemBinding binding = viewHolder.binding;
        binding.companyNameTextview.setText(returns.get(i).getPartie_name());
        binding.companyUnitTextview.setText(returns.get(i).getPartie_station_name());
        binding.data.setText(returns.get(i).getInvoice_date());
        binding.returnNo.setText(returns.get(i).getNo_invoice());
        binding.vlera.setText(round(BigDecimal.valueOf(Double.parseDouble(returns.get(i).getAmount_with_vat()))) + "");
        if (returns.get(i).getSynced()) {
            binding.syncedIndicator.setImageResource(R.drawable.ic_close);

        } else {
            binding.syncedIndicator.setImageResource(R.drawable.ic_cancel);
        }
        binding.reprintReturn.setOnClickListener(v -> EventBus.getDefault().post(new RePrintInvoiceEvent(returns.get(i).getId(), returns.get(i).getIsFromServer())));
        binding.getRoot().setOnClickListener(view -> {
            EventBus.getDefault().post(new UploadInvoiceEvent(returns.get(i).getId()));
        });

        if (selectedPos == i) {
            viewHolder.binding.mainLayout.setBackgroundColor(R.color.colorLogoLight);
            viewHolder.binding.companyNameTextview.setTextColor(R.color.planetPurple2);
        } else {
            viewHolder.binding.mainLayout.setBackgroundColor(Color.WHITE);
            viewHolder.binding.companyNameTextview.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return returns.size();
    }

    public void setCompanies(ArrayList<InvoicePost> returns) {
        this.returns = returns;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ReturnListItemBinding binding;
        public TextView textView;

        ViewHolder(ReturnListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            textView = binding.companyNameTextview;
//            binding.companyNameTextview.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    notifyItemChanged(selectedPos);
//                    selectedPos = getAdapterPosition();
//                    notifyItemChanged(selectedPos);
//                }
//            });
            binding.mainLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyItemChanged(selectedPos);
                    selectedPos = getAdapterPosition();
                    notifyItemChanged(selectedPos);
                }
            });
        }
    }

    public static BigDecimal round(BigDecimal number) {
        return number.setScale(2, RoundingMode.HALF_DOWN);
    }
}
