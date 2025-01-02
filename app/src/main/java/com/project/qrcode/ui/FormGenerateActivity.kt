package com.project.qrcode.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.project.qrcode.MainActivity
import com.project.qrcode.R.layout.spinner_item
import com.project.qrcode.data.entity.GenerateQrEntity
import com.project.qrcode.data.room.AppDatabase
import com.project.qrcode.databinding.ActivityFormGenerateBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class FormGenerateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormGenerateBinding
    private val qrCodeDatabase by lazy { AppDatabase.getDatabase(this).generateQrDao() }
    private var selectedImageByteArray: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormGenerateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupVehicleOwnerSpinner()

        binding.btnSelectImage.setOnClickListener {
            selectImageFromCameraOrGallery()
        }

        binding.btnGenerateQr.setOnClickListener {
            if (validateInputs()) {
                generateQrCode()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }


    private fun validateInputs(): Boolean {
        val vehicleName = binding.edtVehicleName.text.toString()
        val vehicleColour = binding.edtVehicleColour.text.toString()
        val vehicleOwner = binding.vehicleOwnerSpinner.selectedItem.toString()
        val vehicleNumber = binding.edtVehicleNumber.text.toString()

        return vehicleName.isNotEmpty() && vehicleColour.isNotEmpty() && vehicleOwner.isNotEmpty() && vehicleOwner != "" && vehicleNumber.isNotEmpty()
    }

    private fun setupVehicleOwnerSpinner() {
        val vehicleOwners = listOf("", "Pt.Tel", "Kontraktor", "Sampah", "Tamu")
        val adapter = ArrayAdapter(
            this,
            spinner_item,
            vehicleOwners
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.vehicleOwnerSpinner.adapter = adapter
    }

    private fun selectImageFromCameraOrGallery() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image From")
            .setItems(options) { _, which ->
                if (which == 0) {
                    takePicture.launch(null)
                } else {
                    pickImageFromGallery.launch("image/*")
                }
            }
        builder.create().show()
    }

    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.imgVehicle.setImageBitmap(bitmap)
                selectedImageByteArray = bitmapToByteArray(bitmap)
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                binding.imgVehicle.setImageBitmap(bitmap)
                selectedImageByteArray = bitmapToByteArray(bitmap)
            }
        }

    private fun generateQrCode() {
        val vehicleName = binding.edtVehicleName.text.toString()
        val vehicleColour = binding.edtVehicleColour.text.toString()
        val vehicleOwner = binding.vehicleOwnerSpinner.selectedItem.toString()
        val vehicleNumber = binding.edtVehicleNumber.text.toString()

        if (selectedImageByteArray == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val qrCodeData = "VHC" + UUID.randomUUID().toString().take(4).toUpperCase(Locale.ROOT)
        val qrBitmap = generateQRCodeBitmap(qrCodeData)
        val compressedQrBitmap = compressBitmap(qrBitmap, 50)
        val qrImageByteArray = bitmapToByteArray(compressedQrBitmap)
        val originalBitmap =
            BitmapFactory.decodeByteArray(selectedImageByteArray, 0, selectedImageByteArray!!.size)
        val compressedBitmap = compressBitmap(originalBitmap, 50)
        val compressedImageByteArray = bitmapToByteArray(compressedBitmap)
        val timestamp = System.currentTimeMillis()

        val qrEntity = GenerateQrEntity(
            qrCode = qrCodeData,
            vehicleName = vehicleName,
            vehicleColour = vehicleColour,
            vehicleOwner = vehicleOwner,
            vehicleNumber = vehicleNumber,
            timestamp = timestamp,
            qrImage = qrImageByteArray,
            uploadedImage = compressedImageByteArray,
            entry = null,
            out = null,
            isGenerated = true

        )

        saveQrToDatabase(qrEntity)
    }

    private fun generateQRCodeBitmap(data: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(
                            x,
                            y
                        )
                    ) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        return bitmap
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun saveQrToDatabase(qrEntity: GenerateQrEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            qrCodeDatabase.insertQrCode(qrEntity)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@FormGenerateActivity,
                    "QR Code generated and saved!",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this@FormGenerateActivity, MainActivity::class.java)
                intent.putExtra("navigateTo", "QrcodeFragment")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }
    }
}
