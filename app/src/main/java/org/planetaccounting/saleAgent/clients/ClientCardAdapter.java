package org.planetaccounting.saleAgent.clients;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.planetaccounting.saleAgent.databinding.ClientCardListItemBinding;
import org.planetaccounting.saleAgent.model.clients.CardItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by macb on 16/12/17.
 */

public class ClientCardAdapter extends RecyclerView.Adapter<ClientCardAdapter.ViewHolder> {

    private List<CardItem> cardItems = new ArrayList<>();
    private Context ctx;
    double balance = 0.0;

    @Override
    public ClientCardAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ctx = parent.getContext();
        ClientCardListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(ctx),
                org.planetaccounting.saleAgent.R.layout.client_card_list_item, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ClientCardAdapter.ViewHolder holder, int position) {
        ClientCardListItemBinding binding = holder.binding;
        binding.id.setText(cardItems.get(position).getDocumentNumber());
        binding.data.setText(cardItems.get(position).getData());
        binding.desc.setText(cardItems.get(position).getDescription());
        binding.type.setText(cardItems.get(position).getType());
        binding.afatiPageses.setText(cardItems.get(position).getPaymentDate());
        binding.value.setText(""+cardItems.get(position).getAmount());
        try{
            balance += cardItems.get(position).getAmount();
        }catch (Exception e){

        }
        binding.balance.setText(""+cutTo2(balance));

        //pjesa e fontit...
        setLatoRegularFont(ctx, binding.id);
        setLatoRegularFont(ctx, binding.data);
        setLatoRegularFont(ctx, binding.desc);
        setLatoRegularFont(ctx, binding.type);
        setLatoRegularFont(ctx, binding.afatiPageses);
        setLatoRegularFont(ctx, binding.value);
        setLatoRegularFont(ctx, binding.balance);

    }

    @Override
    public int getItemCount() {
        return cardItems.size();
    }

    public void setCardItems(List<CardItem> cardItems) {
        this.cardItems = cardItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ClientCardListItemBinding binding;

        ViewHolder(ClientCardListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }
    public double cutTo2(double value) {
        return Double.parseDouble(String.format(Locale.ENGLISH,"%.2f", value));
    }

    public static void setLatoRegularFont(Context context, TextView textView){
        Typeface latoRegular = Typeface.createFromAsset(context.getAssets(), "fonts/lato_regular.ttf");
        textView.setTypeface(latoRegular);
    }
}