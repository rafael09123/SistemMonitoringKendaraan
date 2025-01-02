package com.project.qrcode.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.qrcode.data.entity.GenerateQrEntity
import com.project.qrcode.data.room.AppDatabase
import com.project.qrcode.databinding.FragmentQrcodeBinding
import com.project.qrcode.ui.DetailActivity
import com.project.qrcode.ui.FormGenerateActivity
import com.project.qrcode.ui.adapter.QrCodeAdapter

class QrcodeFragment : Fragment() {

    private lateinit var binding: FragmentQrcodeBinding
    private val qrCodeDatabase by lazy { AppDatabase.getDatabase(requireContext()).generateQrDao() }

    private lateinit var qrCodeAdapter: QrCodeAdapter
    private val qrCodeList = mutableListOf<GenerateQrEntity>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentQrcodeBinding.inflate(inflater, container, false)

        qrCodeAdapter = QrCodeAdapter(qrCodeList, { qrCode ->
            navigateToDetailActivity(qrCode)
        }, { qrCode ->
            showDeleteConfirmationDialog(qrCode)
        })
        binding.rvQrCodes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQrCodes.adapter = qrCodeAdapter

        binding.fabGenerateQr.setOnClickListener {
            val intent = Intent(requireContext(), FormGenerateActivity::class.java)
            startActivity(intent)
        }

        qrCodeDatabase.getAllQrCodes().observe(viewLifecycleOwner, Observer { qrCodes ->
            val filteredList = qrCodes.filter { it.isGenerated }
            qrCodeList.clear()
            qrCodeList.addAll(filteredList)
            qrCodeAdapter.notifyDataSetChanged()
        })

        return binding.root
    }

    private fun navigateToDetailActivity(qrCode: GenerateQrEntity) {
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra("qrCodeId", qrCode.id)
        }
        startActivity(intent)
    }

    private fun deleteQrCode(qrCode: GenerateQrEntity) {
        Thread {
            qrCodeDatabase.deleteQrCode(qrCode)
            requireActivity().runOnUiThread {
                qrCodeList.remove(qrCode)
                qrCodeAdapter.notifyDataSetChanged()
            }
        }.start()
    }

    private fun showDeleteConfirmationDialog(qrCode: GenerateQrEntity) {
        if (qrCode.entry != null || qrCode.out != null) {
            requireActivity().runOnUiThread {
                AlertDialog.Builder(requireContext())
                    .setTitle("Penghapusan Gagal")
                    .setMessage("Tidak dapat menghapus, QR Code sudah memiliki aktivitas!")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Hapus QR Code")
            .setMessage("Apakah Anda yakin ingin menghapus QR Code ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteQrCode(qrCode)
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
}
