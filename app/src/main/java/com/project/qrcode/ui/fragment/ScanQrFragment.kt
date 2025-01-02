package com.project.qrcode.ui.fragment

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.project.qrcode.data.room.AppDatabase
import com.project.qrcode.databinding.FragmentScanQrBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.InputStream
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.Result
import com.project.qrcode.data.entity.GenerateQrEntity
import com.project.qrcode.utils.CaptureAct

class ScanQrFragment : Fragment() {

    private var _binding: FragmentScanQrBinding? = null
    private val binding get() = _binding!!
    private val qrCodeDatabase by lazy { AppDatabase.getDatabase(requireContext()).generateQrDao() }

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanQrBinding.inflate(inflater, container, false)

        binding.btnScan.setOnClickListener {
            initiateQrCodeScan()
        }

        binding.btnSelectFromGallery.setOnClickListener {
            selectImageFromGallery()
        }

        return binding.root
    }

    private fun initiateQrCodeScan() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setPrompt("Scan QR Code")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(false)
        integrator.captureActivity = CaptureAct::class.java
        integrator.initiateScan()
    }


    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage: Uri = data.data ?: return
            try {
                val inputStream: InputStream =
                    requireActivity().contentResolver.openInputStream(selectedImage)!!
                val bitmap = BitmapFactory.decodeStream(inputStream)

                decodeQRCodeFromBitmap(bitmap)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gambar tidak ditemukan", Toast.LENGTH_SHORT)
                    .show()
            }
        } else if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result: IntentResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result.contents != null) {
                processQrResult(result.contents)
            } else {
                Toast.makeText(requireContext(), "Scan dibatalkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decodeQRCodeFromBitmap(bitmap: android.graphics.Bitmap) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source: LuminanceSource = com.google.zxing.RGBLuminanceSource(width, height, pixels)

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader()
            val result: Result = reader.decode(binaryBitmap)

            result.let {
                processQrResult(it.text)
            } ?: run {
                Toast.makeText(requireContext(), "QR Code tidak ditemukan", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error decoding QR Code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processQrResult(qrContent: String) {
        if (qrContent.contains("VHC")) {
            showEntryOutDialog(qrContent)
        } else {
            Toast.makeText(requireContext(), "QR Code tidak sesuai", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEntryOutDialog(qrContent: String) {
        val options = arrayOf("Entry", "Exit")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Pilih Status")
            .setItems(options) { _: DialogInterface, which: Int ->
                if (which == 0) {
                    updateQrCodeStatus(qrContent, "entry")
                } else {
                    updateQrCodeStatus(qrContent, "out")
                }
            }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }


    private fun updateQrCodeStatus(qrContent: String, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingQrCode = qrCodeDatabase.getQrCodeByQrCode(qrContent) // Cari QR Code di database

            if (existingQrCode == null) {
                // Jika QR Code belum pernah dibuat sebelumnya
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Gagal: QR Code tidak ditemukan!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            // Cari semua entri dengan QR Code ini
            val lastEntry = qrCodeDatabase.getLastEntry(qrContent)

            when (status) {
                "entry" -> {
                    if (lastEntry != null && lastEntry.entry == null) {
                        // Jika ada entri terakhir yang belum memiliki entry, perbarui kolom entry
                        lastEntry.entry = System.currentTimeMillis()
                        qrCodeDatabase.updateQrCode(lastEntry)
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Entry berhasil diperbarui!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (lastEntry == null || lastEntry.out != null) {
                        // Jika tidak ada entri aktif atau entri terakhir sudah selesai, buat baris baru
                        val newEntry = GenerateQrEntity(
                            qrCode = qrContent,
                            vehicleName = existingQrCode.vehicleName,
                            vehicleOwner = existingQrCode.vehicleOwner,
                            vehicleNumber = existingQrCode.vehicleNumber,
                            vehicleColour = existingQrCode.vehicleColour,
                            qrImage = existingQrCode.qrImage,
                            uploadedImage = existingQrCode.uploadedImage,
                            timestamp = System.currentTimeMillis(), // Timestamp baru untuk baris baru
                            entry = System.currentTimeMillis(),
                            out = null,
                            isGenerated = false
                        )
                        qrCodeDatabase.insertQrCode(newEntry)
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Entry berhasil ditambahkan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Jika entry sudah aktif, tolak entry baru
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Gagal: Entry sudah diperbarui!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                "out" -> {
                    if (lastEntry != null && lastEntry.entry != null && lastEntry.out == null) {
                        // Jika ada entri aktif dengan entry tetapi belum ada out, perbarui kolom out
                        lastEntry.out = System.currentTimeMillis()
                        qrCodeDatabase.updateQrCode(lastEntry)
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Exit berhasil diperbarui",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (lastEntry == null || lastEntry.entry == null) {
                        // Jika tidak ada entry, tolak exit
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Gagal: Harus Entry terlebih dahulu",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Jika out sudah terisi
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Gagal: Exit sudah diperbarui sebelumnya!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                else -> {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Status tidak valid!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}