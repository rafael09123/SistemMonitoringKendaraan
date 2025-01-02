package com.project.qrcode.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.project.qrcode.data.entity.GenerateQrEntity

@Dao
interface GenerateQrDao {

    @Query("SELECT * FROM generate_qr_table WHERE id = :id LIMIT 1")
    suspend fun getQrCodeById(id: Long): GenerateQrEntity?

    @Query("SELECT * FROM generate_qr_table WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getQrCodeByQrCode(qrCode: String): GenerateQrEntity?

    @Query("SELECT * FROM generate_qr_table WHERE qrCode = :qrCode")
    suspend fun getAllEntries(qrCode: String): List<GenerateQrEntity>

    @Query("SELECT * FROM generate_qr_table WHERE qrCode = :qrCode AND out IS NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEntry(qrCode: String): GenerateQrEntity?

    @Delete
    fun deleteQrCode(generateQrEntity: GenerateQrEntity)

    @Insert
    suspend fun insertQrCode(generateQrEntity: GenerateQrEntity)

    @Update
    suspend fun updateQrCode(generateQrEntity: GenerateQrEntity)

    @Query("SELECT * FROM generate_qr_table")
    fun getAllQrCodes(): LiveData<List<GenerateQrEntity>>
}
