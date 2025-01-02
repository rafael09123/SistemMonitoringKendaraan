package com.project.qrcode.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generate_qr_table")
data class GenerateQrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val qrCode: String,
    val vehicleName: String,
    val vehicleColour: String,
    val vehicleOwner: String,
    val vehicleNumber: String,
    val timestamp: Long,
    val qrImage: ByteArray,
    val uploadedImage: ByteArray?,
    var entry: Long? = null,
    var out: Long? = null,
    val isGenerated: Boolean = true

)