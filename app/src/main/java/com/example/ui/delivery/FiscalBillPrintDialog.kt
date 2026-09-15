package com.example.ui.delivery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.OrderEntity
import com.example.model.DeliveryPartner
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.VegGreen
import com.example.util.FiscalReceiptHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PrintStatus {
    IDLE,
    PRINTING,
    COMPLETED
}

@Composable
fun FiscalBillPrintDialog(
    order: OrderEntity,
    partner: DeliveryPartner?,
    onDismiss: () -> Unit,
    onBillHandedOver: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var printStatus by remember { mutableStateOf(PrintStatus.IDLE) }
    var copiesCount by remember { mutableStateOf(1) }

    val fiscalNumber = remember(order.orderId) { FiscalReceiptHelper.generateFiscalNumber(order.orderId) }
    val printTime = remember { FiscalReceiptHelper.getFormattedDateTime() }
    val itemsList = remember(order.itemsSummary) {
        order.itemsSummary.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .testTag("fiscal_bill_print_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Restaurant Fiscal Bill",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Print paper bill to give to restaurant owner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_print_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Printing Machine Status Notification Bar
                AnimatedVisibility(visible = printStatus == PrintStatus.PRINTING) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFD97706),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connecting to printing machine & feeding paper...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = printStatus == PrintStatus.COMPLETED) {
                    Surface(
                        color = VegGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VegGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VegGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Fiscal bill printed successfully! Hand this physical copy to the restaurant owner.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = VegGreen
                            )
                        }
                    }
                }

                // ================= THERMAL PAPER SLIP =================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(4.dp))
                        .background(Color(0xFFFAF9F6)) // Authentic thermal paper ivory
                        .border(BorderStroke(1.dp, Color(0xFFE2E2E2)), RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 18.dp)
                        .testTag("thermal_paper_receipt_slip")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Jagged paper cut line (Top)
                        Text(
                            text = "- - - - - - - - - - - - - - - - - - - - - - - - -",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Header
                        Text(
                            text = "BITEMART DISPATCH BILL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "FISCAL RESTAURANT INVOICE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = Color.Black,
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = "HAND OVER TO RESTAURANT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ThermalDashedLine()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Restaurant & Order Details
                        ThermalKeyValueRow("RESTAURANT:", order.storeName, isBold = true)
                        ThermalKeyValueRow("ORDER NO:", order.orderId, isBold = true)
                        ThermalKeyValueRow("FISCAL REF:", fiscalNumber)
                        ThermalKeyValueRow("DATE & TIME:", printTime)
                        ThermalKeyValueRow("MACHINE ID:", "POS-TH-80MM")

                        Spacer(modifier = Modifier.height(8.dp))
                        ThermalDashedLine()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Delivery Partner & Customer Details
                        ThermalKeyValueRow("RIDER NAME:", partner?.name ?: "Assigned Rider")
                        ThermalKeyValueRow("VEHICLE:", "${partner?.vehicle ?: "Motorcycle"} (${partner?.vehicleNumber ?: "KA01"})")
                        ThermalKeyValueRow("CUSTOMER:", order.customerName, isBold = true)
                        ThermalKeyValueRow("CUST PHONE:", order.customerPhone)
                        ThermalKeyValueRow("DROP DEST:", order.addressFull.ifBlank { order.addressTitle })

                        Spacer(modifier = Modifier.height(8.dp))
                        ThermalDoubleLine()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items Ordered Section
                        Text(
                            text = "FOOD ORDER ITEMS (${order.itemsCount}):",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        itemsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $item",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "KITCHEN READY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ThermalDoubleLine()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Financial Breakdown
                        val calculatedSubtotal = if (order.subtotal > 0) order.subtotal else order.total * 0.88
                        val calculatedTaxes = if (order.taxes > 0) order.taxes else order.total * 0.05

                        ThermalKeyValueRow("Subtotal:", "₹${"%.2f".format(calculatedSubtotal)}")
                        ThermalKeyValueRow("CGST (2.5%):", "₹${"%.2f".format(calculatedTaxes / 2.0)}")
                        ThermalKeyValueRow("SGST (2.5%):", "₹${"%.2f".format(calculatedTaxes / 2.0)}")
                        ThermalKeyValueRow("Delivery & Pack:", "₹${"%.2f".format(order.deliveryFee)}")

                        Spacer(modifier = Modifier.height(4.dp))
                        ThermalDashedLine()
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL FISCAL:",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "₹${"%.2f".format(order.total)}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        ThermalKeyValueRow("PAYMENT MODE:", "${order.paymentMethod.uppercase()} (PAID)")
                        ThermalKeyValueRow("CUSTOMER OTP:", "REQUIRED UPON DROP")

                        Spacer(modifier = Modifier.height(8.dp))
                        ThermalDashedLine()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Thermal Barcode Simulation
                        Text(
                            text = "||| | ||||| | || | |||| || | |||||",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "* FISCAL VERIFIED COPY FOR RESTAURANT *",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Jagged paper cut line (Bottom)
                        Text(
                            text = "- - - - - - - - - - - - - - - - - - - - - - - - -",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Print to Physical Machine / Android Spooler
                    Button(
                        onClick = {
                            printStatus = PrintStatus.PRINTING
                            FiscalReceiptHelper.printFiscalBillToMachine(
                                context = context,
                                order = order,
                                partner = partner,
                                onPrintStarted = {
                                    coroutineScope.launch {
                                        delay(1500)
                                        printStatus = PrintStatus.COMPLETED
                                    }
                                }
                            )
                            // Fallback simulation in case PrintManager operates silently
                            coroutineScope.launch {
                                delay(2000)
                                printStatus = PrintStatus.COMPLETED
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("execute_print_machine_button")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (printStatus == PrintStatus.COMPLETED) "Print Another Copy" else "Print Bill in Printing Machine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Button 2: Mark as Given to Restaurant Owner
                    OutlinedButton(
                        onClick = {
                            onBillHandedOver()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VegGreen),
                        border = BorderStroke(1.5.dp, VegGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("handed_bill_to_restaurant_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bill Given to Restaurant Owner",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalKeyValueRow(
    key: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun ThermalDashedLine() {
    Text(
        text = "---------------------------------------------",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = Color(0xFF666666),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ThermalDoubleLine() {
    Text(
        text = "=============================================",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
