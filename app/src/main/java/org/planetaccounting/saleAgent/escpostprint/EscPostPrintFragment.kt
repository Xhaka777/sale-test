package org.planetaccounting.saleAgent.escpostprint


import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.leerybit.escpos.DeviceCallbacks
import com.leerybit.escpos.PosPrinter80mm
import com.leerybit.escpos.Ticket
import com.leerybit.escpos.TicketBuilder
import com.leerybit.escpos.bluetooth.BTService
import com.leerybit.escpos.widgets.TicketPreview
import org.planetaccounting.saleAgent.R
import org.planetaccounting.saleAgent.model.clients.Client
import org.planetaccounting.saleAgent.model.invoice.InvoicePost
import org.planetaccounting.saleAgent.persistence.RealmHelper
import org.planetaccounting.saleAgent.utils.Preferences
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*


class EscPostPrintFragment : Fragment() {

    private var printer = PosPrinter80mm(activity)
    private val preview by lazy { view?.findViewById<TicketPreview>(R.id.ticket) }
    private val messageView by lazy { view?.findViewById<TextView>(R.id.tv_message) }
    private val stateView by lazy { view?.findViewById<TextView>(R.id.tv_state) }
    private var ticketNumber = 0

    private var isKode = true
    private var isBarcode = true
    private var isQuantity_UNIT = true
    private var isDiscount = true
    private var isDiscount_Extra = true
    private var itemsString: List<String>? = null
    private var isName = true;
    private var isVAT = true;

    companion object {
        private var minvoicePost: InvoicePost? = null
        private var preferences: Preferences? = null
        private var realmHelper: RealmHelper? = null
        private var client: Client? = null
        private var pay: String? = null
        private var isPrinted = false

        fun newInstace(_invoicePost: InvoicePost, _preferences: Preferences? = null, _realmHelper: RealmHelper? = null, _client: Client, _pay: String): EscPostPrintFragment {
            val fragment = EscPostPrintFragment()
            val b = Bundle()
            isPrinted = false
            minvoicePost = null
            preferences = null
            realmHelper = null
            client = null
            pay = null

            minvoicePost = _invoicePost
            preferences = _preferences
            realmHelper = _realmHelper
            client = _client
            pay = _pay
            fragment.arguments = b
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_esc_post_print, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        printer = PosPrinter80mm(activity)
        initSettings()
        getItems()
        val btnSearch = view.findViewById<Button>(R.id.btn_search)
        printer.setCharsetName("UTF-8")
        printer.setDeviceCallbacks(object : DeviceCallbacks {
            override fun onConnected() {
                btnSearch.setText(R.string.action_disconnect)
                printTicket()
            }

            override fun onFailure() {
                Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_SHORT).show()
            }

            override fun onDisconnected() {
                btnSearch.setText(R.string.action_connect)
            }
        })

        printer.setStateChangedListener { state, msg ->
            when (state) {
                BTService.STATE_NONE -> setState("NONE", R.color.text)
                BTService.STATE_CONNECTED -> {
                    setState("CONNECTED", R.color.green)
                }
                BTService.STATE_CONNECTING -> setState("CONNECTING", R.color.blue)
                BTService.STATE_LISTENING -> setState("LISTENING", R.color.amber)

            }

            when (msg.arg2) {
                BTService.MESSAGE_STATE_CHANGE -> setMessage("STATE CHANGED", R.color.text)
                BTService.MESSAGE_READ -> setMessage("READ (${msg.what})", R.color.green)
                BTService.MESSAGE_WRITE -> setMessage("WRITE (${msg.what})", R.color.green)
                BTService.MESSAGE_CONNECTION_LOST -> setMessage("CONNECTION LOST", R.color.red)
                BTService.MESSAGE_UNABLE_TO_CONNECT -> setMessage("UNABLE TO CONNECT", R.color.red)
            }
        }

        printer.connect()

        btnSearch.setOnClickListener {
            if (!printer.isConnected) printer.connect() else printer.disconnect()

        }
    }


