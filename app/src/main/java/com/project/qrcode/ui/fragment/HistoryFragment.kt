package com.project.qrcode.ui.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.project.qrcode.R
import com.project.qrcode.data.room.AppDatabase
import com.project.qrcode.databinding.FragmentHistoryBinding
import com.project.qrcode.databinding.ItemHistoryBinding
import com.project.qrcode.utils.formatTimestamp
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private val qrCodeDatabase by lazy { AppDatabase.getDatabase(requireContext()).generateQrDao() }
    private var selectedDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        observeData()
        setupDatePicker()
        return binding.root
    }

    private fun observeData(filter: ((timestamp: Long) -> Boolean)? = null) {
        qrCodeDatabase.getAllQrCodes().observe(viewLifecycleOwner, Observer { historyList ->
            clearTable()

            val filteredList = filter?.let {
                historyList.filter { history -> filter(history.timestamp) }
            } ?: historyList

            for (history in filteredList) {
                addRowToTable(
                    history.qrCode,
                    history.vehicleName,
                    history.vehicleOwner,
                    history.vehicleNumber,
                    formatTimestamp(history.timestamp),
                    history.entry?.let { formatTimestamp(it) } ?: "No Entry",
                    history.out?.let { formatTimestamp(it) } ?: "No Exit"
                )
            }
        })
    }

    private fun setupDatePicker() {
        binding.btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.tvSelectedDate.text =
                        "Selected Date: ${sdf.format(selectedDate!!.time)}"

                    val startOfDay = selectedDate!!.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val endOfDay = selectedDate!!.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    observeData { timestamp ->
                        timestamp in startOfDay..endOfDay
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun clearTable() {
        val childCount = binding.tableLayout.childCount
        if (childCount > 1) {
            binding.tableLayout.removeViews(1, childCount - 1)
        }
    }

    private fun addRowToTable(
        qrCode: String,
        vehicleName: String,
        vehicleOwner: String,
        vehicleNumber: String,
        timestamp: String,
        entry: String,
        out: String
    ) {
        val rowBinding = ItemHistoryBinding.inflate(layoutInflater)

        val textColor = ContextCompat.getColor(
            requireContext(),
            if (isDarkTheme()) R.color.text_color_light else R.color.text_color_dark
        )

        rowBinding.tvQrCode.setTextColor(textColor)
        rowBinding.tvVehicleName.setTextColor(textColor)
        rowBinding.tvVehicleOwner.setTextColor(textColor)
        rowBinding.tvVehicleNumber.setTextColor(textColor)
        rowBinding.tvTimestamp.setTextColor(textColor)
        rowBinding.tvEntry.setTextColor(textColor)
        rowBinding.tvExit.setTextColor(textColor)
        rowBinding.tvQrCode.text = qrCode
        rowBinding.tvVehicleName.text = vehicleName
        rowBinding.tvVehicleOwner.text = vehicleOwner
        rowBinding.tvVehicleNumber.text = vehicleNumber
        rowBinding.tvTimestamp.text = timestamp
        rowBinding.tvEntry.text = entry
        rowBinding.tvExit.text = out
        binding.tableLayout.addView(rowBinding.root)
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}