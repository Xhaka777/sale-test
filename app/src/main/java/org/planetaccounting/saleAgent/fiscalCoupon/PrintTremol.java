package org.planetaccounting.saleAgent.fiscalCoupon;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.planetaccounting.saleAgent.MainActivity;
import org.planetaccounting.saleAgent.model.invoice.InvoiceItemPost;

import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.HashMap;

import TremolZFP.FP;
import TremolZFP.FPcore;
import TremolZFP.OptionPrintType;
import TremolZFP.OptionVATClass;
import io.realm.Realm;
import io.realm.RealmList;

public class PrintTremol extends AppCompatActivity {

    RealmList<InvoiceItemPost> receipt;
    Boolean print_duplicate = false;
    String receipt_type = "";


    public PrintTremol(Boolean print_duplicate, String receipt_type) {

        this.print_duplicate = print_duplicate;
        this.receipt_type = receipt_type;

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(...)

//        new Thread duhet me qit ne metode void
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Do network action in this function
                try {

                    FP fp = make_connection("http://localhost:4444/");

                    switch (receipt_type) {
                        case "receipt":
                            create_receipt(fp, receipt, print_duplicate);
                            break;
                        case "X":
                            daily_reports(fp, 'X');
                            break;
                        case "Z":
                            daily_reports(fp, 'Z');
                            break;
                        case "TEST":
                            test_fiscal(fp);
                            break;
                        default:
                            System.out.println("kun hiq...");

                            //nese ka nevoj me shtu hala
                    }
                } catch (Exception e) {
                    handleException(e);
                }
            }
        }).start();
    }