    private fun printTicket() {
        if (isPrinted) {
            return
        }
        try {
            isPrinted = true
            val date = Date()

            val ticket: Ticket = TicketBuilder(printer)
                    .isCyrillic(true)
                    .feedLine(2)
                    .divider()
                    //
                    .text("Shitesi: ${realmHelper?.companyInfo?.name}")
                    .text(if (realmHelper?.companyInfo?.address != null) "Adresa : ${realmHelper?.companyInfo?.address}" else null)
                    .text(if (realmHelper?.companyInfo?.city != null) "Qyteti: ${realmHelper?.companyInfo?.city}" else null)
                    .text(if (realmHelper?.companyInfo?.state_name != null) "Shteti: ${realmHelper?.companyInfo?.state_name}" else null)
                    .text(if (realmHelper?.companyInfo?.phone != null || realmHelper?.companyInfo?.email != null) "Kontant: ${realmHelper?.companyInfo?.phone}" else null)
                    .text(if (realmHelper?.companyInfo?.phone != null || realmHelper?.companyInfo?.email != null) "       : ${realmHelper?.companyInfo?.email}" else null)
                    .text(if (realmHelper?.companyInfo?.fiscalNumber != null) "Nr.Fiskal: ${realmHelper?.companyInfo?.fiscalNumber}" else null)
                    .text(if (realmHelper?.companyInfo?.busniessNumber != null) "Nr. Biznesit: ${realmHelper?.companyInfo?.busniessNumber}" else null)
                    .text(if (realmHelper?.companyInfo?.vatNumber != null) "Nr. TVSH: ${realmHelper?.companyInfo?.vatNumber}" else null)
                    .divider()
                    //
                    .text("Fature: ${minvoicePost?.no_invoice}")
                    .text("E dates: ${minvoicePost?.invoice_date}")
                    .text("Njesia shitjes: ${preferences?.stationNbame}")
                    .text("Agjenti Shitjes: ${preferences?.fullName}")
                    .divider()
                    //
                    .text("Bleresi: ${minvoicePost?.partie_name}")
                    .text("Njesia: ${minvoicePost?.partie_station_name}")
                    .text("Adresa: ${minvoicePost?.partie_address}")
                    .text(if (client?.phone != null) "Kontant: ${client?.phone}" else null)
                    .text(if (client?.numberFiscal != null) "Nr.Fiskal: ${client?.numberFiscal}" else null)
                    .text(if (client?.numberBusniess != null) "Nr. Biznesit: ${client?.numberBusniess}" else null)
                    .text(if (client?.numberVat != null) "Nr. TVSH: ${client?.numberVat}" else null)
                    .divider()
                    //
                    .text("${
                        if (isBarcode) {
                            "Barkodi         "
                        } else {
                            ""
                        }
                    } ${
                        if (isName) {
                            "    Emertimi"
                        } else {
                            ""
                        }

                    }             TVSH")
                    .text("Sasia${
                        if (isQuantity_UNIT) {
                            " Sasia.C"
                        } else {
                            ""
                        }
                    } Cm. pa zb.${
                        if (isDiscount) {
                            " Zbr"
                        } else {
                            ""
                        }
                    }${
                        if (isDiscount_Extra) {
                            " Zbr.Ex"
                        } else {
                            ""
                        }
                    } Cmimi Vlera", TicketBuilder.TextAlignment.CENTER)
                    .divider()
                    //
                    .textList(getItems())
                    //
                    .dividerDouble()
                    .menuLine("Totali pa Zbritje", "${minvoicePost?.total_without_discount} EUR")
                    .menuLine("Vlera e Zbritur", "${minvoicePost?.amount_discount} EUR")
                    .menuLine("TVSH", "${minvoicePost?.amount_of_vat} EUR")
                    .menuLine("Pagesa", if (pay != null) {
                        "$pay EUR"
                    } else null)
                    .menuLine("Vlera Totale", "${minvoicePost?.amount_with_vat} EUR")

                    .dividerDouble()
                    //
                    .subHeader("Te dhenat per pagese")
                    .textList(createCashHoles())
                    .divider()
                    //
                    .menuLine("Faturoi", "Pranoi")
                    .feedLine(1)
                    .menuLine("____________", "____________")
                    .feedLine()
                    .divider()
                    //
                    .text("Gjeneruar me:${DateFormat.format("dd.MM.yyyy", date)} ${DateFormat.format("HH:mm", date)}")
                    .text("Nga:PlanetAccounting.org")
                    .text("info@planetaccounting.org")
                    .text("044/049 908 400")
                    .divider()
                    //
                    .feedLine(4)
                    .build()

            preview?.setTicket(ticket)
            printer.send(ticket)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getItems(): List<String> {
        if (itemsString != null) {
            return itemsString!!
        }
        val items: MutableList<String> = mutableListOf()

        minvoicePost?.items?.forEach {
            if (it.relacioni == null) {
                it.relacioni = "1"
            }
            var discount = client?.getDiscount()
            discount = if (it.isCollection) {
                ""
            } else {
                client?.getDiscount()
            }
            var sasia = ""
            if (it.relacioni.toDouble() > 1) {
                sasia = it.quantity
            }
            items.add("${
                if (isBarcode) {
                    "${it.barcode}     "
                }else if ("${it.barcode}".length == 8){
                    "${it.barcode}           "
                }else if ("${it.barcode}".length == 9){
                    "${it.barcode}         "
                }else if ("${it.barcode}".length == 10){
                    "${it.barcode}       "
                }else if ("${it.barcode}".length == 11){
                    "${it.barcode}     "
                }else if ("${it.barcode}".length == 12){
                    "${it.barcode}    "
                }else if ("${it.barcode}".length == 13){
                    "${it.barcode}   "
                }else if("${it.barcode}".length == 14){
                    "${it.barcode}  "
                }else {
                    ""
                }
            } ${
                if (isName) {
                    if ("${it.name}".length > 20) {

                        val barcode = it.barcode.replace("[^\\s]".toRegex(), " ")

                        val strN: String = "${it.name.subSequence(0, 20)}" + "    ${it.vat_rate}" + "\t\t" + barcode + "     " + "${it.name.subSequence(20, "${it.name}".length)}"
                        strN
                    } else if ("${it.name}".length == 19) {
                        "${it.name}" + "     ${it.vat_rate}"
                    } else if ("${it.name}".length == 18) {
                        "${it.name}" + "      ${it.vat_rate}"
                    } else if ("${it.name}".length == 17) {
                        "${it.name}" + "       ${it.vat_rate}"
                    } else if ("${it.name}".length == 16) {
                        "${it.name}" + "        ${it.vat_rate}"
                    } else if ("${it.name}".length == 15) {
                        "${it.name}" + "         ${it.vat_rate}"
                    } else if ("${it.name}".length == 14) {
                        "${it.name}" + "          ${it.vat_rate}"
                    } else if ("${it.name}".length == 13) {
                        "${it.name}" + "           ${it.vat_rate}"
                    } else if ("${it.name}".length == 12) {
                        "${it.name}" + "            ${it.vat_rate}"
                    } else if ("${it.name}".length == 11) {
                        "${it.name}" + "             ${it.vat_rate}"
                    } else if ("${it.name}".length == 10) {
                        "${it.name}" + "              ${it.vat_rate}"
                    } else if ("${it.name}".length == 9) {
                        "${it.name}" + "               ${it.vat_rate}"
                    } else if ("${it.name}".length == 8) {
                        "${it.name}" + "                ${it.vat_rate}"
                    } else if ("${it.name}".length == 7) {
                        "${it.name}" + "                 ${it.vat_rate}"
                    } else if ("${it.name}".length == 6) {
                        "${it.name}" + "                  ${it.vat_rate}"
                    } else if ("${it.name}".length == 5) {
                        "${it.name}" + "                   ${it.vat_rate}"
                    } else if ("${it.name}".length == 4) {
                        "${it.name}" + "                    ${it.vat_rate}"
                    } else if ("${it.name}".length == 3) {
                        "${it.name}" + "                     ${it.vat_rate}"
                    } else if ("${it.name}".length == 2) {
                        "${it.name}" + "                       ${it.vat_rate}"
                    } else {
                        "${it.name}" + "                         ${it.vat_rate}"
                    }
                } else {
                    ""
                }
            } ")

            items.add("$sasia${
                if (isQuantity_UNIT) {
                    "${it.quantity.toDouble() * it.relacioni.toDouble()}   "
                } else {
                    ""
                }
            }  ${round((it.price_base).toBigDecimal())}   ${

                if (isDiscount) {
                    "  $discount  "
                } else {
                    ""
                }
            }${
                if (isDiscount_Extra) {
                    "  ${it.discount} "
                } else {
                    ""
                }
            }    ${round((it.price_vat).toBigDecimal())}    ${round((it.totalPrice).toBigDecimal())}")
            items.add("")
        }
        itemsString = items
        return items
    }


    private fun createCashHoles(): List<String> {
        val items: MutableList<String> = mutableListOf()
        realmHelper?.companyInfo?.bankAccounts?.forEach {
            items.add("${it.name}: ${it.bankAccountNumber}")
        }
        return items
    }

    private fun initSettings() {
        isKode = preferences?.kodeSettings ?: true
        isBarcode = preferences?.barcodeSettings ?: true
        isQuantity_UNIT = preferences?.quantitySettings ?: true
        isDiscount = preferences?.discountSettings ?: true
        isDiscount_Extra = preferences?.discountExtraSettings ?: true
    }

    private fun printTicketTest() {
        try {
            val date = Date()
            val ticket: Ticket

            ticket = TicketBuilder(printer)
                    .isCyrillic(true)
                    .header("PosPrinter")
                    .image(BitmapFactory.decodeResource(getResources(), R.drawable.common_full_open_on_phone))
                    .divider()
                    .text("Date: ${DateFormat.format("dd.MM.yyyy", date)}")
                    .text("Time: ${DateFormat.format("HH:mm", date)}")
                    .text("Ticket No: ${++ticketNumber}")
                    .fiscalInt("ticket_no", ticketNumber)
                    .divider()
                    .subHeader("Hot dishes")
                    .menuLine("- 3 Kazan kabob", "60,00")
                    .menuLine("- 2 Full-Rack Ribs", "32,00")
                    .right("Total: 92,00")
                    .feedLine()
                    .subHeader("Salads")
                    .menuLine("- 1 Turkey & Swiss", "4,50")
                    .menuLine("- 1 Classic Cheese", "3,30")
                    .menuLine("- 1 Chicken Caesar Salad", "7,00")
                    .right("Total: 14,80")
                    .feedLine()
                    .subHeader("Desserts")
                    .menuLine("- 1 Blondie", "5,00")
                    .menuLine("- 2 Chocolate Cake", "7,00")
                    .right("Total: 12,00")
                    .feedLine()
                    .subHeader("Drinkables")
                    .center("50% sale for Coke on mondays!")
                    .menuLine("- 3 Coca-Cola", "6,00")
                    .menuLine("- 7 Tea", "3,50")
                    .menuLine("- 2 Coffee", "3,00")
                    .right("Total: 12,50")
                    .dividerDouble()
                    .menuLine("Total gift", "3,00")
                    .menuLine("Total", "128,30")
                    .fiscalDouble("gift", 3.0, 2)
                    .fiscalDouble("price", 131.30, 2)
                    .fiscalDouble("out_price", 128.30, 2)
                    .dividerDouble()
                    .stared("THANK YOU")
                    .feedLine(4)
                    .build()

            ticket

            preview?.setTicket(ticket)
            printer.send(ticket)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setState(value: String, color: Int) {
//        stateView?.text = "State: $value"
//        stateView?.setTextColor(ContextCompat.getColor(context!!, color))
    }

    @SuppressLint("SetTextI18n")
    private fun setMessage(value: String, color: Int) {
//        messageView?.text = "State: $value"
//        messageView?.setTextColor(ContextCompat.getColor(context!!, color))
    }

    fun cutTo2(value: Double): Double {
        return String.format(Locale.ENGLISH, "%.2f", value).toDouble()
    }

    fun round(number: BigDecimal): BigDecimal? {
        return number.setScale(2, RoundingMode.HALF_UP)
    }
}