package org.planetaccounting.saleAgent.aksionet;

import android.annotation.SuppressLint;
import android.databinding.DataBindingUtil;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.planetaccounting.saleAgent.Kontabiliteti;
import org.planetaccounting.saleAgent.R;
import org.planetaccounting.saleAgent.databinding.ActivityActionDetailBinding;
import org.planetaccounting.saleAgent.databinding.AllActionDetailHolderBinding;
import org.planetaccounting.saleAgent.model.stock.Item;
import org.planetaccounting.saleAgent.model.stock.SubItem;
import org.planetaccounting.saleAgent.persistence.RealmHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.realm.RealmResults;

public class ActionDetailActivity extends AppCompatActivity {

    private ActivityActionDetailBinding binding;

    @Inject
    RealmHelper realmHelper;
    ActionData actionData;
    RealmResults<Item> stockItems;

    private String barTitle = "";
    private String title = "";
    private String titleContent = "";
    private String status = "";
    private String shifra = "";
    private String client = "";
    private String discount = "";
    private String amount = "";
    private String nga = "";
    private String deri = "";
    private String from = "";
    private String id = "";
    private String type = "";
    private String unit = "";
    private String item_unit = "";


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_action_detail);

        from = getIntent().getStringExtra("from");
        id = getIntent().getStringExtra("id");

        barTitle = getIntent().getStringExtra("bar_title");
        title = getIntent().getStringExtra("title");
        titleContent = getIntent().getStringExtra("title_count");
        status = getIntent().getStringExtra("status");
        shifra = getIntent().getStringExtra("shifra");
        client = getIntent().getStringExtra("client");
        discount = getIntent().getStringExtra("discount");
        amount = getIntent().getStringExtra("amount");
        type = getIntent().getStringExtra("type");
        unit = getIntent().getStringExtra("unit");
        item_unit = getIntent().getStringExtra("item_unit");


        nga = getIntent().getStringExtra("nga");
        deri = getIntent().getStringExtra("deri");

        Kontabiliteti.getKontabilitetiComponent().inject(this);
        stockItems = realmHelper.getStockItems();

        binding.barTitle.setText(barTitle);
        binding.title.setText(title);
        binding.titleText.setText(titleContent);
        binding.status.setText(status);
        binding.shifra.setText(shifra);
        binding.client.setText(client);
        binding.discount.setText(round(BigDecimal.valueOf(Double.parseDouble(discount))) + "%");
        binding.amount.setText((round(BigDecimal.valueOf(Double.parseDouble(amount)))) + "");
        binding.itemUnit.setText(item_unit);
        binding.type.setText(type);
        binding.unit.setText(unit);
        binding.nga.setText(nga);
        binding.deri.setText(deri);
        actionData = realmHelper.getAksionet();

        if (type.equals("specific")){
            binding.actionSteps.setVisibility(View.GONE);
            binding.allActionHolder.setVisibility(View.GONE);
        }

        if (type.equals("amount")){
            binding.titleActionAmount.setVisibility(View.VISIBLE);
        }

        if (type.equals("quantity")){
            binding.titleActionQuantity.setVisibility(View.VISIBLE);
        }

        switch (from){
            case "article" :
                for (ActionArticleItems action: actionData.getArticleItems()) {
                    if (action.getId().equals(id)){
                        fillSteps(action.getSteps(),action.getType());
                    }
                }
                break;

            case "brand":
                for (ActionBrandItem action : actionData.getArticleBrandItem()) {
                    if (action.getId().equals(id)) {
                        fillSteps(action.getSteps(), action.getType());
                    }
                }
                break;

            case "category":
                for (ActionBrandItem action : actionData.getArticleBrandItem()) {
                    if (action.getId().equals(id)) {
                        fillSteps(action.getSteps(), action.getType());
                    }
                }
                break;
        }

    }


    @SuppressLint("SetTextI18n")
    private void fillSteps(List<ActionSteps> steps, String type){
        for (int i = 0; i < steps.size(); i++){

            ActionSteps actionSteps = steps.get(i);

            AllActionDetailHolderBinding itemBinding = DataBindingUtil.inflate(getLayoutInflater(),
                    R.layout.all_action_detail_holder, binding.allActionHolder, false);

            itemBinding.from.setText(round(BigDecimal.valueOf(Double.parseDouble(actionSteps.getFrom()))) + "");
            itemBinding.to.setText(round(BigDecimal.valueOf(Double.parseDouble(actionSteps.getTo()))) + "");
            itemBinding.discount.setText(round(BigDecimal.valueOf(Double.parseDouble(actionSteps.getDiscount()))) + "");
            binding.allActionHolder.addView(itemBinding.getRoot());
        }
    }

    public static BigDecimal round(BigDecimal number) {
        return number.setScale(2, RoundingMode.HALF_DOWN);
    }
}