//    updated Thread as a method...

    public void print_coupon() throws Exception {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String server_address = "http://localhost:4444/";
                //Do network action in this function
                try {
                    FP fp = make_connection(server_address);

                    switch (receipt_type) {
                        case "receipt":
                            create_receipt(fp, receipt, print_duplicate);
                            break;
                        case "X":
                            daily_reports(fp, 'X');
                            break;
                        case "Z":
                            daily_reports(fp, 'Z');
                            break;
                        case "TEST":
                            test_fiscal(fp);
                            break;
                        case "CUT":
                            cut_paper(fp);
                        default:
                            System.out.println("kun hiq...");
                    }
                } catch (Exception e) {
                    handleException(e);
                }
            }
        });
        thread.start();
    }

    public void set_receipt_data(RealmList<InvoiceItemPost> receipt_data) {


        this.receipt = receipt_data;
    }


    public FP make_connection(String server_address) {
        FP fp = new FP();
        fp.setServerAddress(server_address);
        return fp;
    }

    private void daily_reports(FP fp, Character T) throws Exception {
        try {
            fp.PrintDailyReport(T);
        } catch (Exception e) {
            //make these toast.
            handleException(e);
        }
    }

    /**
     * metoda me te cilen e marr statusin e printerit
     * krejt qka mduhet prej statusit te printerit esht me dit printeri a esht online apo offline
     * duhet mja qu ni variabel te tipi boolean
     * me check nese esht true e kthejm online
     * @param fp
     */

    public void status_printer(FP fp) {
        try {
            fp.ReadStatus();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void cut_paper(FP fp) {
        try {
            fp.CutPaper();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void test_fiscal(FP fp) {
        try {
            fp.PaperFeed();
        } catch (Exception e) {
            handleException(e);
        }
    }


    private OptionVATClass vat_type(String vat) {
        OptionVATClass vat_class = null;
        switch (vat.trim()) {
            //dallim 18 me 18.00
            case "18.00":
                vat_class = OptionVATClass.VAT_Class_E;
                break;
            case "8.00":
                vat_class = OptionVATClass.VAT_Class_D;
                break;
            case "0.00":
                vat_class = OptionVATClass.VAT_Class_C;
                break;
            default:
                System.out.println("Ska TVSH artikulli");
        }
        return vat_class;
    }

    private boolean create_receipt(FP fp, RealmList<InvoiceItemPost> receipt, boolean print_duplicate) throws Exception {
//        System.out.println(receipt + "receipt");
        OptionPrintType print_type = OptionPrintType.Postponed_printing;
        double operator = 1.0;

        try {
            //open recipe and tell the print type
            fp.OpenReceipt(operator, "0", print_type);


            //for all articles in array add the m to the coupon with their respective values
            for (InvoiceItemPost article : receipt) {

                fp.SellPLUwithSpecifiedVAT(article.getName(),
                        (OptionVATClass) vat_type(article.getVat_rate()),
                        Double.parseDouble(article.getPrice_vat()),
                        Double.parseDouble(article.getQuantity()),
                        Double.parseDouble("0"),
                        -Double.parseDouble(article.getAmount_of_discount()), 0);
            }
            //Discount is calculated per article????
            //Close Receipt with cash
            fp.CashPayCloseReceipt();
            //Print duplicate non fiscal coupon if is set to true
            if (print_duplicate)
                fp.PrintLastReceiptDuplicate();
        } catch (Exception e) {
            handleException(e);
            return false;
        }
        return true;
    }
    //duhet me kriju definitions se munet me na qit problem

    public static void handleException(Exception ex) {
        if (ex instanceof FPcore.SException) {
            FPcore.SException sx = ((FPcore.SException) ex);
            if (sx.isFpException()) {
                /*
                 Possible reasons:
                 sx.getSTE1() =                                              sx.getSTE2() =
                 0x30 OK                                                   0x30 OK
                 0x31 Out of paper, printer failure                        0x31 Invalid command
                 0x32 Registers overflow                                   0x32 Illegal command
                 0x33 Clock failure or incorrect date&time                 0x33 Z daily report is not zero
                 0x34 Opened fiscal receipt                                0x34 Syntax error
                 0x35 Payment residue account                              0x35 Input registers overflow
                 0x36 Opened non-fiscal receipt                            0x36 Zero input registers
                 0x37 Registered payment but receipt is not closed         0x37 Unavailable transaction for correction
                 0x38 Fiscal memory failure                                0x38 Insufficient amount on hand
                 0x39 Incorrect password                                   0x3A No access
                 0x3a Missing external display
                 0x3b 24hours block – missing Z report
                 0x3c Overheated printer thermal head.
                 0x3d Interrupt power supply in fiscal receipt (one time until status is read)
                 0x3e Overflow EJ
                 0x3f Insufficient conditions
                 **/
                if (sx.getSTE1() == 0x30 && sx.getSTE2() == 0x32) {
                    alert("sx.getSTE1() == 0x30 - command is OK  AND  sx.getSTE2() == 0x32 - command is Illegal in current context");
                } else if (sx.getSTE1() == 0x30 && sx.getSTE2() == 0x33) {
                    alert("sx.getSTE1() == 0x30 - command is OK  AND sx.getSTE2() == 0x33 - make Z report");
                } else if (sx.getSTE1() == 0x34 && sx.getSTE2() == 0x32) {
                    alert("sx.getSTE1() == 0x34 - Opened fiscal receipt  AND  sx.getSTE2() == 0x32 - command is Illegal in current context");
                } else {
                    alert(sx.getMessage() + "\nSTE1=" + sx.getSTE1() + ", STE2=" + sx.getSTE2());
                }
            } else if (sx.ErrType == FPcore.SErrorType.ServerDefsMismatch) {
                alert("The current library version and server definitions version do not match\n" + sx.getMessage());
            } else if (sx.ErrType == FPcore.SErrorType.ServMismatchBetweenDefinitionAndFPResult) {
                alert("The current library version and the fiscal device firmware is not match\n" + sx.getMessage());
            } else if (sx.ErrType == FPcore.SErrorType.ServerAddressNotSet) {
                alert("Specify server ServerAddress property\n" + sx.getMessage());
            } else if (sx.ErrType == FPcore.SErrorType.ServerConnectionError) {
                alert("Connection from this app to the server is not established\n" + sx.getMessage());
            } else if (sx.ErrType == FPcore.SErrorType.ServSockConnectionFailed) {
                alert("Server can not connect to the fiscal device\n" + sx.getMessage());
            } else if (sx.ErrType == FPcore.SErrorType.ServTCPAuth) {
                alert("Wrong device ТCP password\n" + sx.getMessage());
            } else if (sx.ErrType == FPcore.SErrorType.ServWaitOtherClientCmdProcessingTimeOut) {
                alert("Processing of other clients command is taking too long\n" + sx.getMessage());
            } else {
                alert(sx.getMessage());
            }
        } else {
            String msg = ex.getMessage();
            if (msg == null || msg.equals("")) {
                msg = ex.toString();
            }
            alert(msg);
        }
    }


    //shembull program nga Ilami
    //duhet mi bo alertat nbaze tonen qysh na duhet
    public static void alert(final String message) {
        //TextView show_error = MainActivity.show_error;
        // Toast.makeText(MainActivity.this,message,short);
        System.out.println("error" + message);
    }

}
