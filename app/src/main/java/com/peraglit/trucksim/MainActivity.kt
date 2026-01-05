package com.peraglit.trucksim

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.peraglit.trucksim.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        billingManager = BillingManager(this)
        billingManager.startConnection(
            onConnected = {
                binding.purchaseStatusText.text = getString(R.string.purchase_status_ready)
                binding.purchaseButton.isEnabled = true
            },
            onError = {
                binding.purchaseStatusText.text = getString(R.string.purchase_status_unavailable)
                binding.purchaseButton.isEnabled = false
            }
        )

        binding.purchaseButton.isEnabled = false
        binding.purchaseButton.setOnClickListener {
            billingManager.launchFuelPackPurchase()
        }

        billingManager.onPurchaseAcknowledged = {
            Toast.makeText(this, "Yakıt paketi aktif!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
