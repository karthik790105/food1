package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DeliveryAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BiteMart Partner", appName)
  }

  @Test
  fun `delivery partner login with demo credentials succeeds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = DeliveryAuthManager(context)
    val result = authManager.login("9845012345", "1234")
    assertTrue(result.first)
    assertNotNull(authManager.currentPartner.value)
    assertEquals("Vikram Rathore", authManager.currentPartner.value?.name)
  }

  @Test
  fun `delivery partner registration succeeds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = DeliveryAuthManager(context)
    val result = authManager.registerPartner(
      name = "Amit Patel",
      phone = "9820011223",
      email = "amit@delivery.com",
      password = "password123",
      vehicle = "Honda Activa 6G",
      vehicleNumber = "KA-05-AB-1234",
      drivingLicense = "KA0520230009182",
      city = "Bangalore"
    )
    assertTrue(result.first)
    assertNotNull(authManager.currentPartner.value)
    assertEquals("Amit Patel", authManager.currentPartner.value?.name)
  }
}


