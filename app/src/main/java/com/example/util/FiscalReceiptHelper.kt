package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.entity.OrderEntity
import com.example.model.DeliveryPartner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FiscalReceiptHelper {

    fun generateFiscalNumber(orderId: String): String {
        val clean = orderId.replace("#", "").replace("BM-", "")
        return "FISC-2026-$clean"
    }

    fun getFormattedDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Sends the formatted fiscal order bill to the Android Print Spooler,
     * which connects directly to physical Bluetooth POS printers, WiFi thermal receipt printers,
     * or standard Android Print Services.
     */
    fun printFiscalBillToMachine(
        context: Context,
        order: OrderEntity,
        partner: DeliveryPartner?,
        onPrintStarted: () -> Unit = {}
    ) {
        Handler(Looper.getMainLooper()).post {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) return@post

            val jobName = "BiteMart_Fiscal_Receipt_${order.orderId}"
            val webView = WebView(context)

            val fiscalNumber = generateFiscalNumber(order.orderId)
            val printDate = getFormattedDateTime()
            val riderName = partner?.name ?: "Verified Delivery Partner"
            val riderVehicle = partner?.let { "${it.vehicle} (${it.vehicleNumber})" } ?: "Two Wheeler"

            val itemsHtml = order.itemsSummary.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("") { item ->
                    """
                    <tr style="border-bottom: 1px dashed #cccccc;">
                        <td style="padding: 4px 0; font-family: monospace; font-size: 11px;">$item</td>
                        <td style="text-align: right; padding: 4px 0; font-family: monospace; font-size: 11px;">INCLUDED</td>
                    </tr>
                    """.trimIndent()
                }

            val htmlDocument = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Fiscal Receipt</title>
                    <style>
                        body {
                            font-family: 'Courier New', Courier, monospace;
                            width: 280px;
                            margin: 0 auto;
                            padding: 12px;
                            color: #000000;
                            background: #ffffff;
                            font-size: 11px;
                        }
                        .center { text-align: center; }
                        .right { text-align: right; }
                        .bold { font-weight: bold; }
                        .divider { border-top: 1px dashed #000000; margin: 6px 0; }
                        .double-divider { border-top: 2px solid #000000; margin: 6px 0; }
                        .tag {
                            border: 1px solid #000000;
                            padding: 2px 6px;
                            display: inline-block;
                            font-size: 9px;
                            font-weight: bold;
                            margin: 4px 0;
                        }
                        table { width: 100%; border-collapse: collapse; }
                    </style>
                </head>
                <body>
                    <div class="center bold" style="font-size: 14px; letter-spacing: 1px;">BITEMART DISPATCH</div>
                    <div class="center bold">FISCAL RESTAURANT BILL</div>
                    <div class="center" style="font-size: 9px;">TAX INVOICE & KITCHEN DOCKET (KOT)</div>
                    <div class="center">
                        <span class="tag">HAND OVER TO RESTAURANT OWNER</span>
                    </div>

                    <div class="divider"></div>

                    <table>
                        <tr>
                            <td class="bold">RESTAURANT:</td>
                            <td class="right bold">${order.storeName}</td>
                        </tr>
                        <tr>
                            <td>ORDER ID:</td>
                            <td class="right bold">${order.orderId}</td>
                        </tr>
                        <tr>
                            <td>FISCAL NO:</td>
                            <td class="right">$fiscalNumber</td>
                        </tr>
                        <tr>
                            <td>DATE & TIME:</td>
                            <td class="right">$printDate</td>
                        </tr>
                    </table>

                    <div class="divider"></div>

                    <table>
                        <tr>
                            <td class="bold">DELIVERY PARTNER:</td>
                            <td class="right">$riderName</td>
                        </tr>
                        <tr>
                            <td>VEHICLE:</td>
                            <td class="right">$riderVehicle</td>
                        </tr>
                        <tr>
                            <td class="bold">CUSTOMER:</td>
                            <td class="right bold">${order.customerName}</td>
                        </tr>
                        <tr>
                            <td>PHONE:</td>
                            <td class="right">${order.customerPhone}</td>
                        </tr>
                        <tr>
                            <td>DESTINATION:</td>
                            <td class="right" style="font-size: 10px;">${order.addressFull.ifBlank { order.addressTitle }}</td>
                        </tr>
                    </table>

                    <div class="double-divider"></div>
                    <div class="bold" style="font-size: 11px;">ITEMIZED KITCHEN ORDER (${order.itemsCount} ITEMS):</div>
                    <div class="divider"></div>

                    <table>
                        $itemsHtml
                    </table>

                    <div class="double-divider"></div>

                    <table>
                        <tr>
                            <td>Order Subtotal:</td>
                            <td class="right">₹${"%.2f".format(order.subtotal.takeIf { it > 0 } ?: (order.total * 0.88))}</td>
                        </tr>
                        <tr>
                            <td>CGST (2.5%):</td>
                            <td class="right">₹${"%.2f".format(order.taxes / 2.0)}</td>
                        </tr>
                        <tr>
                            <td>SGST (2.5%):</td>
                            <td class="right">₹${"%.2f".format(order.taxes / 2.0)}</td>
                        </tr>
                        <tr>
                            <td>Packaging & Fee:</td>
                            <td class="right">₹${"%.2f".format(order.deliveryFee)}</td>
                        </tr>
                        <tr class="bold" style="font-size: 13px;">
                            <td style="padding-top: 4px;">TOTAL PAID:</td>
                            <td class="right" style="padding-top: 4px;">₹${"%.2f".format(order.total)}</td>
                        </tr>
                    </table>

                    <div class="divider"></div>
                    <div>PAYMENT: <span class="bold">${order.paymentMethod.uppercase()} (PREPAID)</span></div>
                    <div>DELIVERY OTP REQUIRED AT DOORSTEP: <span class="bold">YES</span></div>

                    <div class="divider"></div>

                    <div class="center" style="font-size: 14px; letter-spacing: 2px; margin: 8px 0;">
                        ||| | |||| | || ||| |||| | |||
                    </div>
                    <div class="center" style="font-size: 9px; color: #333333;">
                        * This official fiscal paper bill confirms rider order collection.<br/>
                        Please hand over this slip to the restaurant manager/kitchen.
                    </div>
                </body>
                </html>
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                        .setResolution(PrintAttributes.Resolution("thermal", "POS Thermal Printer", 203, 203))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                    onPrintStarted()
                }
            }

            webView.loadDataWithBaseURL(null, htmlDocument, "text/html", "UTF-8", null)
        }
    }
}
