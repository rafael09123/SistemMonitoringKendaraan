package com.project.qrcode.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.qrcode.R
import com.project.qrcode.R.layout.spinner_item
import com.project.qrcode.data.room.AppDatabase
import com.project.qrcode.databinding.ActivityDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val qrCodeDatabase by lazy { AppDatabase.getDatabase(this).generateQrDao() }

    private var qrCodeId: Long = 0L
    private val ownerList = listOf("", "Pt.Tel", "Kontraktor", "Sampah", "Tamu")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        qrCodeId = intent.getLongExtra("qrCodeId", 0L)

        val ownerAdapter = ArrayAdapter(
            this,
            spinner_item,
            ownerList
        )
        ownerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.vehicleOwnerSpinner.adapter = ownerAdapter

        fetchQrCodeDetails()

        binding.btnEdit.setOnClickListener {
            val vehicleName = binding.edtVehicleName.text.toString()
            val vehicleColour = binding.edtVehicleColour.text.toString()
            val vehicleOwner = binding.vehicleOwnerSpinner.selectedItem.toString()
            val vehicleNumber = binding.edtVehicleNumber.text.toString()

            if (vehicleName.isNotEmpty() && vehicleColour.isNotEmpty() && vehicleNumber.isNotEmpty() && vehicleOwner != "") {
                updateQrCode(vehicleName, vehicleColour, vehicleOwner, vehicleNumber)
            } else {
                Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.imgQrCode.setOnClickListener {
            lifecycleScope.launch {
                val qrCode = qrCodeDatabase.getQrCodeById(qrCodeId)
                if (qrCode != null) {
                    val bitmap =
                        BitmapFactory.decodeByteArray(qrCode.qrImage, 0, qrCode.qrImage.size)
                    saveQrCodeToGallery(bitmap)
                } else {
                    Toast.makeText(this@DetailActivity, "QR Code not found!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun saveQrCodeToGallery(bitmap: Bitmap) {
        val filename = "QR_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRCode")
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(uri)
                outputStream?.let { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                Toast.makeText(this, "QR Code saved to gallery!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save QR Code", Toast.LENGTH_SHORT).show()
            } finally {
                outputStream?.close()
            }
        } else {
            Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchQrCodeDetails() {
        lifecycleScope.launch {
            val qrCode = qrCodeDatabase.getQrCodeById(qrCodeId)
            if (qrCode != null) {
                binding.edtVehicleName.setText(qrCode.vehicleName)
                binding.edtVehicleColour.setText(qrCode.vehicleColour)
                binding.edtVehicleNumber.setText(qrCode.vehicleNumber)

                val bitmapQr = BitmapFactory.decodeByteArray(qrCode.qrImage, 0, qrCode.qrImage.size)
                binding.imgQrCode.setImageBitmap(bitmapQr)
                val bitmapUpload = qrCode.uploadedImage?.let {
                    BitmapFactory.decodeByteArray(
                        qrCode.uploadedImage,
                        0,
                        it.size
                    )
                }
                binding.imgUpload.setImageBitmap(bitmapUpload)
                val ownerPosition = ownerList.indexOf(qrCode.vehicleOwner)
                if (ownerPosition != -1) {
                    binding.vehicleOwnerSpinner.setSelection(ownerPosition)
                }
            } else {
                Toast.makeText(this@DetailActivity, "QR Code not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateQrCode(
        vehicleName: String,
        vehicleColour: String,
        vehicleOwner: String,
        vehicleNumber: String
    ) {
        lifecycleScope.launch {
            val qrCode = qrCodeDatabase.getQrCodeById(qrCodeId)

            if (qrCode != null) {
                val updatedQrCode = qrCode.copy(
                    vehicleName = vehicleName,
                    vehicleColour = vehicleColour,
                    vehicleOwner = vehicleOwner,
                    vehicleNumber = vehicleNumber
                )
                qrCodeDatabase.updateQrCode(updatedQrCode)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DetailActivity,
                        "Data updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }
}
