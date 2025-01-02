package com.project.qrcode.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.qrcode.data.entity.GenerateQrEntity
import com.project.qrcode.databinding.ItemQrCodeBinding

class QrCodeAdapter(
    private val qrCodeList: MutableList<GenerateQrEntity>,
    private val onItemClick: (GenerateQrEntity) -> Unit,
    private val onDeleteClick: (GenerateQrEntity) -> Unit
) : RecyclerView.Adapter<QrCodeAdapter.QrCodeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QrCodeViewHolder {
        val binding = ItemQrCodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QrCodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QrCodeViewHolder, position: Int) {
        val qrCode = qrCodeList[position]
        holder.bind(qrCode)
    }

    override fun getItemCount(): Int {
        return qrCodeList.size
    }

    inner class QrCodeViewHolder(private val binding: ItemQrCodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(qrCode: GenerateQrEntity) {
            binding.tvVehicleName.text = qrCode.vehicleName
            binding.tvVehicleNumber.text = qrCode.vehicleNumber
            binding.tvColor.text = qrCode.vehicleColour
            binding.tvOwner.text = qrCode.vehicleOwner

            val bitmap = BitmapFactory.decodeByteArray(qrCode.qrImage, 0, qrCode.qrImage.size)
            binding.imgQrCode.setImageBitmap(bitmap)

            itemView.setOnClickListener {
                onItemClick(qrCode)
            }

            binding.imgTrash.setOnClickListener {
                onDeleteClick(qrCode)
            }
        }
    }
}
